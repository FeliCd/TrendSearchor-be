package com.fpt.swp.controller;

import com.fpt.swp.dto.PaperDto;
import com.fpt.swp.dto.PaperSearchResponse;
import com.fpt.swp.dto.ai.*;
import java.util.List;
import com.fpt.swp.exception.RateLimitExceededException;
import com.fpt.swp.service.AiRateLimiter;
import com.fpt.swp.service.AiService;
import com.fpt.swp.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho các tính năng AI:
 * <ul>
 *   <li>POST /api/ai/abstract          — FR-10.6: AI hỗ trợ viết/review abstract</li>
 *   <li>GET  /api/ai/recommendations   — R-10.4:  Gợi ý chủ đề nghiên cứu</li>
 *   <li>POST /api/ai/search            — FR-10.1: Tìm kiếm bằng ngôn ngữ tự nhiên</li>
 *   <li>POST /api/ai/trend-qa          — FR-10.2: Hỏi AI về xu hướng nghiên cứu</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AuthUtils authUtils;
    private final AiRateLimiter aiRateLimiter;

    /**
     * Chặn lạm dụng các endpoint AI (gọi LLM trả phí). Giới hạn theo user nếu đã
     * đăng nhập, ngược lại theo IP client. Vượt hạn mức → 429 Too Many Requests.
     */
    private void enforceRateLimit(UserDetails userDetails, HttpServletRequest httpRequest) {
        Long userId = authUtils.extractUserId(userDetails);
        String key = userId != null ? "user:" + userId
                : "ip:" + com.fpt.swp.util.RequestUtils.clientIp(httpRequest);
        if (!aiRateLimiter.tryAcquire(key)) {
            throw new RateLimitExceededException(
                    "Too many AI requests. Please wait a moment before trying again.");
        }
    }

    // -------------------------------------------------------------------------
    // FR-10.6: Abstract Assistant
    // -------------------------------------------------------------------------

    /**
     * AI hỗ trợ viết/review abstract của Researcher.
     * Các action: SPELLCHECK | SUGGEST_MISSING
     *
     * @param request  action và nội dung abstract
     * @return kết quả xử lý từ AI
     */
    @PostMapping("/abstract")
    public ResponseEntity<AbstractAssistResponse> processAbstract(
            @Valid @RequestBody AbstractAssistRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        // Endpoint yêu cầu đăng nhập — security filter sẽ chặn nếu chưa auth
        enforceRateLimit(userDetails, httpRequest);
        AbstractAssistResponse response = aiService.processAbstract(request);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // R-10.4: Research Recommendations
    // -------------------------------------------------------------------------

    /**
     * Gợi ý keyword/topic nghiên cứu mới dựa trên bookmark và follow của user.
     * Cần đăng nhập.
     *
     * @return danh sách gợi ý cá nhân hóa
     */
    @GetMapping("/recommendations")
    public ResponseEntity<ResearchRecommendationResponse> getRecommendations(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        enforceRateLimit(userDetails, httpRequest);
        Long userId = authUtils.extractUserId(userDetails);
        ResearchRecommendationResponse response = aiService.getRecommendations(userId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // FR-10.1: Natural Language Search
    // -------------------------------------------------------------------------

    /**
     * Tìm kiếm bài báo bằng câu hỏi tự nhiên.
     * VD: "Tìm bài báo của Vaswani về attention năm 2017"
     *
     * @param request  câu hỏi tự nhiên
     * @return kết quả tìm kiếm (cùng format với GET /api/papers/search)
     */
    @PostMapping("/search")
    public ResponseEntity<PaperSearchResponse> naturalLanguageSearch(
            @Valid @RequestBody NlSearchRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        enforceRateLimit(userDetails, httpRequest);
        Long userId = authUtils.extractUserId(userDetails);
        PaperSearchResponse response = aiService.naturalLanguageSearch(request, userId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // FR-10.2: Trend Q&A
    // -------------------------------------------------------------------------

    /**
     * Hỏi AI về xu hướng nghiên cứu bằng ngôn ngữ tự nhiên.
     * AI sẽ lấy dữ liệu trend thực tế từ hệ thống để trả lời có phân tích.
     *
     * @param request  câu hỏi và keyword tùy chọn
     * @return câu trả lời phân tích kèm data context
     */
    @PostMapping("/trend-qa")
    public ResponseEntity<TrendQaResponse> answerTrendQuestion(
            @Valid @RequestBody TrendQaRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        enforceRateLimit(userDetails, httpRequest);
        TrendQaResponse response = aiService.answerTrendQuestion(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/summarize")
    public ResponseEntity<PaperSummaryResponse> summarizePaper(
            @Valid @RequestBody PaperSummaryRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        enforceRateLimit(userDetails, httpRequest);
        PaperSummaryResponse response = aiService.summarizePaper(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rerank")
    public ResponseEntity<List<PaperDto>> rerankPapers(
            @Valid @RequestBody PaperRerankRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        enforceRateLimit(userDetails, httpRequest);
        List<PaperDto> response = aiService.rerankPapers(request);
        return ResponseEntity.ok(response);
    }
}
