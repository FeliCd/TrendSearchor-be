package com.fpt.swp.controller;

import com.fpt.swp.dto.CollectionDto;
import com.fpt.swp.dto.CollectionRequest;
import com.fpt.swp.service.CollectionService;
import com.fpt.swp.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final AuthUtils authUtils;

    @GetMapping
    public ResponseEntity<List<CollectionDto>> getCollections(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(collectionService.getUserCollections(userId));
    }

    @PostMapping
    public ResponseEntity<CollectionDto> createCollection(
            @RequestBody CollectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(collectionService.createCollection(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionDto> updateCollection(
            @PathVariable Long id,
            @RequestBody CollectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(collectionService.updateCollection(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        collectionService.deleteCollection(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/bookmarks/{bookmarkId}")
    public ResponseEntity<Void> addBookmarkToCollection(
            @PathVariable Long id,
            @PathVariable Long bookmarkId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        collectionService.addBookmarkToCollection(userId, id, bookmarkId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/bookmarks/{bookmarkId}")
    public ResponseEntity<Void> removeBookmarkFromCollection(
            @PathVariable Long id,
            @PathVariable Long bookmarkId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        collectionService.removeBookmarkFromCollection(userId, id, bookmarkId);
        return ResponseEntity.ok().build();
    }
}
