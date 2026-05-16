package com.fpt.swp.repository;

import com.fpt.swp.model.Bookmark;
import com.fpt.swp.model.BookmarkType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    Page<Bookmark> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.bookmarkType = :type ORDER BY b.createdAt DESC")
    Page<Bookmark> findByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") BookmarkType type,
            Pageable pageable);

    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.paper.id = :paperId")
    Optional<Bookmark> findByUserIdAndPaperId(@Param("userId") Long userId, @Param("paperId") Long paperId);

    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND b.keyword.id = :keywordId")
    Optional<Bookmark> findByUserIdAndKeywordId(@Param("userId") Long userId, @Param("keywordId") Long keywordId);

    Boolean existsByUserIdAndPaperId(Long userId, Long paperId);

    Boolean existsByUserIdAndKeywordId(Long userId, Long keywordId);

    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId AND b.bookmarkType = :type")
    Long countByUserIdAndType(@Param("userId") Long userId, @Param("type") BookmarkType type);

    List<Bookmark> findByUserIdAndPaperIdIsNotNull(Long userId);

    List<Bookmark> findByUserIdAndKeywordIdIsNotNull(Long userId);
}
