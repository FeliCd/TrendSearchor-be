package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlyData {
    private Integer year;
    private Integer paperCount;
    private Integer citationCount;
    private Double avgCitations;
    private Double yoyGrowth;

    public static YearlyData empty(int year) {
        return YearlyData.builder()
                .year(year)
                .paperCount(0)
                .citationCount(0)
                .avgCitations(0.0)
                .yoyGrowth(null)
                .build();
    }
}
