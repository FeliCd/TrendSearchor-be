package com.fpt.swp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request cho FR-10.2: hỏi AI về xu hướng nghiên cứu.
 * VD: "Tại sao Transformer trending mạnh từ 2020?"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendQaRequest {

    @NotBlank(message = "Question must not be blank")
    @Size(max = 1000, message = "Question must not exceed 1000 characters")
    private String question;

    /**
     * Từ khóa cụ thể để lấy dữ liệu trend làm context.
     * Nếu null, AI sẽ trả lời dựa trên kiến thức chung.
     */
    @Size(max = 200, message = "Keyword must not exceed 200 characters")
    private String keyword;
}
