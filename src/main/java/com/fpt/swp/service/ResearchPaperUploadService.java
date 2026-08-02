package com.fpt.swp.service;

import com.fpt.swp.dto.AuthorDto;
import com.fpt.swp.dto.PaperApprovalRequest;
import com.fpt.swp.dto.PaperDto;
import com.fpt.swp.dto.UploadPaperRequest;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearchPaperUploadService {

    private final ResearchPaperRepository paperRepository;
    private final AuthorRepository authorRepository;
    private final JournalRepository journalRepository;
    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final NotificationService notificationService;

    /**
     * Phiên bản Terms of Service hiện hành — được đóng dấu vào từng bản ghi upload
     * làm bằng chứng uploader đã đồng ý điều khoản nào. Không tin giá trị từ client.
     */
    @org.springframework.beans.factory.annotation.Value("${app.legal.terms-version:1.0}")
    private String currentTermsVersion;

    @Transactional
    public PaperDto uploadPaper(UploadPaperRequest request, User user, String clientIp) {
        log.info("User {} uploading research paper: {}", user.getMail(), request.getTitle());

        ResearchPaper paper = new ResearchPaper();
        paper.setTitle(request.getTitle());
        paper.setAbstractText(request.getAbstractText());
        paper.setYear(request.getYear());
        paper.setPaperUri(request.getPaperUri());
        paper.setStatus(PaperStatus.PENDING);
        paper.setSource(PaperSource.USER_UPLOAD);
        paper.setUploadedBy(user);
        paper.setExternalId("uploaded_" + UUID.randomUUID());
        paper.setCitationCount(0);
        paper.setOpenAccess(true);

        // ─── Legal / Copyright: lưu khai báo của uploader + audit trail ─────────
        paper.setLicense(request.getLicense());
        paper.setPublicationType(request.getPublicationType());
        // isSelfPublished suy ra từ khai báo, không hardcode: chỉ luận văn/nghiên cứu
        // gốc mới là self-published; bài đăng lại thì publisher có thể giữ bản quyền.
        paper.setIsSelfPublished(request.getPublicationType() == PublicationType.ORIGINAL_THESIS);
        paper.setOwnershipConfirmed(Boolean.TRUE.equals(request.getOwnershipConfirmed()));
        paper.setTermsAccepted(Boolean.TRUE.equals(request.getTermsAccepted()));
        paper.setTermsVersion(currentTermsVersion);
        paper.setTermsAcceptedAt(java.time.LocalDateTime.now());
        paper.setUploadedByIp(clientIp);
        paper.setEmbargoUntil(request.getEmbargoUntil());

        if (request.getAuthors() != null) {
            for (String authorName : request.getAuthors()) {
                if (authorName == null || authorName.isBlank())
                    continue;
                Author author = authorRepository.findByNameIgnoreCase(authorName.trim())
                        .orElseGet(() -> {
                            Author a = new Author();
                            a.setName(authorName.trim());
                            a.setExternalId("uploaded_author_" + UUID.randomUUID());
                            return authorRepository.save(a);
                        });
                paper.getAuthors().add(author);
            }
        }

        if (request.getJournals() != null) {
            for (String journalName : request.getJournals()) {
                if (journalName == null || journalName.isBlank())
                    continue;
                Journal journal = journalRepository.findByNameIgnoreCase(journalName.trim())
                        .orElseGet(() -> {
                            Journal j = new Journal();
                            j.setName(journalName.trim());
                            j.setExternalId("uploaded_journal_" + UUID.randomUUID());
                            return journalRepository.save(j);
                        });
                paper.getJournals().add(journal);
            }
        }

        if (request.getKeywords() != null) {
            for (String kwName : request.getKeywords()) {
                if (kwName == null || kwName.isBlank())
                    continue;
                String normalizedKw = kwName.trim().toLowerCase();
                Keyword keyword = keywordRepository.findByName(normalizedKw)
                        .orElseGet(() -> {
                            Keyword k = new Keyword();
                            k.setName(normalizedKw);
                            k.setDisplayName(kwName.trim());
                            return keywordRepository.save(k);
                        });
                paper.getKeywords().add(keyword);
            }
        }

        ResearchPaper savedPaper = paperRepository.save(paper);

        // FR3: Notify all admins
        try {
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            for (User admin : admins) {
                notificationService.createNotification(
                        admin.getId(),
                        NotificationType.SYSTEM,
                        "New Research Paper Submitted",
                        "Researcher " + user.getFullName() + " (" + user.getMail() + ") has submitted a new paper: "
                                + paper.getTitle());
            }
        } catch (Exception e) {
            log.error("Failed to send submission notification: {}", e.getMessage());
        }

        return mapLocalPaperToDto(savedPaper);
    }

    @Transactional
    public PaperDto approveOrRejectPaper(Long paperId, PaperApprovalRequest request, User admin) {
        log.info("Admin {} reviewing paper {}: {}", admin.getMail(), paperId, request.getStatus());

        ResearchPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Research paper not found"));

        PaperStatus newStatus = PaperStatus.valueOf(request.getStatus());
        paper.setStatus(newStatus);
        paper.setApprovedBy(admin);
        paper.setStatusComments(request.getComments());

        ResearchPaper savedPaper = paperRepository.save(paper);

        // Notification 1: Notify the RESEARCHER (paper author) about approval/rejection result
        if (paper.getUploadedBy() != null) {
            try {
                String researcherTitle;
                String researcherMessage;
                if (newStatus == PaperStatus.APPROVED) {
                    researcherTitle = "Your Paper Has Been Approved";
                    researcherMessage = "Congratulations! Your paper \"" + paper.getTitle()
                            + "\" has been approved by the admin and is now publicly available.";
                } else {
                    researcherTitle = "Your Paper Has Been Rejected";
                    researcherMessage = "Your paper \"" + paper.getTitle() + "\" has been rejected. Feedback: "
                            + (request.getComments() != null && !request.getComments().isBlank()
                               ? request.getComments()
                               : "No feedback provided.");
                }
                notificationService.createNotification(
                        paper.getUploadedBy().getId(),
                        newStatus == PaperStatus.APPROVED ? NotificationType.PAPER_APPROVED : NotificationType.PAPER_REJECTED,
                        researcherTitle,
                        researcherMessage);
            } catch (Exception e) {
                log.error("Failed to send approval/rejection notification to researcher: {}", e.getMessage());
            }
        }

        // Notification 2: Broadcast to ALL other users (except the uploader) when paper is APPROVED
        if (newStatus == PaperStatus.APPROVED) {
            try {
                String researcherName = (paper.getUploadedBy() != null && paper.getUploadedBy().getFullName() != null)
                        ? paper.getUploadedBy().getFullName()
                        : "a researcher";

                Long uploaderId = paper.getUploadedBy() != null ? paper.getUploadedBy().getId() : null;

                List<Long> broadcastUserIds = userRepository.findAll().stream()
                        .map(User::getId)
                        // Exclude the uploader themselves
                        .filter(id -> uploaderId == null || !id.equals(uploaderId))
                        .collect(Collectors.toList());

                if (!broadcastUserIds.isEmpty()) {
                    notificationService.createBulkNotifications(
                            broadcastUserIds,
                            NotificationType.NEW_PAPER,
                            "New Research Paper Available",
                            "Researcher " + researcherName + " has just published a new paper: \""
                                    + paper.getTitle() + "\". Check it out now!");
                }
            } catch (Exception e) {
                log.error("Failed to send new paper broadcast to all users: {}", e.getMessage());
            }

            // Notification 3 (existing): Notify topic/journal followers
            try {
                Set<Long> followerIds = new HashSet<>();
                for (ResearchTopic topic : paper.getTopics()) {
                    followerIds.addAll(userFollowRepository.findUserIdsByTopicId(topic.getId()));
                }
                for (Journal journal : paper.getJournals()) {
                    followerIds.addAll(userFollowRepository.findUserIdsByJournalId(journal.getId()));
                }
                if (paper.getUploadedBy() != null) {
                    followerIds.remove(paper.getUploadedBy().getId());
                }
                if (!followerIds.isEmpty()) {
                    notificationService.createBulkNotifications(
                            new ArrayList<>(followerIds),
                            NotificationType.SYSTEM,
                            "New Paper Available in Your Followed Topics",
                            "A new paper matching your followed topics/journals has been approved: \""
                                    + paper.getTitle() + "\"");
                }
            } catch (Exception e) {
                log.error("Failed to send follower notifications: {}", e.getMessage());
            }
        }

        return mapLocalPaperToDto(savedPaper);
    }

    @Transactional(readOnly = true)
    public Page<PaperDto> getPendingPapers(String search, Pageable pageable) {
        return paperRepository.findPendingWithSearch(search, pageable)
                .map(this::mapLocalPaperToDto);
    }

    @Transactional(readOnly = true)
    public Page<PaperDto> getModerationHistory(String search, PaperStatus status, Pageable pageable) {
        return paperRepository.findHistoryWithSearchAndStatus(search, status, pageable)
                .map(this::mapLocalPaperToDto);
    }

    @Transactional
    public PaperDto revokePaper(Long paperId, User admin) {
        log.info("Admin {} revoking paper {}", admin.getMail(), paperId);

        ResearchPaper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new EntityNotFoundException("Research paper not found"));

        if (paper.getStatus() != PaperStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only an approved paper can be revoked. Current status: " + paper.getStatus());
        }

        // Thu hồi: gỡ khỏi công khai. Giữ approvedBy làm lịch sử. Muốn đăng lại thì
        // admin gọi /api/admin/papers/{id}/approve với status=APPROVED.
        paper.setStatus(PaperStatus.REVOKED);

        ResearchPaper savedPaper = paperRepository.save(paper);

        // Notify researcher that the paper has been revoked
        if (paper.getUploadedBy() != null) {
            try {
                notificationService.createNotification(
                        paper.getUploadedBy().getId(),
                        NotificationType.PAPER_REVOKED,
                        "Your paper has been revoked",
                        "Your paper \"" + paper.getTitle() + "\" has been revoked by the admin and is no longer publicly visible."
                );
            } catch (Exception e) {
                log.error("Failed to send revocation notification to researcher: {}", e.getMessage());
            }
        }

        return mapLocalPaperToDto(savedPaper);
    }

    @Transactional(readOnly = true)
    public Page<PaperDto> getMyUploadedPapers(Long userId, Pageable pageable) {
        return paperRepository.findByUploadedById(userId, pageable)
                .map(this::mapLocalPaperToDto);
    }

    @Transactional(readOnly = true)
    public List<com.fpt.swp.dto.ResearcherLeaderboardDto> getTopResearchersLeaderboard(int limit) {
        return paperRepository.findTopResearchersByApprovedPapersCount(org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<com.fpt.swp.dto.ResearcherLeaderboardDto> getLeaderboard(int limit) {
        return getTopResearchersLeaderboard(limit);
    }

    private PaperDto mapLocalPaperToDto(ResearchPaper paper) {
        return PaperDto.builder()
                .id(paper.getId())
                .externalId(paper.getExternalId())
                .title(paper.getTitle())
                .abstractText(paper.getAbstractText())
                .year(paper.getYear())
                .citationCount(paper.getCitationCount())
                .openAccess(paper.getOpenAccess())
                .paperUri(paper.getPaperUri())
                .status(paper.getStatus() != null ? paper.getStatus().name() : null)
                .uploadStatus(paper.getUploadStatus() != null ? paper.getUploadStatus().name() : null)
                .rejectionReason(paper.getRejectionReason())
                .uploadedBy(paper.getUploadedBy() != null ? paper.getUploadedBy().getMail() : null)
                .isSelfPublished(paper.getIsSelfPublished())
                .statusComments(paper.getStatusComments())
                .license(paper.getLicense() != null ? paper.getLicense().name() : null)
                .publicationType(paper.getPublicationType() != null ? paper.getPublicationType().name() : null)
                .embargoUntil(paper.getEmbargoUntil())
                .authors(paper.getAuthors().stream()
                        .map(a -> AuthorDto.builder()
                                .id(a.getId())
                                .externalId(a.getExternalId())
                                .name(a.getName())
                                .orcid(a.getOrcid())
                                .hIndex(a.getHIndex())
                                .paperCount(a.getPaperCount())
                                .build())
                        .collect(Collectors.toList()))
                .journals(paper.getJournals().stream().map(Journal::getName).collect(Collectors.toList()))
                .keywords(paper.getKeywords().stream().map(Keyword::getName).collect(Collectors.toList()))
                .build();
    }
}
