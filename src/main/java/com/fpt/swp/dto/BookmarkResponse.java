package com.fpt.swp.dto;

import com.fpt.swp.model.Bookmark;
import com.fpt.swp.model.BookmarkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {
    private Long id;
    private BookmarkType bookmarkType;
    private LocalDateTime createdAt;
    private PaperInfo paper;
    private KeywordInfo keyword;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaperInfo {
        private Long id;
        private String externalId;
        private String title;
        private Integer year;
        private Integer citationCount;
        private Boolean openAccess;
        private String paperUri;
        private java.util.List<String> keywords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordInfo {
        private Long id;
        private String name;
        private String displayName;
    }

    public static BookmarkResponse fromBookmark(Bookmark bookmark) {
        if (bookmark == null)
            return null;

        PaperInfo paperInfo = null;
        KeywordInfo keywordInfo = null;

        if (bookmark.getPaper() != null) {
            var p = bookmark.getPaper();
            paperInfo = PaperInfo.builder()
                    .id(p.getId())
                    .externalId(p.getExternalId())
                    .title(p.getTitle())
                    .year(p.getYear())
                    .citationCount(p.getCitationCount())
                    .openAccess(p.getOpenAccess())
                    .paperUri(p.getPaperUri())
                    .keywords(p.getKeywords() != null
                            ? p.getKeywords().stream()
                                    .map(k -> k.getDisplayName() != null ? k.getDisplayName() : k.getName()).toList()
                            : java.util.List.of())
                    .build();
        }

        if (bookmark.getKeyword() != null) {
            var k = bookmark.getKeyword();
            keywordInfo = KeywordInfo.builder()
                    .id(k.getId())
                    .name(k.getName())
                    .displayName(k.getDisplayName())
                    .build();
        }

        return BookmarkResponse.builder()
                .id(bookmark.getId())
                .bookmarkType(bookmark.getBookmarkType())
                .createdAt(bookmark.getCreatedAt())
                .paper(paperInfo)
                .keyword(keywordInfo)
                .build();
    }
}
