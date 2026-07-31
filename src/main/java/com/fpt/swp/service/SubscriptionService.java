package com.fpt.swp.service;

import com.fpt.swp.dto.AdminUserSubscriptionDto;
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
    public static final String PRO_CODE = "PRO";
    public static final String UNLIMITED_CODE = "UNLIMITED";

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

    @Transactional
    public SubscriptionPlan ensureUnlimitedPlanExists() {
        return planRepository.findByCode(UNLIMITED_CODE)
                .orElseGet(() -> planRepository.saveAndFlush(SubscriptionPlan.builder()
                        .code(UNLIMITED_CODE)
                        .name("Unlimited")
                        .description("Unlimited AI search queries and token generations 24/7.")
                        .price(new java.math.BigDecimal("499000.00"))
                        .durationDays(30)
                        .dailyPromptLimit(-1)
                        .active(true)
                        .build()));
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
                .orElseGet(() -> {
                    if (UNLIMITED_CODE.equalsIgnoreCase(planCode)) {
                        return ensureUnlimitedPlanExists();
                    }
                    throw new IllegalArgumentException("Plan not found: " + planCode);
                });

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

    // ─── Admin Management ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AdminUserSubscriptionDto> getAllUserSubscriptionsForAdmin() {
        List<User> users = userRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        return users.stream().map(user -> {
            List<UserSubscription> activeSubs = subscriptionRepository.findActiveByUserId(user.getId(), now);
            UserSubscription activeSub = activeSubs.isEmpty() ? null : activeSubs.get(0);

            String planId = activeSub != null ? activeSub.getPlan().getCode() : FREE_CODE;
            String status = activeSub != null ? activeSub.getStatus().name() : "ACTIVE";
            LocalDateTime startDate = activeSub != null ? activeSub.getStartDate() : user.getCreatedAt();
            LocalDateTime endDate = activeSub != null ? activeSub.getEndDate() : null;

            return AdminUserSubscriptionDto.builder()
                    .id(activeSub != null ? String.valueOf(activeSub.getId()) : "sub_user_" + user.getId())
                    .userId(user.getId())
                    .userName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getMail())
                    .userEmail(user.getMail())
                    .role(user.getRole() != null ? user.getRole().name() : "RESEARCHER")
                    .planId(planId)
                    .status(status)
                    .startDate(startDate)
                    .endDate(endDate)
                    .grantedByAdmin(activeSub != null && activeSub.getStartDate() != null && activeSub.getEndDate() != null)
                    .build();
        }).toList();
    }

    @Transactional
    public AdminUserSubscriptionDto grantSubscriptionForAdmin(Long userId, String planCode, Integer durationDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        SubscriptionPlan plan = planRepository.findByCode(planCode)
                .orElseGet(() -> {
                    if (UNLIMITED_CODE.equalsIgnoreCase(planCode)) {
                        return ensureUnlimitedPlanExists();
                    }
                    throw new IllegalArgumentException("Plan not found: " + planCode);
                });


        LocalDateTime now = LocalDateTime.now();
        int days = (durationDays != null && durationDays > 0) ? durationDays : (plan.getDurationDays() > 0 ? plan.getDurationDays() : 30);
        LocalDateTime endDate = days > 0 ? now.plusDays(days) : null;

        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .endDate(endDate)
                .build();

        subscription = subscriptionRepository.save(subscription);

        return AdminUserSubscriptionDto.builder()
                .id(String.valueOf(subscription.getId()))
                .userId(user.getId())
                .userName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getMail())
                .userEmail(user.getMail())
                .role(user.getRole() != null ? user.getRole().name() : "RESEARCHER")
                .planId(plan.getCode())
                .status(SubscriptionStatus.ACTIVE.name())
                .startDate(now)
                .endDate(endDate)
                .grantedByAdmin(true)
                .build();
    }

    @Transactional
    public void revokeSubscriptionForAdmin(Long userId) {
        List<UserSubscription> activeSubs = subscriptionRepository.findActiveByUserId(userId, LocalDateTime.now());
        for (UserSubscription sub : activeSubs) {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            sub.setEndDate(LocalDateTime.now());
            subscriptionRepository.save(sub);
        }
    }
}

