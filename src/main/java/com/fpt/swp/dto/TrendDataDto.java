package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataDto {
    private String keyword;
    private Integer year;
    private Integer month;
    private Integer paperCount;
    private Integer citationCount;
    private Double avgCitations;
}
