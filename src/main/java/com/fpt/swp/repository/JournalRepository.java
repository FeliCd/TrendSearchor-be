package com.fpt.swp.repository;

import com.fpt.swp.model.Journal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JournalRepository extends JpaRepository<Journal, Long> {

    Optional<Journal> findByExternalId(String externalId);

    Boolean existsByExternalId(String externalId);

    @Query("SELECT j FROM Journal j WHERE LOWER(j.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Journal> searchByName(@Param("query") String query, Pageable pageable);

    Optional<Journal> findByIssn(String issn);

    Optional<Journal> findByNameIgnoreCase(String name);

    @Query("SELECT j FROM Journal j ORDER BY j.name ASC")
    Page<Journal> findAllOrderByName(Pageable pageable);

    List<Journal> findTop10ByOrderByNameAsc();
}
