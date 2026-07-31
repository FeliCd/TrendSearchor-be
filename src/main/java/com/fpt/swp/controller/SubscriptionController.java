package com.fpt.swp.controller;

import com.fpt.swp.dto.MySubscriptionDto;
import com.fpt.swp.dto.PlanDto;
import com.fpt.swp.dto.QuotaStatusDto;
import com.fpt.swp.dto.SubscribeRequest;
import com.fpt.swp.model.PaymentTransaction;
import com.fpt.swp.model.SubscriptionPlan;
import com.fpt.swp.model.UserSubscription;
import com.fpt.swp.service.AiQuotaService;
import com.fpt.swp.service.SubscriptionService;
import com.fpt.swp.util.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AiQuotaService aiQuotaService;
    private final AuthUtils authUtils;

    /** Trang pricing — công khai. */
    @GetMapping("/api/plans")
    public ResponseEntity<List<PlanDto>> listPlans() {
        return ResponseEntity.ok(subscriptionService.listActivePlans().stream()
                .map(PlanDto::fromEntity).toList());
    }

    /** Gói + quota hiện tại của user. */
    @GetMapping("/api/subscriptions/me")
    public ResponseEntity<MySubscriptionDto> mySubscription(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        boolean admin = authUtils.isAdmin(userDetails);
        Optional<UserSubscription> active = subscriptionService.getActiveSubscription(userId);
        SubscriptionPlan plan = subscriptionService.getEffectivePlan(userId);

        QuotaStatusDto quota = admin
                ? QuotaStatusDto.builder().tier("ADMIN").dailyLimit(-1).used(0).remaining(-1).unlimited(true).build()
                : aiQuotaService.getQuotaStatus(userId);

        MySubscriptionDto dto = MySubscriptionDto.builder()
                .tier(admin ? "ADMIN" : plan.getCode())
                .proActive(active.isPresent())
                .status(active.map(s -> s.getStatus().name()).orElse(null))
                .planName(admin ? "Administrator" : plan.getName())
                .startDate(active.map(UserSubscription::getStartDate).orElse(null))
                .endDate(active.map(UserSubscription::getEndDate).orElse(null))
                .quota(quota)
                .build();
        return ResponseEntity.ok(dto);
    }

    /** Bắt đầu đăng ký gói trả phí → tạo giao dịch PENDING (chờ xác nhận thanh toán). */
    @PostMapping("/api/subscriptions/subscribe")
    public ResponseEntity<?> subscribe(@Valid @RequestBody SubscribeRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        PaymentTransaction txn = subscriptionService.subscribe(userId, request.getPlanCode(), request.getPaymentMethod());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Subscription created. Confirm the payment to activate.",
                "subscriptionId", txn.getSubscription().getId(),
                "transactionId", txn.getTransactionId(),
                "amount", txn.getAmount(),
                "status", txn.getStatus().name(),
                "mockConfirmEndpoint", "/api/payments/mock-confirm"
        ));
    }

    // ─── Admin Management Endpoints ────────────────────────────────────────────

    /** Danh sách tất cả người dùng và gói dịch vụ — Dành cho ADMIN. */
    @GetMapping("/api/admin/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.fpt.swp.dto.AdminUserSubscriptionDto>> getAllSubscriptionsForAdmin() {
        return ResponseEntity.ok(subscriptionService.getAllUserSubscriptionsForAdmin());
    }

    /** Admin cấp gói trực tiếp cho một user. */
    @PostMapping("/api/admin/subscriptions/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> grantSubscriptionForAdmin(@Valid @RequestBody com.fpt.swp.dto.AdminGrantSubscriptionRequest request) {
        com.fpt.swp.dto.AdminUserSubscriptionDto dto = subscriptionService.grantSubscriptionForAdmin(
                request.getUserId(), request.getPlanCode(), request.getDurationDays());
        return ResponseEntity.ok(Map.of(
                "message", "Subscription granted successfully",
                "data", dto
        ));
    }

    /** Admin thu hồi gói của một user (trở về FREE). */
    @PostMapping("/api/admin/subscriptions/revoke/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeSubscriptionForAdmin(@PathVariable Long userId) {
        subscriptionService.revokeSubscriptionForAdmin(userId);
        return ResponseEntity.ok(Map.of("message", "Subscription revoked successfully"));
    }
}

