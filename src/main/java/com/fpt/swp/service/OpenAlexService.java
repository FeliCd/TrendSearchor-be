package com.fpt.swp.service;

import com.fasterxml.jackson.databind.JsonNode;
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
    private final String mailto;

    public OpenAlexService(ApiRetryHandler retryHandler,
                            ApiCacheService cacheService,
                            @Value("${app.openalex.mailto:phuc.fpt.student@gmail.com}") String mailto) {
        this.retryHandler = retryHandler;
        this.cacheService = cacheService;
        this.mailto = mailto;
    }

    private String buildUrl(String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(BASE_URL).append(path);
        sb.append("?mailto=").append(mailto);
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                sb.append("&").append(e.getKey()).append("=").append(e.getValue());
            }
        }
        return sb.toString();
    }

    public Map<String, Object> searchPapersRaw(String query, int offset, int limit) {
        return searchPapersRaw(query, offset, limit, null, null, null, null);
    }

    public Map<String, Object> searchPapersRaw(String query, int offset, int limit,
                                               Integer year, String journal,
                                               String author, String sortBy) {
        String encodedQuery = query.replace(" ", "%20");

        String filterStr = buildFilterString(year, journal, author);
        String sortStr = buildSortString(sortBy);
        int pageNum = (offset / limit) + 1;

        String cacheKey = CACHE_PREFIX_SEARCH
                + encodedQuery + ":"
                + filterStr.replace(" ", "_").replace(":", "-") + ":"
                + sortStr.replace(" ", "_").replace(":", "-") + ":"
                + offset + ":" + limit;

        Optional<String> cached = cacheService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            try {
                Map<String, Object> parsed = new ObjectMapper().readValue(cached.get(), Map.class);
                log.debug("OpenAlex cache HIT for query: {}", query);
                return parsed;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached OpenAlex result: {}", e.getMessage());
            }
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("search", encodedQuery);
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
                cacheService.put(cacheKey, responseBody, java.time.Duration.ofMinutes(15));
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
                Map<String, Object> parsed = new ObjectMapper().readValue(cached.get(), Map.class);
                log.debug("OpenAlex paper cache HIT: id={}", openAlexId);
                return parsed;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached OpenAlex paper: {}", e.getMessage());
            }
        }

        String id = openAlexId.startsWith("https://openalex.org/")
                ? openAlexId : BASE_URL + "/works/" + openAlexId;
        String url = id + "?mailto=" + mailto;

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

    private String buildFilterString(Integer year, String journal, String author) {
        List<String> filters = new ArrayList<>();

        if (year != null && year > 0) {
            filters.add("publication_year:" + year);
        }

        if (journal != null && !journal.isBlank()) {
            String encodedJournal = journal.replace(" ", "%20").replace(",", "%2C");
            filters.add("primary_location.source.display_name:" + encodedJournal);
        }

        if (author != null && !author.isBlank()) {
            String encodedAuthor = author.replace(" ", "%20").replace(",", "%2C");
            filters.add("authorships.author.display_name:" + encodedAuthor);
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
}
