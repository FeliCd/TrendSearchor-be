package com.fpt.swp.controller;

import com.fpt.swp.dto.PaperApprovalRequest;
import com.fpt.swp.dto.PaperDto;
import com.fpt.swp.dto.UploadPaperRequest;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.UserRepository;
import com.fpt.swp.service.ResearchPaperUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://localhost:5173",
                "https://trend-searchor-fe.vercel.app",
                "https://trend-searchor-fe-*.vercel.app",
                "https://trendsearchor-be-production.up.railway.app"
        },
        allowCredentials = "true",
        allowedHeaders = {"Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"}
)
public class ResearchPaperUploadController {

    private final ResearchPaperUploadService uploadService;
    private final UserRepository userRepository;

    @PostMapping("/api/papers/upload")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<PaperDto> uploadPaper(@Valid @RequestBody UploadPaperRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PaperDto paperDto = uploadService.uploadPaper(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(paperDto);
    }

    @GetMapping("/api/papers/my-uploads")
    @PreAuthorize("hasAnyRole('RESEARCHER', 'ADMIN')")
    public ResponseEntity<Page<PaperDto>> getMyUploads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<PaperDto> papers = uploadService.getMyUploadedPapers(user.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(papers);
    }

    @GetMapping("/api/admin/papers/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaperDto>> getPendingPapers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PaperDto> papers = uploadService.getPendingPapers(search, PageRequest.of(page, size));
        return ResponseEntity.ok(papers);
    }

    @GetMapping("/api/admin/papers/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaperDto>> getModerationHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.fpt.swp.model.PaperStatus paperStatus = null;
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            try {
                paperStatus = com.fpt.swp.model.PaperStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid status
            }
        }
        Page<PaperDto> papers = uploadService.getModerationHistory(search, paperStatus, PageRequest.of(page, size));
        return ResponseEntity.ok(papers);
    }

    @GetMapping("/api/papers/leaderboard")
    public ResponseEntity<java.util.List<com.fpt.swp.dto.ResearcherLeaderboardDto>> getLeaderboard(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(uploadService.getTopResearchersLeaderboard(limit));
    }

    @PostMapping("/api/admin/papers/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaperDto> revokePaper(
            @PathVariable Long id,
            @RequestBody(required = false) com.fpt.swp.dto.PaperRevokeRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByMail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        String comments = request != null ? request.getComments() : null;
        PaperDto revoked = uploadService.revokePaper(id, admin, comments);
        return ResponseEntity.ok(revoked);
    }

    @PostMapping("/api/admin/papers/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaperDto> approvePaper(
            @PathVariable Long id,
            @Valid @RequestBody PaperApprovalRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByMail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        PaperDto approved = uploadService.approveOrRejectPaper(id, request, admin);
        return ResponseEntity.ok(approved);
    }
}
