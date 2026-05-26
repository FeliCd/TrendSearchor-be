package com.fpt.swp.dto;

import com.fpt.swp.model.ReportType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ReportResponseDto {
    private Long id;
    private ReportType reportType;
    private String title;
    private Map<String, Object> content;
    private Map<String, Object> parameters;
    private LocalDateTime createdAt;
}
