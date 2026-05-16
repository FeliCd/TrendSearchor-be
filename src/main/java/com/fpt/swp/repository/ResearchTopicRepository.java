package com.fpt.swp.repository;

import com.fpt.swp.model.ResearchTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResearchTopicRepository extends JpaRepository<ResearchTopic, Long> {

    Optional<ResearchTopic> findByName(String name);

    Boolean existsByName(String name);

    @Query("SELECT t FROM ResearchTopic t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ResearchTopic> searchByName(@Param("query") String query, Pageable pageable);

    @Query("SELECT t FROM ResearchTopic t WHERE t.category = :category")
    Page<ResearchTopic> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT t FROM ResearchTopic t ORDER BY t.popularityScore DESC")
    Page<ResearchTopic> findTopByPopularity(Pageable pageable);
}
