package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_mail", columnList = "mail"),
        @Index(name = "idx_users_role_status", columnList = "role, status")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    private LocalDate dob;

    @Column(nullable = false, unique = true, length = 100)
    private String mail;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 200)
    private String workplace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // created_at / updated_at — nullable=true để Hibernate thêm cột vào bảng đã có dữ liệu
    // Backfill giá trị ngay bên dưới
    @Column(nullable = true, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    private LocalDateTime lastLogin;

    // ─── Lifecycle callbacks ───────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
