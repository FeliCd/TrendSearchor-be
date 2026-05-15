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
    private String journal;
    private String author;
    private String sortBy; // "relevance", "citationCount", "year"
    private String sortOrder; // "desc", "asc"
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 10;
}
