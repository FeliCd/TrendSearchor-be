package com.fpt.swp.service;

import com.fpt.swp.dto.CopyrightReportDto;
import com.fpt.swp.dto.CreateCopyrightReportRequest;
import com.fpt.swp.dto.ResolveCopyrightReportRequest;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.CopyrightReportRepository;
import com.fpt.swp.repository.ResearchPaperRepository;
import com.fpt.swp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Luồng notice-and-takedown cho vi phạm bản quyền:
 * user gửi report → moderator/admin xem xét → DISMISS (bác bỏ) hoặc
 * TAKE_DOWN (gỡ bài khỏi hiển thị công khai + đóng mọi report chờ của bài đó).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CopyrightReportService {

    private final CopyrightReportRepository reportRepository;
    private final ResearchPaperRepository paperRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ─── User: gửi report ──────────────────────────────────────────────────────

    @Transactional
    public CopyrightReportDto submitReport(Long paperId, Long reporterId, CreateCopyrightReportRequest request) {
        ResearchPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + reporterId));

        if (paper.getStatus() == PaperStatus.TAKEN_DOWN) {
            throw new IllegalArgumentException("This paper has already been taken down.");
        }
        if (reportRepository.existsByPaperIdAndReportedByIdAndStatus(
                paperId, reporterId, CopyrightReportStatus.PENDING)) {
            throw new IllegalArgumentException("You already have a pending report for this paper.");
        }

        CopyrightReport report = CopyrightReport.builder()
                .paper(paper)
                .reportedBy(reporter)
                .reason(request.getReason())
                .status(CopyrightReportStatus.PENDING)
                .build();

        CopyrightReport saved = reportRepository.save(report);
        log.info("Copyright report #{} submitted for paper {} by user {}", saved.getId(), paperId, reporterId);
        return CopyrightReportDto.fromEntity(saved);
    }

    // ─── Moderator: xem hàng đợi ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CopyrightReportDto> getReportsByStatus(CopyrightReportStatus status, int page, int size) {
        return reportRepository.findByStatus(status, PageRequest.of(page, size))
                .map(CopyrightReportDto::fromEntity);
    }

    // ─── Moderator: xử lý report ───────────────────────────────────────────────

    @Transactional
    public CopyrightReportDto resolveReport(Long reportId, Long reviewerId, ResolveCopyrightReportRequest request) {
        CopyrightReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Copyright report not found: " + reportId));
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + reviewerId));

        if (report.getStatus() != CopyrightReportStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Report has already been resolved. Current status: " + report.getStatus());
        }

        if (request.getAction() == ResolveCopyrightReportRequest.Action.DISMISS) {
            closeReport(report, reviewer, CopyrightReportStatus.DISMISSED, request.getNotes());
            notifyReporter(report, "Your copyright report was reviewed",
                    "Your report on \"" + report.getPaper().getTitle()
                            + "\" was reviewed and dismissed. The paper remains available.");
        } else {
            takeDownPaper(report, reviewer, request.getNotes());
        }

        return CopyrightReportDto.fromEntity(report);
    }

    private void takeDownPaper(CopyrightReport report, User reviewer, String notes) {
        ResearchPaper paper = report.getPaper();
        paper.setStatus(PaperStatus.TAKEN_DOWN);
        paperRepository.save(paper);
        log.info("Paper {} taken down by moderator {} (report #{})",
                paper.getId(), reviewer.getMail(), report.getId());

        closeReport(report, reviewer, CopyrightReportStatus.ACTION_TAKEN, notes);

        // Đóng mọi report còn chờ của cùng bài — vi phạm đã được xử lý một lần cho tất cả
        List<CopyrightReport> otherPending =
                reportRepository.findByPaperIdAndStatus(paper.getId(), CopyrightReportStatus.PENDING);
        for (CopyrightReport other : otherPending) {
            closeReport(other, reviewer, CopyrightReportStatus.ACTION_TAKEN,
                    "Resolved together with report #" + report.getId());
            notifyReporter(other, "Your copyright report was resolved",
                    "The paper \"" + paper.getTitle() + "\" you reported has been taken down.");
        }

        notifyReporter(report, "Your copyright report was resolved",
                "The paper \"" + paper.getTitle() + "\" you reported has been taken down.");

        // Báo cho uploader biết bài bị gỡ và lý do
        if (paper.getUploadedBy() != null) {
            notificationService.createNotification(
                    paper.getUploadedBy().getId(),
                    NotificationType.SYSTEM,
                    "Your paper has been taken down",
                    "Your paper \"" + paper.getTitle()
                            + "\" was removed following a copyright review."
                            + (notes != null && !notes.isBlank() ? " Moderator notes: " + notes : ""));
        }
    }

    private void closeReport(CopyrightReport report, User reviewer,
                             CopyrightReportStatus status, String notes) {
        report.setStatus(status);
        report.setResolutionNotes(notes);
        report.setReviewedBy(reviewer);
        report.setReviewedAt(LocalDateTime.now());
        reportRepository.save(report);
    }

    private void notifyReporter(CopyrightReport report, String title, String message) {
        if (report.getReportedBy() != null) {
            notificationService.createNotification(
                    report.getReportedBy().getId(), NotificationType.SYSTEM, title, message);
        }
    }
}
