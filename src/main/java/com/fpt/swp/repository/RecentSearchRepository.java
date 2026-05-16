package com.fpt.swp.repository;

import com.fpt.swp.model.RecentSearch;
import com.fpt.swp.model.SearchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {

    @Query("SELECT s FROM RecentSearch s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    Page<RecentSearch> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM RecentSearch s WHERE s.user.id = :userId AND s.searchType = :type ORDER BY s.createdAt DESC")
    Page<RecentSearch> findByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") SearchType type,
            Pageable pageable);

    @Query("SELECT DISTINCT s.searchQuery FROM RecentSearch s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<String> findDistinctQueriesByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM RecentSearch s WHERE s.user.id = :userId AND s.searchQuery = :query AND s.searchType = :type")
    List<RecentSearch> findByUserIdAndQueryAndType(
            @Param("userId") Long userId,
            @Param("query") String query,
            @Param("type") SearchType type);

    @Modifying
    @Query("DELETE FROM RecentSearch s WHERE s.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RecentSearch s WHERE s.id = :id AND s.user.id = :userId")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
