package com.fpt.swp.service;

import com.fpt.swp.model.PaymentStatus;
import com.fpt.swp.model.PaymentTransaction;
import com.fpt.swp.model.SubscriptionStatus;
import com.fpt.swp.model.UserSubscription;
import com.fpt.swp.repository.PaymentTransactionRepository;
import com.fpt.swp.repository.UserSubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xử lý thanh toán + kích hoạt subscription.
 *
 * <p>Hỗ trợ 2 luồng:
 * <ul>
 *   <li><b>Mock</b> ({@link #confirmMockPayment}) — dùng để test nhanh, không qua cổng.</li>
 *   <li><b>VNPay</b> ({@link #createVnpayPaymentUrl}, {@link #handleVnpayReturn},
 *       {@link #handleVnpayIpn}) — cổng thật, verify chữ ký HMAC-SHA512.</li>
 * </ul>
 * Việc kích hoạt gói ({@link #activate}) dùng chung cho cả hai và idempotent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final VNPayService vnPayService;

    @Value("${app.vnpay.fe-return-url:http://localhost:3000/researcher/subscription}")
    private String feReturnUrl;

    // ─── Kích hoạt gói (dùng chung) ─────────────────────────────────────────────

    /** PENDING → ACTIVE, đặt thời hạn theo plan. */
    private void activate(UserSubscription sub) {
        LocalDateTime now = LocalDateTime.now();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(now);
        sub.setEndDate(now.plusDays(sub.getPlan().getDurationDays()));
        subscriptionRepository.save(sub);
    }

    // ─── Mock ───────────────────────────────────────────────────────────────────

    /**
     * Xác nhận một giao dịch PENDING → SUCCESS và kích hoạt gói (bản mock, không tiền thật).
     */
    @Transactional
    public PaymentTransaction confirmMockPayment(Long userId, String transactionId) {
        PaymentTransaction txn = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        if (txn.getUser() == null || !txn.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("This transaction does not belong to you.");
        }
        if (txn.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Transaction already processed. Status: " + txn.getStatus());
        }

        txn.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(txn);
        activate(txn.getSubscription());

        log.info("Mock payment confirmed: txn={}, user={}, plan={}, active until {}",
                transactionId, userId, txn.getSubscription().getPlan().getCode(), txn.getSubscription().getEndDate());
        return txn;
    }

    // ─── VNPay ──────────────────────────────────────────────────────────────────

    /**
     * Tạo URL thanh toán VNPay cho một giao dịch PENDING của user.
     *
     * @return URL để FE redirect trình duyệt sang VNPay
     */
    @Transactional(readOnly = true)
    public String createVnpayPaymentUrl(Long userId, String transactionId, String clientIp) {
        PaymentTransaction txn = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
        if (txn.getUser() == null || !txn.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("This transaction does not belong to you.");
        }
        if (txn.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Transaction already processed. Status: " + txn.getStatus());
        }
        String planCode = txn.getSubscription() != null && txn.getSubscription().getPlan() != null
                ? txn.getSubscription().getPlan().getCode() : "PRO";
        String orderInfo = "Thanh toan goi " + planCode;
        return vnPayService.createPaymentUrl(txn.getTransactionId(), txn.getAmount(), orderInfo, clientIp);
    }

    /**
     * Xử lý khi VNPay redirect trình duyệt về (return URL). Verify chữ ký, cập nhật
     * trạng thái giao dịch, và trả về URL FE để redirect kèm ?status=...
     *
     * <p>Đây chỉ phục vụ trải nghiệm người dùng; xác nhận đáng tin cậy nằm ở IPN.
     */
    @Transactional
    public String handleVnpayReturn(Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            log.warn("VNPay return: invalid signature. params={}", params);
            return feReturnUrl + "?status=invalid";
        }
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            try {
                confirmVnpayPayment(txnRef, params.get("vnp_Amount"));
                return feReturnUrl + "?status=success&txnRef=" + txnRef;
            } catch (Exception e) {
                log.error("VNPay return: confirm failed for txn={}: {}", txnRef, e.getMessage());
                return feReturnUrl + "?status=error";
            }
        }
        markVnpayFailed(txnRef);
        return feReturnUrl + "?status=failed&code=" + responseCode;
    }

    /**
     * Xử lý IPN (server-to-server) từ VNPay — nguồn xác nhận đáng tin cậy. Trả về
     * body theo chuẩn VNPay: {RspCode, Message}.
     */
    @Transactional
    public Map<String, String> handleVnpayIpn(Map<String, String> params) {
        if (!vnPayService.verifySignature(params)) {
            return ipnResponse("97", "Invalid Checksum");
        }
        String txnRef = params.get("vnp_TxnRef");
        PaymentTransaction txn = paymentRepository.findByTransactionId(txnRef).orElse(null);
        if (txn == null) {
            return ipnResponse("01", "Order not Found");
        }
        // Kiểm tra số tiền khớp (vnp_Amount = tiền × 100)
        try {
            long expected = txn.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
            long got = Long.parseLong(params.getOrDefault("vnp_Amount", "-1"));
            if (expected != got) {
                return ipnResponse("04", "Invalid amount");
            }
        } catch (NumberFormatException e) {
            return ipnResponse("04", "Invalid amount");
        }
        if (txn.getStatus() != PaymentStatus.PENDING) {
            return ipnResponse("02", "Order already confirmed");
        }

        String responseCode = params.get("vnp_ResponseCode");
        String txnStatus = params.get("vnp_TransactionStatus");
        if ("00".equals(responseCode) && "00".equals(txnStatus)) {
            confirmVnpayPayment(txnRef, params.get("vnp_Amount"));
        } else {
            markVnpayFailed(txnRef);
        }
        return ipnResponse("00", "Confirm Success");
    }

    /** Xác nhận thanh toán VNPay thành công (idempotent — return & IPN có thể cùng gọi). */
    @Transactional
    public PaymentTransaction confirmVnpayPayment(String txnRef, String vnpAmount) {
        PaymentTransaction txn = paymentRepository.findByTransactionId(txnRef)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + txnRef));
        if (txn.getStatus() == PaymentStatus.SUCCESS) {
            return txn; // đã xử lý rồi
        }
        if (txn.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Transaction already processed. Status: " + txn.getStatus());
        }
        txn.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(txn);
        activate(txn.getSubscription());
        log.info("VNPay payment confirmed: txn={}, plan={}, active until {}",
                txnRef, txn.getSubscription().getPlan().getCode(), txn.getSubscription().getEndDate());
        return txn;
    }

    private void markVnpayFailed(String txnRef) {
        paymentRepository.findByTransactionId(txnRef).ifPresent(txn -> {
            if (txn.getStatus() == PaymentStatus.PENDING) {
                txn.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(txn);
                log.info("VNPay payment marked FAILED: txn={}", txnRef);
            }
        });
    }

    private Map<String, String> ipnResponse(String code, String message) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("RspCode", code);
        r.put("Message", message);
        return r;
    }
}
