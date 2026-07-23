package com.fpt.swp.controller;

import com.fpt.swp.dto.CopyrightReportDto;
import com.fpt.swp.dto.CreateCopyrightReportRequest;
import com.fpt.swp.service.CopyrightReportService;
import com.fpt.swp.util.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Cho phép user đăng nhập báo cáo một bài vi phạm bản quyền (notice-and-takedown).
 * Phần xử lý report nằm ở /api/moderation/copyright-reports (ModerationController).
 */
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class CopyrightReportController {

    private final CopyrightReportService copyrightReportService;
    private final AuthUtils authUtils;

    @PostMapping("/{id}/copyright-report")
    public ResponseEntity<?> reportPaper(
            @PathVariable Long id,
            @Valid @RequestBody CreateCopyrightReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) return ResponseEntity.status(401).build();

        CopyrightReportDto report = copyrightReportService.submitReport(id, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }
}
