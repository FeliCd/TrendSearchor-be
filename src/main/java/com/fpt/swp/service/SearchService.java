package com.fpt.swp.service;

import com.fpt.swp.dto.*;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchService {

    private final OpenAlexService openAlexService;
    private final DataSyncService dataSyncService;
    private final ResearchPaperRepository paperRepository;
    private final AuthorRepository authorRepository;
    private final JournalRepository journalRepository;
    private final KeywordRepository keywordRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserFollowRepository followRepository;
    private final RecentSearchRepository searchRepository;
    private final UserRepository userRepository;

    public SearchService(OpenAlexService openAlexService,
                         DataSyncService dataSyncService,
                         ResearchPaperRepository paperRepository,
                         AuthorRepository authorRepository,
                         JournalRepository journalRepository,
                         KeywordRepository keywordRepository,
                         BookmarkRepository bookmarkRepository,
                         UserFollowRepository followRepository,
                         RecentSearchRepository searchRepository,
                         UserRepository userRepository) {
        this.openAlexService = openAlexService;
        this.dataSyncService = dataSyncService;
        this.paperRepository = paperRepository;
        this.authorRepository = authorRepository;
        this.journalRepository = journalRepository;
        this.keywordRepository = keywordRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.followRepository = followRepository;
        this.searchRepository = searchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaperSearchResponse searchPapers(PaperSearchRequest request, Long userId) {
        Map<String, Object> rawResult = openAlexService.searchPapersRaw(
                request.getQuery(),
                request.getPage() * request.getSize(),
                request.getSize(),
                request.getYear(),
                request.getJournal(),
                request.getAuthor(),
                request.getSortBy()
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawPapers = (List<Map<String, Object>>) rawResult.getOrDefault("papers", Collections.emptyList());
        long total = ((Number) rawResult.getOrDefault("total", 0)).longValue();

        List<PaperDto> papers = rawPapers.stream()
                .map(p -> mapToPaperDto(p, userId))
                .collect(Collectors.toList());

        if (userId != null && request.getQuery() != null && !request.getQuery().isBlank()) {
            saveRecentSearch(request.getQuery(), SearchType.PAPER, userId);
        }

        return PaperSearchResponse.builder()
                .papers(papers)
                .total(total)
                .page(request.getPage())
                .size(request.getSize())
                .totalPages((int) Math.ceil((double) total / request.getSize()))
                .build();
    }

    private PaperDto mapToPaperDto(Map<String, Object> raw, Long userId) {
        PaperDto dto = PaperDto.builder()
                .externalId((String) raw.get("paperId"))
                .title((String) raw.get("title"))
                .abstractText((String) raw.get("abstract"))
                .year((Integer) raw.get("year"))
                .citationCount(raw.get("citationCount") != null ? ((Number) raw.get("citationCount")).intValue() : 0)
                .openAccess(raw.get("openAccess") != null ? (Boolean) raw.get("openAccess") : false)
                .paperUri((String) raw.get("url"))
                .build();

        Object rawAuthors = raw.get("authors");
        if (rawAuthors instanceof List) {
            List<?> authorList = (List<?>) rawAuthors;
            List<AuthorDto> authorDtos = new ArrayList<>();
            for (Object item : authorList) {
                if (item instanceof String name) {
                    authorDtos.add(AuthorDto.builder().name(name).build());
                } else if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> authorMap = (Map<String, Object>) item;
                    authorDtos.add(AuthorDto.builder()
                            .name((String) authorMap.get("name"))
                            .build());
                }
            }
            dto.setAuthors(authorDtos);
        }

        if (raw.get("journal") != null) {
            dto.setJournals(List.of((String) raw.get("journal")));
        }

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) raw.getOrDefault("keywords", Collections.emptyList());
        dto.setKeywords(keywords);

        if (userId != null && dto.getExternalId() != null) {
            Optional<ResearchPaper> localPaper = paperRepository.findByExternalId(dto.getExternalId());
            if (localPaper.isPresent()) {
                dto.setId(localPaper.get().getId());
                dto.setIsBookmarked(bookmarkRepository.existsByUserIdAndPaperId(userId, localPaper.get().getId()));
            }
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public Page<ResearchPaper> searchLocalPapers(String query, int page, int size) {
        return paperRepository.searchByTitle(query, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Journal> searchJournals(String query, int page, int size) {
        return journalRepository.searchByName(query, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Author> searchAuthors(String query, int page, int size) {
        return authorRepository.searchByName(query, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<Keyword> searchKeywords(String query, int page, int size) {
        return keywordRepository.searchByName(query, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public PaperDto getPaperById(Long id, Long userId) {
        ResearchPaper paper = paperRepository.findById(id).orElse(null);
        if (paper == null) return null;

        PaperDto dto = mapLocalPaperToDto(paper);
        if (userId != null) {
            dto.setIsBookmarked(bookmarkRepository.existsByUserIdAndPaperId(userId, id));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public PaperDto getPaperByExternalId(String externalId, Long userId) {
        ResearchPaper paper = paperRepository.findByExternalId(externalId).orElse(null);
        if (paper == null) return null;

        PaperDto dto = mapLocalPaperToDto(paper);
        if (userId != null) {
            dto.setIsBookmarked(bookmarkRepository.existsByUserIdAndPaperId(userId, paper.getId()));
        }
        return dto;
    }

    private PaperDto mapLocalPaperToDto(ResearchPaper paper) {
        return PaperDto.builder()
                .id(paper.getId())
                .externalId(paper.getExternalId())
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .year(paper.getYear())
                .citationCount(paper.getCitationCount())
                .openAccess(paper.getOpenAccess())
                .paperUri(paper.getPaperUri())
                .authors(paper.getAuthors().stream()
                        .map(a -> AuthorDto.builder()
                                .id(a.getId())
                                .externalId(a.getExternalId())
                                .name(a.getName())
                                .orcid(a.getOrcid())
                                .hIndex(a.getHIndex())
                                .paperCount(a.getPaperCount())
                                .build())
                        .collect(Collectors.toList()))
                .journals(paper.getJournals().stream().map(Journal::getName).collect(Collectors.toList()))
                .keywords(paper.getKeywords().stream().map(Keyword::getName).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public JournalDto getJournalById(Long id, Long userId) {
        Journal journal = journalRepository.findById(id).orElse(null);
        if (journal == null) return null;

        JournalDto dto = JournalDto.builder()
                .id(journal.getId())
                .externalId(journal.getExternalId())
                .name(journal.getName())
                .publisher(journal.getPublisher())
                .issn(journal.getIssn())
                .country(journal.getCountry())
                .homepageUrl(journal.getHomepageUrl())
                .paperCount((long) journal.getPapers().size())
                .build();

        if (userId != null) {
            dto.setIsFollowed(followRepository.existsByUserIdAndJournalId(userId, id));
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public AuthorDto getAuthorById(Long id, Long userId) {
        Author author = authorRepository.findById(id).orElse(null);
        if (author == null) return null;

        AuthorDto dto = AuthorDto.builder()
                .id(author.getId())
                .externalId(author.getExternalId())
                .name(author.getName())
                .orcid(author.getOrcid())
                .hIndex(author.getHIndex())
                .paperCount(author.getPaperCount())
                .build();

        if (userId != null) {
            dto.setIsFollowed(false);
        }
        return dto;
    }

    private void saveRecentSearch(String query, SearchType type, Long userId) {
        if (userId == null || query == null || query.isBlank()) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        RecentSearch search = RecentSearch.builder()
                .user(user)
                .searchQuery(query)
                .searchType(type)
                .build();
        searchRepository.save(search);
    }
}
