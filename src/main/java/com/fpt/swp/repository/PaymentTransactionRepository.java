package com.fpt.swp.repository;

import com.fpt.swp.dto.MonthlyRevenueRow;
import com.fpt.swp.model.PaymentStatus;
import com.fpt.swp.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    // ─── Revenue aggregations (chỉ tính giao dịch SUCCESS) ─────────────────────

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p WHERE p.status = com.fpt.swp.model.PaymentStatus.SUCCESS")
    BigDecimal totalRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p " +
           "WHERE p.status = com.fpt.swp.model.PaymentStatus.SUCCESS AND p.createdAt >= :from AND p.createdAt < :to")
    BigDecimal revenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.status = :status")
    long countByStatus(@Param("status") PaymentStatus status);

    /** Số user riêng biệt đã có ít nhất 1 giao dịch thành công (mẫu số của conversion). */
    @Query("SELECT COUNT(DISTINCT p.user.id) FROM PaymentTransaction p WHERE p.status = com.fpt.swp.model.PaymentStatus.SUCCESS")
    long countPayingUsers();

    /** Doanh thu + số giao dịch theo tháng (YYYY-MM), từ mốc :from trở đi. MySQL-specific. */
    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS ym, " +
                   "COALESCE(SUM(amount), 0) AS revenue, COUNT(*) AS cnt " +
                   "FROM payment_transactions " +
                   "WHERE status = 'SUCCESS' AND created_at >= :from " +
                   "GROUP BY ym ORDER BY ym", nativeQuery = true)
    List<MonthlyRevenueRow> monthlyRevenueSince(@Param("from") LocalDateTime from);
}
