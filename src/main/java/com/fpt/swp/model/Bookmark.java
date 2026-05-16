package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bookmarks", indexes = {
        @Index(name = "idx_bookmarks_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_paper", columnNames = {"user_id", "paper_id"}),
        @UniqueConstraint(name = "uk_user_keyword", columnNames = {"user_id", "keyword_id"})
})
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id")
    private ResearchPaper paper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id")
    private Keyword keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "bookmark_type", nullable = false)
    private BookmarkType bookmarkType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
