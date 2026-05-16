package com.fpt.swp.repository;

import com.fpt.swp.model.ApiDataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiDataSourceRepository extends JpaRepository<ApiDataSource, Long> {

    Optional<ApiDataSource> findBySourceName(String sourceName);

    Boolean existsBySourceName(String sourceName);

    @Query("SELECT d FROM ApiDataSource d WHERE d.isActive = true")
    List<ApiDataSource> findAllActive();

    @Query("SELECT d FROM ApiDataSource d ORDER BY d.sourceName ASC")
    List<ApiDataSource> findAllOrderByName();
}
