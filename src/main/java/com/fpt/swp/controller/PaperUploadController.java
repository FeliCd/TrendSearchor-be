package com.fpt.swp.controller;

import com.fpt.swp.dto.PaperDto;
import com.fpt.swp.dto.PaperUploadRequest;
import com.fpt.swp.model.ResearchPaper;
import com.fpt.swp.service.PaperUploadService;
import com.fpt.swp.service.SearchService;
import com.fpt.swp.util.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperUploadController {

    private final PaperUploadService paperUploadService;
    private final SearchService searchService;
    private final AuthUtils authUtils;

    /**
     * Upload a new research paper for moderation review.
     * Only RESEARCHER can upload.
     * POST /api/papers/upload
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('RESEARCHER')")
    public ResponseEntity<?> uploadPaper(
            @Valid @RequestBody PaperUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        ResearchPaper paper = paperUploadService.uploadPaper(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Paper submitted for review",
                "paperId", paper.getId(),
                "status", paper.getUploadStatus().name()
        ));
    }

    /**
     * Get papers uploaded by the current user.
     * GET /api/papers/my-uploads?page=0&size=10
     */
    @GetMapping("/my-uploads")
    @PreAuthorize("hasRole('RESEARCHER')")
    public ResponseEntity<Page<PaperDto>> getMyUploads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        Page<ResearchPaper> papers = paperUploadService.getMyUploads(userId, page, size);
        return ResponseEntity.ok(papers.map(searchService::mapLocalPaperToDto));
    }
}
