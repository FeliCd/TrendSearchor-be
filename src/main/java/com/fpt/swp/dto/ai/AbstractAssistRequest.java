package com.fpt.swp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request cho FR-10.6: AI hỗ trợ viết/review abstract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbstractAssistRequest {

    /**
     * Loại hành động AI cần thực hiện trên abstract.
     */
    public enum Action {
        /** Phát hiện và sửa lỗi chính tả, ngữ pháp */
        SPELLCHECK,
        /** Gợi ý các vấn đề/khía cạnh chưa được đề cập trong abstract */
        SUGGEST_MISSING
    }

    @NotNull(message = "Action is required")
    private Action action;

    @NotBlank(message = "Abstract text must not be blank")
    private String text;
}
