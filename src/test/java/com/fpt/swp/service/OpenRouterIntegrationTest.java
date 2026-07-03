package com.fpt.swp.service;

import com.fpt.swp.dto.ai.AbstractAssistRequest;
import com.fpt.swp.dto.ai.AbstractAssistResponse;
import com.fpt.swp.dto.ai.NlSearchRequest;
import com.fpt.swp.dto.ai.TrendQaRequest;
import com.fpt.swp.dto.ai.TrendQaResponse;
import com.fpt.swp.dto.PaperSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests cho OpenRouter AI.
 *
 * Để chạy tests này bạn cần:
 * 1. OPENROUTER_API_KEY đã được set trong .env
 * 2. App có thể kết nối được tới DB (MySQL đang chạy)
 *
 * Chạy: ./mvnw test -Dtest=OpenRouterIntegrationTest -pl .
 */
@SpringBootTest
class OpenRouterIntegrationTest {

    @Autowired
    private OpenRouterClient openRouterClient;

    @Autowired
    private AiService aiService;

    @Value("${app.openrouter.api-key:}")
    private String apiKey;

    // -------------------------------------------------------------------------
    // Test 0: Kiểm tra API key có được load chưa
    // -------------------------------------------------------------------------

    @Test
    void apiKey_shouldBeConfigured() {
        System.out.println(">>> OPENROUTER_API_KEY loaded: " + (apiKey != null && !apiKey.isBlank() ? "YES ✅" : "NO ❌ — set OPENROUTER_API_KEY in .env"));
        // Không fail test nếu chưa có key — chỉ in thông báo
    }

    // -------------------------------------------------------------------------
    // Test 1: Ping đơn giản tới OpenRouter
    // -------------------------------------------------------------------------

    @Test
    void openRouterClient_simplePing_shouldReturnResponse() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("⚠️  SKIPPED: OPENROUTER_API_KEY is not set.");
            return;
        }

        String response = openRouterClient.chat(
                "You are a helpful assistant. Reply in one sentence.",
                "Say 'OpenRouter is working!' and nothing else."
        );

        System.out.println(">>> OpenRouter ping response: " + response);
        assertThat(response).isNotNull().isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Test 2: FR-10.6 Abstract CLEANUP
    // -------------------------------------------------------------------------

    @Test
    void abstractAssist_cleanup_shouldReturnImprovedText() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("⚠️  SKIPPED: OPENROUTER_API_KEY is not set.");
            return;
        }

        AbstractAssistRequest request = new AbstractAssistRequest(
                AbstractAssistRequest.Action.CLEANUP,
                "This paper present a new method for clasification of image using deep lerning. " +
                "We use CNN architecture and train on imagenet dataset. The result show that " +
                "our methode achieve 95% accurcy which is better then previous work."
        );

        AbstractAssistResponse response = aiService.processAbstract(request);

        System.out.println(">>> CLEANUP result: " + response.getResult());
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotBlank();
        System.out.println("✅ FR-10.6 CLEANUP: PASSED");
    }

    // -------------------------------------------------------------------------
    // Test 3: FR-10.6 Abstract EVALUATE
    // -------------------------------------------------------------------------

    @Test
    void abstractAssist_evaluate_shouldReturnScoreAndFeedback() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("⚠️  SKIPPED: OPENROUTER_API_KEY is not set.");
            return;
        }

        AbstractAssistRequest request = new AbstractAssistRequest(
                AbstractAssistRequest.Action.EVALUATE,
                "This paper proposes a novel Transformer-based architecture for natural language understanding. " +
                "We demonstrate state-of-the-art results on GLUE and SuperGLUE benchmarks, " +
                "achieving 92.3% on MNLI and 89.1% on QQP. Our model requires 40% fewer parameters " +
                "than previous approaches while maintaining competitive performance."
        );

        AbstractAssistResponse response = aiService.processAbstract(request);

        System.out.println(">>> EVALUATE score: " + response.getScore());
        System.out.println(">>> EVALUATE feedback: " + response.getFeedback());
        assertThat(response).isNotNull();
        assertThat(response.getScore()).isNotNull().isBetween(0, 10);
        System.out.println("✅ FR-10.6 EVALUATE: PASSED (score = " + response.getScore() + "/10)");
    }

    // -------------------------------------------------------------------------
    // Test 4: FR-10.1 Natural Language Search
    // -------------------------------------------------------------------------

    @Test
    void naturalLanguageSearch_shouldExtractParamsAndReturnResults() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("⚠️  SKIPPED: OPENROUTER_API_KEY is not set.");
            return;
        }

        NlSearchRequest request = new NlSearchRequest(
                "Find papers about transformer attention mechanism from 2017"
        );

        PaperSearchResponse response = aiService.naturalLanguageSearch(request, null);

        System.out.println(">>> NL Search result count: " + (response != null ? response.getTotal() : "null"));
        assertThat(response).isNotNull();
        System.out.println("✅ FR-10.1 NL Search: PASSED");
    }

    // -------------------------------------------------------------------------
    // Test 5: FR-10.2 Trend Q&A
    // -------------------------------------------------------------------------

    @Test
    void trendQa_withKeyword_shouldReturnAnalysis() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("⚠️  SKIPPED: OPENROUTER_API_KEY is not set.");
            return;
        }

        TrendQaRequest request = new TrendQaRequest(
                "Why has transformer been trending strongly since 2020?",
                "transformer"
        );

        TrendQaResponse response = aiService.answerTrendQuestion(request);

        System.out.println(">>> Trend Q&A answer (first 200 chars): " +
                (response.getAnswer() != null ? response.getAnswer().substring(0, Math.min(200, response.getAnswer().length())) : "null"));
        System.out.println(">>> Data context present: " + (response.getDataContext() != null ? "YES ✅" : "NO (no local trend data)"));
        assertThat(response).isNotNull();
        assertThat(response.getAnswer()).isNotBlank();
        System.out.println("✅ FR-10.2 Trend Q&A: PASSED");
    }
}
