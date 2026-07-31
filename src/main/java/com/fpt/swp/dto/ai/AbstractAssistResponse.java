package com.fpt.swp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response cho FR-10.6: kết quả AI hỗ trợ abstract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbstractAssistResponse {

    /** Loại action đã thực hiện */
    private String action;

    /**
     * Kết quả text đã được AI xử lý.
     * - SPELLCHECK: abstract đã sửa lỗi
     * - SUGGEST_MISSING: không dùng field này (dùng suggestions)
     */
    private String result;

    /**
     * Danh sách gợi ý các vấn đề/khía cạnh còn thiếu.
     * Chỉ có giá trị khi action = SUGGEST_MISSING.
     */
    private List<String> suggestions;

    /**
     * Nhận xét tổng thể từ AI.
     * Dùng cho tất cả các action.
     */
    private String feedback;
}
