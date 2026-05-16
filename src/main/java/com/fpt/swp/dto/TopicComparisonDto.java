package com.fpt.swp.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicComparisonDto {
    private List<String> keywords;
    private List<Integer> years;
    private Map<String, List<YearlyData>> yearlyDataMap;
    private String insight;
    private String maxGrowthKeyword;
    private String maxGrowthLabel;
    private Double[] keywordTotalPapers;
    private Double[] keywordGrowthRates;
}
