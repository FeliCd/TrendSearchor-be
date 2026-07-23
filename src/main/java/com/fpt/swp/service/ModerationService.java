package com.fpt.swp.service;

import com.fpt.swp.dto.ModerationStatsDto;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.ResearchPaperRepository;
import com.fpt.swp.repository.ResearchTopicRepository;
import com.fpt.swp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for moderators to review and approve/reject uploaded papers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {

    private final ResearchPaperRepository paperRepository;
    private final UserRepository userRepository;
    private final ResearchTopicRepository topicRepository;
    private final NotificationService notificationService;
    private final com.fpt.swp.repository.CopyrightReportRepository copyrightReportRepository;

    /**
     * Get papers filtered by moderation status for the moderation queue.
     */
    @Transactional(readOnly = true)
    public Page<ResearchPaper> getPapersByStatus(PaperStatus status, int page, int size) {
        return paperRepository.findModerationByStatus(status, PageRequest.of(page, size));
    }

    /**
     * Get a single paper for detailed review.
     */
    @Transactional(readOnly = true)
    public ResearchPaper getPaperForReview(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));
    }

    /**
     * Approve a pending paper. Triggers notifications to the uploader and all users.
     */
    @Transactional
    public ResearchPaper approvePaper(Long paperId, Long moderatorId) {
        ResearchPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));

        if (paper.getStatus() != PaperStatus.PENDING) {
            throw new IllegalArgumentException("Paper is not in PENDING status. Current: " + paper.getStatus());
        }

        paper.setStatus(PaperStatus.APPROVED);
        paper.setRejectionReason(null);
        
        // Approve any pending topics associated with this paper
        if (paper.getTopics() != null && !paper.getTopics().isEmpty()) {
            for (ResearchTopic topic : paper.getTopics()) {
                if (topic.getIsApproved() != null && !topic.getIsApproved()) {
                    topic.setIsApproved(true);
                    topicRepository.save(topic);
                }
            }
        }
        
        ResearchPaper saved = paperRepository.save(paper);

        log.info("Paper approved: id={}, title='{}', by moderatorId={}", paperId, paper.getTitle(), moderatorId);

        // Notify the uploader
        if (paper.getUploadedBy() != null) {
            notificationService.createNotification(
                    paper.getUploadedBy().getId(),
                    NotificationType.PAPER_APPROVED,
                    "Your paper has been approved!",
                    String.format("Your paper \"%s\" has been approved and is now visible in search results.",
                            paper.getTitle())
            );
        }

        // Notify all other active users about the new paper
        List<Long> allUserIds = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE && 
                        (paper.getUploadedBy() == null || !u.getId().equals(paper.getUploadedBy().getId())))
                .map(User::getId)
                .toList();

        if (!allUserIds.isEmpty()) {
            notificationService.createBulkNotifications(
                    allUserIds,
                    NotificationType.NEW_PAPER,
                    "New Paper Published!",
                    String.format("A new paper titled \"%s\" is now available on TrendSearchor.", paper.getTitle())
            );
        }

        return saved;
    }

    /**
     * Reject a pending paper with a reason. Notifies the uploader.
     */
    @Transactional
    public ResearchPaper rejectPaper(Long paperId, String reason, Long moderatorId) {
        ResearchPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));

        if (paper.getStatus() != PaperStatus.PENDING) {
            throw new IllegalArgumentException("Paper is not in PENDING status. Current: " + paper.getStatus());
        }

        paper.setStatus(PaperStatus.REJECTED);
        paper.setRejectionReason(reason);
        ResearchPaper saved = paperRepository.save(paper);

        log.info("Paper rejected: id={}, title='{}', reason='{}', by moderatorId={}",
                paperId, paper.getTitle(), reason, moderatorId);

        // Notify the uploader about rejection
        if (paper.getUploadedBy() != null) {
            notificationService.createNotification(
                    paper.getUploadedBy().getId(),
                    NotificationType.PAPER_REJECTED,
                    "Your paper was not approved",
                    String.format("Your paper \"%s\" was not approved. Reason: %s",
                            paper.getTitle(), reason != null ? reason : "No reason provided.")
            );
        }

        return saved;
    }

    /**
     * Get moderation dashboard statistics.
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
