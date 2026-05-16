package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalStatsDto {
    private Long id;
    private String name;
    private String publisher;
    private Long paperCount;
    private Long citationCount;
}
