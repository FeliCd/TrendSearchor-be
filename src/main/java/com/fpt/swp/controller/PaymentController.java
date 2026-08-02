package com.fpt.swp.controller;

import com.fpt.swp.dto.MockConfirmRequest;
import com.fpt.swp.model.PaymentTransaction;
import com.fpt.swp.model.UserSubscription;
import com.fpt.swp.service.PaymentService;
import com.fpt.swp.util.AuthUtils;
import com.fpt.swp.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thanh toán: bản mock (test nhanh) + VNPay thật (create-url + callback return/IPN).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthUtils authUtils;

    // ─── Mock ─────────────────────────────────────────────────────────────────

    @PostMapping("/mock-confirm")
    public ResponseEntity<?> confirm(@Valid @RequestBody MockConfirmRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        PaymentTransaction txn = paymentService.confirmMockPayment(userId, request.getTransactionId());
        UserSubscription sub = txn.getSubscription();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Payment confirmed. Your subscription is now active.");
        body.put("transactionId", txn.getTransactionId());
        body.put("paymentStatus", txn.getStatus().name());
        body.put("subscriptionStatus", sub.getStatus().name());
        body.put("endDate", sub.getEndDate());
        return ResponseEntity.ok(body);
    }

    // ─── VNPay ────────────────────────────────────────────────────────────────

    /**
     * Tạo URL thanh toán VNPay cho một giao dịch PENDING (đã tạo qua /subscribe).
     * FE gọi endpoint này rồi redirect trình duyệt tới {@code paymentUrl} trả về.
     * Body: { "transactionId": "TXN-..." }
     */
    @PostMapping("/vnpay/create-url")
    public ResponseEntity<?> createVnpayUrl(@Valid @RequestBody MockConfirmRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails,
                                            HttpServletRequest httpRequest) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        String clientIp = RequestUtils.clientIp(httpRequest);
        String url = paymentService.createVnpayPaymentUrl(userId, request.getTransactionId(), clientIp);
        return ResponseEntity.ok(Map.of("paymentUrl", url));
    }

    /**
     * VNPay redirect trình duyệt về đây sau khi người dùng thanh toán (PUBLIC — không JWT).
     * Verify chữ ký, cập nhật giao dịch, rồi 302 redirect về trang FE kèm ?status=...
     */
    @GetMapping("/vnpay/return")
    public void vnpayReturn(@RequestParam Map<String, String> params,
                            HttpServletResponse response) throws IOException {
        String redirectUrl = paymentService.handleVnpayReturn(params);
        response.sendRedirect(redirectUrl);
    }

    /**
     * IPN server-to-server từ VNPay (PUBLIC — không JWT). Nguồn xác nhận đáng tin cậy.
     * Trả về {RspCode, Message} theo chuẩn VNPay.
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVnpayIpn(params));
    }
}
