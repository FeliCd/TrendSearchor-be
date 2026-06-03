package com.fpt.swp.service;

import com.fpt.swp.dto.PaperNoteDto;
import com.fpt.swp.dto.PaperNoteRequest;
import com.fpt.swp.model.PaperNote;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.PaperNoteRepository;
import com.fpt.swp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaperNoteService {

    private final PaperNoteRepository paperNoteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PaperNoteDto getNote(Long userId, String externalId) {
        Optional<PaperNote> note = paperNoteRepository.findByUserIdAndPaperExternalId(userId, externalId);
        return note.map(this::mapToDto).orElse(null);
    }

    @Transactional
    public PaperNoteDto saveNote(Long userId, String externalId, PaperNoteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PaperNote note = paperNoteRepository.findByUserIdAndPaperExternalId(userId, externalId)
                .orElse(PaperNote.builder()
                        .user(user)
                        .paperExternalId(externalId)
                        .build());

        note.setContent(request.getContent());
        PaperNote saved = paperNoteRepository.save(note);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteNote(Long userId, String externalId) {
        paperNoteRepository.findByUserIdAndPaperExternalId(userId, externalId)
                .ifPresent(paperNoteRepository::delete);
    }

    private PaperNoteDto mapToDto(PaperNote note) {
        return PaperNoteDto.builder()
                .id(note.getId())
                .paperExternalId(note.getPaperExternalId())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
