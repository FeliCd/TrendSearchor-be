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

    private static final int DEFAULT_MIN_YEAR = 2015;
    private static final int DEFAULT_MAX_YEAR = java.time.Year.now().getValue();
    private static final int CITATION_HISTORY_YEARS = 5;

    // --- Trend Analysis for a keyword ---

    public TrendAnalysisDto analyzeKeyword(String keyword, Integer startYear, Integer endYear) {
        String kw = keyword.trim().toLowerCase();
        int start = startYear != null && startYear > 2000 ? startYear : DEFAULT_MIN_YEAR;
        int end = endYear != null && endYear > 2000 ? endYear : DEFAULT_MAX_YEAR;

        // Step 1: Try to get from DB cache first, then fallback to OpenAlex
        List<YearlyData> yearlyData = fetchYearlyData(kw, start, end);

        // Step 2: Calculate metrics
        TrendAnalysisDto.TopicStatus status = classifyStatus(yearlyData);
        String statusLabel = getStatusLabel(status);
        Double growthRate = calculateLatestGrowthRate(yearlyData);
        Double cumulativeGrowth = calculateCumulativeGrowth(yearlyData);
        Double[] yoyGrowth = calculateYoYGrowth(yearlyData);
        Double momentum = calculateMomentum(yearlyData);
        Double forecastNextYear = forecastNextYear(yearlyData, end);
        Double forecastConfidence = calculateForecastConfidence(yearlyData);
        String insight = generateInsight(keyword.trim(), yearlyData, status, growthRate, cumulativeGrowth, momentum);

        // Stats
        int totalPapers = yearlyData.stream().mapToInt(y -> y.getPaperCount() != null ? y.getPaperCount() : 0).sum();
        int totalCitations = yearlyData.stream().mapToInt(y -> y.getCitationCount() != null ? y.getCitationCount() : 0).sum();
        Integer peakYear = yearlyData.stream()
                .filter(y -> y.getPaperCount() != null)
                .max(Comparator.comparingInt(y -> y.getPaperCount()))
                .map(YearlyData::getYear)
                .orElse(null);
        int peakPaperCount = yearlyData.stream()
                .filter(y -> y.getPaperCount() != null)
                .mapToInt(y -> y.getPaperCount())
                .max()
                .orElse(0);

        return TrendAnalysisDto.builder()
                .keyword(kw)
                .displayName(keyword.trim())
                .yearlyData(yearlyData)
                .growthRate(growthRate)
                .cumulativeGrowth(cumulativeGrowth)
                .yoyGrowth(yoyGrowth)
                .status(status)
                .statusLabel(statusLabel)
                .momentum(momentum)
                .insight(insight)
                .forecastNextYear(forecastNextYear)
                .forecastConfidence(forecastConfidence)
                .totalPapers(totalPapers)
                .totalCitations(totalCitations)
                .peakYear(peakYear)
                .peakPaperCount(peakPaperCount)
                .build();
    }

    // --- Trending Topics Ranking ---

    public List<TrendingTopicDto> getTrendingTopicsRanking(int limit) {
        // Step 1: Get top keywords from DB by paper count
        List<Keyword> keywords = keywordRepository.findAllOrderByName(
                org.springframework.data.domain.PageRequest.of(0, limit * 2)).getContent();

        List<TrendingTopicDto> results = new ArrayList<>();

        for (Keyword kw : keywords) {
            try {
                TrendAnalysisDto analysis = analyzeKeyword(kw.getName(), null, null);
                if (analysis.getTotalPapers() == 0) continue;

                double trendScore = calculateTrendScore(analysis);

                results.add(TrendingTopicDto.builder()
                        .keyword(analysis.getKeyword())
                        .displayName(analysis.getDisplayName())
                        .trendScore(trendScore)
                        .growthRate(analysis.getGrowthRate())
                        .recentPapers(sumRecentPapers(analysis.getYearlyData(), 2))
                        .totalPapers(analysis.getTotalPapers())
                        .totalCitations(analysis.getTotalCitations())
                        .avgCitations(analysis.getTotalPapers() > 0
                                ? (double) analysis.getTotalCitations() / analysis.getTotalPapers() : 0.0)
                        .status(analysis.getStatus())
                        .statusLabel(analysis.getStatusLabel())
                        .momentum(analysis.getMomentum())
                        .rank(0)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to analyze keyword '{}': {}", kw.getName(), e.getMessage());
            }
        }

        // Sort by trend score descending, then assign ranks
        results.sort((a, b) -> Double.compare(
                b.getTrendScore() != null ? b.getTrendScore() : 0,
                a.getTrendScore() != null ? a.getTrendScore() : 0));

        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        return results.stream().limit(limit).collect(Collectors.toList());
    }

    // --- Emerging Topics Detection ---

    public List<TrendingTopicDto> getEmergingTopics(int limit) {
        // Get all keywords and analyze them
        List<Keyword> keywords = keywordRepository.findAllOrderByName(
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();

        List<TrendingTopicDto> candidates = new ArrayList<>();

        for (Keyword kw : keywords) {
            try {
                TrendAnalysisDto analysis = analyzeKeyword(kw.getName(), null, null);
                if (analysis.getYearlyData().isEmpty()) continue;

                double recentGrowth = sumRecentPapers(analysis.getYearlyData(), 2);
                double historicalAvg = averageHistoricalPapers(analysis.getYearlyData(), 3);

                // Emerging criteria: low historical + high recent acceleration
                boolean isEmerging = historicalAvg < 100 && recentGrowth > 50
                        && analysis.getGrowthRate() != null && analysis.getGrowthRate() > 0.5;

                if (isEmerging) {
                    double trendScore = calculateTrendScore(analysis);
                    candidates.add(TrendingTopicDto.builder()
                            .keyword(analysis.getKeyword())
                            .displayName(analysis.getDisplayName())
                            .trendScore(trendScore)
                            .growthRate(analysis.getGrowthRate())
                            .recentPapers((int) recentGrowth)
                            .totalPapers(analysis.getTotalPapers())
                            .totalCitations(analysis.getTotalCitations())
                            .avgCitations(analysis.getTotalPapers() > 0
                                    ? (double) analysis.getTotalCitations() / analysis.getTotalPapers() : 0.0)
                            .status(TrendAnalysisDto.TopicStatus.EMERGING)
                            .statusLabel("Emerging")
                            .momentum(analysis.getMomentum())
                            .rank(0)
                            .build());
                }
            } catch (Exception e) {
                // Skip failed analyses silently
            }
        }

        candidates.sort((a, b) -> Double.compare(
                b.getTrendScore() != null ? b.getTrendScore() : 0,
                a.getTrendScore() != null ? a.getTrendScore() : 0));

        for (int i = 0; i < candidates.size(); i++) {
            candidates.get(i).setRank(i + 1);
        }

        return candidates.stream().limit(limit).collect(Collectors.toList());
    }

    // --- Topic Comparison ---

    public TopicComparisonDto compareTopics(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return TopicComparisonDto.builder()
                    .keywords(Collections.emptyList())
                    .years(Collections.emptyList())
                    .yearlyDataMap(Collections.emptyMap())
                    .insight("")
                    .build();
        }

        Map<String, List<YearlyData>> dataMap = new LinkedHashMap<>();
        Set<Integer> allYears = new TreeSet<>();
        List<Double> growthRates = new ArrayList<>();
        String maxGrowthKeyword = keywords.get(0);
        double maxGrowth = 0;

        for (String kw : keywords) {
            TrendAnalysisDto analysis = analyzeKeyword(kw, null, null);
            dataMap.put(kw.toLowerCase(), analysis.getYearlyData());
            analysis.getYearlyData().forEach(yd -> {
                if (yd.getYear() != null) allYears.add(yd.getYear());
            });
            if (analysis.getGrowthRate() != null && analysis.getGrowthRate() > maxGrowth) {
                maxGrowth = analysis.getGrowthRate();
                maxGrowthKeyword = kw;
            }
            growthRates.add(analysis.getGrowthRate() != null ? analysis.getGrowthRate() : 0.0);
        }

        String insight = generateComparisonInsight(keywords, dataMap, maxGrowthKeyword, maxGrowth);
        List<Integer> sortedYears = new ArrayList<>(allYears);

        return TopicComparisonDto.builder()
                .keywords(keywords.stream().map(String::toLowerCase).collect(Collectors.toList()))
                .years(sortedYears)
                .yearlyDataMap(dataMap)
                .insight(insight)
                .maxGrowthKeyword(maxGrowthKeyword)
                .maxGrowthLabel(maxGrowthKeyword + " (+" + String.format("%.0f", maxGrowth * 100) + "%)")
                .keywordTotalPapers(dataMap.entrySet().stream()
                        .mapToDouble(e -> e.getValue().stream().mapToInt(y -> y.getPaperCount() != null ? y.getPaperCount() : 0).sum())
                        .boxed()
                        .toArray(Double[]::new))
                .keywordGrowthRates(growthRates.stream()
                        .mapToDouble(Double::doubleValue)
                        .boxed()
                        .toArray(Double[]::new))
                .build();
    }

    // --- Keyword Co-occurrence (for network graph) ---

    public Map<String, Object> getKeywordCooccurrence(String keyword, int maxResults) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword.toLowerCase());
        result.put("relatedKeywords", new ArrayList<Map<String, Object>>());

        try {
            // Fetch papers from OpenAlex containing the keyword
            Map<String, Object> response = openAlexService.searchPapersRaw(keyword, 0, 50);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> papers = (List<Map<String, Object>>) response.getOrDefault("papers", Collections.emptyList());

            // Count keyword co-occurrences from titles, abstracts, and topics
            Map<String, Integer> cooccurrenceCount = new HashMap<>();
            Set<String> normalizedKw = new HashSet<>(List.of(keyword.toLowerCase().split("\\s+")));

            for (Map<String, Object> paper : papers) {
                Set<String> paperKeywords = new HashSet<>();

                // From abstract
                Object abstractObj = paper.get("abstract");
                if (abstractObj instanceof String abstractText && abstractText != null) {
                    String[] words = abstractText.toLowerCase().split("\\s+");
                    for (String word : words) {
                        word = word.replaceAll("[^a-z]", "");
                        if (word.length() > 4) paperKeywords.add(word);
                    }
                }

                // From title
                Object titleObj = paper.get("title");
                if (titleObj instanceof String title && title != null) {
                    String[] words = title.toLowerCase().split("\\s+");
                    for (String word : words) {
                        word = word.replaceAll("[^a-z]", "");
                        if (word.length() > 4) paperKeywords.add(word);
                    }
                }

                // From keywords/tags
                Object keywordsObj = paper.get("keywords");
                if (keywordsObj instanceof List<?> kwList) {
                    for (Object kw : kwList) {
                        if (kw instanceof String kwStr) {
                            paperKeywords.addAll(List.of(kwStr.toLowerCase().split("\\s+")));
                        }
                    }
                }

                // Count co-occurrences (excluding the main keyword)
                for (String pk : paperKeywords) {
                    if (!normalizedKw.contains(pk) && pk.length() > 4) {
                        cooccurrenceCount.merge(pk, 1, Integer::sum);
                    }
                }
            }

            // Sort and take top N
            final int limit = maxResults;
            List<Map<String, Object>> topRelated = cooccurrenceCount.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(limit)
                    .map(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("keyword", e.getKey());
                        item.put("cooccurrenceCount", e.getValue());
                        item.put("relevance", Math.min(1.0, (double) e.getValue() / 10));
                        return item;
                    })
                    .collect(Collectors.toList());

            result.put("relatedKeywords", topRelated);
            result.put("totalPapers", papers.size());

        } catch (Exception e) {
            log.error("Failed to get keyword co-occurrence for '{}': {}", keyword, e.getMessage());
        }

        return result;
    }

    // --- Search keyword (real-time from OpenAlex) ---

    public TrendAnalysisDto searchAndAnalyze(String query) {
        return analyzeKeyword(query, null, null);
    }

    // ==================== PRIVATE HELPERS ====================

    private List<YearlyData> fetchYearlyData(String keyword, int startYear, int endYear) {
        // Try DB first
        List<PublicationTrend> dbTrends = trendRepository.findByKeywordSinceYear(keyword, startYear);
        List<YearlyData> yearlyData = new ArrayList<>();

        if (!dbTrends.isEmpty()) {
            Map<Integer, List<PublicationTrend>> byYear = dbTrends.stream()
                    .collect(Collectors.groupingBy(PublicationTrend::getYear));

            for (Map.Entry<Integer, List<PublicationTrend>> entry : byYear.entrySet()) {
                int year = entry.getKey();
                if (year > endYear) continue;

                int paperCount = entry.getValue().stream()
                        .mapToInt(t -> t.getPaperCount() != null ? t.getPaperCount() : 0)
                        .sum();
                int citationCount = entry.getValue().stream()
                        .mapToInt(t -> t.getCitationCount() != null ? t.getCitationCount() : 0)
                        .sum();
                double avgCitations = paperCount > 0 ? (double) citationCount / paperCount : 0.0;

                yearlyData.add(YearlyData.builder()
                        .year(year)
                        .paperCount(paperCount)
                        .citationCount(citationCount)
                        .avgCitations(avgCitations)
                        .yoyGrowth(null)
                        .build());
            }
        }

        // If DB has no data, fetch from OpenAlex
        if (yearlyData.isEmpty()) {
            Map<String, Object> oaResult = openAlexService.getWorksCountByYear(keyword, startYear, endYear);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> yearlyList = (List<Map<String, Object>>) oaResult.getOrDefault("yearlyData", Collections.emptyList());

            int prevCount = 0;
            for (Map<String, Object> item : yearlyList) {
                Integer year = item.get("year") instanceof Number n ? n.intValue() : null;
                Integer count = item.get("count") instanceof Number n ? n.intValue() : 0;
                if (year == null || year > endYear) continue;

                Double yoyGrowth = null;
                if (prevCount > 0 && count > 0) {
                    yoyGrowth = ((double) count - prevCount) / prevCount;
                }

                yearlyData.add(YearlyData.builder()
                        .year(year)
                        .paperCount(count)
                        .citationCount(0)
                        .avgCitations(0.0)
                        .yoyGrowth(yoyGrowth)
                        .build());
                prevCount = count;
            }
        }

        yearlyData.sort(Comparator.comparing(y -> y.getYear() != null ? y.getYear() : 0));
        return yearlyData;
    }

    private TrendAnalysisDto.TopicStatus classifyStatus(List<YearlyData> yearlyData) {
        if (yearlyData.size() < 2) return TrendAnalysisDto.TopicStatus.STABLE;

        double recentGrowth = calculateLatestGrowthRate(yearlyData);
        int recentVolume = sumRecentPapers(yearlyData, 2);
        double historicalAvg = averageHistoricalPapers(yearlyData, 5);

        if (recentVolume < 100 && recentGrowth > 0.5) {
            return TrendAnalysisDto.TopicStatus.EMERGING;
        }
        if (recentVolume > 500 && recentGrowth > 0.3) {
            return TrendAnalysisDto.TopicStatus.HOT;
        }
        if (recentVolume > 300 && Math.abs(recentGrowth) < 0.2) {
            return TrendAnalysisDto.TopicStatus.MATURE;
        }
        if (recentGrowth < -0.2) {
            return TrendAnalysisDto.TopicStatus.DECLINING;
        }
        return TrendAnalysisDto.TopicStatus.STABLE;
    }

    private String getStatusLabel(TrendAnalysisDto.TopicStatus status) {
        return switch (status) {
            case EMERGING -> "Emerging";
            case HOT -> "Hot";
            case STABLE -> "Stable";
            case MATURE -> "Mature";
            case DECLINING -> "Declining";
        };
    }

    private Double calculateLatestGrowthRate(List<YearlyData> yearlyData) {
        if (yearlyData.size() < 2) return 0.0;

        List<YearlyData> sorted = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null && y.getPaperCount() > 0)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();

        if (sorted.size() < 2) return 0.0;

        YearlyData latest = sorted.get(sorted.size() - 1);
        YearlyData previous = sorted.get(sorted.size() - 2);

        if (previous.getPaperCount() == null || previous.getPaperCount() == 0) return 0.0;
        return ((double) latest.getPaperCount() - previous.getPaperCount()) / previous.getPaperCount();
    }

    private Double calculateCumulativeGrowth(List<YearlyData> yearlyData) {
        if (yearlyData.size() < 2) return 0.0;

        List<YearlyData> valid = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null && y.getPaperCount() > 0)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();

        if (valid.isEmpty()) return 0.0;

        int first = valid.get(0).getPaperCount();
        int last = valid.get(valid.size() - 1).getPaperCount();

        if (first == 0) return last > 0 ? 1.0 : 0.0;
        return ((double) last - first) / first;
    }

    private Double[] calculateYoYGrowth(List<YearlyData> yearlyData) {
        List<YearlyData> valid = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();

        Double[] yoy = new Double[valid.size()];
        for (int i = 1; i < valid.size(); i++) {
            int prev = valid.get(i - 1).getPaperCount() != null ? valid.get(i - 1).getPaperCount() : 0;
            int curr = valid.get(i).getPaperCount() != null ? valid.get(i).getPaperCount() : 0;
            yoy[i] = prev > 0 ? ((double) curr - prev) / prev : 0.0;
        }
        return yoy;
    }

    private Double calculateMomentum(List<YearlyData> yearlyData) {
        Double[] yoy = calculateYoYGrowth(yearlyData);
        if (yoy == null || yoy.length < 2) return 0.0;

        double[] weights = {0.1, 0.2, 0.3, 0.4}; // older to newer
        double momentum = 0.0;
        int count = 0;
        int weightIdx = 0;

        for (int i = yoy.length - 1; i >= Math.max(0, yoy.length - 4); i--) {
            if (yoy[i] != null) {
                int w = Math.min(weightIdx, weights.length - 1);
                momentum += yoy[i] * weights[w];
                count++;
                weightIdx++;
            }
        }

        return count > 0 ? momentum / count : 0.0;
    }

    private Double forecastNextYear(List<YearlyData> yearlyData, int lastYear) {
        List<YearlyData> valid = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null && y.getPaperCount() > 0)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();

        if (valid.size() < 3) return null;

        // Simple linear regression: y = mx + b
        int n = valid.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = valid.get(i).getYear();
            double y = valid.get(i).getPaperCount();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 0.001) return null;

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        double forecast = slope * (lastYear + 1) + intercept;
        return forecast > 0 ? forecast : null;
    }

    private Double calculateForecastConfidence(List<YearlyData> yearlyData) {
        List<YearlyData> valid = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null && y.getPaperCount() > 0)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();

        if (valid.size() < 4) return 0.3;
        if (valid.size() < 6) return 0.5;
        return 0.7;
    }

    private double calculateTrendScore(TrendAnalysisDto analysis) {
        double growth = analysis.getGrowthRate() != null ? analysis.getGrowthRate() : 0.0;
        double momentum = analysis.getMomentum() != null ? analysis.getMomentum() : 0.0;
        int recentPapers = sumRecentPapers(analysis.getYearlyData(), 2);

        double growthScore = Math.max(-1, Math.min(growth, 5.0)) * 30;
        double momentumScore = Math.max(-1, Math.min(momentum, 3.0)) * 20;
        double volumeScore = Math.log1p(Math.min(recentPapers, 5000)) * 2;

        return growthScore + momentumScore + volumeScore;
    }

    private int sumRecentPapers(List<YearlyData> yearlyData, int years) {
        if (yearlyData == null || yearlyData.isEmpty()) return 0;
        List<YearlyData> sorted = yearlyData.stream()
                .filter(y -> y.getYear() != null)
                .sorted(Comparator.comparing(YearlyData::getYear).reversed())
                .toList();

        return sorted.stream()
                .limit(years)
                .mapToInt(y -> y.getPaperCount() != null ? y.getPaperCount() : 0)
                .sum();
    }

    private double averageHistoricalPapers(List<YearlyData> yearlyData, int years) {
        if (yearlyData == null || yearlyData.isEmpty()) return 0;
        List<YearlyData> sorted = yearlyData.stream()
                .filter(y -> y.getYear() != null)
                .sorted(Comparator.comparing(YearlyData::getYear).reversed())
                .toList();

        int count = Math.min(years, sorted.size());
        if (count == 0) return 0;

        return sorted.stream()
                .limit(count)
                .mapToInt(y -> y.getPaperCount() != null ? y.getPaperCount() : 0)
                .average()
                .orElse(0.0);
    }

    private String generateInsight(String keyword, List<YearlyData> yearlyData,
                                   TrendAnalysisDto.TopicStatus status,
                                   Double growthRate, Double cumulativeGrowth, Double momentum) {
        if (yearlyData == null || yearlyData.isEmpty()) {
            return "No trend data available for '" + keyword + "' yet.";
        }

        List<YearlyData> valid = yearlyData.stream()
                .filter(y -> y.getYear() != null && y.getPaperCount() != null)
                .sorted(Comparator.comparing(YearlyData::getYear))
                .toList();

        if (valid.isEmpty()) {
            return "No valid publication data found for '" + keyword + "'.";
        }

        int total = valid.stream().mapToInt(y -> y.getPaperCount()).sum();
        int latest = valid.get(valid.size() - 1).getPaperCount();
        int first = valid.get(0).getPaperCount();
        int yearRange = valid.get(valid.size() - 1).getYear() - valid.get(0).getYear() + 1;

        StringBuilder insight = new StringBuilder();
        insight.append("The '").append(keyword).append("' field has accumulated ");

        if (total > 1000) {
            insight.append("over ").append(total / 1000).append("K publications");
        } else {
            insight.append(total).append(" publications");
        }

        if (yearRange > 0) {
            insight.append(" between ").append(valid.get(0).getYear())
                    .append("-").append(valid.get(valid.size() - 1).getYear());
        }

        if (growthRate != null && Math.abs(growthRate) > 0.05) {
            String direction = growthRate > 0 ? "increased" : "decreased";
            insight.append(", ").append(direction).append(" ");
            insight.append(String.format("%.0f%%", Math.abs(growthRate) * 100));

            if (growthRate > 0 && growthRate > 0.3) {
                insight.append(" year-over-year, showing strong momentum");
            } else if (growthRate < 0 && growthRate < -0.2) {
                insight.append(" year-over-year, indicating a declining trend");
            }
        }

        if (momentum != null && momentum > 0.3) {
            insight.append(". Momentum is accelerating");
        } else if (momentum != null && momentum < -0.2) {
            insight.append(". Momentum is decelerating");
        }

        // Status-specific
        switch (status) {
            case EMERGING -> insight.append(". This is an emerging research area with high growth potential.");
            case HOT -> insight.append(". This topic is currently experiencing explosive growth.");
            case DECLINING -> insight.append(". Interest in this area has been declining recently.");
            case STABLE -> insight.append(". This remains a steady, consistent research area.");
            case MATURE -> insight.append(". This is a mature research field with established foundations.");
        }

        return insight.toString();
    }

    private String generateComparisonInsight(List<String> keywords, Map<String, List<YearlyData>> dataMap,
                                            String maxGrowthKeyword, double maxGrowth) {
        if (keywords == null || keywords.isEmpty()) return "";

        if (keywords.size() == 1) {
            String kw = keywords.get(0);
            List<YearlyData> data = dataMap.get(kw.toLowerCase());
            Double growth = calculateLatestGrowthRate(data != null ? data : Collections.emptyList());
            if (growth != null && growth > 0.5) {
                return "'" + kw + "' is experiencing explosive growth, significantly outpacing other research areas.";
            } else if (growth != null && growth < -0.2) {
                return "'" + kw + "' is showing declining publication trends compared to previous years.";
            } else {
                return "'" + kw + "' maintains a stable publication trajectory.";
            }
        }

        if (keywords.size() == 2) {
            String kw1 = keywords.get(0);
            String kw2 = keywords.get(1);
            List<YearlyData> d1 = dataMap.get(kw1.toLowerCase());
            List<YearlyData> d2 = dataMap.get(kw2.toLowerCase());

            Double g1 = calculateLatestGrowthRate(d1 != null ? d1 : Collections.emptyList());
            Double g2 = calculateLatestGrowthRate(d2 != null ? d2 : Collections.emptyList());

            if (g1 != null && g2 != null) {
                double diff = g1 - g2;
                if (diff > 0.3) {
                    return "'" + kw1 + "' publications are growing " + String.format("%.0f%%", diff * 100)
                            + " faster than '" + kw2 + "', suggesting a significant shift in research focus.";
                } else if (diff < -0.3) {
                    return "'" + kw2 + "' publications are growing " + String.format("%.0f%%", Math.abs(diff) * 100)
                            + " faster than '" + kw1 + "'.";
                } else {
                    return "'" + kw1 + "' and '" + kw2 + "' show similar growth trajectories.";
                }
            }
        }

        // Multi-keyword
        String topKw = maxGrowthKeyword;
        double topGrowth = maxGrowth;
        return "'" + topKw + "' leads with " + String.format("%.0f%%", topGrowth * 100) + " growth, "
                + "outpacing " + (keywords.size() - 1) + " other research areas in the comparison.";
    }
}
