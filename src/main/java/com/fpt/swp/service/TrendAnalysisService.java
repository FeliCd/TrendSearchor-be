package com.fpt.swp.service;

import com.fpt.swp.dto.*;
import com.fpt.swp.model.Keyword;
import com.fpt.swp.model.PublicationTrend;
import com.fpt.swp.repository.KeywordRepository;
import com.fpt.swp.repository.PublicationTrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendAnalysisService {

    private final OpenAlexService openAlexService;
    private final PublicationTrendRepository trendRepository;
    private final KeywordRepository keywordRepository;
    private final TrendCalculator calc;

    private static final int DEFAULT_MIN_YEAR = 2015;
    private static final int DEFAULT_MAX_YEAR = java.time.Year.now().getValue();

    public TrendAnalysisDto analyzeKeyword(String keyword, Integer startYear, Integer endYear) {
        String kw = keyword.trim().toLowerCase();
        int start = startYear != null && startYear > 2000 ? startYear : DEFAULT_MIN_YEAR;
        int end = endYear != null && endYear > 2000 ? endYear : DEFAULT_MAX_YEAR;

        List<YearlyData> yearlyData = fetchYearlyData(kw, start, end);

        TrendAnalysisDto.TopicStatus status = calc.classifyStatus(yearlyData);
        Double growthRate = calc.calculateLatestGrowthRate(yearlyData);
        Double cumulativeGrowth = calc.calculateCumulativeGrowth(yearlyData);
        Double[] yoyGrowth = calc.calculateYoYGrowth(yearlyData);
        Double momentum = calc.calculateMomentum(yearlyData);
        Double forecastNextYear = calc.forecastNextYear(yearlyData, end);
        Double forecastConfidence = calc.calculateForecastConfidence(yearlyData);
        String insight = calc.generateInsight(kw, yearlyData, status, growthRate, cumulativeGrowth, momentum);

        int totalPapers = yearlyData.stream().mapToInt(y -> y.getPaperCount() != null ? y.getPaperCount() : 0).sum();
        int totalCitations = yearlyData.stream().mapToInt(y -> y.getCitationCount() != null ? y.getCitationCount() : 0).sum();
        Integer peakYear = yearlyData.stream().filter(y -> y.getPaperCount() != null)
                .max(Comparator.comparingInt(y -> y.getPaperCount())).map(YearlyData::getYear).orElse(null);
        int peakPaperCount = yearlyData.stream().filter(y -> y.getPaperCount() != null)
                .mapToInt(y -> y.getPaperCount()).max().orElse(0);

        return TrendAnalysisDto.builder()
                .keyword(kw).displayName(keyword.trim()).yearlyData(yearlyData)
                .growthRate(growthRate).cumulativeGrowth(cumulativeGrowth).yoyGrowth(yoyGrowth)
                .status(status).statusLabel(calc.getStatusLabel(status))
                .momentum(momentum).insight(insight)
                .forecastNextYear(forecastNextYear).forecastConfidence(forecastConfidence)
                .totalPapers(totalPapers).totalCitations(totalCitations)
                .peakYear(peakYear).peakPaperCount(peakPaperCount).build();
    }

    public List<TrendingTopicDto> getTrendingTopicsRanking(int limit) {
        List<Keyword> keywords = keywordRepository.findAllOrderByName(
                org.springframework.data.domain.PageRequest.of(0, limit * 2)).getContent();
        List<TrendingTopicDto> results = new ArrayList<>();

        for (Keyword kw : keywords) {
            try {
                TrendAnalysisDto analysis = analyzeKeyword(kw.getName(), null, null);
                if (analysis.getTotalPapers() == 0) continue;
                double trendScore = calc.calculateTrendScore(analysis);
                results.add(buildTopicDto(analysis, trendScore));
            } catch (Exception e) {
                log.warn("Failed to analyze keyword '{}': {}", kw.getName(), e.getMessage());
            }
        }

        results.sort((a, b) -> Double.compare(
                b.getTrendScore() != null ? b.getTrendScore() : 0,
                a.getTrendScore() != null ? a.getTrendScore() : 0));
        for (int i = 0; i < results.size(); i++) results.get(i).setRank(i + 1);
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    public List<TrendingTopicDto> getEmergingTopics(int limit) {
        List<Keyword> keywords = keywordRepository.findAllOrderByName(
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
        List<TrendingTopicDto> candidates = new ArrayList<>();

        for (Keyword kw : keywords) {
            try {
                TrendAnalysisDto analysis = analyzeKeyword(kw.getName(), null, null);
                if (analysis.getYearlyData().isEmpty()) continue;

                double recentGrowth = calc.sumRecentPapers(analysis.getYearlyData(), 2);
                double historicalAvg = calc.averageHistoricalPapers(analysis.getYearlyData(), 3);
                boolean isEmerging = historicalAvg < 100 && recentGrowth > 50
                        && analysis.getGrowthRate() != null && analysis.getGrowthRate() > 0.5;

                if (isEmerging) {
                    candidates.add(buildTopicDto(analysis, calc.calculateTrendScore(analysis))
                            .toBuilder()
                            .status(TrendAnalysisDto.TopicStatus.EMERGING)
                            .statusLabel("Emerging")
                            .recentPapers((int) recentGrowth).build());
                }
            } catch (Exception ignored) { }
        }

        candidates.sort((a, b) -> Double.compare(
                b.getTrendScore() != null ? b.getTrendScore() : 0,
                a.getTrendScore() != null ? a.getTrendScore() : 0));
        for (int i = 0; i < candidates.size(); i++) candidates.get(i).setRank(i + 1);
        return candidates.stream().limit(limit).collect(Collectors.toList());
    }

    public TopicComparisonDto compareTopics(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return TopicComparisonDto.builder()
                    .keywords(Collections.emptyList()).years(Collections.emptyList())
                    .yearlyDataMap(Collections.emptyMap()).insight("").build();
        }

        Map<String, List<YearlyData>> dataMap = new LinkedHashMap<>();
        Set<Integer> allYears = new TreeSet<>();
        List<Double> growthRates = new ArrayList<>();
        String maxGrowthKeyword = keywords.get(0);
        double maxGrowth = 0;

        for (String kw : keywords) {
            TrendAnalysisDto analysis = analyzeKeyword(kw, null, null);
            dataMap.put(kw.toLowerCase(), analysis.getYearlyData());
            analysis.getYearlyData().forEach(yd -> { if (yd.getYear() != null) allYears.add(yd.getYear()); });
            if (analysis.getGrowthRate() != null && analysis.getGrowthRate() > maxGrowth) {
                maxGrowth = analysis.getGrowthRate();
                maxGrowthKeyword = kw;
            }
            growthRates.add(analysis.getGrowthRate() != null ? analysis.getGrowthRate() : 0.0);
        }

        String insight = calc.generateComparisonInsight(keywords, dataMap, maxGrowthKeyword, maxGrowth);
        List<Integer> sortedYears = new ArrayList<>(allYears);

        return TopicComparisonDto.builder()
                .keywords(keywords.stream().map(String::toLowerCase).collect(Collectors.toList()))
                .years(sortedYears).yearlyDataMap(dataMap).insight(insight)
                .maxGrowthKeyword(maxGrowthKeyword)
                .maxGrowthLabel(maxGrowthKeyword + " (+" + String.format("%.0f", maxGrowth * 100) + "%)")
                .keywordTotalPapers(dataMap.entrySet().stream()
                        .mapToDouble(e -> e.getValue().stream().mapToInt(y -> y.getPaperCount() != null ? y.getPaperCount() : 0).sum())
                        .boxed().toArray(Double[]::new))
                .keywordGrowthRates(growthRates.stream().mapToDouble(Double::doubleValue).boxed().toArray(Double[]::new))
                .build();
    }

    public Map<String, Object> getKeywordCooccurrence(String keyword, int maxResults) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword.toLowerCase());
        result.put("relatedKeywords", new ArrayList<Map<String, Object>>());

        try {
            Map<String, Object> response = openAlexService.searchPapersRaw(keyword, 0, 50);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> papers = (List<Map<String, Object>>) response.getOrDefault("papers", Collections.emptyList());
            Map<String, Integer> cooccurrenceCount = new HashMap<>();
            Set<String> normalizedKw = new HashSet<>(List.of(keyword.toLowerCase().split("\\s+")));

            for (Map<String, Object> paper : papers) {
                Set<String> paperKeywords = extractKeywords(paper);
                for (String pk : paperKeywords) {
                    if (!normalizedKw.contains(pk) && pk.length() > 4) {
                        cooccurrenceCount.merge(pk, 1, Integer::sum);
                    }
                }
            }

            List<Map<String, Object>> topRelated = cooccurrenceCount.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(maxResults)
                    .map(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("keyword", e.getKey());
                        item.put("cooccurrenceCount", e.getValue());
                        item.put("relevance", Math.min(1.0, (double) e.getValue() / 10));
                        return item;
                    }).collect(Collectors.toList());

            result.put("relatedKeywords", topRelated);
            result.put("totalPapers", papers.size());
        } catch (Exception e) {
            log.error("Failed to get keyword co-occurrence for '{}': {}", keyword, e.getMessage());
        }
        return result;
    }

    public TrendAnalysisDto searchAndAnalyze(String query) {
        return analyzeKeyword(query, null, null);
    }

    // ==================== PRIVATE HELPERS ====================

    private Set<String> extractKeywords(Map<String, Object> paper) {
        Set<String> keywords = new HashSet<>();
        String[] sources = {
                (String) paper.get("abstract"),
                (String) paper.get("title")
        };
        for (String src : sources) {
            if (src != null) {
                for (String word : src.toLowerCase().split("\\s+")) {
                    word = word.replaceAll("[^a-z]", "");
                    if (word.length() > 4) keywords.add(word);
                }
            }
        }
        Object kwObj = paper.get("keywords");
        if (kwObj instanceof List<?> kwList) {
            for (Object kw : kwList) {
                if (kw instanceof String kwStr) {
                    for (String w : kwStr.toLowerCase().split("\\s+")) keywords.add(w);
                }
            }
        }
        return keywords;
    }

    private List<YearlyData> fetchYearlyData(String keyword, int startYear, int endYear) {
        List<PublicationTrend> dbTrends = trendRepository.findByKeywordSinceYear(keyword, startYear);
        List<YearlyData> yearlyData = new ArrayList<>();

        if (!dbTrends.isEmpty()) {
            Map<Integer, List<PublicationTrend>> byYear = dbTrends.stream()
                    .collect(Collectors.groupingBy(PublicationTrend::getYear));
            for (Map.Entry<Integer, List<PublicationTrend>> entry : byYear.entrySet()) {
                int year = entry.getKey();
                if (year > endYear) continue;
                int paperCount = entry.getValue().stream().mapToInt(t -> t.getPaperCount() != null ? t.getPaperCount() : 0).sum();
                int citationCount = entry.getValue().stream().mapToInt(t -> t.getCitationCount() != null ? t.getCitationCount() : 0).sum();
                yearlyData.add(YearlyData.builder().year(year).paperCount(paperCount)
                        .citationCount(citationCount).avgCitations(paperCount > 0 ? (double) citationCount / paperCount : 0.0).yoyGrowth(null).build());
            }
        }

        if (yearlyData.isEmpty()) {
            Map<String, Object> oaResult = openAlexService.getWorksCountByYear(keyword, startYear, endYear);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> yearlyList = (List<Map<String, Object>>) oaResult.getOrDefault("yearlyData", Collections.emptyList());
            int prevCount = 0;
            for (Map<String, Object> item : yearlyList) {
                Integer year = item.get("year") instanceof Number n ? n.intValue() : null;
                Integer count = item.get("count") instanceof Number n ? n.intValue() : 0;
                if (year == null || year > endYear) continue;
                Double yoyGrowth = prevCount > 0 && count > 0 ? ((double) count - prevCount) / prevCount : null;
                yearlyData.add(YearlyData.builder().year(year).paperCount(count).citationCount(0).avgCitations(0.0).yoyGrowth(yoyGrowth).build());
                prevCount = count;
            }
        }

        yearlyData.sort(Comparator.comparing(y -> y.getYear() != null ? y.getYear() : 0));
        return yearlyData;
    }

    private TrendingTopicDto buildTopicDto(TrendAnalysisDto analysis, double trendScore) {
        return TrendingTopicDto.builder()
                .keyword(analysis.getKeyword()).displayName(analysis.getDisplayName())
                .trendScore(trendScore).growthRate(analysis.getGrowthRate())
                .recentPapers(calc.sumRecentPapers(analysis.getYearlyData(), 2))
                .totalPapers(analysis.getTotalPapers()).totalCitations(analysis.getTotalCitations())
                .avgCitations(analysis.getTotalPapers() > 0
                        ? (double) analysis.getTotalCitations() / analysis.getTotalPapers() : 0.0)
                .status(analysis.getStatus()).statusLabel(analysis.getStatusLabel())
                .momentum(analysis.getMomentum()).rank(0).build();
    }
}
