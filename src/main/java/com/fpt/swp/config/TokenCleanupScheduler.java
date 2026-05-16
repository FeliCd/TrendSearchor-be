package com.fpt.swp.config;

import com.fpt.swp.repository.InvalidatedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * FR-01.3 – Định kỳ xóa các bản ghi token hết hạn khỏi bảng blacklist
 * để tránh bảng invalidated_tokens phình to theo thời gian.
 * Chạy mỗi ngày lúc 02:00 AM.
 */
@Component
public class TokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public TokenCleanupScheduler(InvalidatedTokenRepository invalidatedTokenRepository) {
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    @Scheduled(cron = "0 0 2 * * *") // Mỗi ngày lúc 02:00 AM
    public void cleanupExpiredTokens() {
        log.info("[TokenCleanup] Bắt đầu xóa các invalidated token đã hết hạn...");
        invalidatedTokenRepository.deleteAllExpiredBefore(Instant.now());
        log.info("[TokenCleanup] Hoàn tất.");
    }
}
