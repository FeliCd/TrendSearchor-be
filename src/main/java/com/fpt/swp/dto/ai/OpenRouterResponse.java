package com.fpt.swp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response nhận từ OpenRouter Chat Completions API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenRouterResponse {

    private String id;
    private String object;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private int index;
        private Message message;
        private String finish_reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }

    /**
     * Convenience method — lấy nội dung text từ choice đầu tiên.
     */
    public String getFirstContent() {
        if (choices == null || choices.isEmpty()) return null;
        Choice first = choices.get(0);
        return first.getMessage() != null ? first.getMessage().getContent() : null;
    }
}
