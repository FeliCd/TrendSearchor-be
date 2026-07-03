package com.fpt.swp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpenAlexService {

    private static final String BASE_URL = "https://api.openalex.org";
    private static final String CACHE_PREFIX_SEARCH = "oa:search:";

    private final ApiRetryHandler retryHandler;
    private final ApiCacheService cacheService;
    private final String mailto;

    public OpenAlexService(ApiRetryHandler retryHandler,
                            ApiCacheService cacheService,
                            @Value("${app.openalex.mailto:phuc.fpt.student@gmail.com}") String mailto) {
        this.retryHandler = retryHandler;
        this.cacheService = cacheService;
        this.mailto = mailto;
    }

    private String buildUrl(String path, Map<String, String> params) {
        org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(BASE_URL).path(path)
                .queryParam("mailto", mailto);
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                builder.queryParam(e.getKey(), e.getValue());
            }
        }
        return builder.build().encode().toUriString();
    }

    public Map<String, Object> searchPapersRaw(String query, int offset, int limit) {
        return searchPapersRaw(query, offset, limit, null, null, null, null, null, null, null, null);
    }

    public Map<String, Object> searchPapersRaw(String query, int offset, int limit,
                                               Integer year, Integer yearFrom, Integer yearTo,
                                               String dateFromStr, String dateToStr,
                                               String journal, String author, String sortBy) {
        
        if (journal != null && !journal.isBlank() && !journal.toLowerCase().matches("^(https://openalex\\.org/)?s\\d+$")) {
            query = query + " " + journal;
        }

        String filterStr = buildFilterString(year, yearFrom, yearTo, dateFromStr, dateToStr, journal, author);
        String sortStr = buildSortString(sortBy);
        int pageNum = (offset / limit) + 1;

        String cacheKey = CACHE_PREFIX_SEARCH
                + query.replace(" ", "%20") + ":"
                + filterStr.replace(" ", "_").replace(":", "-") + ":"
                + sortStr.replace(" ", "_").replace(":", "-") + ":"
                + offset + ":" + limit;

        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = new ObjectMapper().readValue(cached.get(), Map.class);
                if (parsed.containsKey("papers")) {
                    log.debug("OpenAlex cache HIT for query: {}", query);
                    return parsed;
                }
                Map<String, Object> processed = parseSearchResponse(cached.get(), offset, limit);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> papers = (List<Map<String, Object>>) processed.getOrDefault("papers", Collections.emptyList());
                long total = ((Number) processed.getOrDefault("total", 0)).longValue();
                if (total > 0 && !papers.isEmpty()) {
                    cacheService.put(cacheKey, processed, java.time.Duration.ofMinutes(15));
                }
                log.debug("OpenAlex cache HIT (legacy format) for query: {}", query);
                return processed;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached OpenAlex result: {}", e.getMessage());
            }
        }

        Map<String, String> params = new LinkedHashMap<>();
        if (query != null && !query.isBlank()) {
            params.put("search", query);
        }
        params.put("filter", "language:en" + (filterStr.isEmpty() ? "" : "," + filterStr));
        params.put("sort", sortStr);
        params.put("per-page", String.valueOf(limit));
        params.put("page", String.valueOf(pageNum));

        String url = buildUrl("/works", params);
        log.info("OpenAlex searching: query={}, offset={}, limit={}, filters={}, sort={}, mailto={}",
                query, offset, limit, filterStr, sortStr, mailto);

        String responseBody = retryHandler.executeWithRetryRaw("openalex/search", url);

        if (responseBody == null) {
            log.error("OpenAlex search failed after all retries: query={}", query);
            return emptyResult();
        }

        try {
            Map<String, Object> parsed = parseSearchResponse(responseBody, offset, limit);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> papers = (List<Map<String, Object>>) parsed.getOrDefault("papers", Collections.emptyList());
            long total = ((Number) parsed.getOrDefault("total", 0)).longValue();

            log.info("OpenAlex search: query={}, total={}, returned={}", query, total, papers.size());

            if (total > 0 && !papers.isEmpty()) {
                cacheService.put(cacheKey, parsed, java.time.Duration.ofMinutes(15));
            }

            return parsed;
        } catch (Exception e) {
            log.error("Failed to parse OpenAlex search response: {}", e.getMessage());
            return emptyResult();
        }
    }

    public Map<String, Object> getPaperDetails(String openAlexId) {
        String cacheKey = "oa:paper:" + openAlexId;

        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = new ObjectMapper().readValue(cached.get(), Map.class);
                log.debug("OpenAlex paper cache HIT: id={}", openAlexId);
                return parsed;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached OpenAlex paper: {}", e.getMessage());
            }
        }

        String id = openAlexId.startsWith("https://openalex.org/")
                ? openAlexId : BASE_URL + "/works/" + openAlexId;
        String url = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(id)
                .queryParam("mailto", mailto)
                .build().encode().toUriString();

        log.debug("OpenAlex fetching paper: id={}", openAlexId);

        String responseBody = retryHandler.executeWithRetryRaw("openalex/paper", url);

        if (responseBody == null) {
            log.error("OpenAlex paper fetch failed after all retries: id={}", openAlexId);
            return Collections.emptyMap();
        }

        cacheService.put(cacheKey, responseBody, java.time.Duration.ofMinutes(30));

        try {
            return new ObjectMapper().readValue(responseBody, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse OpenAlex paper response: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String buildFilterString(Integer year, Integer yearFrom, Integer yearTo,
                                     String dateFromStr, String dateToStr,
                                     String journal, String author) {
        List<String> filters = new ArrayList<>();

        // Use full date (day/month precision) when available via from_publication_date / to_publication_date
        boolean hasDayPrecision = (dateFromStr != null && dateFromStr.length() >= 8)
                               || (dateToStr   != null && dateToStr.length()   >= 8);

        if (hasDayPrecision) {
            // OpenAlex supports: from_publication_date:YYYY-MM-DD, to_publication_date:YYYY-MM-DD
            if (dateFromStr != null && !dateFromStr.isBlank()) {
                filters.add("from_publication_date:" + dateFromStr.substring(0, 10));
            }
            if (dateToStr != null && !dateToStr.isBlank()) {
                filters.add("to_publication_date:" + dateToStr.substring(0, 10));
            }
        } else {
            // Fall back to year-level filtering
            if (year != null && year > 0) {
                filters.add("publication_year:" + year);
            } else if ((yearFrom != null && yearFrom > 0) || (yearTo != null && yearTo > 0)) {
                if (yearFrom != null && yearTo != null) {
                    filters.add("publication_year:" + yearFrom + "-" + yearTo);
                } else if (yearFrom != null) {
                    filters.add("publication_year:>" + (yearFrom - 1));
                } else {
                    filters.add("publication_year:<" + (yearTo + 1));
                }
            }
        }

        if (journal != null && !journal.isBlank()) {
            if (journal.toLowerCase().matches("^(https://openalex\\.org/)?s\\d+$")) {
                filters.add("primary_location.source.id:" + journal);
            } else {
                filters.add("primary_location.source.display_name.search:" + journal);
            }
        }

        if (author != null && !author.isBlank()) {
            if (author.toLowerCase().matches("^(https://openalex\\.org/)?a\\d+$")) {
                filters.add("authorships.author.id:" + author);
            } else {
                filters.add("raw_author_name.search:" + author);
            }
        }

        return String.join(",", filters);
    }

    private String buildSortString(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "relevance_score:desc";
        }
        return switch (sortBy.toLowerCase()) {
            case "citationcount", "citations" -> "cited_by_count:desc";
            case "year" -> "publication_year:desc";
            case "relevance" -> "relevance_score:desc";
            default -> "relevance_score:desc";
        };
    }

    private Map<String, Object> parseSearchResponse(String responseBody, int offset, int limit) throws Exception {
        JsonNode root = new ObjectMapper().readTree(responseBody);

        Map<String, Object> result = new HashMap<>();
        result.put("total", root.has("meta") && root.get("meta").has("count")
                ? root.get("meta").get("count").asLong() : 0);
        result.put("offset", offset);
        result.put("limit", limit);

        List<Map<String, Object>> papers = new ArrayList<>();
        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            for (JsonNode node : results) {
                papers.add(parseWorkNode(node));
            }
        }
        result.put("papers", papers);

        return result;
    }

    private Map<String, Object> parseWorkNode(JsonNode node) {
        Map<String, Object> paper = new LinkedHashMap<>();

        String id = node.has("id") ? node.get("id").asText() : null;
        paper.put("paperId", id != null ? id.replace("https://openalex.org/", "") : null);
        paper.put("id", id);
        paper.put("title", getTextOrNull(node, "title"));

        // Reconstruct abstract from inverted index
        if (node.has("abstract_inverted_index") && !node.get("abstract_inverted_index").isNull()) {
            String abstractText = reconstructAbstract(node.get("abstract_inverted_index"));
            paper.put("abstract", abstractText);
        } else {
            paper.put("abstract", null);
        }

        if (node.has("publication_year")) {
            paper.put("year", node.get("publication_year").asInt());
        } else if (node.has("publication_date")) {
            String date = node.get("publication_date").asText();
            if (date != null && date.length() >= 4) {
                try {
                    paper.put("year", Integer.parseInt(date.substring(0, 4)));
                } catch (NumberFormatException e) {
                    paper.put("year", null);
                }
            }
        } else {
            paper.put("year", null);
        }

        paper.put("citationCount", node.has("cited_by_count") ? node.get("cited_by_count").asInt() : 0);

        boolean openAccess = false;
        if (node.has("open_access") && !node.get("open_access").isNull()) {
            JsonNode oa = node.get("open_access");
            openAccess = oa.has("is_oa") && oa.get("is_oa").asBoolean();
            if (oa.has("oa_url")) {
                paper.put("openAccessUrl", oa.get("oa_url").asText());
            }
        }
        paper.put("openAccess", openAccess);

        String url = getTextOrNull(node, "doi");
        if (url == null && node.has("id")) {
            url = node.get("id").asText();
        }
        paper.put("url", url);

        if (node.has("primary_location") && !node.get("primary_location").isNull()) {
            JsonNode loc = node.get("primary_location");
            if (loc.has("source") && !loc.get("source").isNull()) {
                JsonNode source = loc.get("source");
                paper.put("journal", getTextOrNull(source, "display_name"));
            }
        }

        List<String> authors = new ArrayList<>();
        if (node.has("authorships")) {
            for (JsonNode auth : node.get("authorships")) {
                if (auth.has("author") && !auth.get("author").isNull()) {
                    String name = getTextOrNull(auth.get("author"), "display_name");
                    if (name != null) authors.add(name);
                }
            }
        }
        paper.put("authors", authors);

        List<String> keywords = new ArrayList<>();
        if (node.has("topics")) {
            for (JsonNode topic : node.get("topics")) {
                String kw = getTextOrNull(topic, "display_name");
                if (kw != null) keywords.add(kw);
            }
        }
        if (keywords.isEmpty() && node.has("keywords")) {
            for (JsonNode kw : node.get("keywords")) {
                keywords.add(kw.asText());
            }
        }
        paper.put("keywords", keywords);

        if (node.has("concepts")) {
            List<String> concepts = new ArrayList<>();
            for (JsonNode concept : node.get("concepts")) {
                String name = getTextOrNull(concept, "display_name");
                if (name != null) concepts.add(name);
            }
            if (!concepts.isEmpty() && keywords.isEmpty()) {
                paper.put("keywords", concepts);
            }
        }

        return paper;
    }

    private String reconstructAbstract(JsonNode invertedIndex) {
        if (invertedIndex == null || !invertedIndex.isObject()) {
            return null;
        }

        Map<String, List<int[]>> index = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = invertedIndex.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String word = entry.getKey();
            List<int[]> positions = new ArrayList<>();
            if (entry.getValue().isArray()) {
                for (JsonNode posNode : entry.getValue()) {
                    if (posNode.isArray() && posNode.size() >= 2
                            && !posNode.get(0).isNull() && !posNode.get(1).isNull()) {
                        positions.add(new int[]{posNode.get(0).asInt(), posNode.get(1).asInt()});
                    }
                }
            }
            index.put(word, positions);
        }

        if (index.isEmpty()) {
            return null;
        }

        int maxPos = index.values().stream()
                .flatMap(List::stream)
                .mapToInt(arr -> arr[0])
                .max()
                .orElse(0);

        String[] words = new String[maxPos + 1];
        for (Map.Entry<String, List<int[]>> entry : index.entrySet()) {
            String word = entry.getKey();
            for (int[] pos : entry.getValue()) {
                words[pos[0]] = word;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word != null) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word);
            }
        }

        return sb.length() > 0 ? sb.toString() : null;
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
        log.info("Evicted all OpenAlex search cache entries");
    }

    /**
     * Get works count grouped by publication year using OpenAlex group_by.
     * Falls back to paginated search if group_by is not supported.
     */
    public Map<String, Object> getWorksCountByYear(String query, Integer startYear, Integer endYear) {
        String cacheKey = "oa:groupby:" + query.replace(" ", "%20") + ":" + startYear + ":" + endYear;

        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = new ObjectMapper().readValue(cached.get(), Map.class);
                return parsed;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached group-by result: {}", e.getMessage());
            }
        }

        try {
            org.springframework.web.util.UriComponentsBuilder builder = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(BASE_URL).path("/works")
                    .queryParam("search", query)
                    .queryParam("mailto", mailto)
                    .queryParam("per-page", "200")
                    .queryParam("group_by", "publication_year");

            if (startYear != null && startYear > 0 && endYear != null && endYear > 0) {
                builder.queryParam("filter", "publication_year:" + startYear + "-" + endYear);
            } else if (startYear != null && startYear > 0) {
                builder.queryParam("filter", "publication_year:>" + (startYear - 1));
            } else if (endYear != null && endYear > 0) {
                builder.queryParam("filter", "publication_year:<" + (endYear + 1));
            }

            String url = builder.build().encode().toUriString();
            log.info("OpenAlex group-by: query={}, startYear={}, endYear={}", query, startYear, endYear);

            String responseBody = retryHandler.executeWithRetryRaw("openalex/groupby", url);

            if (responseBody == null) {
                return fallbackGetWorksCountByYear(query, startYear, endYear);
            }

            Map<String, Object> result = parseGroupByResponse(responseBody);
            cacheService.put(cacheKey, new ObjectMapper().writeValueAsString(result), java.time.Duration.ofHours(1));
            return result;

        } catch (Exception e) {
            log.error("OpenAlex group-by failed for query '{}': {}", query, e.getMessage());
            return fallbackGetWorksCountByYear(query, startYear, endYear);
        }
    }

    private Map<String, Object> fallbackGetWorksCountByYear(String query, Integer startYear, Integer endYear) {
        log.info("OpenAlex fallback: fetching papers one by one for query={}", query);
        Map<Integer, Integer> yearCounts = new java.util.LinkedHashMap<>();

        try {
            int offset = 0;
            int pageSize = 200;
            int maxYear = endYear != null ? endYear : java.time.Year.now().getValue();
            int minYear = startYear != null ? startYear : 2015;

            for (int page = 0; page < 20; page++) {
                Map<String, Object> response = searchPapersRaw(query, offset, pageSize);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> papers = (List<Map<String, Object>>) response.getOrDefault("papers", java.util.Collections.emptyList());

                if (papers.isEmpty()) break;

                for (Map<String, Object> paper : papers) {
                    Object yearObj = paper.get("year");
                    if (yearObj == null) continue;
                    int year = ((Number) yearObj).intValue();
                    if (year < minYear || year > maxYear) continue;

                    yearCounts.merge(year, 1, Integer::sum);
                }

                offset += pageSize;
                if (papers.size() < pageSize) break;
            }

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("yearlyData", yearCounts.entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .map(e -> {
                        Map<String, Object> item = new java.util.LinkedHashMap<>();
                        item.put("year", e.getKey());
                        item.put("count", e.getValue());
                        return item;
                    })
                    .collect(java.util.stream.Collectors.toList()));
            result.put("keyword", query.toLowerCase());
            result.put("source", "fallback");

            return result;

        } catch (Exception e) {
            log.error("Fallback group-by also failed: {}", e.getMessage());
            Map<String, Object> empty = new java.util.LinkedHashMap<>();
            empty.put("yearlyData", java.util.Collections.emptyList());
            empty.put("keyword", query.toLowerCase());
            return empty;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGroupByResponse(String responseBody) throws Exception {
        JsonNode root = new ObjectMapper().readTree(responseBody);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        List<Map<String, Object>> yearlyData = new java.util.ArrayList<>();

        JsonNode groups = root.get("group_by");
        if (groups != null && groups.isArray()) {
            for (JsonNode group : groups) {
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                JsonNode key = group.get("key");
                item.put("year", key != null && !key.isNull() ? key.asInt() : 0);
                item.put("count", group.has("count") ? group.get("count").asInt() : 0);
                yearlyData.add(item);
            }
        }

        yearlyData.sort((a, b) -> Integer.compare((Integer) a.get("year"), (Integer) b.get("year")));
        result.put("yearlyData", yearlyData);
        result.put("keyword", root.has("meta") && root.get("meta").has("query") ? root.get("meta").get("query").asText() : "unknown");
        result.put("source", "openalex");
        return result;
    }
}
