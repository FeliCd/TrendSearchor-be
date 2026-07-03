package com.fpt.swp.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperDto {
    private Long id;
    private String externalId;
    private String title;
    private String abstractText;
    private Integer year;
    private Integer citationCount;
    private Boolean openAccess;
    private String paperUri;
    private List<AuthorDto> authors;
    private List<String> journals;
    private List<String> keywords;
    private Boolean isBookmarked;
    private String source;
    private String uploadStatus;
    private String rejectionReason;
    private String status;
    private String uploadedBy;
    private Boolean isSelfPublished;
    private String statusComments;
    private Integer aiRelevanceScore;
    private String aiRelevanceReason;
}
