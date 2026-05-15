package com.fpt.swp.repository;

import com.fpt.swp.model.Keyword;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByName(String name);

    Boolean existsByName(String name);

    @Query("SELECT k FROM Keyword k WHERE LOWER(k.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Keyword> searchByName(@Param("query") String query, Pageable pageable);

    @Query("SELECT k FROM Keyword k ORDER BY k.name ASC")
    Page<Keyword> findAllOrderByName(Pageable pageable);

    List<Keyword> findTop20ByOrderByNameAsc();
}
