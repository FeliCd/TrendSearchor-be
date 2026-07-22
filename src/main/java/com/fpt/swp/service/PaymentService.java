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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Xử lý xác nhận thanh toán. Bản mock: user tự gọi mock-confirm để mô phỏng
 * webhook từ cổng. Khi thay bằng cổng thật, chỉ cần thay điểm gọi confirm bằng
 * handler webhook có verify chữ ký — phần kích hoạt subscription giữ nguyên.
 *
 * ⚠️ Đây là mock, KHÔNG xử lý tiền/thẻ thật.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    /**
     * Xác nhận một giao dịch PENDING → SUCCESS và kích hoạt gói tương ứng.
     *
     * @param userId        user đang thao tác (phải là chủ giao dịch)
     * @param transactionId mã giao dịch cần xác nhận
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

        // Kích hoạt subscription: PENDING → ACTIVE, đặt thời hạn theo plan
        UserSubscription sub = txn.getSubscription();
        LocalDateTime now = LocalDateTime.now();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(now);
        sub.setEndDate(now.plusDays(sub.getPlan().getDurationDays()));
        subscriptionRepository.save(sub);

        log.info("Mock payment confirmed: txn={}, user={}, plan={}, active until {}",
                transactionId, userId, sub.getPlan().getCode(), sub.getEndDate());
        return txn;
    }
}
