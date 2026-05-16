package com.fpt.swp.repository;

import com.fpt.swp.model.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * FR-01.3 – Repository thao tác bảng blacklist token.
 */
@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {

    /** Kiểm tra token đã bị invalidate chưa (tra cứu theo jti). */
    boolean existsByJti(String jti);

    /** Xóa các bản ghi đã hết hạn để tránh bảng phình to (dùng cho scheduled cleanup). */
    @Modifying
    @Transactional
    @Query("DELETE FROM InvalidatedToken t WHERE t.expiresAt < :now")
    void deleteAllExpiredBefore(Instant now);
}
