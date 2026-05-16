package com.fpt.swp.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperSearchResponse {
    private List<PaperDto> papers;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}
