package com.fpt.swp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response cho R-10.4: gợi ý chủ đề/keyword nghiên cứu mới cho user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResearchRecommendationResponse {

    /** Danh sách keyword được AI gợi ý, chưa có trong bookmark của user */
    private List<String> suggestedKeywords;

    /** Danh sách topic được AI gợi ý, chưa có trong follow của user */
    private List<String> suggestedTopics;

    /** Lý do AI đưa ra các gợi ý này, dựa trên profile của user */
    private String rationale;
}
