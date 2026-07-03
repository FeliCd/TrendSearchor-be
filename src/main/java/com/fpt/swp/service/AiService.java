package com.fpt.swp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpt.swp.dto.PaperSearchRequest;
import com.fpt.swp.dto.PaperSearchResponse;
import com.fpt.swp.dto.TrendAnalysisDto;
import com.fpt.swp.dto.ai.*;
import com.fpt.swp.model.Bookmark;
import com.fpt.swp.model.UserFollow;
import com.fpt.swp.repository.BookmarkRepository;
import com.fpt.swp.repository.UserFollowRepository;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic layer cho các tính năng AI:
 * <ul>
 *   <li>FR-10.6: Abstract assistant (cleanup, spellcheck, suggest missing, evaluate)</li>
 *   <li>R-10.4:  Research recommendations dựa trên bookmark/follow profile của user</li>
 *   <li>FR-10.1: Natural language paper search</li>
 *   <li>FR-10.2: Trend Q&A — trả lời câu hỏi xu hướng bằng ngôn ngữ tự nhiên</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final OpenRouterClient openRouterClient;
    private final BookmarkRepository bookmarkRepository;
    private final UserFollowRepository userFollowRepository;
    private final TrendAnalysisService trendAnalysisService;
    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // FR-10.6: Abstract Assistant
    // =========================================================================

    /**
     * Xử lý abstract theo hành động mà user yêu cầu.
     */
    public AbstractAssistResponse processAbstract(AbstractAssistRequest request) {
        String systemPrompt = buildAbstractSystemPrompt(request.getAction());
        String userPrompt   = "Abstract:\n\n" + request.getText();

        String raw = openRouterClient.chat(systemPrompt, userPrompt);
        if (raw == null) {
            return AbstractAssistResponse.builder()
                    .action(request.getAction().name())
                    .feedback("AI service is currently unavailable. Please try again later.")
                    .build();
        }

        return parseAbstractResponse(request.getAction(), raw);
    }

    private String buildAbstractSystemPrompt(AbstractAssistRequest.Action action) {
        return switch (action) {
            case CLEANUP ->
                "You are an expert academic editor. Clean up and reformat the provided abstract: "
                + "improve sentence structure, remove redundancy, ensure formal academic tone, "
                + "and fix minor grammatical issues. Return only the improved abstract text with no additional commentary.";

            case SPELLCHECK ->
                "You are a meticulous proofreader. Identify and fix all spelling and grammar errors in the provided abstract. "
                + "Return only the corrected text with no additional commentary.";

            case SUGGEST_MISSING ->
                "You are a research expert reviewing an academic abstract. "
                + "List the key aspects or information that are missing or underdeveloped in the abstract. "
                + "Format your response as a JSON object: "
                + "{\"suggestions\": [\"suggestion 1\", \"suggestion 2\", ...], \"feedback\": \"overall comment\"}";

            case EVALUATE ->
                "You are a senior academic reviewer. Evaluate the quality of the provided abstract on a scale of 0–10. "
                + "Consider clarity, completeness, structure, and academic rigor. "
                + "Format your response as a JSON object: "
                + "{\"score\": <integer 0-10>, \"feedback\": \"detailed evaluation\"}";
        };
    }

    private AbstractAssistResponse parseAbstractResponse(AbstractAssistRequest.Action action, String raw) {
        return switch (action) {
            case CLEANUP, SPELLCHECK -> AbstractAssistResponse.builder()
                    .action(action.name())
                    .result(raw.trim())
                    .build();

            case SUGGEST_MISSING -> {
                try {
                    SuggestMissingAiPayload payload = objectMapper.readValue(
                            extractJson(raw), SuggestMissingAiPayload.class);
                    yield AbstractAssistResponse.builder()
                            .action(action.name())
                            .suggestions(payload.getSuggestions())
                            .feedback(payload.getFeedback())
                            .build();
                } catch (Exception e) {
                    log.warn("Could not parse SUGGEST_MISSING JSON, returning raw text. Error: {}", e.getMessage());
                    yield AbstractAssistResponse.builder()
                            .action(action.name())
                            .feedback(raw)
                            .build();
                }
            }

            case EVALUATE -> {
                try {
                    EvaluateAiPayload payload = objectMapper.readValue(
                            extractJson(raw), EvaluateAiPayload.class);
                    yield AbstractAssistResponse.builder()
                            .action(action.name())
                            .score(payload.getScore())
                            .feedback(payload.getFeedback())
                            .build();
                } catch (Exception e) {
                    log.warn("Could not parse EVALUATE JSON, returning raw text. Error: {}", e.getMessage());
                    yield AbstractAssistResponse.builder()
                            .action(action.name())
                            .feedback(raw)
                            .build();
                }
            }
        };
    }

    // =========================================================================
    // R-10.4: Research Recommendations
    // =========================================================================

    /**
     * Gợi ý keyword/topic nghiên cứu mới cho user, dựa trên lịch sử bookmark và follow.
     *
     * @param userId ID của user đang đăng nhập
     * @return gợi ý được cá nhân hóa từ AI
     */
    public ResearchRecommendationResponse getRecommendations(Long userId) {
        // --- 1. Lấy keywords user đã bookmark ---
        List<Bookmark> keywordBookmarks = bookmarkRepository.findByUserIdAndKeywordIdIsNotNull(userId);
        List<String> bookmarkedKeywords = keywordBookmarks.stream()
                .filter(b -> b.getKeyword() != null)
                .map(b -> b.getKeyword().getName())
                .distinct()
                .collect(Collectors.toList());

        // --- 2. Lấy topics user đang follow ---
        List<UserFollow> topicFollows = userFollowRepository.findAll().stream()
                .filter(f -> f.getUser().getId().equals(userId) && f.getTopic() != null)
                .collect(Collectors.toList());
        List<String> followedTopics = topicFollows.stream()
                .map(f -> f.getTopic().getName())
                .distinct()
                .collect(Collectors.toList());

        // --- 3. Lấy paper keywords user đã bookmark ---
        List<Bookmark> paperBookmarks = bookmarkRepository.findByUserIdAndPaperIdIsNotNull(userId);
        List<String> bookmarkedPaperTitles = paperBookmarks.stream()
                .filter(b -> b.getPaper() != null)
                .map(b -> b.getPaper().getTitle())
                .limit(10)
                .collect(Collectors.toList());

        // --- 4. Build context và gọi AI ---
        String systemPrompt =
            "You are a research advisor helping a researcher discover new topics. "
            + "Based on the user's existing interests, suggest new keywords and research topics they haven't explored yet. "
            + "Respond in JSON format: {\"suggestedKeywords\": [...], \"suggestedTopics\": [...], \"rationale\": \"...\"}";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("My current research interests:\n");
        if (!bookmarkedKeywords.isEmpty()) {
            userPrompt.append("- Bookmarked keywords: ").append(String.join(", ", bookmarkedKeywords)).append("\n");
        }
        if (!followedTopics.isEmpty()) {
            userPrompt.append("- Followed research topics: ").append(String.join(", ", followedTopics)).append("\n");
        }
        if (!bookmarkedPaperTitles.isEmpty()) {
            userPrompt.append("- Recently bookmarked papers:\n");
            bookmarkedPaperTitles.forEach(t -> userPrompt.append("  * ").append(t).append("\n"));
        }
        userPrompt.append("\nSuggest 5 new keywords and 3 new research topics I should explore.");

        if (bookmarkedKeywords.isEmpty() && followedTopics.isEmpty()) {
            return ResearchRecommendationResponse.builder()
                    .suggestedKeywords(List.of())
                    .suggestedTopics(List.of())
                    .rationale("No bookmarks or follows found. Please bookmark some papers or follow topics to get personalized recommendations.")
                    .build();
        }

        RecommendationAiPayload payload = openRouterClient.chatJson(
                systemPrompt, userPrompt.toString(), RecommendationAiPayload.class);

        if (payload == null) {
            return ResearchRecommendationResponse.builder()
                    .suggestedKeywords(List.of())
                    .suggestedTopics(List.of())
                    .rationale("AI service is currently unavailable. Please try again later.")
                    .build();
        }

        return ResearchRecommendationResponse.builder()
                .suggestedKeywords(payload.getSuggestedKeywords())
                .suggestedTopics(payload.getSuggestedTopics())
                .rationale(payload.getRationale())
                .build();
    }

    // =========================================================================
    // FR-10.1: Natural Language Paper Search
    // =========================================================================

    /**
     * Phân tích câu hỏi tự nhiên, trích xuất tham số tìm kiếm và gọi SearchService.
     *
     * @param nlRequest câu hỏi tự nhiên của user, ví dụ "Tìm bài báo của Vaswani về transformer 2017"
     * @param userId    ID user (có thể null nếu chưa đăng nhập)
     * @return kết quả tìm kiếm giống như search thông thường
     */
    public PaperSearchResponse naturalLanguageSearch(NlSearchRequest nlRequest, Long userId) {
        String systemPrompt =
            "You are a search parameter extractor for an academic paper search engine. "
            + "Extract search parameters from the user's natural language query. "
            + "Respond in JSON format: "
            + "{\"query\": \"<main search keywords>\", \"author\": \"<author name or null>\", "
            + "\"journal\": \"<journal name or null>\", \"year\": <year integer or null>}. "
            + "If a parameter is not mentioned, use null. The query field must not be null.";

        NlSearchParamsPayload params = openRouterClient.chatJson(
                systemPrompt, nlRequest.getQuery(), NlSearchParamsPayload.class);

        PaperSearchRequest searchRequest;
        if (params == null || params.getQuery() == null) {
            // Fallback: dùng nguyên câu query nếu AI không parse được
            log.warn("AI could not extract search params from: '{}'. Falling back to raw query.", nlRequest.getQuery());
            searchRequest = PaperSearchRequest.builder()
                    .query(nlRequest.getQuery())
                    .build();
        } else {
            searchRequest = PaperSearchRequest.builder()
                    .query(params.getQuery())
                    .author(params.getAuthor())
                    .journal(params.getJournal())
                    .year(params.getYear())
                    .build();
        }

        return searchService.searchPapers(searchRequest, userId);
    }

    // =========================================================================
    // FR-10.2: Trend Q&A
    // =========================================================================

    /**
     * Trả lời câu hỏi về xu hướng nghiên cứu, sử dụng dữ liệu trend thực tế làm context.
     *
     * @param request câu hỏi và keyword tùy chọn
     * @return câu trả lời phân tích từ AI kèm data context
     */
    public TrendQaResponse answerTrendQuestion(TrendQaRequest request) {
        TrendAnalysisDto trendData = null;

        // Lấy dữ liệu trend thực tế nếu user cung cấp keyword
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            try {
                trendData = trendAnalysisService.analyzeKeyword(request.getKeyword(), null, null);
            } catch (Exception e) {
                log.warn("Could not fetch trend data for keyword '{}': {}", request.getKeyword(), e.getMessage());
            }
        }

        String systemPrompt =
            "You are an expert academic trend analyst specializing in computer science and research trends. "
            + "Answer the user's question about research trends in a clear, analytical, and insightful way. "
            + "If trend data is provided, use it to support your answer with specific numbers and evidence. "
            + "Your response should explain causes, comparisons, and implications — not just show charts.";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Question: ").append(request.getQuestion()).append("\n\n");

        if (trendData != null) {
            userPrompt.append("Real trend data for '").append(trendData.getDisplayName()).append("':\n");
            userPrompt.append("- Status: ").append(trendData.getStatusLabel()).append("\n");
            userPrompt.append("- Growth rate: ").append(trendData.getGrowthRate()).append("%\n");
            userPrompt.append("- Cumulative growth: ").append(trendData.getCumulativeGrowth()).append("%\n");
            userPrompt.append("- Total papers: ").append(trendData.getTotalPapers()).append("\n");
            userPrompt.append("- Peak year: ").append(trendData.getPeakYear()).append(" (").append(trendData.getPeakPaperCount()).append(" papers)\n");
            if (trendData.getInsight() != null) {
                userPrompt.append("- System insight: ").append(trendData.getInsight()).append("\n");
            }
            userPrompt.append("\nUse this data to answer the question analytically.");
        }

        String answer = openRouterClient.chat(systemPrompt, userPrompt.toString());

        return TrendQaResponse.builder()
                .answer(answer != null ? answer : "AI service is currently unavailable. Please try again later.")
                .dataContext(trendData)
                .build();
    }

    // =========================================================================
    // Helper — JSON extraction
    // =========================================================================

    /**
     * Trích xuất JSON block từ response AI (đề phòng AI wrap trong markdown code fence).
     */
    private String extractJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        // Xử lý ```json ... ``` markdown fence
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start != -1 && end > start) {
                return trimmed.substring(start + 1, end).trim();
            }
        }
        return trimmed;
    }

    // =========================================================================
    // Private inner DTOs for AI JSON parsing (không expose ra ngoài)
    // =========================================================================

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SuggestMissingAiPayload {
        private List<String> suggestions;
        private String feedback;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EvaluateAiPayload {
        private Integer score;
        private String feedback;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class RecommendationAiPayload {
        private List<String> suggestedKeywords;
        private List<String> suggestedTopics;
        private String rationale;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NlSearchParamsPayload {
        private String query;
        private String author;
        private String journal;
        private Integer year;
    }
}
