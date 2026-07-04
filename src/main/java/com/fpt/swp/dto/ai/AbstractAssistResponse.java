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
     * - CLEANUP: abstract đã được dọn dẹp/chuẩn hóa
     * - SPELLCHECK: abstract đã sửa lỗi
     * - SUGGEST_MISSING: không dùng field này (dùng suggestions)
     * - EVALUATE: không dùng field này
     */
    private String result;

    /**
     * Danh sách gợi ý các vấn đề/khía cạnh còn thiếu.
     * Chỉ có giá trị khi action = SUGGEST_MISSING.
     */
    private List<String> suggestions;

    /**
     * Điểm chất lượng abstract từ 0 đến 10.
     * Chỉ có giá trị khi action = EVALUATE.
     */
    private Integer score;

    /**
     * Nhận xét tổng thể từ AI.
     * Dùng cho tất cả các action.
     */
    private String feedback;
}
