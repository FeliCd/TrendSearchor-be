package com.fpt.swp.service;

import com.fpt.swp.dto.PaperUploadRequest;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.KeywordRepository;
import com.fpt.swp.repository.ResearchPaperRepository;
import com.fpt.swp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for handling researcher paper uploads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaperUploadService {

    private final ResearchPaperRepository paperRepository;
    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Upload a new paper as a researcher. Paper goes into PENDING status.
     */
    @Transactional
    public ResearchPaper uploadPaper(PaperUploadRequest request, Long userId) {
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build the paper entity
        ResearchPaper paper = ResearchPaper.builder()
                .title(request.getTitle())
                .abstractText(request.getAbstractText())
                .year(request.getYear())
                .pdfUrl(request.getPdfUrl())
                .source(PaperSource.USER_UPLOAD)
                .uploadStatus(UploadStatus.PENDING)
                .uploadedBy(uploader)
                .citationCount(0)
                .openAccess(true)
                .build();

        // Link keywords
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            Set<Keyword> keywords = new HashSet<>();
            for (String kwName : request.getKeywords()) {
                String normalized = kwName.trim().toLowerCase();
                if (normalized.isEmpty()) continue;

                Keyword keyword = keywordRepository.findByName(normalized)
                        .orElseGet(() -> keywordRepository.save(
                                Keyword.builder()
                                        .name(normalized)
                                        .displayName(kwName.trim())
                                        .build()
                        ));
                keywords.add(keyword);
            }
            paper.setKeywords(keywords);
        }

        ResearchPaper saved = paperRepository.save(paper);
        log.info("Paper uploaded: id={}, title='{}', by userId={}", saved.getId(), saved.getTitle(), userId);

        // Notify all moderators about the new upload
        notifyModerators(saved, uploader);

        return saved;
    }

    /**
     * Get papers uploaded by a specific user.
     */
    @Transactional(readOnly = true)
    public Page<ResearchPaper> getMyUploads(Long userId, int page, int size) {
        return paperRepository.findByUploadedBy(userId, PageRequest.of(page, size));
    }

    /**
     * Notify all moderators and admins about a new paper upload.
     */
    private void notifyModerators(ResearchPaper paper, User uploader) {
        String uploaderName = uploader.getFullName() != null ? uploader.getFullName() : uploader.getMail();

        List<User> moderators = userRepository.findByRole(Role.MODERATOR);
        List<User> admins = userRepository.findByRole(Role.ADMIN);

        String title = "New paper submitted for review";
        String message = String.format("Researcher %s uploaded: \"%s\". Please review and approve/reject.",
                uploaderName, paper.getTitle());

        for (User mod : moderators) {
            try {
                notificationService.createNotification(
                        mod.getId(), NotificationType.PAPER_UPLOADED, title, message);
            } catch (Exception e) {
                log.warn("Failed to notify moderator {}: {}", mod.getId(), e.getMessage());
            }
        }
        for (User admin : admins) {
            try {
                notificationService.createNotification(
                        admin.getId(), NotificationType.PAPER_UPLOADED, title, message);
            } catch (Exception e) {
                log.warn("Failed to notify admin {}: {}", admin.getId(), e.getMessage());
            }
        }
    }
}
