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
    private final SemanticScholarService semanticScholarService;
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
                         SemanticScholarService semanticScholarService,
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
        this.semanticScholarService = semanticScholarService;
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
        List<PaperDto> localApprovedPapers = new ArrayList<>();
        if (request.getPage() == 0) {
            Page<ResearchPaper> localPage;
            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                localPage = paperRepository.searchApprovedLocalPapers(request.getQuery(), PageRequest.of(0, 100));
            } else {
                localPage = paperRepository.findByStatus(PaperStatus.APPROVED, PageRequest.of(0, 100));
            }
            localApprovedPapers = localPage.getContent().stream()
                    .filter(p -> {
                        // Filter by year
                        if (request.getYear() != null && request.getYear() > 0) {
                            if (p.getYear() == null || !p.getYear().equals(request.getYear())) {
                                return false;
                            }
                        }
                        // Filter by year range (from yearFrom to yearTo)
                        if (request.getYearFrom() != null && request.getYearFrom() > 0) {
                            if (p.getYear() == null || p.getYear() < request.getYearFrom()) {
                                return false;
                            }
                        }
                        if (request.getYearTo() != null && request.getYearTo() > 0) {
                            if (p.getYear() == null || p.getYear() > request.getYearTo()) {
                                return false;
                            }
                        }
                        // Filter by journal
                        if (request.getJournal() != null && !request.getJournal().isBlank()) {
                            if (p.getJournals() == null) return false;
                            boolean hasJournal = p.getJournals().stream()
                                    .anyMatch(j -> j.getName() != null && j.getName().toLowerCase().contains(request.getJournal().toLowerCase()));
                            if (!hasJournal) return false;
                        }
                        // Filter by author
                        if (request.getAuthor() != null && !request.getAuthor().isBlank()) {
                            if (p.getAuthors() == null) return false;
                            boolean hasAuthor = p.getAuthors().stream()
                                    .anyMatch(a -> a.getName() != null && a.getName().toLowerCase().contains(request.getAuthor().toLowerCase()));
                            if (!hasAuthor) return false;
                        }
                        return true;
                    })
                    .map(this::mapLocalPaperToDto)
                    .collect(Collectors.toList());

            if (localApprovedPapers.size() > request.getSize()) {
                localApprovedPapers = localApprovedPapers.subList(0, request.getSize());
            }
        }

        Map<String, Object> rawResult = openAlexService.searchPapersRaw(
                request.getQuery(),
                request.getPage() * request.getSize(),
                request.getSize(),
                request.getYear(),
                request.getYearFrom(),
                request.getYearTo(),
                request.getDateFromStr(),
                request.getDateToStr(),
                request.getJournal(),
                request.getAuthor(),
                request.getSortBy()
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawPapers = findPapersList(rawResult);
        long total = findTotal(rawResult);

        if (rawPapers.isEmpty() && request.getQuery() != null && !request.getQuery().isBlank()) {
            log.info("OpenAlex returned 0 papers or rate limited. Falling back to SemanticScholar for query: {}", request.getQuery());
            try {
                rawResult = semanticScholarService.searchPapersRaw(
                        request.getQuery(),
                        request.getPage() * request.getSize(),
                        request.getSize(),
                        request.getSortBy(),
                        request.getYear()
                );
                rawPapers = findPapersList(rawResult);
                total = findTotal(rawResult);
            } catch (Exception e) {
                log.warn("SemanticScholar search fallback failed: {}", e.getMessage());
            }
        }

        if (rawPapers.isEmpty() && request.getQuery() != null && !request.getQuery().isBlank()) {
            log.info("SemanticScholar returned 0 papers or rate limited. Falling back to Crossref for query: {}", request.getQuery());
            try {
                rawResult = searchCrossrefFallback(
                        request.getQuery(),
                        request.getPage() * request.getSize(),
                        request.getSize()
                );
                rawPapers = findPapersList(rawResult);
                total = findTotal(rawResult);
            } catch (Exception e) {
                log.warn("Crossref search fallback failed: {}", e.getMessage());
            }
        }

        List<PaperDto> openAlexPapers = rawPapers.stream()
                .map(p -> mapToPaperDto(p, userId))
                .collect(Collectors.toList());

        List<PaperDto> mergedPapers = new ArrayList<>(localApprovedPapers);
        Set<String> localTitles = localApprovedPapers.stream()
                .map(p -> p.getTitle().toLowerCase().trim())
                .collect(Collectors.toSet());
        for (PaperDto oaPaper : openAlexPapers) {
            if (oaPaper.getTitle() != null && !localTitles.contains(oaPaper.getTitle().toLowerCase().trim())) {
                mergedPapers.add(oaPaper);
            }
        }

        if (mergedPapers.size() > request.getSize()) {
            mergedPapers = mergedPapers.subList(0, request.getSize());
        }

        long adjustedTotal = total + localApprovedPapers.size();

        if (userId != null && request.getQuery() != null && !request.getQuery().isBlank()) {
            saveRecentSearch(request.getQuery(), SearchType.PAPER, userId);
        }

        return PaperSearchResponse.builder()
                .papers(mergedPapers)
                .total(adjustedTotal)
                .page(request.getPage())
                .size(request.getSize())
                .totalPages((int) Math.ceil((double) adjustedTotal / request.getSize()))
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> findPapersList(Map<String, Object> rawResult) {
        if (rawResult.containsKey("papers")) {
            return (List<Map<String, Object>>) rawResult.get("papers");
        }
        if (rawResult.containsKey("results")) {
            return (List<Map<String, Object>>) rawResult.get("results");
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private long findTotal(Map<String, Object> rawResult) {
        if (rawResult.containsKey("total")) {
            Object t = rawResult.get("total");
            if (t instanceof Number) return ((Number) t).longValue();
        }
        if (rawResult.containsKey("meta")) {
            Object meta = rawResult.get("meta");
            if (meta instanceof Map) {
                Object c = ((Map<String, Object>) meta).get("count");
                if (c instanceof Number) return ((Number) c).longValue();
            }
        }
        return 0L;
    }

    private Map<String, Object> searchCrossrefFallback(String query, int offset, int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            String url = "https://api.crossref.org/works?query=" + encodedQuery + "&offset=" + offset + "&rows=" + limit;
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String responseBody = restTemplate.getForObject(url, String.class);
            if (responseBody != null) {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
                if (root.has("message")) {
                    com.fasterxml.jackson.databind.JsonNode msg = root.get("message");
                    long totalCount = msg.has("total-results") ? msg.get("total-results").asLong() : 0;
                    result.put("total", totalCount);
                    List<Map<String, Object>> papers = new ArrayList<>();
                    com.fasterxml.jackson.databind.JsonNode items = msg.get("items");
                    if (items != null && items.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode item : items) {
                            Map<String, Object> p = new LinkedHashMap<>();
                            String doi = item.has("DOI") ? item.get("DOI").asText() : null;
                            p.put("paperId", doi != null ? "https://doi.org/" + doi : null);
                            String title = "Untitled";
                            if (item.has("title") && item.get("title").isArray() && item.get("title").size() > 0) {
                                title = item.get("title").get(0).asText();
                            }
                            p.put("title", title);
                            String abs = null;
                            if (item.has("abstract") && !item.get("abstract").isNull()) {
                                abs = item.get("abstract").asText().replaceAll("<[^>]*>", "");
                            }
                            p.put("abstract", abs);
                            Integer year = null;
                            if (item.has("created") && item.get("created").has("date-parts")) {
                                com.fasterxml.jackson.databind.JsonNode dp = item.get("created").get("date-parts");
                                if (dp.isArray() && dp.size() > 0 && dp.get(0).isArray() && dp.get(0).size() > 0) {
                                    year = dp.get(0).get(0).asInt();
                                }
                            }
                            p.put("year", year);
                            p.put("citationCount", item.has("is-referenced-by-count") ? item.get("is-referenced-by-count").asInt() : 0);
                            p.put("openAccess", true);
                            p.put("url", item.has("URL") ? item.get("URL").asText() : (doi != null ? "https://doi.org/" + doi : null));

                            List<String> authors = new ArrayList<>();
                            if (item.has("author") && item.get("author").isArray()) {
                                for (com.fasterxml.jackson.databind.JsonNode auth : item.get("author")) {
                                    String given = auth.has("given") ? auth.get("given").asText() : "";
                                    String family = auth.has("family") ? auth.get("family").asText() : "";
                                    String name = (given + " " + family).trim();
                                    if (!name.isEmpty()) authors.add(name);
                                }
                            }
                            p.put("authors", authors);
                            if (item.has("container-title") && item.get("container-title").isArray() && item.get("container-title").size() > 0) {
                                p.put("journal", item.get("container-title").get(0).asText());
                            }
                            papers.add(p);
                        }
                    }
                    result.put("papers", papers);
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("Crossref fallback search failed: {}", e.getMessage());
        }
        result.put("total", 0L);
        result.put("papers", Collections.emptyList());
        return result;
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
                .status(paper.getStatus() != null ? paper.getStatus().name() : null)
                .uploadedBy(paper.getUploadedBy() != null ? paper.getUploadedBy().getMail() : null)
                .isSelfPublished(paper.getIsSelfPublished())
                .statusComments(paper.getStatusComments())
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
