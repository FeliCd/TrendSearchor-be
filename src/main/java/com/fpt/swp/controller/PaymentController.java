package com.fpt.swp.controller;

import com.fpt.swp.dto.MockConfirmRequest;
import com.fpt.swp.model.PaymentTransaction;
import com.fpt.swp.model.UserSubscription;
import com.fpt.swp.service.PaymentService;
import com.fpt.swp.util.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xác nhận thanh toán (bản mock). Ở cổng thật, endpoint này được thay bằng webhook
 * có verify chữ ký từ VNPay/MoMo/PayOS.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthUtils authUtils;

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
}
