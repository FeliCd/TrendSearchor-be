package com.fpt.swp.repository;

import com.fpt.swp.model.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByExternalId(String externalId);

    Boolean existsByExternalId(String externalId);

    Optional<Author> findByOrcid(String orcid);

    Optional<Author> findFirstByName(String name);
    Optional<Author> findByNameIgnoreCase(String name);

    @Query("SELECT a FROM Author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Author> searchByName(@Param("query") String query, Pageable pageable);

    @Query("SELECT a FROM Author a ORDER BY a.hIndex DESC")
    Page<Author> findTopByHIndex(Pageable pageable);
}
