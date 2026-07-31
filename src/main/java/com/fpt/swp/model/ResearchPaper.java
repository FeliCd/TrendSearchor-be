package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "research_papers", indexes = {
        @Index(name = "idx_papers_year", columnList = "year"),
        @Index(name = "idx_papers_external_id", columnList = "external_id")
})
public class ResearchPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    private Integer year;

    @Column(name = "citation_count")
    @Builder.Default
    private Integer citationCount = 0;

    @Column(name = "open_access")
    @Builder.Default
    private Boolean openAccess = false;

    @Column(name = "paper_uri", length = 500)
    private String paperUri;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "paper_journals",
            joinColumns = @JoinColumn(name = "paper_id"),
            inverseJoinColumns = @JoinColumn(name = "journal_id")
    )
    @Builder.Default
    private Set<Journal> journals = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "paper_authors",
            joinColumns = @JoinColumn(name = "paper_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Builder.Default
    private Set<Author> authors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "paper_keywords",
            joinColumns = @JoinColumn(name = "paper_id"),
            inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    @Builder.Default
    private Set<Keyword> keywords = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "paper_topics",
            joinColumns = @JoinColumn(name = "paper_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    @Builder.Default
    private Set<ResearchTopic> topics = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "paper_citations",
            joinColumns = @JoinColumn(name = "citing_paper_id"),
            inverseJoinColumns = @JoinColumn(name = "cited_paper_id")
    )
    @Builder.Default
    private Set<ResearchPaper> citedPapers = new HashSet<>();

    // ─── Upload / Moderation fields ────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private PaperSource source = PaperSource.OPENALEX;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    // ─── Timestamps ────────────────────────────────────────────────────────────

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaperStatus status = PaperStatus.APPROVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "is_self_published", nullable = false)
    @Builder.Default
    private Boolean isSelfPublished = false;

    @Column(name = "status_comments", columnDefinition = "TEXT")
    private String statusComments;

    // ─── Legal / Copyright fields ──────────────────────────────────────────────

    /** Giấy phép do uploader chọn. Null cho các bài nạp từ OpenAlex/nguồn ngoài. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private License license;

    /** Bài là nghiên cứu gốc của uploader hay đã từng công bố ở nơi khác. */
    @Enumerated(EnumType.STRING)
    @Column(name = "publication_type", length = 30)
    private PublicationType publicationType;

    /** Uploader xác nhận là tác giả hoặc có quyền hợp pháp để đăng bài này. */
    @Column(name = "ownership_confirmed", nullable = false)
    @Builder.Default
    private Boolean ownershipConfirmed = false;

    /** Uploader đã đồng ý Upload Agreement / Terms of Service tại thời điểm upload. */
    @Column(name = "terms_accepted", nullable = false)
    @Builder.Default
    private Boolean termsAccepted = false;

    /** Phiên bản Terms of Service mà uploader đã đồng ý (phục vụ audit khi terms thay đổi). */
    @Column(name = "terms_version", length = 20)
    private String termsVersion;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    /** IP của uploader tại thời điểm upload — bằng chứng audit khi có tranh chấp. */
    @Column(name = "uploaded_by_ip", length = 45)
    private String uploadedByIp;

    /** Nếu khác null, bài chỉ hiển thị công khai sau ngày này dù đã được duyệt. */
    @Column(name = "embargo_until")
    private LocalDate embargoUntil;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Backwards-compatible view of the moderation status for API consumers that
     * still read {@code uploadStatus}. Derived from the single source of truth,
     * {@link #status}.
     *
     * <p>If a {@link PaperStatus} value has no matching {@link UploadStatus}
     * constant (e.g. a newly added status not mirrored here), this returns
     * {@code null} instead of throwing — so serialization never breaks with a
     * 500. API consumers should read {@code status} for the authoritative value.
     */
    @Transient
    public UploadStatus getUploadStatus() {
        if (status == null) return null;
        try {
            return UploadStatus.valueOf(status.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Bài đang trong thời gian embargo — không được hiển thị công khai dù đã duyệt. */
    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isUnderEmbargo() {
        return embargoUntil != null && embargoUntil.isAfter(LocalDate.now());
    }
}
