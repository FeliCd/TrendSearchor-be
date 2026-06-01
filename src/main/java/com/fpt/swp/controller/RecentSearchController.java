package com.fpt.swp.controller;

import com.fpt.swp.model.RecentSearch;
import com.fpt.swp.model.SearchType;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.RecentSearchRepository;
import com.fpt.swp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recent-searches")
@PreAuthorize("isAuthenticated()")
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
public class RecentSearchController {

    private final RecentSearchRepository recentSearchRepository;
    private final UserRepository userRepository;

    public RecentSearchController(RecentSearchRepository recentSearchRepository, UserRepository userRepository) {
        this.recentSearchRepository = recentSearchRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<RecentSearch>> getRecentSearches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50));

        if (type != null && !type.isBlank()) {
            SearchType searchType = SearchType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(recentSearchRepository.findByUserIdAndType(userId, searchType, pageable));
        }
        return ResponseEntity.ok(recentSearchRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(recentSearchRepository.findDistinctQueriesByUserId(userId, PageRequest.of(0, Math.min(limit, 20))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecentSearch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        recentSearchRepository.deleteByIdAndUserId(id, userId);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }

    @DeleteMapping
    public ResponseEntity<?> clearRecentSearches(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        recentSearchRepository.deleteAllByUserId(userId);
        return ResponseEntity.ok(Map.of("message", "All recent searches cleared"));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByMail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return user.getId();
    }
}
