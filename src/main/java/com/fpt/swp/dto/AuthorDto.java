package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDto {
    private Long id;
    private String externalId;
    private String name;
    private String orcid;
    private Integer hIndex;
    private Integer paperCount;
    private Boolean isFollowed;
}
