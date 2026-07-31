package com.fpt.swp.repository;

import com.fpt.swp.model.CopyrightReport;
import com.fpt.swp.model.CopyrightReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CopyrightReportRepository extends JpaRepository<CopyrightReport, Long> {

    @Query("SELECT r FROM CopyrightReport r WHERE r.status = :status ORDER BY r.createdAt ASC")
    Page<CopyrightReport> findByStatus(@Param("status") CopyrightReportStatus status, Pageable pageable);

    @Query("SELECT r FROM CopyrightReport r WHERE r.paper.id = :paperId ORDER BY r.createdAt DESC")
    Page<CopyrightReport> findByPaperId(@Param("paperId") Long paperId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM CopyrightReport r WHERE r.status = :status")
    long countByStatus(@Param("status") CopyrightReportStatus status);

    /** Chống spam: mỗi user chỉ được giữ 1 report PENDING trên mỗi bài. */
    boolean existsByPaperIdAndReportedByIdAndStatus(Long paperId, Long reportedById, CopyrightReportStatus status);

    /** Dùng khi take down: đóng tất cả report còn chờ của cùng một bài. */
    java.util.List<CopyrightReport> findByPaperIdAndStatus(Long paperId, CopyrightReportStatus status);
}
