package com.fpt.swp.service;

import com.fpt.swp.dto.PaperDto;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final ResearchPaperRepository paperRepository;
    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;

    @Transactional
    public Bookmark addPaperBookmark(Long userId, Long paperId) {
        User user = userRepository.findById(userId).orElse(null);
        ResearchPaper paper = paperRepository.findById(paperId).orElse(null);

        if (user == null || paper == null) {
            throw new IllegalArgumentException("User or paper not found");
        }

        if (bookmarkRepository.existsByUserIdAndPaperId(userId, paperId)) {
            throw new IllegalStateException("Paper already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .paper(paper)
                .bookmarkType(BookmarkType.PAPER)
                .build();

        return bookmarkRepository.save(bookmark);
    }

    @Transactional
    public Bookmark addKeywordBookmark(Long userId, Long keywordId) {
        User user = userRepository.findById(userId).orElse(null);
        Keyword keyword = keywordRepository.findById(keywordId).orElse(null);

        if (user == null || keyword == null) {
            throw new IllegalArgumentException("User or keyword not found");
        }

        if (bookmarkRepository.existsByUserIdAndKeywordId(userId, keywordId)) {
            throw new IllegalStateException("Keyword already bookmarked");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .keyword(keyword)
                .bookmarkType(BookmarkType.KEYWORD)
                .build();

        return bookmarkRepository.save(bookmark);
    }

    @Transactional
    public void removePaperBookmark(Long userId, Long paperId) {
        Optional<Bookmark> bookmark = bookmarkRepository.findByUserIdAndPaperId(userId, paperId);
        bookmark.ifPresent(bookmarkRepository::delete);
    }

    @Transactional
    public void removeKeywordBookmark(Long userId, Long keywordId) {
        Optional<Bookmark> bookmark = bookmarkRepository.findByUserIdAndKeywordId(userId, keywordId);
        bookmark.ifPresent(bookmarkRepository::delete);
    }

    @Transactional
    public void removeBookmarkById(Long userId, Long bookmarkId) {
        bookmarkRepository.findById(bookmarkId).ifPresent(b -> {
            if (b.getUser().getId().equals(userId)) {
                bookmarkRepository.delete(b);
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<Bookmark> getUserBookmarks(Long userId, String type, int page, int size) {
        if (type != null && !type.isBlank()) {
            BookmarkType bookmarkType = BookmarkType.valueOf(type.toUpperCase());
            return bookmarkRepository.findByUserIdAndType(userId, bookmarkType, PageRequest.of(page, size));
        }
        return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public boolean isPaperBookmarked(Long userId, Long paperId) {
        return bookmarkRepository.existsByUserIdAndPaperId(userId, paperId);
    }

    @Transactional(readOnly = true)
    public boolean isKeywordBookmarked(Long userId, Long keywordId) {
        return bookmarkRepository.existsByUserIdAndKeywordId(userId, keywordId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBookmarkStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", bookmarkRepository.countByUserId(userId));
        stats.put("paperCount", bookmarkRepository.countByUserIdAndType(userId, BookmarkType.PAPER));
        stats.put("keywordCount", bookmarkRepository.countByUserIdAndType(userId, BookmarkType.KEYWORD));
        return stats;
    }
}
