package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingTopicDto {
    private String keyword;
    private String displayName;
    private Double trendScore;
    private Double growthRate;
    private Integer recentPapers;
    private Integer totalPapers;
    private Integer totalCitations;
    private Double avgCitations;
    private TrendAnalysisDto.TopicStatus status;
    private String statusLabel;
    private Double momentum;
    private Integer rank;
}
