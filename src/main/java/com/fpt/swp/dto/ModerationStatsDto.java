package com.fpt.swp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModerationStatsDto {
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
    private long totalUploads;
    private long takenDownCount;
    private long pendingCopyrightReports;
}
