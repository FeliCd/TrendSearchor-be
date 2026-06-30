package com.fpt.swp.repository;

import com.fpt.swp.model.PaperStatus;
import com.fpt.swp.model.ResearchPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResearchPaperRepository extends JpaRepository<ResearchPaper, Long> {

    @Query("SELECT p FROM ResearchPaper p WHERE p.status = :status")
    Page<ResearchPaper> findByStatus(@Param("status") PaperStatus status, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.uploadedBy.id = :userId ORDER BY p.createdAt DESC")
    Page<ResearchPaper> findByUploadedById(@Param("userId") Long userId, Pageable pageable);

    Optional<ResearchPaper> findByExternalId(String externalId);

    Boolean existsByExternalId(String externalId);

    @Query("SELECT p FROM ResearchPaper p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) AND p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Page<ResearchPaper> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :query, '%'))) AND p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Page<ResearchPaper> searchApprovedLocalPapers(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.year = :year AND p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Page<ResearchPaper> findByYear(@Param("year") Integer year, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.year BETWEEN :startYear AND :endYear AND p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Page<ResearchPaper> findByYearRange(
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear,
            Pageable pageable);

    @Query("SELECT COUNT(p) FROM ResearchPaper p WHERE p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Long countTotalPapers();

    @Query("SELECT COUNT(p) FROM ResearchPaper p WHERE p.year = :year AND p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Long countByYear(@Param("year") Integer year);

    @Query("SELECT DISTINCT p.year FROM ResearchPaper p WHERE p.year IS NOT NULL AND p.status = com.fpt.swp.model.PaperStatus.APPROVED ORDER BY p.year DESC")
    List<Integer> findDistinctYears();

    @Query("SELECT p FROM ResearchPaper p JOIN p.journals j WHERE j.id = :journalId AND p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Page<ResearchPaper> findByJournalId(@Param("journalId") Long journalId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p JOIN p.authors a WHERE a.id = :authorId AND p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Page<ResearchPaper> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p JOIN p.keywords k WHERE k.id = :keywordId AND p.status = com.fpt.swp.model.PaperStatus.APPROVED")
    Page<ResearchPaper> findByKeywordId(@Param("keywordId") Long keywordId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.status = com.fpt.swp.model.PaperStatus.APPROVED ORDER BY p.citationCount DESC")
    Page<ResearchPaper> findTopByCitations(Pageable pageable);
}
