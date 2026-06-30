package com.fpt.swp.service;

import com.fpt.swp.dto.ModerationStatsDto;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.ResearchPaperRepository;
import com.fpt.swp.repository.UserRepository;
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
    private final NotificationService notificationService;

    /**
     * Get papers filtered by upload status for moderation queue.
     */
    @Transactional(readOnly = true)
    public Page<ResearchPaper> getPapersByStatus(UploadStatus status, int page, int size) {
        return paperRepository.findByUploadStatus(status, PageRequest.of(page, size));
    }

    /**
     * Get a single paper for detailed review.
     */
    @Transactional(readOnly = true)
    public ResearchPaper getPaperForReview(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));
    }

    /**
     * Approve a pending paper. Triggers notifications to the uploader and all users.
     */
    @Transactional
    public ResearchPaper approvePaper(Long paperId, Long moderatorId) {
        ResearchPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));

        if (paper.getUploadStatus() != UploadStatus.PENDING) {
            throw new RuntimeException("Paper is not in PENDING status. Current: " + paper.getUploadStatus());
        }

        paper.setUploadStatus(UploadStatus.APPROVED);
        paper.setRejectionReason(null);
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

        // Notify all users about the new paper
        notifyUsersNewPaper(paper);

        return saved;
    }

    /**
     * Reject a pending paper with a reason. Notifies the uploader.
     */
    @Transactional
    public ResearchPaper rejectPaper(Long paperId, String reason, Long moderatorId) {
        ResearchPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));

        if (paper.getUploadStatus() != UploadStatus.PENDING) {
            throw new RuntimeException("Paper is not in PENDING status. Current: " + paper.getUploadStatus());
        }

        paper.setUploadStatus(UploadStatus.REJECTED);
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
                .pendingCount(paperRepository.countByUploadStatus(UploadStatus.PENDING))
                .approvedCount(paperRepository.countByUploadStatus(UploadStatus.APPROVED))
                .rejectedCount(paperRepository.countByUploadStatus(UploadStatus.REJECTED))
                .totalUploads(paperRepository.countUserUploads())
                .build();
    }

    /**
     * Notify all researchers and users that a new paper is available.
     */
    private void notifyUsersNewPaper(ResearchPaper paper) {
        List<User> researchers = userRepository.findByRole(Role.RESEARCHER);
        List<User> users = userRepository.findByRole(Role.USER);

        String title = "New research paper available";
        String message = String.format("A new paper has been published: \"%s\"", paper.getTitle());

        for (User user : researchers) {
            // Skip the uploader — they already got an "approved" notification
            if (paper.getUploadedBy() != null && user.getId().equals(paper.getUploadedBy().getId())) continue;
            try {
                notificationService.createNotification(user.getId(), NotificationType.NEW_PAPER, title, message);
            } catch (Exception e) {
                log.warn("Failed to notify user {}: {}", user.getId(), e.getMessage());
            }
        }
        for (User user : users) {
            try {
                notificationService.createNotification(user.getId(), NotificationType.NEW_PAPER, title, message);
            } catch (Exception e) {
                log.warn("Failed to notify user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
