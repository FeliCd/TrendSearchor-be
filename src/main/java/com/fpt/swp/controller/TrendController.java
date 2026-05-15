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

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;
    private final AuthUtils authUtils;

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
}
