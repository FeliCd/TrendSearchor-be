package com.fpt.swp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearcherLeaderboardDto {
    private String fullName;
    private String mail;
    private Long approvedPapersCount;
}
