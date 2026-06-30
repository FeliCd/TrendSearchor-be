package com.fpt.swp.dto;

import lombok.*;
import java.util.List;

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
    private String status;
    private String uploadedBy;
    private String statusComments;
}
