package com.fpt.swp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * FR-01.3 – Đăng xuất
 * Lưu các access token đã bị invalidate (blacklist).
 * Token sẽ bị xóa khỏi bảng khi đã hết hạn tự nhiên (cleanup job).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invalidated_tokens")
public class InvalidatedToken {

    /** JWT ID (jti claim) – dùng làm khóa chính để tra cứu nhanh */
    @Id
    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    /** Thời điểm token hết hạn – dùng cho scheduled cleanup */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
