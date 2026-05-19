package com.fpt.swp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class OpenAlexParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> parseSearchResponse(String responseBody, int offset, int limit) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        Map<String, Object> result = new HashMap<>();
        result.put("total", root.has("meta") && root.get("meta").has("count")
                ? root.get("meta").get("count").asLong() : 0);
        result.put("offset", offset);
        result.put("limit", limit);

        List<Map<String, Object>> papers = new ArrayList<>();
        JsonNode results = root.get("results");
        if (results != null && results.isArray()) {
            for (JsonNode node : results) papers.add(parseWorkNode(node));
        }
        result.put("papers", papers);
        return result;
    }

    public Map<String, Object> parseGroupByResponse(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> yearlyData = new ArrayList<>();

        JsonNode groups = root.get("group_by");
        if (groups != null && groups.isArray()) {
            for (JsonNode group : groups) {
                Map<String, Object> item = new LinkedHashMap<>();
                JsonNode key = group.get("key");
                item.put("year", key != null && !key.isNull() ? key.asInt() : 0);
                item.put("count", group.has("count") ? group.get("count").asInt() : 0);
                yearlyData.add(item);
            }
        }

        yearlyData.sort((a, b) -> Integer.compare((Integer) a.get("year"), (Integer) b.get("year")));
        result.put("yearlyData", yearlyData);
        result.put("keyword", root.has("meta") && root.get("meta").has("query")
                ? root.get("meta").get("query").asText() : "unknown");
        result.put("source", "openalex");
        return result;
    }

    public Map<String, Object> parseWorkNode(JsonNode node) {
        Map<String, Object> paper = new LinkedHashMap<>();
        String id = node.has("id") ? node.get("id").asText() : null;
        paper.put("paperId", id != null ? id.replace("https://openalex.org/", "") : null);
        paper.put("id", id);
        paper.put("title", getTextOrNull(node, "title"));

        if (node.has("abstract_inverted_index") && !node.get("abstract_inverted_index").isNull()) {
            paper.put("abstract", reconstructAbstract(node.get("abstract_inverted_index")));
        } else {
            paper.put("abstract", null);
        }

        if (node.has("publication_year")) {
            paper.put("year", node.get("publication_year").asInt());
        } else if (node.has("publication_date")) {
            String date = node.get("publication_date").asText();
            paper.put("year", date != null && date.length() >= 4 ? parseYear(date) : null);
        } else {
            paper.put("year", null);
        }

        paper.put("citationCount", node.has("cited_by_count") ? node.get("cited_by_count").asInt() : 0);

        boolean openAccess = false;
        if (node.has("open_access") && !node.get("open_access").isNull()) {
            JsonNode oa = node.get("open_access");
            openAccess = oa.has("is_oa") && oa.get("is_oa").asBoolean();
            if (oa.has("oa_url")) paper.put("openAccessUrl", oa.get("oa_url").asText());
        }
        paper.put("openAccess", openAccess);

        String url = getTextOrNull(node, "doi");
        paper.put("url", url != null ? url : id);

        if (node.has("primary_location") && !node.get("primary_location").isNull()) {
            JsonNode loc = node.get("primary_location");
            if (loc.has("source") && !loc.get("source").isNull()) {
                paper.put("journal", getTextOrNull(loc.get("source"), "display_name"));
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
            for (JsonNode kw : node.get("keywords")) keywords.add(kw.asText());
        }
        paper.put("keywords", keywords);

        if (node.has("concepts")) {
            List<String> concepts = new ArrayList<>();
            for (JsonNode concept : node.get("concepts")) {
                String name = getTextOrNull(concept, "display_name");
                if (name != null) concepts.add(name);
            }
            if (!concepts.isEmpty() && keywords.isEmpty()) paper.put("keywords", concepts);
        }

        return paper;
    }

    private Integer parseYear(String date) {
        try { return Integer.parseInt(date.substring(0, 4)); }
        catch (NumberFormatException e) { return null; }
    }

    private String reconstructAbstract(JsonNode invertedIndex) {
        if (invertedIndex == null || !invertedIndex.isObject()) return null;

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
        if (index.isEmpty()) return null;

        int maxPos = index.values().stream().flatMap(List::stream)
                .mapToInt(arr -> arr[0]).max().orElse(0);
        String[] words = new String[maxPos + 1];
        for (Map.Entry<String, List<int[]>> entry : index.entrySet()) {
            for (int[] pos : entry.getValue()) words[pos[0]] = entry.getKey();
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
        return fieldNode.isTextual() ? fieldNode.asText() : fieldNode.toString();
    }
}
