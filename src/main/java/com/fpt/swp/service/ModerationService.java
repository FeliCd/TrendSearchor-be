package com.fpt.swp.service;

import com.fpt.swp.dto.ModerationStatsDto;
import com.fpt.swp.model.PaperStatus;
import com.fpt.swp.repository.ResearchPaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thống kê phục vụ trang moderation.
 *
 * <p>Việc duyệt/từ chối bài đã được HỢP NHẤT về
 * {@link ResearchPaperUploadService#approveOrRejectPaper} (endpoint
 * {@code POST /api/admin/papers/{id}/approve}) — đó là luồng FE thực sự dùng.
 * Bản duyệt/từ chối trùng lặp trước đây ở service này đã được gỡ để tránh code chết
 * và tránh notification bị lệch giữa hai luồng.
 */
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ResearchPaperRepository paperRepository;
    private final com.fpt.swp.repository.CopyrightReportRepository copyrightReportRepository;

    /**
     * Thống kê dashboard moderation (số bài theo trạng thái + báo cáo bản quyền chờ xử lý).
     */
    @Transactional(readOnly = true)
    public ModerationStatsDto getStats() {
        return ModerationStatsDto.builder()
                .pendingCount(paperRepository.countByStatus(PaperStatus.PENDING))
                .approvedCount(paperRepository.countByStatus(PaperStatus.APPROVED))
                .rejectedCount(paperRepository.countByStatus(PaperStatus.REJECTED))
                .totalUploads(paperRepository.countUserUploads())
                .takenDownCount(paperRepository.countByStatus(PaperStatus.TAKEN_DOWN))
                .revokedCount(paperRepository.countByStatus(PaperStatus.REVOKED))
                .pendingCopyrightReports(copyrightReportRepository.countByStatus(
                        com.fpt.swp.model.CopyrightReportStatus.PENDING))
                .build();
    }
}
