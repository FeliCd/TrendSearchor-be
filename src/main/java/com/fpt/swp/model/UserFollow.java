package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_follows", indexes = {
        @Index(name = "idx_follows_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_journal", columnNames = {"user_id", "journal_id"}),
        @UniqueConstraint(name = "uk_user_topic", columnNames = {"user_id", "topic_id"})
})
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private Journal journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private ResearchTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_type", nullable = false)
    private FollowType followType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
