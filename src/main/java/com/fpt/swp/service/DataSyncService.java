package com.fpt.swp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSyncService {

    private final OpenAlexService openAlexService;
    private final ResearchPaperRepository paperRepository;
    private final AuthorRepository authorRepository;
    private final JournalRepository journalRepository;
    private final KeywordRepository keywordRepository;
    private final ResearchTopicRepository topicRepository;
    private final PublicationTrendRepository trendRepository;
    private final ApiDataSourceRepository dataSourceRepository;
    private final ObjectMapper objectMapper;

    private static final List<String> DEFAULT_KEYWORDS = List.of(
            "machine learning", "deep learning", "natural language processing",
            "computer vision", "neural network", "artificial intelligence",
            "reinforcement learning", "data mining", "information retrieval",
            "recommender system", "big data", "cloud computing",
            "internet of things", "cybersecurity", "blockchain"
    );

    @Async
    @Transactional
    public void syncPapersForKeyword(String keyword, int limit) {
        log.info("Starting sync for keyword: {}", keyword);
        int offset = 0;
        int fetched = 0;

        try {
            while (fetched < limit) {
                Map<String, Object> response =
                        openAlexService.searchPapersRaw(keyword, offset, 25);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> papers = (List<Map<String, Object>>) response.getOrDefault("papers", Collections.emptyList());

                if (papers.isEmpty()) {
                    break;
                }

                for (Map<String, Object> paperData : papers) {
                    savePaperFromMap(paperData, keyword);
                    fetched++;
                }

                if (papers.size() < 25) {
                    break;
                }

                offset += 25;
                Thread.sleep(1000);
            }

            updateTrendData(keyword);
            log.info("Sync completed for keyword: {}, papers saved: {}", keyword, fetched);

        } catch (Exception e) {
            log.error("Error syncing papers for keyword {}: {}", keyword, e.getMessage());
        }
    }

    @Transactional
    public void savePaperFromMap(Map<String, Object> data, String keyword) {
        String paperId = (String) data.get("paperId");
        String title = (String) data.get("title");

        if (paperId == null || title == null) {
            return;
        }

        ResearchPaper paper = paperRepository.findByExternalId(paperId)
                .orElseGet(ResearchPaper::new);

        paper.setExternalId(paperId);
        paper.setTitle(title);
        paper.setAbstractText((String) data.get("abstract"));

        if (data.get("year") != null) {
            Integer year = data.get("year") instanceof Number
                    ? ((Number) data.get("year")).intValue()
                    : null;
            paper.setYear(year);
        }

        Object citationCount = data.get("citationCount");
        paper.setCitationCount(citationCount != null ? ((Number) citationCount).intValue() : 0);

        Object openAccess = data.get("openAccess");
        paper.setOpenAccess(openAccess != null ? (Boolean) openAccess : false);

        paper.setPaperUri((String) data.get("url"));

        Object journal = data.get("journal");
        if (journal != null) {
            Journal j = journalRepository.findByExternalId(paperId + "_journal")
                    .orElseGet(() -> {
                        Journal jNew = new Journal();
                        jNew.setExternalId(paperId + "_journal");
                        jNew.setName(journal.toString());
                        return journalRepository.save(jNew);
                    });
            paper.getJournals().add(j);
        }

        Object authorsObj = data.get("authors");
        if (authorsObj instanceof List) {
            for (String authorName : (List<String>) authorsObj) {
                if (authorName == null || authorName.isBlank()) continue;
                Author author = authorRepository.findAll().stream()
                        .filter(a -> a.getName().equalsIgnoreCase(authorName))
                        .findFirst()
                        .orElseGet(() -> {
                            Author a = new Author();
                            a.setName(authorName);
                            return authorRepository.save(a);
                        });
                paper.getAuthors().add(author);
            }
        }

        Object keywordsObj = data.get("keywords");
        if (keywordsObj instanceof List) {
            for (String field : (List<String>) keywordsObj) {
                if (field == null || field.isBlank()) continue;
                Keyword kw = keywordRepository.findByName(field.toLowerCase())
                        .orElseGet(() -> {
                            Keyword k = new Keyword();
                            k.setName(field.toLowerCase());
                            k.setDisplayName(field);
                            return keywordRepository.save(k);
                        });
                paper.getKeywords().add(kw);
            }
        }

        Keyword mainKeyword = keywordRepository.findByName(keyword.toLowerCase())
                .orElseGet(() -> {
                    Keyword k = new Keyword();
                    k.setName(keyword.toLowerCase());
                    k.setDisplayName(keyword);
                    return keywordRepository.save(k);
                });
        paper.getKeywords().add(mainKeyword);

        paperRepository.save(paper);
    }

    @Transactional
    public void updateTrendData(String keyword) {
        List<ResearchPaper> papers = paperRepository.findAll().stream()
                .filter(p -> p.getKeywords().stream()
                        .anyMatch(k -> k.getName().equalsIgnoreCase(keyword)))
                .toList();

        Map<Integer, Map<Integer, List<ResearchPaper>>> grouped = papers.stream()
                .filter(p -> p.getYear() != null)
                .collect(Collectors.groupingBy(
                        ResearchPaper::getYear,
                        Collectors.groupingBy(p -> 1)
                ));

        for (Map.Entry<Integer, Map<Integer, List<ResearchPaper>>> yearEntry : grouped.entrySet()) {
            int year = yearEntry.getKey();
            int paperCount = yearEntry.getValue().values().stream()
                    .mapToInt(List::size)
                    .sum();
            int citationCount = yearEntry.getValue().values().stream()
                    .flatMap(List::stream)
                    .mapToInt(p -> p.getCitationCount() != null ? p.getCitationCount() : 0)
                    .sum();
            double avgCitations = paperCount > 0 ? (double) citationCount / paperCount : 0;

            PublicationTrend trend = trendRepository.findByKeywordOrderByYearAscMonthAsc(keyword)
                    .stream()
                    .filter(t -> t.getYear().equals(year) && t.getMonth() == null)
                    .findFirst()
                    .orElse(new PublicationTrend());

            trend.setKeywordName(keyword.toLowerCase());
            trend.setYear(year);
            trend.setMonth(null);
            trend.setPaperCount(paperCount);
            trend.setCitationCount(citationCount);
            trend.setAvgCitations(avgCitations);

            trendRepository.save(trend);
        }
    }

    @Transactional
    public void initializeDefaultDataSource() {
        if (!dataSourceRepository.existsBySourceName("OPENALEX")) {
            ApiDataSource source = ApiDataSource.builder()
                    .sourceName("OPENALEX")
                    .baseUrl("https://api.openalex.org")
                    .rateLimitPerDay(864000)
                    .isActive(true)
                    .lastSyncStatus(null)
                    .recordsSynced(0)
                    .build();
            dataSourceRepository.save(source);
        }
    }

    @Transactional
    public void triggerFullSync(int papersPerKeyword) {
        initializeDefaultDataSource();

        ApiDataSource source = dataSourceRepository.findBySourceName("OPENALEX").orElse(null);
        if (source == null) return;

        for (String keyword : DEFAULT_KEYWORDS) {
            syncPapersForKeyword(keyword, papersPerKeyword);
        }

        source.setLastSyncAt(LocalDateTime.now());
        source.setLastSyncStatus(SyncStatus.SUCCESS);
        source.setRecordsSynced((int) paperRepository.count());
        dataSourceRepository.save(source);
    }
}
