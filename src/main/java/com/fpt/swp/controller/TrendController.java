package com.fpt.swp.controller;

import com.fpt.swp.dto.*;
import com.fpt.swp.service.TrendService;
import com.fpt.swp.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;
    private final AuthUtils authUtils;

    // === Existing endpoints ===

    @GetMapping("/keyword/{keyword}")
    public ResponseEntity<List<TrendDataDto>> getTrendByKeyword(
            @PathVariable String keyword,
            @RequestParam(required = false) Integer startYear
    ) {
        List<TrendDataDto> trends;
        if (startYear != null) {
            trends = trendService.getTrendByKeywordSinceYear(keyword, startYear);
        } else {
            trends = trendService.getTrendByKeyword(keyword);
        }
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/compare")
    public ResponseEntity<List<TrendDataDto>> compareTrends(
            @RequestParam List<String> keywords
    ) {
        return ResponseEntity.ok(trendService.compareTrends(keywords));
    }

    @GetMapping("/top-keywords")
    public ResponseEntity<List<KeywordTrendDto>> getTopKeywords(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        return ResponseEntity.ok(trendService.getTopTrendingKeywords(limit, userId));
    }

    @GetMapping("/keywords")
    public ResponseEntity<List<KeywordTrendDto>> getTrendingKeywords(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(trendService.getTrendingKeywords(limit));
    }

    @GetMapping("/yearly")
    public ResponseEntity<List<YearlyStatsDto>> getYearlyStats() {
        return ResponseEntity.ok(trendService.getYearlyStats());
    }

    // === Phase 1: Publication Trend (MVP) ===

    /**
     * Full analysis of a keyword: trend data, growth rates, status, insight, forecast.
     * GET /api/trends/analyze/{keyword}?startYear=2015&endYear=2025
     */
    @GetMapping("/analyze/{keyword}")
    public ResponseEntity<TrendAnalysisDto> analyzeKeyword(
            @PathVariable String keyword,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear
    ) {
        TrendAnalysisDto analysis = trendService.getPublicationTrend(keyword, startYear, endYear);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Search a keyword and get real-time trend analysis from OpenAlex.
     * GET /api/trends/search?query=llm
     */
    @GetMapping("/search")
    public ResponseEntity<TrendAnalysisDto> searchAndAnalyze(
            @RequestParam String query
    ) {
        TrendAnalysisDto analysis = trendService.searchAndAnalyze(query);
        return ResponseEntity.ok(analysis);
    }

    // === Phase 2: Trending Topics ===

    /**
     * Get ranked list of trending topics with TrendScore, growth rate, status.
     * GET /api/trends/ranking?limit=20
     */
    @GetMapping("/ranking")
    public ResponseEntity<List<TrendingTopicDto>> getTrendingRanking(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(trendService.getTopicRanking(limit));
    }

    /**
     * Get topics that are emerging (low historical + high recent growth).
     * GET /api/trends/emerging?limit=10
     */
    @GetMapping("/emerging")
    public ResponseEntity<List<TrendingTopicDto>> getEmergingTopics(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(trendService.getEmergingTopics(limit));
    }

    /**
     * Compare multiple topics side by side with auto-generated insight.
     * GET /api/trends/compare?keywords=llm,computer%20vision&startYear=2015&endYear=2025
     */
    @GetMapping("/compare-full")
    public ResponseEntity<TopicComparisonDto> compareTopicsFull(
            @RequestParam List<String> keywords,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear
    ) {
        TopicComparisonDto comparison = trendService.compareTopics(keywords);
        return ResponseEntity.ok(comparison);
    }

    // === Phase 3: Keyword Relations ===

    /**
     * Get keyword co-occurrence data for network graph visualization.
     * GET /api/trends/related?keyword=llm&limit=15
     */
    @GetMapping("/related")
    public ResponseEntity<Map<String, Object>> getRelatedKeywords(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "15") int limit
    ) {
        return ResponseEntity.ok(trendService.getKeywordCooccurrence(keyword, limit));
    }
}
