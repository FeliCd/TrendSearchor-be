package com.fpt.swp.repository;

import com.fpt.swp.model.FollowType;
import com.fpt.swp.model.UserFollow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    @Query("SELECT f FROM UserFollow f WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    Page<UserFollow> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f FROM UserFollow f WHERE f.user.id = :userId AND f.followType = :type ORDER BY f.createdAt DESC")
    Page<UserFollow> findByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") FollowType type,
            Pageable pageable);

    Optional<UserFollow> findByUserIdAndJournalId(Long userId, Long journalId);

    Optional<UserFollow> findByUserIdAndTopicId(Long userId, Long topicId);

    Boolean existsByUserIdAndJournalId(Long userId, Long journalId);

    Boolean existsByUserIdAndTopicId(Long userId, Long topicId);

    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM UserFollow f WHERE f.user.id = :userId AND f.followType = :type")
    Long countByUserIdAndType(@Param("userId") Long userId, @Param("type") FollowType type);

    void deleteByUserIdAndJournalId(Long userId, Long journalId);

    void deleteByUserIdAndTopicId(Long userId, Long topicId);

    @Query("SELECT f.user.id FROM UserFollow f WHERE f.topic.id = :topicId")
    java.util.List<Long> findUserIdsByTopicId(@Param("topicId") Long topicId);

    @Query("SELECT f.user.id FROM UserFollow f WHERE f.journal.id = :journalId")
    java.util.List<Long> findUserIdsByJournalId(@Param("journalId") Long journalId);
}
