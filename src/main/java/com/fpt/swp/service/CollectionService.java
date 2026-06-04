package com.fpt.swp.service;

import com.fpt.swp.dto.CollectionDto;
import com.fpt.swp.dto.CollectionRequest;
import com.fpt.swp.model.Bookmark;
import com.fpt.swp.model.Collection;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.BookmarkRepository;
import com.fpt.swp.repository.CollectionRepository;
import com.fpt.swp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CollectionDto> getUserCollections(Long userId) {
        return collectionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(CollectionDto::fromCollection).toList();
    }

    @Transactional
    public CollectionDto createCollection(Long userId, CollectionRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Collection name cannot be empty");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Collection collection = Collection.builder()
                .user(user)
                .name(request.getName().trim())
                .description(request.getDescription())
                .build();

        return CollectionDto.fromCollection(collectionRepository.save(collection));
    }

    @Transactional
    public CollectionDto updateCollection(Long userId, Long collectionId, CollectionRequest request) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            collection.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            collection.setDescription(request.getDescription());
        }

        return CollectionDto.fromCollection(collectionRepository.save(collection));
    }

    @Transactional
    public void deleteCollection(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        collectionRepository.delete(collection);
    }

    @Transactional
    public void addBookmarkToCollection(Long userId, Long collectionId, Long bookmarkId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found"));

        if (!bookmark.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Unauthorized to access this bookmark");
        }

        collection.getBookmarks().add(bookmark);
        collectionRepository.save(collection);
    }

    @Transactional
    public void removeBookmarkFromCollection(Long userId, Long collectionId, Long bookmarkId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found"));

        collection.getBookmarks().remove(bookmark);
        collectionRepository.save(collection);
    }
}
