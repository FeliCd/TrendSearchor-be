package com.fpt.swp.controller;

import com.fpt.swp.dto.ModerationStatsDto;
import com.fpt.swp.model.PaperStatus;
import com.fpt.swp.model.ResearchPaper;
import com.fpt.swp.service.ModerationService;
import com.fpt.swp.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;
    private final com.fpt.swp.service.CopyrightReportService copyrightReportService;
    private final AuthUtils authUtils;

    /**
     * List papers by upload status for moderation.
     * GET /api/moderation/papers?status=PENDING&page=0&size=10
     */
    @GetMapping("/papers")
    public ResponseEntity<Page<ResearchPaper>> getPapers(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PaperStatus paperStatus = PaperStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(moderationService.getPapersByStatus(paperStatus, page, size));
    }

    /**
     * Get a specific paper for detailed review.
     * GET /api/moderation/papers/{id}
     */
    @GetMapping("/papers/{id}")
    public ResponseEntity<ResearchPaper> getPaper(@PathVariable Long id) {
        return ResponseEntity.ok(moderationService.getPaperForReview(id));
    }

    /**
     * Approve a pending paper.
     * PATCH /api/moderation/papers/{id}/approve
     */
    @PatchMapping("/papers/{id}/approve")
    public ResponseEntity<?> approvePaper(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long moderatorId = authUtils.extractUserId(userDetails);
        ResearchPaper paper = moderationService.approvePaper(id, moderatorId);
        return ResponseEntity.ok(Map.of(
                "message", "Paper approved successfully",
                "paperId", paper.getId(),
                "status", paper.getStatus().name()
        ));
    }

    /**
     * Reject a pending paper with a reason.
     * PATCH /api/moderation/papers/{id}/reject
     * Body: { "reason": "..." }
     */
    @PatchMapping("/papers/{id}/reject")
    public ResponseEntity<?> rejectPaper(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long moderatorId = authUtils.extractUserId(userDetails);
        String reason = body.getOrDefault("reason", "No reason provided");
        ResearchPaper paper = moderationService.rejectPaper(id, reason, moderatorId);
        return ResponseEntity.ok(Map.of(
                "message", "Paper rejected",
                "paperId", paper.getId(),
                "status", paper.getStatus().name(),
                "reason", reason
        ));
    }

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
