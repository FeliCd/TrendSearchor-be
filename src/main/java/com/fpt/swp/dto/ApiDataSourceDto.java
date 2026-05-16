package com.fpt.swp.dto;

import com.fpt.swp.model.SyncStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiDataSourceDto {
    private Long id;
    private String sourceName;
    private String baseUrl;
    private String apiKey;
    private Integer rateLimitPerDay;
    private Boolean isActive;
    private LocalDateTime lastSyncAt;
    private SyncStatus lastSyncStatus;
    private Integer recordsSynced;
}
