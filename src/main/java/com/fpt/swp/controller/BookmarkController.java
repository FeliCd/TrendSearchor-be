package com.fpt.swp.controller;

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
    public ResponseEntity<Page<Bookmark>> getBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(bookmarkService.getUserBookmarks(userId, type, page, size));
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
                Long paperId = ((Number) request.get("paperId")).longValue();
                Bookmark bookmark = bookmarkService.addPaperBookmark(userId, paperId);
                return ResponseEntity.ok(bookmark);
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

    @DeleteMapping("/paper/{paperId}")
    public ResponseEntity<Void> removePaperBookmark(
            @PathVariable Long paperId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        bookmarkService.removePaperBookmark(userId, paperId);
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

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBookmarkStats(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(bookmarkService.getBookmarkStats(userId));
    }

    @GetMapping("/check/paper/{paperId}")
    public ResponseEntity<Map<String, Boolean>> checkPaperBookmark(
            @PathVariable Long paperId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(Map.of("isBookmarked", bookmarkService.isPaperBookmarked(userId, paperId)));
    }
}
