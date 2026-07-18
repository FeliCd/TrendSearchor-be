package com.fpt.swp.dto;

import com.fpt.swp.model.CopyrightReport;
import com.fpt.swp.model.CopyrightReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** View phẳng của CopyrightReport — tránh serialize trực tiếp entity có quan hệ LAZY. */
@Data
@Builder
public class CopyrightReportDto {

    private Long id;
    private Long paperId;
    private String paperTitle;
    private String paperStatus;
    private String reportedByMail;
    private String reason;
    private CopyrightReportStatus status;
    private String resolutionNotes;
    private String reviewedByMail;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public static CopyrightReportDto fromEntity(CopyrightReport report) {
        return CopyrightReportDto.builder()
                .id(report.getId())
                .paperId(report.getPaper() != null ? report.getPaper().getId() : null)
                .paperTitle(report.getPaper() != null ? report.getPaper().getTitle() : null)
                .paperStatus(report.getPaper() != null && report.getPaper().getStatus() != null
                        ? report.getPaper().getStatus().name() : null)
                .reportedByMail(report.getReportedBy() != null ? report.getReportedBy().getMail() : null)
                .reason(report.getReason())
                .status(report.getStatus())
                .resolutionNotes(report.getResolutionNotes())
                .reviewedByMail(report.getReviewedBy() != null ? report.getReviewedBy().getMail() : null)
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
