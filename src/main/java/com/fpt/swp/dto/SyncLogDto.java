package com.fpt.swp.dto;

import com.fpt.swp.model.SyncLog;
import com.fpt.swp.model.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncLogDto {
    private Long id;
    private Long sourceId;
    private LocalDateTime syncStartTime;
    private LocalDateTime syncEndTime;
    private SyncStatus status;
    private Integer papersAdded;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static SyncLogDto fromEntity(SyncLog log) {
        if (log == null) return null;
        return SyncLogDto.builder()
                .id(log.getId())
                .sourceId(log.getSource() != null ? log.getSource().getId() : null)
                .syncStartTime(log.getSyncStartTime())
                .syncEndTime(log.getSyncEndTime())
                .status(log.getStatus())
                .papersAdded(log.getPapersAdded())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
