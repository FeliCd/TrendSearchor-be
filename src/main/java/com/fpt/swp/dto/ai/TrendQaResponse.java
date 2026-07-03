package com.fpt.swp.dto.ai;

import com.fpt.swp.dto.TrendAnalysisDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho FR-10.2: câu trả lời AI về xu hướng nghiên cứu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendQaResponse {

    /** Câu trả lời phân tích từ AI */
    private String answer;

    /**
     * Dữ liệu trend thực tế mà AI dùng làm context khi trả lời.
     * Null nếu không cung cấp keyword hoặc không tìm thấy data.
     */
    private TrendAnalysisDto dataContext;
}
