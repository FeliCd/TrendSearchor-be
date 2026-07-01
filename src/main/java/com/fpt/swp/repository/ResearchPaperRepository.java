package com.fpt.swp.repository;

import com.fpt.swp.model.ResearchPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResearchPaperRepository extends JpaRepository<ResearchPaper, Long> {

    Optional<ResearchPaper> findByExternalId(String externalId);

    Boolean existsByExternalId(String externalId);

    @Query("SELECT p FROM ResearchPaper p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ResearchPaper> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.year = :year")
    Page<ResearchPaper> findByYear(@Param("year") Integer year, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.year BETWEEN :startYear AND :endYear")
    Page<ResearchPaper> findByYearRange(
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear,
            Pageable pageable);

    @Query("SELECT COUNT(p) FROM ResearchPaper p")
    Long countTotalPapers();

    @Query("SELECT COUNT(p) FROM ResearchPaper p WHERE p.year = :year")
    Long countByYear(@Param("year") Integer year);

    @Query("SELECT DISTINCT p.year FROM ResearchPaper p WHERE p.year IS NOT NULL ORDER BY p.year DESC")
    List<Integer> findDistinctYears();

    @Query("SELECT p FROM ResearchPaper p JOIN p.journals j WHERE j.id = :journalId")
    Page<ResearchPaper> findByJournalId(@Param("journalId") Long journalId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p JOIN p.authors a WHERE a.id = :authorId")
    Page<ResearchPaper> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p JOIN p.keywords k WHERE k.id = :keywordId")
    Page<ResearchPaper> findByKeywordId(@Param("keywordId") Long keywordId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p ORDER BY p.citationCount DESC")
    Page<ResearchPaper> findTopByCitations(Pageable pageable);

    // ─── Upload / Moderation queries ────────────────────────────────────────────

    @EntityGraph(attributePaths = {"authors", "keywords", "uploadedBy"})
    @Query("SELECT p FROM ResearchPaper p WHERE p.uploadStatus = :status ORDER BY p.createdAt DESC")
    Page<ResearchPaper> findByUploadStatus(@Param("status") com.fpt.swp.model.UploadStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"authors", "keywords"})
    @Query("SELECT p FROM ResearchPaper p WHERE p.uploadedBy.id = :userId ORDER BY p.createdAt DESC")
    Page<ResearchPaper> findByUploadedBy(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM ResearchPaper p WHERE p.uploadStatus = :status")
    long countByUploadStatus(@Param("status") com.fpt.swp.model.UploadStatus status);

    @Query("SELECT COUNT(p) FROM ResearchPaper p WHERE p.source = com.fpt.swp.model.PaperSource.USER_UPLOAD")
    long countUserUploads();

    @EntityGraph(attributePaths = {"authors", "keywords"})
    @Query("SELECT p FROM ResearchPaper p WHERE p.source = com.fpt.swp.model.PaperSource.USER_UPLOAD " +
           "AND p.uploadStatus = com.fpt.swp.model.UploadStatus.APPROVED " +
           "AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY p.createdAt DESC")
    List<ResearchPaper> searchApprovedUploads(@Param("query") String query, Pageable pageable);
}
