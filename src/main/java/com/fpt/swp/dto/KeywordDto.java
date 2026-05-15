package com.fpt.swp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordDto {
    private Long id;
    private String name;
    private String displayName;
    private Long paperCount;
    private Boolean isFollowed;
    private Boolean isBookmarked;
}
