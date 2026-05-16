package com.fpt.swp.service;

import com.fpt.swp.model.*;
import com.fpt.swp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final UserFollowRepository followRepository;
    private final JournalRepository journalRepository;
    private final ResearchTopicRepository topicRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserFollow followJournal(Long userId, Long journalId) {
        User user = userRepository.findById(userId).orElse(null);
        Journal journal = journalRepository.findById(journalId).orElse(null);

        if (user == null || journal == null) {
            throw new IllegalArgumentException("User or journal not found");
        }

        if (followRepository.existsByUserIdAndJournalId(userId, journalId)) {
            throw new IllegalStateException("Already following this journal");
        }

        UserFollow follow = UserFollow.builder()
                .user(user)
                .journal(journal)
                .followType(FollowType.JOURNAL)
                .build();

        return followRepository.save(follow);
    }

    @Transactional
    public UserFollow followTopic(Long userId, Long topicId) {
        User user = userRepository.findById(userId).orElse(null);
        ResearchTopic topic = topicRepository.findById(topicId).orElse(null);

        if (user == null || topic == null) {
            throw new IllegalArgumentException("User or topic not found");
        }

        if (followRepository.existsByUserIdAndTopicId(userId, topicId)) {
            throw new IllegalStateException("Already following this topic");
        }

        UserFollow follow = UserFollow.builder()
                .user(user)
                .topic(topic)
                .followType(FollowType.TOPIC)
                .build();

        return followRepository.save(follow);
    }

    @Transactional
    public void unfollowJournal(Long userId, Long journalId) {
        Optional<UserFollow> follow = followRepository.findByUserIdAndJournalId(userId, journalId);
        follow.ifPresent(followRepository::delete);
    }

    @Transactional
    public void unfollowTopic(Long userId, Long topicId) {
        Optional<UserFollow> follow = followRepository.findByUserIdAndTopicId(userId, topicId);
        follow.ifPresent(followRepository::delete);
    }

    @Transactional
    public void unfollowById(Long userId, Long followId) {
        followRepository.findById(followId).ifPresent(f -> {
            if (f.getUser().getId().equals(userId)) {
                followRepository.delete(f);
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<UserFollow> getUserFollows(Long userId, String type, int page, int size) {
        if (type != null && !type.isBlank()) {
            FollowType followType = FollowType.valueOf(type.toUpperCase());
            return followRepository.findByUserIdAndType(userId, followType, PageRequest.of(page, size));
        }
        return followRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public boolean isFollowingJournal(Long userId, Long journalId) {
        return followRepository.existsByUserIdAndJournalId(userId, journalId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowingTopic(Long userId, Long topicId) {
        return followRepository.existsByUserIdAndTopicId(userId, topicId);
    }
}
