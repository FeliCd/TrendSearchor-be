package com.fpt.swp.dto;

import com.fpt.swp.model.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotBlank(message = "Keyword/Topic cannot be blank")
    private String keyword;
    
    @NotNull(message = "Report type is required")
    private ReportType reportType;
    
    private Integer startYear;
    private Integer endYear;
}
