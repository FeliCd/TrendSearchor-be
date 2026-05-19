package com.fpt.swp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class OpenAlexService {

    private static final String BASE_URL = "https://api.openalex.org";
    private static final String CACHE_PREFIX_SEARCH = "oa:search:";

    private final ApiRetryHandler retryHandler;
    private final ApiCacheService cacheService;
    private final OpenAlexParser parser;
    private final ObjectMapper objectMapper;
    private final String mailto;

    public OpenAlexService(ApiRetryHandler retryHandler,
                           ApiCacheService cacheService,
                           OpenAlexParser parser,
                           @Value("${app.openalex.mailto:phuc.fpt.student@gmail.com}") String mailto) {
        this.retryHandler = retryHandler;
        this.cacheService = cacheService;
        this.parser = parser;
        this.objectMapper = new ObjectMapper();
        this.mailto = mailto;
    }

    private String buildUrl(String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(BASE_URL).append(path).append("?mailto=").append(mailto);
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                sb.append("&").append(e.getKey()).append("=").append(e.getValue());
            }
        }
        return sb.toString();
    }

    public Map<String, Object> searchPapersRaw(String query, int offset, int limit,
                                               Integer year, String journal, String author, String sortBy) {
        String encodedQuery = query.replace(" ", "%20");
        String filterStr = buildFilterString(year, journal, author);
        String sortStr = buildSortString(sortBy);
        String cacheKey = CACHE_PREFIX_SEARCH + encodedQuery + ":" + filterStr.replace(" ", "_").replace(":", "-")
                + ":" + sortStr.replace(" ", "_").replace(":", "-") + ":" + offset + ":" + limit;

        Map<String, Object> cached = getCachedResult(cacheKey);
        if (cached != null) return cached;

        Map<String, String> params = new LinkedHashMap<>();
        params.put("search", encodedQuery);
        params.put("filter", "language:en" + (filterStr.isEmpty() ? "" : "," + filterStr));
        params.put("sort", sortStr);
        params.put("per-page", String.valueOf(limit));
        params.put("page", String.valueOf((offset / limit) + 1));

        String url = buildUrl("/works", params);
        log.info("OpenAlex searching: query={}, offset={}, limit={}", query, offset, limit);

        String responseBody = retryHandler.executeWithRetryRaw("openalex/search", url);
        if (responseBody == null) return emptyResult();

        try {
            Map<String, Object> parsed = parser.parseSearchResponse(responseBody, offset, limit);
            cacheParsedResult(cacheKey, parsed);
            log.info("OpenAlex search: query={}, total={}, returned={}",
                    query, parsed.get("total"), ((List<?>) parsed.get("papers")).size());
            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse OpenAlex search response: {}", e.getMessage());
            return emptyResult();
        }
    }

    public Map<String, Object> searchPapersRaw(String query, int offset, int limit) {
        return searchPapersRaw(query, offset, limit, null, null, null, null);
    }

    public Map<String, Object> getPaperDetails(String openAlexId) {
        String cacheKey = "oa:paper:" + openAlexId;
        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(cached.get(), Map.class);
                log.debug("OpenAlex paper cache HIT: id={}", openAlexId);
                return parsed;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached OpenAlex paper: {}", e.getMessage());
            }
        }

        String id = openAlexId.startsWith("https://openalex.org/") ? openAlexId : BASE_URL + "/works/" + openAlexId;
        String responseBody = retryHandler.executeWithRetryRaw("openalex/paper", id + "?mailto=" + mailto);
        if (responseBody == null) return Collections.emptyMap();

        cacheService.put(cacheKey, responseBody, java.time.Duration.ofMinutes(30));
        try {
            return objectMapper.readValue(responseBody, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse OpenAlex paper response: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> getWorksCountByYear(String query, Integer startYear, Integer endYear) {
        String encodedQuery = query.replace(" ", "%20");
        String cacheKey = "oa:groupby:" + encodedQuery + ":" + startYear + ":" + endYear;

        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(cached.get(), Map.class);
                return parsed;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached group-by result: {}", e.getMessage());
            }
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                    .append("/works?search=").append(encodedQuery)
                    .append("&mailto=").append(mailto)
                    .append("&per-page=1")
                    .append("&group-by=publication_year");
            if (startYear != null && startYear > 0) urlBuilder.append("&filter=publication_year:>").append(startYear - 1);
            if (endYear != null && endYear > 0) urlBuilder.append(",publication_year:<").append(endYear + 1);

            String responseBody = retryHandler.executeWithRetryRaw("openalex/groupby", urlBuilder.toString());
            if (responseBody == null) return fallbackGetWorksCountByYear(query, startYear, endYear);

            Map<String, Object> result = parser.parseGroupByResponse(responseBody);
            cacheService.put(cacheKey, objectMapper.writeValueAsString(result), java.time.Duration.ofHours(1));
            return result;
        } catch (Exception e) {
            log.error("OpenAlex group-by failed for query '{}': {}", query, e.getMessage());
            return fallbackGetWorksCountByYear(query, startYear, endYear);
        }
    }

    public void evictSearchCache() {
        cacheService.evictByPrefix(CACHE_PREFIX_SEARCH);
        log.info("Evicted all OpenAlex search cache entries");
    }

    // ==================== PRIVATE HELPERS ====================

    private Map<String, Object> getCachedResult(String cacheKey) {
        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isEmpty()) return null;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(cached.get(), Map.class);
            if (parsed.containsKey("papers")) {
                log.debug("OpenAlex cache HIT for key: {}", cacheKey);
                return parsed;
            }
            Map<String, Object> processed = parser.parseSearchResponse(cached.get(), 0, 10);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> papers = (List<Map<String, Object>>) processed.getOrDefault("papers", Collections.emptyList());
            long total = ((Number) processed.getOrDefault("total", 0)).longValue();
            if (total > 0 && !papers.isEmpty()) {
                cacheService.put(cacheKey, processed, java.time.Duration.ofMinutes(15));
            }
            return processed;
        } catch (Exception e) {
            log.warn("Failed to deserialize cached OpenAlex result: {}", e.getMessage());
            return null;
        }
    }

    private void cacheParsedResult(String cacheKey, Map<String, Object> parsed) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> papers = (List<Map<String, Object>>) parsed.getOrDefault("papers", Collections.emptyList());
        long total = ((Number) parsed.getOrDefault("total", 0)).longValue();
        if (total > 0 && !papers.isEmpty()) {
            cacheService.put(cacheKey, parsed, java.time.Duration.ofMinutes(15));
        }
    }

    private String buildFilterString(Integer year, String journal, String author) {
        List<String> filters = new ArrayList<>();
        if (year != null && year > 0) filters.add("publication_year:" + year);
        if (journal != null && !journal.isBlank()) {
            filters.add("primary_location.source.display_name:" + journal.replace(" ", "%20").replace(",", "%2C"));
        }
        if (author != null && !author.isBlank()) {
            filters.add("authorships.author.display_name:" + author.replace(" ", "%20").replace(",", "%2C"));
        }
        return String.join(",", filters);
    }

    private String buildSortString(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return "relevance_score:desc";
        return switch (sortBy.toLowerCase()) {
            case "citationcount", "citations" -> "cited_by_count:desc";
            case "year" -> "publication_year:desc";
            default -> "relevance_score:desc";
        };
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> empty = new HashMap<>();
        empty.put("total", 0);
        empty.put("papers", Collections.emptyList());
        return empty;
    }

    private Map<String, Object> fallbackGetWorksCountByYear(String query, Integer startYear, Integer endYear) {
        log.info("OpenAlex fallback: fetching papers one by one for query={}", query);
        Map<Integer, Integer> yearCounts = new LinkedHashMap<>();

        try {
            int offset = 0;
            int pageSize = 200;
            int maxYear = endYear != null ? endYear : java.time.Year.now().getValue();
            int minYear = startYear != null ? startYear : 2015;

            for (int page = 0; page < 20; page++) {
                Map<String, Object> response = searchPapersRaw(query, offset, pageSize);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> papers = (List<Map<String, Object>>) response.getOrDefault("papers", Collections.emptyList());
                if (papers.isEmpty()) break;

                for (Map<String, Object> paper : papers) {
                    Object yearObj = paper.get("year");
                    if (yearObj == null) continue;
                    int year = ((Number) yearObj).intValue();
                    if (year >= minYear && year <= maxYear) yearCounts.merge(year, 1, Integer::sum);
                }

                offset += pageSize;
                if (papers.size() < pageSize) break;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("yearlyData", yearCounts.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .map(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("year", e.getKey());
                        item.put("count", e.getValue());
                        return item;
                    }).collect(Collectors.toList()));
            result.put("keyword", query.toLowerCase());
            result.put("source", "fallback");
            return result;
        } catch (Exception e) {
            log.error("Fallback group-by also failed: {}", e.getMessage());
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("yearlyData", Collections.emptyList());
            empty.put("keyword", query.toLowerCase());
            return empty;
        }
    }
}
