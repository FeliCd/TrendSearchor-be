package com.fpt.swp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body cho POST /api/papers/{id}/copyright-report — user báo cáo bài vi phạm bản quyền. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCopyrightReportRequest {

    @NotBlank(message = "Reason is required")
    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;
}
