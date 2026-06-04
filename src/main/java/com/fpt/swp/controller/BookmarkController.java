package com.fpt.swp.controller;

import com.fpt.swp.dto.BookmarkResponse;
import com.fpt.swp.model.Bookmark;
import com.fpt.swp.service.BookmarkService;
import com.fpt.swp.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final AuthUtils authUtils;

    @GetMapping
    public ResponseEntity<?> getBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long collectionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        Page<Bookmark> bookmarks = bookmarkService.getUserBookmarks(userId, type, collectionId, page, size);
        var content = bookmarks.getContent().stream()
                .map(BookmarkResponse::fromBookmark)
                .toList();

        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(
                new java.util.ArrayList<>(content),
                bookmarks.getPageable(),
                bookmarks.getTotalElements()
        ));
    }

    @PostMapping
    public ResponseEntity<?> addBookmark(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        String bookmarkType = (String) request.get("type");
        try {
            if ("PAPER".equalsIgnoreCase(bookmarkType)) {
                String externalId = (String) request.get("externalId");
                Bookmark bookmark = bookmarkService.addPaperBookmark(userId, externalId);
                return ResponseEntity.ok(BookmarkResponse.fromBookmark(bookmark));
            } else if ("KEYWORD".equalsIgnoreCase(bookmarkType)) {
                Long keywordId = ((Number) request.get("keywordId")).longValue();
                Bookmark bookmark = bookmarkService.addKeywordBookmark(userId, keywordId);
                return ResponseEntity.ok(bookmark);
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid bookmark type"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/paper/{externalId}")
    public ResponseEntity<Void> removePaperBookmark(
            @PathVariable String externalId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        bookmarkService.removePaperBookmark(userId, externalId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/keyword/{keywordId}")
    public ResponseEntity<Void> removeKeywordBookmark(
            @PathVariable Long keywordId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        bookmarkService.removeKeywordBookmark(userId, keywordId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeBookmark(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        bookmarkService.removeBookmarkById(userId, id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/network")
    public ResponseEntity<com.fpt.swp.dto.GraphResponse> getBookmarkNetwork(
            @RequestParam(required = false) Long collectionId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(bookmarkService.getUserBookmarkNetwork(userId, collectionId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBookmarkStats(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(bookmarkService.getBookmarkStats(userId));
    }

    @GetMapping("/check/paper/{externalId}")
    public ResponseEntity<Map<String, Boolean>> checkPaperBookmark(
            @PathVariable String externalId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(Map.of("isBookmarked", bookmarkService.isPaperBookmarkedByExternalId(userId, externalId)));
    }

    @GetMapping("/check/keyword/{keywordId}")
    public ResponseEntity<Map<String, Boolean>> checkKeywordBookmark(
            @PathVariable Long keywordId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(Map.of("isBookmarked", bookmarkService.isKeywordBookmarked(userId, keywordId)));
    }
}
