package com.fpt.swp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request cho FR-10.2: hỏi AI về xu hướng nghiên cứu.
 * VD: "Tại sao Transformer trending mạnh từ 2020?"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendQaRequest {

    @NotBlank(message = "Question must not be blank")
    private String question;

    /**
     * Từ khóa cụ thể để lấy dữ liệu trend làm context.
     * Nếu null, AI sẽ trả lời dựa trên kiến thức chung.
     */
    private String keyword;
}
