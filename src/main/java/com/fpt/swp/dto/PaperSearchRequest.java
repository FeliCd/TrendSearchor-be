package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperSearchRequest {
    private String query;
    private Integer year;
    private Integer yearFrom;
    private Integer yearTo;
    private String dateFromStr; // full ISO date e.g. "2020-03-15"
    private String dateToStr;   // full ISO date e.g. "2024-06-30"
    private String journal;
    private String author;
    private String sortBy;    // "relevance", "citationCount", "year"
    private String sortOrder; // "desc", "asc"
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 10;
}
