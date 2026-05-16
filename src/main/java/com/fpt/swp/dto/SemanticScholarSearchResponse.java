package com.fpt.swp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticScholarSearchResponse<T> {
    private int offset;
    private int limit;
    private int total;
    private T[] data;
}
