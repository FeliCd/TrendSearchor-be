package com.fpt.swp.service;

import com.fpt.swp.dto.*;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ResearchPaperRepository paperRepository;
    private final JournalRepository journalRepository;
    private final AuthorRepository authorRepository;
    private final KeywordRepository keywordRepository;
    private final ResearchTopicRepository topicRepository;
    private final UserFollowRepository followRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final TrendService trendService;
    private final UserRepository userRepository;
    private final ApiDataSourceRepository apiDataSourceRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDto getPublicDashboardStats() {
        return DashboardStatsDto.builder()
                .totalPapers(paperRepository.countTotalPapers())
                .totalJournals(journalRepository.count())
                .totalAuthors(authorRepository.count())
                .totalKeywords(keywordRepository.count())
                .topKeywords(trendService.getTrendingKeywords(10))
                .topJournals(getTopJournals(5))
                .yearlyStats(trendService.getYearlyStats())
                .newPapersThisWeek(0L)
                .newPapersThisMonth(0L)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getUserDashboardStats(Long userId) {
        DashboardStatsDto stats = getPublicDashboardStats();

        if (userId != null) {
            List<JournalStatsDto> followed = getFollowedJournals(userId, 5);
            if (followed != null && !followed.isEmpty()) {
                stats.setTopJournals(followed);
            }
        }

        return stats;
    }

    @Transactional(readOnly = true)
    public List<JournalStatsDto> getTopJournals(int limit) {
        List<Journal> journals = journalRepository.findTop10ByOrderByNameAsc();
        return journals.stream()
                .limit(limit)
                .map(j -> JournalStatsDto.builder()
                        .id(j.getId())
                        .name(j.getName())
                        .publisher(j.getPublisher())
                        .paperCount((long) j.getPapers().size())
                        .citationCount(0L)
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<JournalStatsDto> getFollowedJournals(Long userId, int limit) {
        Page<UserFollow> follows = followRepository.findByUserIdAndType(
                userId, FollowType.JOURNAL, PageRequest.of(0, limit));

        return follows.getContent().stream()
                .filter(f -> f.getJournal() != null)
                .map(f -> {
                    Journal j = f.getJournal();
                    return JournalStatsDto.builder()
                            .id(j.getId())
                            .name(j.getName())
                            .publisher(j.getPublisher())
                            .paperCount((long) j.getPapers().size())
                            .citationCount(0L)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserActivityStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("bookmarkCount", bookmarkRepository.countByUserId(userId));
        stats.put("paperBookmarks", bookmarkRepository.countByUserIdAndType(userId, BookmarkType.PAPER));
        stats.put("keywordBookmarks", bookmarkRepository.countByUserIdAndType(userId, BookmarkType.KEYWORD));
        stats.put("followCount", followRepository.countByUserId(userId));
        stats.put("journalFollows", followRepository.countByUserIdAndType(userId, FollowType.JOURNAL));
        stats.put("topicFollows", followRepository.countByUserIdAndType(userId, FollowType.TOPIC));
        stats.put("unreadNotifications", notificationRepository.countUnreadByUserId(userId));
        stats.put("totalNotifications", notificationRepository.countByUserId(userId));

        return stats;
    }

    @Transactional(readOnly = true)
    public List<PaperDto> getTopPapers(int limit) {
        Page<ResearchPaper> papers = paperRepository.findTopByCitations(PageRequest.of(0, limit));
        return papers.getContent().stream()
                .map(p -> PaperDto.builder()
                        .id(p.getId())
                        .externalId(p.getExternalId())
                        .title(p.getTitle())
                        .abstractText(p.getAbstractText())
                        .year(p.getYear())
                        .citationCount(p.getCitationCount())
                        .openAccess(p.getOpenAccess())
                        .paperUri(p.getPaperUri())
                        .authors(p.getAuthors().stream().map(a ->
                                AuthorDto.builder()
                                        .id(a.getId())
                                        .name(a.getName())
                                        .build()
                        ).collect(Collectors.toList()))
                        .journals(p.getJournals().stream().map(Journal::getName).collect(Collectors.toList()))
                        .keywords(p.getKeywords().stream().map(Keyword::getName).collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPapers", paperRepository.countTotalPapers());
        stats.put("totalJournals", journalRepository.count());
        stats.put("totalAuthors", authorRepository.count());
        stats.put("totalKeywords", keywordRepository.count());
        stats.put("totalTopics", topicRepository.count());
        stats.put("totalUsers", userRepository.count());
        
        List<Map<String, Object>> apiSyncStats = apiDataSourceRepository.findAll().stream()
            .map(api -> {
                Map<String, Object> apiStat = new HashMap<>();
                apiStat.put("sourceName", api.getSourceName());
                apiStat.put("lastSyncAt", api.getLastSyncAt());
                apiStat.put("lastSyncStatus", api.getLastSyncStatus());
                apiStat.put("recordsSynced", api.getRecordsSynced());
                return apiStat;
            }).collect(Collectors.toList());
        stats.put("apiSyncStatuses", apiSyncStats);
        
        return stats;
    }
}
