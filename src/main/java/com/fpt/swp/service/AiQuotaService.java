package com.fpt.swp.service;

import com.fpt.swp.dto.QuotaStatusDto;
import com.fpt.swp.exception.QuotaExceededException;
import com.fpt.swp.model.AiUsageLog;
import com.fpt.swp.model.SubscriptionPlan;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.AiUsageLogRepository;
import com.fpt.swp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Áp hạn mức số lượt gọi AI theo cửa sổ trượt 24h, dựa trên tier hiện tại của user.
 * Hạn mức lấy từ {@link SubscriptionPlan#getDailyPromptLimit()} (FREE=3, PRO=50).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuotaService {

    private static final long WINDOW_HOURS = 24;

    private final SubscriptionService subscriptionService;
    private final AiUsageLogRepository usageLogRepository;
    private final UserRepository userRepository;

    private LocalDateTime windowStart() {
        return LocalDateTime.now().minusHours(WINDOW_HOURS);
    }

    /**
     * Kiểm tra quota trước khi gọi LLM. Ném {@link QuotaExceededException} (→ 402) nếu hết.
     */
    @Transactional(readOnly = true)
    public void checkQuota(Long userId) {
        SubscriptionPlan plan = subscriptionService.getEffectivePlan(userId);
        int limit = plan.getDailyPromptLimit();
        LocalDateTime since = windowStart();
        long used = usageLogRepository.countByUserSince(userId, since);
        if (used >= limit) {
            LocalDateTime oldest = usageLogRepository.findOldestInWindow(userId, since);
            LocalDateTime nextAvailable = oldest != null ? oldest.plusHours(WINDOW_HOURS) : null;
            throw new QuotaExceededException(plan.getCode(), limit, (int) used, nextAvailable);
        }
    }

    /**
     * Ghi nhận một lượt dùng AI (gọi SAU khi LLM thực sự trả kết quả — xem AiController).
     */
    @Transactional
    public void recordUsage(Long userId, String feature) {
        User userRef = userRepository.getReferenceById(userId);
        usageLogRepository.save(AiUsageLog.builder()
                .user(userRef)
                .feature(feature)
                .build());
    }

    @Transactional(readOnly = true)
    public QuotaStatusDto getQuotaStatus(Long userId) {
        SubscriptionPlan plan = subscriptionService.getEffectivePlan(userId);
        int limit = plan.getDailyPromptLimit();
        LocalDateTime since = windowStart();
        long used = usageLogRepository.countByUserSince(userId, since);
        long remaining = Math.max(0, limit - used);
        LocalDateTime nextAvailableAt = null;
        if (remaining == 0) {
            LocalDateTime oldest = usageLogRepository.findOldestInWindow(userId, since);
            nextAvailableAt = oldest != null ? oldest.plusHours(WINDOW_HOURS) : null;
        }
        return QuotaStatusDto.builder()
                .tier(plan.getCode())
                .dailyLimit(limit)
                .used(used)
                .remaining(remaining)
                .unlimited(false)
                .nextAvailableAt(nextAvailableAt)
                .build();
    }
}
