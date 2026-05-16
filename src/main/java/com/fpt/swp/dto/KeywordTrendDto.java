package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordTrendDto {
    private String keyword;
    private String displayName;
    private Long paperCount;
    private Double growthRate;
    private Integer[] yearlyPapers;
}
