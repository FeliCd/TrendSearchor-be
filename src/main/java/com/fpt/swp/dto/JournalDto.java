package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalDto {
    private Long id;
    private String externalId;
    private String name;
    private String publisher;
    private String issn;
    private String country;
    private String homepageUrl;
    private Long paperCount;
    private Boolean isFollowed;
}
