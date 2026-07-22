package com.fpt.swp.service;

import com.fpt.swp.model.*;
import com.fpt.swp.repository.PaymentTransactionRepository;
import com.fpt.swp.repository.SubscriptionPlanRepository;
import com.fpt.swp.repository.UserRepository;
import com.fpt.swp.repository.UserSubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Quản lý gói dịch vụ (tier) và khởi tạo luồng đăng ký.
 *
 * <p>Tier hiệu lực của user = gói PRO có một {@link UserSubscription} ACTIVE còn hạn;
 * nếu không có thì mặc định là gói FREE. Việc kích hoạt (chuyển PENDING → ACTIVE)
 * do luồng thanh toán đảm nhiệm (xem PaymentService — giai đoạn sau).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    public static final String FREE_CODE = "FREE";

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final UserRepository userRepository;

    // ─── Tier resolution ───────────────────────────────────────────────────────

    /** Gói đang áp dụng cho user (PRO nếu còn hạn, ngược lại FREE). */
    @Transactional(readOnly = true)
    public SubscriptionPlan getEffectivePlan(Long userId) {
        return getActiveSubscription(userId)
                .map(UserSubscription::getPlan)
                .orElseGet(this::getFreePlan);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan getFreePlan() {
        return planRepository.findByCode(FREE_CODE)
                .orElseThrow(() -> new IllegalStateException("FREE plan is not configured (seed missing)"));
    }

    @Transactional(readOnly = true)
    public Optional<UserSubscription> getActiveSubscription(Long userId) {
        List<UserSubscription> active = subscriptionRepository.findActiveByUserId(userId, LocalDateTime.now());
        return active.isEmpty() ? Optional.empty() : Optional.of(active.get(0));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> listActivePlans() {
        return planRepository.findByActiveTrueOrderByPriceAsc();
    }

    // ─── Subscribe (tạo bản ghi chờ thanh toán) ────────────────────────────────

    /**
     * Bắt đầu đăng ký một gói trả phí: tạo {@link UserSubscription} PENDING và
     * {@link PaymentTransaction} PENDING. Chưa kích hoạt — chờ xác nhận thanh toán.
     *
     * @return giao dịch PENDING vừa tạo (chứa transactionId để confirm sau)
     */
    @Transactional
    public PaymentTransaction subscribe(Long userId, String planCode, String paymentMethod) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        SubscriptionPlan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planCode));

        if (FREE_CODE.equalsIgnoreCase(plan.getCode()) || plan.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("This plan is free and does not require a subscription.");
        }

        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.PENDING)
                .build();
        subscription = subscriptionRepository.save(subscription);

        PaymentTransaction txn = PaymentTransaction.builder()
                .user(user)
                .subscription(subscription)
                .amount(plan.getPrice())
                .paymentMethod(paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "MOCK")
                .transactionId("TXN-" + UUID.randomUUID())
                .status(PaymentStatus.PENDING)
                .build();
        txn = paymentRepository.save(txn);

        log.info("Subscription initiated: user={}, plan={}, txn={}", userId, planCode, txn.getTransactionId());
        return txn;
    }
}
