package com.fpt.swp.repository;

import com.fpt.swp.model.PublicationTrend;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationTrendRepository extends JpaRepository<PublicationTrend, Long> {

    @Query("SELECT t FROM PublicationTrend t WHERE t.keywordName = :keyword ORDER BY t.year ASC, t.month ASC")
    List<PublicationTrend> findByKeywordOrderByYearAscMonthAsc(@Param("keyword") String keyword);

    @Query("SELECT t FROM PublicationTrend t WHERE t.year = :year")
    Page<PublicationTrend> findByYear(@Param("year") Integer year, Pageable pageable);

    @Query("SELECT DISTINCT t.keywordName FROM PublicationTrend t ORDER BY t.keywordName")
    List<String> findDistinctKeywordNames();

    @Query("SELECT t FROM PublicationTrend t WHERE t.keywordName IN :keywords ORDER BY t.year ASC, t.month ASC")
    List<PublicationTrend> findByKeywordInOrderByYearAscMonthAsc(@Param("keywords") List<String> keywords);

    @Query("SELECT t FROM PublicationTrend t WHERE t.keywordName = :keyword AND t.year >= :startYear ORDER BY t.year ASC, t.month ASC")
    List<PublicationTrend> findByKeywordSinceYear(
            @Param("keyword") String keyword,
            @Param("startYear") Integer startYear);

    @Query("SELECT MAX(t.year) FROM PublicationTrend t")
    Integer findMaxYear();

    @Query("SELECT MIN(t.year) FROM PublicationTrend t")
    Integer findMinYear();
}
