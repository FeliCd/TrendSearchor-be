package com.fpt.swp.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendAnalysisDto {
    private String keyword;
    private String displayName;
    private List<YearlyData> yearlyData;
    private Double growthRate;
    private Double cumulativeGrowth;
    private Double[] yoyGrowth;
    private TopicStatus status;
    private String statusLabel;
    private Double momentum;
    private String insight;
    private Double forecastNextYear;
    private Double forecastConfidence;
    private Integer totalPapers;
    private Integer totalCitations;
    private Integer peakYear;
    private Integer peakPaperCount;

    public enum TopicStatus {
        EMERGING,
        HOT,
        STABLE,
        MATURE,
        DECLINING
    }
}
