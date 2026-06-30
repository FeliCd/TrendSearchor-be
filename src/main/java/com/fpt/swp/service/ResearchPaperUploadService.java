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

    @Transactional
    public PaperDto uploadPaper(UploadPaperRequest request, User user) {
        log.info("User {} uploading research paper: {}", user.getMail(), request.getTitle());

        ResearchPaper paper = new ResearchPaper();
        paper.setTitle(request.getTitle());
        paper.setAbstractText(request.getAbstractText());
        paper.setYear(request.getYear());
        paper.setPaperUri(request.getPaperUri());
        paper.setStatus(PaperStatus.PENDING);
        paper.setUploadedBy(user);
        paper.setExternalId("uploaded_" + UUID.randomUUID());
        paper.setCitationCount(0);
        paper.setOpenAccess(true);

        if (request.getAuthors() != null) {
            for (String authorName : request.getAuthors()) {
                if (authorName == null || authorName.isBlank()) continue;
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
                if (journalName == null || journalName.isBlank()) continue;
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
                if (kwName == null || kwName.isBlank()) continue;
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
        return mapLocalPaperToDto(savedPaper);
    }

    @Transactional(readOnly = true)
    public Page<PaperDto> getPendingPapers(Pageable pageable) {
        return paperRepository.findByStatus(PaperStatus.PENDING, pageable)
                .map(this::mapLocalPaperToDto);
    }

    @Transactional(readOnly = true)
    public Page<PaperDto> getMyUploadedPapers(Long userId, Pageable pageable) {
        return paperRepository.findByUploadedById(userId, pageable)
                .map(this::mapLocalPaperToDto);
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
                .uploadedBy(paper.getUploadedBy() != null ? paper.getUploadedBy().getMail() : null)
                .statusComments(paper.getStatusComments())
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
