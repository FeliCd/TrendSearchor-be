package com.fpt.swp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class SemanticScholarService {

    private static final String BASE_URL = "https://api.semanticscholar.org/graph/v1";
    private static final String CACHE_PREFIX_SEARCH = "ss:search:";

    private final ApiRetryHandler retryHandler;
    private final ApiCacheService cacheService;
    private final RestTemplate semanticScholarRestTemplate;
    /** Chỉ lấy bài thuộc các fieldsOfStudy này (giới hạn Công nghệ). Rỗng = không lọc. */
    private final String fieldsOfStudy;

    public SemanticScholarService(ApiRetryHandler retryHandler,
                                  ApiCacheService cacheService,
                                  @Qualifier("semanticScholarRestTemplate") RestTemplate semanticScholarRestTemplate,
                                  @org.springframework.beans.factory.annotation.Value(
                                      "${app.search.semantic-scholar-fields:Computer Science,Mathematics,Engineering}") String fieldsOfStudy) {
        this.retryHandler = retryHandler;
        this.cacheService = cacheService;
        this.semanticScholarRestTemplate = semanticScholarRestTemplate;
        this.fieldsOfStudy = fieldsOfStudy;
    }

    public Map<String, Object> searchPapersRaw(String query, int offset, int limit) {
        return searchPapersRaw(query, offset, limit, null, null);
    }

    public Map<String, Object> searchPapersRaw(String query, int offset, int limit,
                                               String sortBy, Integer year) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String sortStr = buildSortString(sortBy);
        int pageOffset = offset;

        // sortStr có thể null (khi sort theo relevance) -> dùng "rel" cho cache key.
        String sortKey = (sortStr != null ? sortStr.replace(":", "-") : "rel");
        String cacheKey = CACHE_PREFIX_SEARCH
                + encodedQuery + ":"
                + (year != null ? "y" + year : "all") + ":"
                + sortKey + ":"
                + pageOffset + ":" + limit;

        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            try {
                Map<String, Object> parsed = new ObjectMapper().readValue(cached.get(), Map.class);
                log.debug("SemanticScholar cache HIT for query: {}", query);
                return parsed;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached SemanticScholar result: {}", e.getMessage());
            }
        }

        String fields = "title,abstract,year,citationCount,isOpenAccess,"
                + "openAccessPdf.url,url,venue,"
                + "authors.name,fieldsOfStudy,"
                + "publicationDate,journal.name";

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(BASE_URL)
                .append("/paper/search?query=").append(encodedQuery)
                .append("&offset=").append(pageOffset)
                .append("&limit=").append(limit)
                .append("&fields=").append(fields);

        if (sortStr != null && !sortStr.isBlank()) {
            urlBuilder.append("&sort=").append(sortStr);
        }

        if (year != null && year > 0) {
            urlBuilder.append("&year=").append(year);
        }

        // Giới hạn lĩnh vực Công nghệ
        if (fieldsOfStudy != null && !fieldsOfStudy.isBlank()) {
            urlBuilder.append("&fieldsOfStudy=")
                    .append(URLEncoder.encode(fieldsOfStudy, StandardCharsets.UTF_8));
        }

        String url = urlBuilder.toString();
        log.info("SemanticScholar searching: query={}, offset={}, limit={}, sort={}, year={}",
                query, pageOffset, limit, sortStr, year);

        String responseBody = retryHandler.executeWithRetryRaw("semantic-scholar/search", url, semanticScholarRestTemplate);

        if (responseBody == null) {
            log.error("SemanticScholar search failed after all retries: query={}", query);
            return emptyResult();
        }

        try {
            Map<String, Object> parsed = parseSearchResponse(responseBody, offset, limit);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> papers = (List<Map<String, Object>>) parsed.getOrDefault("papers", Collections.emptyList());
            long total = ((Number) parsed.getOrDefault("total", 0)).longValue();

            log.info("SemanticScholar search: query={}, total={}, returned={}", query, total, papers.size());

            if (total > 0 && !papers.isEmpty()) {
                cacheService.put(cacheKey, responseBody, java.time.Duration.ofMinutes(15));
            }

            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse SemanticScholar search response: {}", e.getMessage());
            return emptyResult();
        }
    }

    private String buildSortString(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || sortBy.equalsIgnoreCase("relevance")) {
            return null;
        }
        return switch (sortBy.toLowerCase()) {
            case "citationcount", "citations" -> "citationCount";
            case "year" -> "publicationDate";
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSearchResponse(String responseBody, int offset, int limit) throws Exception {
        JsonNode root = new ObjectMapper().readTree(responseBody);

        Map<String, Object> result = new HashMap<>();
        result.put("total", root.has("total") ? root.get("total").asLong() : 0);
        result.put("offset", root.has("offset") ? root.get("offset").asInt() : offset);
        result.put("limit", limit);

        List<Map<String, Object>> papers = new ArrayList<>();
        JsonNode data = root.get("data");
        if (data != null && data.isArray()) {
            for (JsonNode node : data) {
                papers.add(parsePaperNode(node));
            }
        }
        result.put("papers", papers);

        return result;
    }

    private Map<String, Object> parsePaperNode(JsonNode node) {
        Map<String, Object> paper = new LinkedHashMap<>();

        paper.put("paperId", getTextOrNull(node, "paperId"));
        paper.put("id", node.has("paperId") ? "https://www.semanticscholar.org/paper/" + node.get("paperId").asText() : null);
        paper.put("title", getTextOrNull(node, "title"));
        paper.put("abstract", getTextOrNull(node, "abstract"));

        if (node.has("year") && !node.get("year").isNull()) {
            paper.put("year", node.get("year").asInt());
        } else {
            paper.put("year", null);
        }

        paper.put("citationCount", node.has("citationCount") ? node.get("citationCount").asInt() : 0);

        boolean openAccess = node.has("isOpenAccess") && node.hasNonNull("isOpenAccess")
                && node.get("isOpenAccess").asBoolean();
        paper.put("openAccess", openAccess);

        if (node.has("openAccessPdf") && !node.get("openAccessPdf").isNull()) {
            JsonNode pdf = node.get("openAccessPdf");
            paper.put("openAccessUrl", getTextOrNull(pdf, "url"));
        }

        String url = getTextOrNull(node, "url");
        if (url == null && node.has("paperId")) {
            url = "https://www.semanticscholar.org/paper/" + node.get("paperId").asText();
        }
        paper.put("url", url);

        if (node.has("journal") && !node.get("journal").isNull()) {
            paper.put("journal", getTextOrNull(node.get("journal"), "name"));
        } else {
            paper.put("journal", getTextOrNull(node, "venue"));
        }

        List<String> authors = new ArrayList<>();
        if (node.has("authors") && node.get("authors").isArray()) {
            for (JsonNode auth : node.get("authors")) {
                String name = getTextOrNull(auth, "name");
                if (name != null) authors.add(name);
            }
        }
        paper.put("authors", authors);

        List<String> keywords = new ArrayList<>();
        if (node.has("fieldsOfStudy") && node.get("fieldsOfStudy").isArray()) {
            for (JsonNode kw : node.get("fieldsOfStudy")) {
                if (kw.isTextual()) keywords.add(kw.asText());
            }
        }
        paper.put("keywords", keywords);

        return paper;
    }

    private String getTextOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        JsonNode fieldNode = node.get(field);
        if (fieldNode.isTextual()) return fieldNode.asText();
        return fieldNode.toString();
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> empty = new HashMap<>();
        empty.put("total", 0);
        empty.put("papers", Collections.emptyList());
        return empty;
    }

    public void evictSearchCache() {
        cacheService.evictByPrefix(CACHE_PREFIX_SEARCH);
        log.info("Evicted all SemanticScholar search cache entries");
    }
}
