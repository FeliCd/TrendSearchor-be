package com.fpt.swp.service;

import com.fpt.swp.dto.GraphResponse;
import com.fpt.swp.dto.PaperDto;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {
    
    private final BookmarkRepository bookmarkRepository;
    private final ResearchPaperRepository paperRepository;
    private final KeywordRepository keywordRepository;
    private final AuthorRepository authorRepository;
    private final JournalRepository journalRepository;
    private final UserRepository userRepository;
    private final OpenAlexService openAlexService;
    private final DataSyncService dataSyncService;

    @Transactional
    public Bookmark addPaperBookmark(Long userId, String externalId) {
        User user = userRepository.findById(userId).orElse(null);
        ResearchPaper paper = paperRepository.findByExternalId(externalId).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (paper == null) {
            // Fetch paper from OpenAlex and save to DB
            Map<String, Object> rawPaper = openAlexService.getPaperDetails(externalId);
            if (rawPaper == null || rawPaper.isEmpty()) {
                throw new IllegalArgumentException("Paper not found in OpenAlex: " + externalId);
            }
            paper = buildPaperFromOpenAlex(rawPaper, externalId);
        }

        Bookmark existing = bookmarkRepository.findByUserIdAndPaperId(userId, paper.getId()).orElse(null);
        if (existing != null) {
            return existing;
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
    public void removePaperBookmark(Long userId, String externalId) {
        ResearchPaper paper = paperRepository.findByExternalId(externalId).orElse(null);
        if (paper != null) {
            Optional<Bookmark> bookmark = bookmarkRepository.findByUserIdAndPaperId(userId, paper.getId());
            bookmark.ifPresent(bookmarkRepository::delete);
        }
    }

    @Transactional
    public void removePaperBookmarkByPaperId(Long userId, Long paperId) {
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
    public Page<Bookmark> getUserBookmarks(Long userId, String type, Long collectionId, int page, int size) {
        if (collectionId != null) {
            if (type != null && !type.isBlank()) {
                BookmarkType bookmarkType = BookmarkType.valueOf(type.toUpperCase());
                return bookmarkRepository.findBookmarksByCollectionAndType(userId, collectionId, bookmarkType, PageRequest.of(page, size));
            }
            return bookmarkRepository.findBookmarksByCollection(userId, collectionId, PageRequest.of(page, size));
        }

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
    public boolean isPaperBookmarkedByExternalId(Long userId, String externalId) {
        return paperRepository.findByExternalId(externalId)
                .map(paper -> bookmarkRepository.existsByUserIdAndPaperId(userId, paper.getId()))
                .orElse(false);
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

    @Transactional(readOnly = true)
    public GraphResponse getUserBookmarkNetwork(Long userId, Long collectionId) {
        List<Bookmark> bookmarks;
        if (collectionId != null) {
            bookmarks = bookmarkRepository.findBookmarksByCollectionAndPaperIsNotNull(userId, collectionId);
        } else {
            bookmarks = bookmarkRepository.findByUserIdAndType(userId, BookmarkType.PAPER, PageRequest.of(0, 1000)).getContent();
        }
        
        List<ResearchPaper> bookmarkedPapers = bookmarks.stream()
                .map(Bookmark::getPaper)
                .toList();

        List<GraphResponse.Node> nodes = new ArrayList<>();
        List<GraphResponse.Link> links = new ArrayList<>();

        for (ResearchPaper p : bookmarkedPapers) {
            nodes.add(GraphResponse.Node.builder()
                    .id(p.getId().toString())
                    .title(p.getTitle())
                    .year(p.getYear())
                    .citationCount(p.getCitationCount())
                    .type("PAPER")
                    .build());

            for (ResearchPaper cited : p.getCitedPapers()) {
                if (bookmarkedPapers.contains(cited)) {
                    links.add(GraphResponse.Link.builder()
                            .source(p.getId().toString())
                            .target(cited.getId().toString())
                            .build());
                }
            }
        }

        return new GraphResponse(nodes, links);
    }


    @SuppressWarnings("unchecked")
    private ResearchPaper buildPaperFromOpenAlex(Map<String, Object> raw, String externalId) {
        ResearchPaper paper = new ResearchPaper();
        paper.setExternalId(externalId);
        paper.setTitle((String) raw.getOrDefault("title", "Untitled"));

        Object abstractNode = raw.get("abstract_inverted_index");
        if (abstractNode instanceof Map) {
            String abstractText = reconstructAbstract((Map<String, Object>) abstractNode);
            paper.setAbstractText(abstractText);
        }

        if (raw.get("publication_year") != null) {
            paper.setYear(((Number) raw.get("publication_year")).intValue());
        } else {
            Object date = raw.get("publication_date");
            if (date instanceof String d && d.length() >= 4) {
                try { paper.setYear(Integer.parseInt(d.substring(0, 4))); } catch (Exception ignored) {}
            }
        }

        paper.setCitationCount(raw.get("cited_by_count") != null ? ((Number) raw.get("cited_by_count")).intValue() : 0);

        Object oa = raw.get("open_access");
        if (oa instanceof Map) paper.setOpenAccess(Boolean.TRUE.equals(((Map<String, Object>) oa).get("is_oa")));
        else paper.setOpenAccess(false);

        paper.setPaperUri((String) raw.get("doi"));

        Object primaryLoc = raw.get("primary_location");
        if (primaryLoc instanceof Map) {
            Object source = ((Map<String, Object>) primaryLoc).get("source");
            if (source instanceof Map) {
                String journalName = (String) ((Map<String, Object>) source).get("display_name");
                if (journalName != null) {
                    Journal journal = journalRepository.findByExternalId(externalId + "_journal")
                            .orElseGet(() -> {
                                Journal j = new Journal();
                                j.setExternalId(externalId + "_journal");
                                j.setName(journalName);
                                return journalRepository.save(j);
                            });
                    paper.getJournals().add(journal);
                }
            }
        }

        Object authorsList = raw.get("authorships");
        if (authorsList instanceof List) {
            for (Object authItem : (List<?>) authorsList) {
                if (authItem instanceof Map) {
                    Object authorNode = ((Map<String, Object>) authItem).get("author");
                    if (authorNode instanceof Map) {
                        String name = (String) ((Map<String, Object>) authorNode).get("display_name");
                        if (name != null) {
                            Author author = authorRepository.findAll().stream()
                                    .filter(a -> a.getName().equalsIgnoreCase(name))
                                    .findFirst()
                                    .orElseGet(() -> {
                                        Author a = new Author();
                                        a.setName(name);
                                        return authorRepository.save(a);
                                    });
                            paper.getAuthors().add(author);
                        }
                    }
                }
            }
        }

        Object topicsList = raw.get("topics");
        if (topicsList instanceof List) {
            for (Object topicItem : (List<?>) topicsList) {
                if (topicItem instanceof Map) {
                    Map<String, Object> tMap = (Map<String, Object>) topicItem;
                    addKeywordToPaper(paper, (String) tMap.get("display_name"));
                    
                    Object subfield = tMap.get("subfield");
                    if (subfield instanceof Map) {
                        addKeywordToPaper(paper, (String) ((Map<String, Object>) subfield).get("display_name"));
                    }
                    
                    Object field = tMap.get("field");
                    if (field instanceof Map) {
                        addKeywordToPaper(paper, (String) ((Map<String, Object>) field).get("display_name"));
                    }
                }
            }
        }

        Object keywordList = raw.get("keywords");
        if (keywordList instanceof List) {
            for (Object kwItem : (List<?>) keywordList) {
                if (kwItem instanceof Map) {
                    addKeywordToPaper(paper, (String) ((Map<String, Object>) kwItem).get("display_name"));
                } else if (kwItem instanceof String) {
                    addKeywordToPaper(paper, (String) kwItem);
                }
            }
        }

        Object referencedWorksNode = raw.get("referenced_works");
        if (referencedWorksNode instanceof List) {
            for (Object refItem : (List<?>) referencedWorksNode) {
                if (refItem instanceof String refId) {
                    String shortId = refId;
                    if (refId.startsWith("https://openalex.org/")) {
                        shortId = refId.substring("https://openalex.org/".length());
                    }
                    paperRepository.findByExternalId(shortId).ifPresent(citedPaper -> paper.getCitedPapers().add(citedPaper));
                    if (!shortId.equals(refId)) {
                        paperRepository.findByExternalId(refId).ifPresent(citedPaper -> paper.getCitedPapers().add(citedPaper));
                    }
                }
            }
        }

        return paperRepository.save(paper);
    }

    private String reconstructAbstract(Map<String, Object> invertedIndex) {
        if (invertedIndex == null || invertedIndex.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        invertedIndex.forEach((word, positions) -> {
            if (positions instanceof List) {
                sb.append(word).append(" ");
            }
        });
        return sb.toString().trim();
    }

    private void addKeywordToPaper(ResearchPaper paper, String kwName) {
        if (kwName != null && !kwName.isBlank() && kwName.length() <= 200) {
            Keyword keyword = keywordRepository.findByName(kwName.toLowerCase())
                    .orElseGet(() -> {
                        Keyword k = new Keyword();
                        k.setName(kwName.toLowerCase());
                        k.setDisplayName(kwName);
                        return keywordRepository.save(k);
                    });
            paper.getKeywords().add(keyword);
        }
    }
}
