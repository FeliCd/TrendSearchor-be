package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Một lượt gọi tính năng AI thành công của user. Dùng để tính hạn mức theo
 * cửa sổ trượt 24h: đếm số bản ghi của user có {@code createdAt} trong 24h gần nhất.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_usage_log", indexes = {
        @Index(name = "idx_ai_usage_user_time", columnList = "user_id, created_at")
})
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tên tính năng AI đã dùng (SEARCH, TREND_QA, SUMMARIZE, RERANK, ABSTRACT, RECOMMENDATIONS). */
    @Column(nullable = false, length = 50)
    private String feature;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
