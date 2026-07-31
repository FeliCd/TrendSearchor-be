package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Báo cáo vi phạm bản quyền đối với một bài báo (notice-and-takedown).
 * Bất kỳ user đăng nhập nào cũng có thể gửi report; moderator/admin xem xét
 * và quyết định gỡ bài ({@link PaperStatus#TAKEN_DOWN}) hoặc bác bỏ.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "copyright_reports", indexes = {
        @Index(name = "idx_copyright_reports_paper", columnList = "paper_id"),
        @Index(name = "idx_copyright_reports_status", columnList = "status")
})
public class CopyrightReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private ResearchPaper paper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private User reportedBy;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CopyrightReportStatus status = CopyrightReportStatus.PENDING;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
