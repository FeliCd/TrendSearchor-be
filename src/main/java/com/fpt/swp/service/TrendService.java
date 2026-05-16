package com.fpt.swp.service;

import com.fpt.swp.dto.KeywordTrendDto;
import com.fpt.swp.dto.TopicComparisonDto;
import com.fpt.swp.dto.TrendAnalysisDto;
import com.fpt.swp.dto.TrendDataDto;
import com.fpt.swp.dto.TrendingTopicDto;
import com.fpt.swp.dto.YearlyStatsDto;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendService {

    private final PublicationTrendRepository trendRepository;
    private final ResearchPaperRepository paperRepository;
    private final KeywordRepository keywordRepository;
    private final ResearchTopicRepository topicRepository;
    private final UserFollowRepository followRepository;
    private final BookmarkRepository bookmarkRepository;
    private final TrendAnalysisService trendAnalysisService;

    @Transactional(readOnly = true)
    public List<TrendDataDto> getTrendByKeyword(String keyword) {
        List<PublicationTrend> trends = trendRepository.findByKeywordOrderByYearAscMonthAsc(keyword.toLowerCase());
        return trends.stream()
                .map(t -> TrendDataDto.builder()
                        .keyword(t.getKeywordName())
                        .year(t.getYear())
                        .month(t.getMonth())
                        .paperCount(t.getPaperCount())
                        .citationCount(t.getCitationCount())
                        .avgCitations(t.getAvgCitations())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrendDataDto> getTrendByKeywordSinceYear(String keyword, int startYear) {
        List<PublicationTrend> trends = trendRepository.findByKeywordSinceYear(keyword.toLowerCase(), startYear);
        return trends.stream()
                .map(t -> TrendDataDto.builder()
                        .keyword(t.getKeywordName())
                        .year(t.getYear())
                        .month(t.getMonth())
                        .paperCount(t.getPaperCount())
                        .citationCount(t.getCitationCount())
                        .avgCitations(t.getAvgCitations())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TrendDataDto> compareTrends(List<String> keywords) {
        List<PublicationTrend> trends = trendRepository.findByKeywordInOrderByYearAscMonthAsc(
                keywords.stream().map(String::toLowerCase).collect(Collectors.toList()));

        return trends.stream()
                .map(t -> TrendDataDto.builder()
                        .keyword(t.getKeywordName())
                        .year(t.getYear())
                        .month(t.getMonth())
                        .paperCount(t.getPaperCount())
                        .citationCount(t.getCitationCount())
                        .avgCitations(t.getAvgCitations())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KeywordTrendDto> getTopTrendingKeywords(int limit, Long userId) {
        List<Keyword> allKeywords = keywordRepository.findAll();
        List<KeywordTrendDto> result = new ArrayList<>();

        for (Keyword kw : allKeywords) {
            List<PublicationTrend> trends = trendRepository.findByKeywordOrderByYearAscMonthAsc(kw.getName());

            if (trends.isEmpty()) continue;

            Integer[] yearlyPapers = new Integer[10];
            int totalPapers = 0;
            int lastYearPapers = 0;

            for (PublicationTrend t : trends) {
                int idx = Math.min(9, Math.max(0, 2024 - t.getYear()));
                yearlyPapers[idx] = t.getPaperCount();
                totalPapers += t.getPaperCount();
                lastYearPapers = t.getPaperCount();
            }

            double growthRate = 0.0;
            for (int i = 1; i < yearlyPapers.length; i++) {
                if (yearlyPapers[i] != null && yearlyPapers[i - 1] != null && yearlyPapers[i - 1] > 0) {
                    growthRate = ((double) yearlyPapers[i] - yearlyPapers[i - 1]) / yearlyPapers[i - 1];
                    break;
                }
            }

            KeywordTrendDto dto = KeywordTrendDto.builder()
                    .keyword(kw.getName())
                    .displayName(kw.getDisplayName() != null ? kw.getDisplayName() : kw.getName())
                    .paperCount((long) totalPapers)
                    .growthRate(growthRate)
                    .yearlyPapers(yearlyPapers)
                    .build();

            if (userId != null) {
                kw.getPapers().size();
            }

            result.add(dto);
        }

        result.sort((a, b) -> Long.compare(b.getPaperCount(), a.getPaperCount()));
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KeywordTrendDto> getTrendingKeywords(int limit) {
        List<Keyword> keywords = keywordRepository.findAllOrderByName(PageRequest.of(0, limit)).getContent();
        return keywords.stream()
                .map(kw -> {
                    long paperCount = paperRepository.findByKeywordId(kw.getId(), PageRequest.of(0, 1)).getTotalElements();
                    return KeywordTrendDto.builder()
                            .keyword(kw.getName())
                            .displayName(kw.getDisplayName() != null ? kw.getDisplayName() : kw.getName())
                            .paperCount(paperCount)
                            .growthRate(0.0)
                            .yearlyPapers(new Integer[10])
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ResearchTopic> getTrendingTopics(int page, int size) {
        return topicRepository.findTopByPopularity(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<YearlyStatsDto> getYearlyStats() {
        List<Integer> years = paperRepository.findDistinctYears();
        List<YearlyStatsDto> stats = new ArrayList<>();

        for (Integer year : years) {
            Long count = paperRepository.countByYear(year);
            stats.add(YearlyStatsDto.builder()
                    .year(year)
                    .paperCount(count)
                    .citationCount(0L)
                    .avgCitations(0.0)
                    .build());
        }

        stats.sort(Comparator.comparing(YearlyStatsDto::getYear));
        return stats;
    }

    // === Phase 1: Publication Trend (MVP) ===

    /**
     * Full analysis of a keyword: trend data, growth rates, status, insight, forecast.
     */
    @Transactional(readOnly = true)
    public TrendAnalysisDto getPublicationTrend(String keyword, Integer startYear, Integer endYear) {
        return trendAnalysisService.analyzeKeyword(keyword, startYear, endYear);
    }

    /**
     * Search and analyze a keyword in real-time from OpenAlex.
     */
    @Transactional(readOnly = true)
    public TrendAnalysisDto searchAndAnalyze(String query) {
        return trendAnalysisService.searchAndAnalyze(query);
    }

    // === Phase 2: Trending Topics ===

    /**
     * Get topic ranking with TrendScore, growth rate, status.
     */
    @Transactional(readOnly = true)
    public List<TrendingTopicDto> getTopicRanking(int limit) {
        return trendAnalysisService.getTrendingTopicsRanking(limit);
    }

    /**
     * Get topics that are emerging (low historical + high recent growth).
     */
    @Transactional(readOnly = true)
    public List<TrendingTopicDto> getEmergingTopics(int limit) {
        return trendAnalysisService.getEmergingTopics(limit);
    }

    /**
     * Compare multiple topics side by side with auto-generated insight.
     */
    @Transactional(readOnly = true)
    public TopicComparisonDto compareTopics(List<String> keywords) {
        return trendAnalysisService.compareTopics(keywords);
    }

    // === Phase 3: Keyword Relations ===

    /**
     * Get keyword co-occurrence data for network graph.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getKeywordCooccurrence(String keyword, int maxResults) {
        return trendAnalysisService.getKeywordCooccurrence(keyword, maxResults);
    }
}
