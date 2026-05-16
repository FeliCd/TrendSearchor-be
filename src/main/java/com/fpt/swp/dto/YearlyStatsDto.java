package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YearlyStatsDto {
    private Integer year;
    private Long paperCount;
    private Long citationCount;
    private Double avgCitations;
}
