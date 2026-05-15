package com.fpt.swp.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalPapers;
    private long totalJournals;
    private long totalAuthors;
    private long totalKeywords;
    private List<KeywordTrendDto> topKeywords;
    private List<JournalStatsDto> topJournals;
    private List<YearlyStatsDto> yearlyStats;
    private long newPapersThisWeek;
    private long newPapersThisMonth;
}
