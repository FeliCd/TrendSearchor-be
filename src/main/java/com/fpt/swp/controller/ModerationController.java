package com.fpt.swp.controller;

import com.fpt.swp.dto.ModerationStatsDto;
import com.fpt.swp.service.ModerationService;
import com.fpt.swp.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint moderation còn lại: thống kê + xử lý báo cáo bản quyền.
 *
 * <p>Phần duyệt/từ chối bài đã hợp nhất về {@code ResearchPaperUploadController}
 * ({@code /api/admin/papers/{id}/approve}) — luồng FE thực sự dùng. Các endpoint
 * duyệt/từ chối/list bài trùng lặp ở đây đã được gỡ bỏ.
 */
@RestController
@RequestMapping("/api/moderation")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final com.fpt.swp.service.CopyrightReportService copyrightReportService;
    private final AuthUtils authUtils;

    /**
     * Get moderation dashboard stats.
     * GET /api/moderation/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<ModerationStatsDto> getStats() {
        return ResponseEntity.ok(moderationService.getStats());
    }

    // ─── Copyright reports (notice-and-takedown) ──────────────────────────────

    /**
     * List copyright reports by status for review.
     * GET /api/moderation/copyright-reports?status=PENDING&page=0&size=10
     */
    @GetMapping("/copyright-reports")
    public ResponseEntity<org.springframework.data.domain.Page<com.fpt.swp.dto.CopyrightReportDto>> getCopyrightReports(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        com.fpt.swp.model.CopyrightReportStatus reportStatus =
                com.fpt.swp.model.CopyrightReportStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(copyrightReportService.getReportsByStatus(reportStatus, page, size));
    }

    /**
     * Resolve a copyright report: DISMISS keeps the paper, TAKE_DOWN removes it
     * from public view and closes every pending report on the same paper.
     * PATCH /api/moderation/copyright-reports/{id}/resolve
     */
    @PatchMapping("/copyright-reports/{id}/resolve")
    public ResponseEntity<com.fpt.swp.dto.CopyrightReportDto> resolveCopyrightReport(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody com.fpt.swp.dto.ResolveCopyrightReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long reviewerId = authUtils.extractUserId(userDetails);
        return ResponseEntity.ok(copyrightReportService.resolveReport(id, reviewerId, request));
    }
}
