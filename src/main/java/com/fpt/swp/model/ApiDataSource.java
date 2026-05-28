package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_data_sources", indexes = {
        @Index(name = "idx_sources_active", columnList = "is_active")
})
public class ApiDataSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name", nullable = false, unique = true, length = 100)
    private String sourceName;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "rate_limit_per_day")
    @Builder.Default
    private Integer rateLimitPerDay = 1000;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status")
    private SyncStatus lastSyncStatus;

    @Column(name = "records_synced")
    @Builder.Default
    private Integer recordsSynced = 0;

    @Column(name = "sync_schedule")
    @Builder.Default
    private String syncSchedule = "0 0 2 * * ?";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
