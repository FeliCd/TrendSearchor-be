package com.fpt.swp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticScholarAuthorDto {
    private String authorId;
    private String name;
    private String orcid;
    private Integer hIndex;
    private Integer paperCount;
    private String[] aliases;
    private Integer[] papers;
}
