package com.fpt.swp.controller;

import com.fpt.swp.model.UserFollow;
import com.fpt.swp.service.FollowService;
import com.fpt.swp.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final AuthUtils authUtils;

    @GetMapping
    public ResponseEntity<Page<UserFollow>> getFollows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(followService.getUserFollows(userId, type, page, size));
    }

    @PostMapping
    public ResponseEntity<?> addFollow(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        String followType = (String) request.get("type");
        try {
            if ("JOURNAL".equalsIgnoreCase(followType)) {
                Long journalId = ((Number) request.get("journalId")).longValue();
                UserFollow follow = followService.followJournal(userId, journalId);
                return ResponseEntity.ok(follow);
            } else if ("TOPIC".equalsIgnoreCase(followType)) {
                Long topicId = ((Number) request.get("topicId")).longValue();
                UserFollow follow = followService.followTopic(userId, topicId);
                return ResponseEntity.ok(follow);
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid follow type"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/journal/{journalId}")
    public ResponseEntity<Void> unfollowJournal(
            @PathVariable Long journalId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        followService.unfollowJournal(userId, journalId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/topic/{topicId}")
    public ResponseEntity<Void> unfollowTopic(
            @PathVariable Long topicId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        followService.unfollowTopic(userId, topicId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unfollow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        followService.unfollowById(userId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check/journal/{journalId}")
    public ResponseEntity<Map<String, Boolean>> checkJournalFollow(
            @PathVariable Long journalId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(Map.of("isFollowing", followService.isFollowingJournal(userId, journalId)));
    }
}
