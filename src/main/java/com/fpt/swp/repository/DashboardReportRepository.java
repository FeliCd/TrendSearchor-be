package com.fpt.swp.repository;

import com.fpt.swp.model.DashboardReport;
import com.fpt.swp.model.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardReportRepository extends JpaRepository<DashboardReport, Long> {

    @Query("SELECT r FROM DashboardReport r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    Page<DashboardReport> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT r FROM DashboardReport r WHERE r.user.id = :userId AND r.reportType = :type ORDER BY r.createdAt DESC")
    Page<DashboardReport> findByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") ReportType type,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM DashboardReport r WHERE r.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
}
