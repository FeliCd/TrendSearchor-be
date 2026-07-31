package com.fpt.swp.repository;

import com.fpt.swp.model.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    /** Số lượt AI của user trong cửa sổ trượt (createdAt > since). */
    @Query("SELECT COUNT(a) FROM AiUsageLog a WHERE a.user.id = :userId AND a.createdAt > :since")
    long countByUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** Thời điểm lượt cũ nhất còn trong cửa sổ — dùng tính "lượt kế tiếp khả dụng lúc nào". */
    @Query("SELECT MIN(a.createdAt) FROM AiUsageLog a WHERE a.user.id = :userId AND a.createdAt > :since")
    LocalDateTime findOldestInWindow(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
