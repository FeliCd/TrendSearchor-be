package com.fpt.swp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "journals", indexes = {
        @Index(name = "idx_journals_external_id", columnList = "external_id"),
        @Index(name = "idx_journals_name", columnList = "name")
})
public class Journal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(length = 500)
    private String publisher;

    @Column(length = 20)
    private String issn;

    @Column(length = 100)
    private String country;

    @Column(name = "homepage_url", length = 500)
    private String homepageUrl;

    @ManyToMany(mappedBy = "journals", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private Set<ResearchPaper> papers = new HashSet<>();

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
