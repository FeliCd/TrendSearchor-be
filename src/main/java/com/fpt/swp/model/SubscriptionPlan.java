package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một gói dịch vụ (tier). FREE là gói mặc định (không cần mua), PRO là gói trả phí.
 * {@code dailyPromptLimit} là hạn mức số lượt gọi AI trong cửa sổ trượt 24h.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã định danh gói: FREE / PRO. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Số ngày hiệu lực khi mua (0 cho FREE). */
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    /** Hạn mức số lượt AI trong 24h. */
    @Column(name = "daily_prompt_limit", nullable = false)
    private Integer dailyPromptLimit;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
