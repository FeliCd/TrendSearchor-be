package com.fpt.swp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticScholarPaperDto {
    private String paperId;
    private String title;
    private String paperAbstract;
    private String year;
    private Integer citationCount;
    private Boolean openAccessPdf;
    private String url;
    private String venue;
    private String[] authors;
    private String[] externalIds;
    private String[] embedding;
    private String[] fieldsOfStudy;
    private String publicationDate;
    private String journal;
    private String conferenceVenue;
}
