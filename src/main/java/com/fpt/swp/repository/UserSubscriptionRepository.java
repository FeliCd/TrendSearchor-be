package com.fpt.swp.repository;

import com.fpt.swp.model.SubscriptionStatus;
import com.fpt.swp.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    /**
     * Các gói đang hiệu lực của user (ACTIVE và chưa hết hạn), mới hết hạn nhất trước.
     * JOIN FETCH plan để tránh lazy khi resolve tier ngoài transaction.
     */
    @Query("SELECT s FROM UserSubscription s JOIN FETCH s.plan " +
           "WHERE s.user.id = :userId AND s.status = com.fpt.swp.model.SubscriptionStatus.ACTIVE " +
           "AND (s.endDate IS NULL OR s.endDate > :now) ORDER BY s.endDate DESC")
    List<UserSubscription> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Dùng cho cron: các gói ACTIVE đã quá hạn cần chuyển sang EXPIRED. */
    @Query("SELECT s FROM UserSubscription s WHERE s.status = :status AND s.endDate IS NOT NULL AND s.endDate <= :now")
    List<UserSubscription> findByStatusAndExpired(@Param("status") SubscriptionStatus status,
                                                  @Param("now") LocalDateTime now);
}
