package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchTopicDto {
    private Long id;
    private String name;
    private String description;
    private String category;
    private Double popularityScore;
}
