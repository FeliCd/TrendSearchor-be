package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "publication_trends", indexes = {
        @Index(name = "idx_trends_keyword", columnList = "keyword_name"),
        @Index(name = "idx_trends_year", columnList = "year")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_trend_keyword_year_month", columnNames = {"keyword_name", "year", "month"})
})
public class PublicationTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword_name", nullable = false, length = 255)
    private String keywordName;

    @Column(nullable = false)
    private Integer year;

    private Integer month;

    @Column(name = "paper_count")
    @Builder.Default
    private Integer paperCount = 0;

    @Column(name = "citation_count")
    @Builder.Default
    private Integer citationCount = 0;

    @Column(name = "avg_citations")
    @Builder.Default
    private Double avgCitations = 0.0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
