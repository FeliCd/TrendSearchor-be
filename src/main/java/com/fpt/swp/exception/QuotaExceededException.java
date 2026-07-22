package com.fpt.swp.exception;

import java.time.LocalDateTime;

/**
 * Ném khi user vượt hạn mức số lượt AI trong cửa sổ trượt 24h.
 * GlobalExceptionHandler map thành HTTP 402 (Payment Required) kèm thông tin quota.
 */
public class QuotaExceededException extends RuntimeException {

    private final String tier;
    private final int dailyLimit;
    private final int used;
    private final LocalDateTime nextAvailableAt;

    public QuotaExceededException(String tier, int dailyLimit, int used, LocalDateTime nextAvailableAt) {
        super("Daily AI quota exceeded (" + used + "/" + dailyLimit + " on plan " + tier
                + "). Upgrade to PRO for a higher limit.");
        this.tier = tier;
        this.dailyLimit = dailyLimit;
        this.used = used;
        this.nextAvailableAt = nextAvailableAt;
    }

    public String getTier() { return tier; }
    public int getDailyLimit() { return dailyLimit; }
    public int getUsed() { return used; }
    public LocalDateTime getNextAvailableAt() { return nextAvailableAt; }
}
