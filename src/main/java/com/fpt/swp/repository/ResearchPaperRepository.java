package com.fpt.swp.repository;

import com.fpt.swp.model.PaperStatus;
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

    @Query("SELECT p FROM ResearchPaper p WHERE p.status = :status")
    Page<ResearchPaper> findByStatus(@Param("status") PaperStatus status, Pageable pageable);

    @Query("SELECT COUNT(p) FROM ResearchPaper p WHERE p.status = :status")
    long countByStatus(@Param("status") PaperStatus status);

    @EntityGraph(attributePaths = {"authors", "keywords", "uploadedBy"})
    @Query("SELECT p FROM ResearchPaper p WHERE p.status = :status ORDER BY p.createdAt DESC")
    Page<ResearchPaper> findModerationByStatus(@Param("status") PaperStatus status, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.status = com.fpt.swp.model.PaperStatus.PENDING AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.uploadedBy.mail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.uploadedBy.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ResearchPaper> findPendingWithSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.status != com.fpt.swp.model.PaperStatus.PENDING AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.uploadedBy.mail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.uploadedBy.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ResearchPaper> findHistoryWithSearchAndStatus(
            @Param("search") String search,
            @Param("status") PaperStatus status,
            Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.uploadedBy.id = :userId ORDER BY p.createdAt DESC")
    Page<ResearchPaper> findByUploadedById(@Param("userId") Long userId, Pageable pageable);

    Optional<ResearchPaper> findByExternalId(String externalId);

    Boolean existsByExternalId(String externalId);

    // Các query hiển thị công khai: chỉ APPROVED và đã hết hạn embargo (nếu có)
    @Query("SELECT p FROM ResearchPaper p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) AND p.status = com.fpt.swp.model.PaperStatus.APPROVED AND (p.embargoUntil IS NULL OR p.embargoUntil <= CURRENT_DATE)")
    Page<ResearchPaper> searchByTitle(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.abstractText) LIKE LOWER(CONCAT('%', :query, '%'))) AND p.status = com.fpt.swp.model.PaperStatus.APPROVED AND (p.embargoUntil IS NULL OR p.embargoUntil <= CURRENT_DATE) ORDER BY p.isSelfPublished DESC")
    Page<ResearchPaper> searchApprovedLocalPapers(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.year = :year AND p.status = com.fpt.swp.model.PaperStatus.APPROVED AND (p.embargoUntil IS NULL OR p.embargoUntil <= CURRENT_DATE)")
    Page<ResearchPaper> findByYear(@Param("year") Integer year, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.year BETWEEN :startYear AND :endYear AND p.status = com.fpt.swp.model.PaperStatus.APPROVED AND (p.embargoUntil IS NULL OR p.embargoUntil <= CURRENT_DATE)")
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

    @Query("SELECT p FROM ResearchPaper p JOIN p.journals j WHERE j.id = :journalId AND p.status = com.fpt.swp.model.PaperStatus.APPROVED AND (p.embargoUntil IS NULL OR p.embargoUntil <= CURRENT_DATE)")
    Page<ResearchPaper> findByJournalId(@Param("journalId") Long journalId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p JOIN p.authors a WHERE a.id = :authorId AND p.status = com.fpt.swp.model.PaperStatus.APPROVED AND (p.embargoUntil IS NULL OR p.embargoUntil <= CURRENT_DATE)")
    Page<ResearchPaper> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p JOIN p.keywords k WHERE k.id = :keywordId AND p.status = com.fpt.swp.model.PaperStatus.APPROVED AND (p.embargoUntil IS NULL OR p.embargoUntil <= CURRENT_DATE)")
    Page<ResearchPaper> findByKeywordId(@Param("keywordId") Long keywordId, Pageable pageable);

    @Query("SELECT p FROM ResearchPaper p WHERE p.status = com.fpt.swp.model.PaperStatus.APPROVED AND (p.embargoUntil IS NULL OR p.embargoUntil <= CURRENT_DATE) ORDER BY p.citationCount DESC")
    Page<ResearchPaper> findTopByCitations(Pageable pageable);

    // ─── Upload / Moderation queries ────────────────────────────────────────────

    @EntityGraph(attributePaths = {"authors", "keywords"})
    @Query("SELECT p FROM ResearchPaper p WHERE p.uploadedBy.id = :userId ORDER BY p.createdAt DESC")
    Page<ResearchPaper> findByUploadedBy(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(p) FROM ResearchPaper p WHERE p.source = com.fpt.swp.model.PaperSource.USER_UPLOAD")
    long countUserUploads();

    @Query("SELECT p FROM ResearchPaper p WHERE p.status != com.fpt.swp.model.PaperStatus.PENDING ORDER BY p.updatedAt DESC")
    Page<ResearchPaper> findModerationHistory(Pageable pageable);

    @Query("SELECT new com.fpt.swp.dto.ResearcherLeaderboardDto(p.uploadedBy.fullName, p.uploadedBy.mail, COUNT(p)) " +
           "FROM ResearchPaper p " +
           "WHERE p.status = com.fpt.swp.model.PaperStatus.APPROVED AND p.uploadedBy IS NOT NULL " +
           "GROUP BY p.uploadedBy.id, p.uploadedBy.fullName, p.uploadedBy.mail " +
           "ORDER BY COUNT(p) DESC")
    List<com.fpt.swp.dto.ResearcherLeaderboardDto> findTopResearchersByApprovedPapersCount(Pageable pageable);
}
