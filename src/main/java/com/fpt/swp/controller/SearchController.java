package com.fpt.swp.controller;

import com.fpt.swp.dto.*;
import com.fpt.swp.model.ResearchPaper;
import com.fpt.swp.service.DashboardService;
import com.fpt.swp.service.SearchService;
import com.fpt.swp.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Streamable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final DashboardService dashboardService;
    private final AuthUtils authUtils;

    @GetMapping("/papers/search")
    public ResponseEntity<PaperSearchResponse> searchPapers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String journal,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);

        // Convert date strings to year integers for filtering
        Integer yearFrom = null;
        Integer yearTo = null;
        if (dateFrom != null && dateFrom.length() >= 4) {
            try { yearFrom = Integer.parseInt(dateFrom.substring(0, 4)); } catch (Exception ignored) {}
        }
        if (dateTo != null && dateTo.length() >= 4) {
            try { yearTo = Integer.parseInt(dateTo.substring(0, 4)); } catch (Exception ignored) {}
        }

        PaperSearchRequest request = PaperSearchRequest.builder()
                .query(query)
                .page(page)
                .size(size)
                .year(year)
                .yearFrom(yearFrom)
                .yearTo(yearTo)
                .dateFromStr(dateFrom) // full date for day/month precision
                .dateToStr(dateTo)
                .journal(journal)
                .author(author)
                .sortBy(sortBy)
                .build();

        PaperSearchResponse response = searchService.searchPapers(request, userId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/papers")
    public ResponseEntity<Page<ResearchPaper>> getLocalPapers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(searchService.searchLocalPapers(q, page, size));
        }
        return ResponseEntity.ok(Page.empty());
    }

    @GetMapping("/papers/{id}")
    public ResponseEntity<PaperDto> getPaperById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        PaperDto paper = searchService.getPaperById(id, userId);
        return paper != null ? ResponseEntity.ok(paper) : ResponseEntity.notFound().build();
    }

    @GetMapping("/journals")
    public ResponseEntity<Page<ResearchPaper>> getPapersByJournal(
            @RequestParam Long journalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Streamable<ResearchPaper> filtered = searchService.searchLocalPapers(".", page, size * 10)
                .filter(p -> p.getJournals().stream().anyMatch(j -> j.getId().equals(journalId)));
        List<ResearchPaper> list = filtered.toList();
        int fromIndex = (int) PageRequest.of(page, size).getOffset();
        int toIndex = Math.min(fromIndex + size, list.size());
        List<ResearchPaper> pageContent = fromIndex < list.size() ? list.subList(fromIndex, toIndex) : List.of();
        return ResponseEntity.ok(new PageImpl<>(pageContent, PageRequest.of(page, size), list.size()));
    }

    @GetMapping("/journals/search")
    public ResponseEntity<Page<com.fpt.swp.model.Journal>> searchJournals(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(searchService.searchJournals(query, page, size));
    }

    @GetMapping("/journals/{id}")
    public ResponseEntity<JournalDto> getJournalById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        JournalDto journal = searchService.getJournalById(id, userId);
        return journal != null ? ResponseEntity.ok(journal) : ResponseEntity.notFound().build();
    }

    @GetMapping("/authors/search")
    public ResponseEntity<Page<com.fpt.swp.model.Author>> searchAuthors(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(searchService.searchAuthors(query, page, size));
    }

    @GetMapping("/authors/{id}")
    public ResponseEntity<AuthorDto> getAuthorById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        AuthorDto author = searchService.getAuthorById(id, userId);
        return author != null ? ResponseEntity.ok(author) : ResponseEntity.notFound().build();
    }

    @GetMapping("/keywords/search")
    public ResponseEntity<Page<com.fpt.swp.model.Keyword>> searchKeywords(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(searchService.searchKeywords(query, page, size));
    }

    @GetMapping("/topics")
    public ResponseEntity<Page<com.fpt.swp.model.ResearchTopic>> getTopics(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(searchService.searchTopics(query, page, size));
    }

    @GetMapping("/top-papers")
    public ResponseEntity<List<PaperDto>> getTopPapers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(dashboardService.getTopPapers(limit));
    }

}
