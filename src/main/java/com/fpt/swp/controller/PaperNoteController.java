package com.fpt.swp.controller;

import com.fpt.swp.dto.ApiResponse;
import com.fpt.swp.dto.PaperNoteDto;
import com.fpt.swp.dto.PaperNoteRequest;
import com.fpt.swp.util.AuthUtils;
import com.fpt.swp.service.PaperNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class PaperNoteController {

    private final PaperNoteService paperNoteService;
    private final AuthUtils authUtils;

    @GetMapping("/{externalId}")
    public ResponseEntity<PaperNoteDto> getNote(
            @PathVariable String externalId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = authUtils.extractUserId(userDetails);
        PaperNoteDto note = paperNoteService.getNote(userId, externalId);
        return ResponseEntity.ok(note);
    }

    @PutMapping("/{externalId}")
    public ResponseEntity<PaperNoteDto> saveNote(
            @PathVariable String externalId,
            @RequestBody PaperNoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = authUtils.extractUserId(userDetails);
        PaperNoteDto note = paperNoteService.saveNote(userId, externalId, request);
        return ResponseEntity.ok(note);
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable String externalId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = authUtils.extractUserId(userDetails);
        paperNoteService.deleteNote(userId, externalId);
        return ResponseEntity.ok().build();
    }
}
