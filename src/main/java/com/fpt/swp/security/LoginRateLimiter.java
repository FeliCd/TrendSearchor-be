package com.fpt.swp.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter cho login attempts.
 * Dùng ConcurrentHashMap để thread-safe.
 * Trong production, nên dùng Redis để shared state giữa các instances.
 */
@Service
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000L; // 15 phút

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lockouts = new ConcurrentHashMap<>();

    /**
     * Kiểm tra xem username có bị lockout không.
     *
     * @param username username cần kiểm tra
     * @return true nếu bị lockout
     */
    public boolean isLockedOut(String username) {
        Long lockoutTime = lockouts.get(username);
        if (lockoutTime == null) return false;

        if (System.currentTimeMillis() > lockoutTime) {
            // Lockout đã hết hạn, clear
            lockouts.remove(username);
            attempts.remove(username);
            return false;
        }
        return true;
    }

    /**
     * Ghi nhận một login thất bại. Tăng số lần thử.
     * Nếu đạt MAX_ATTEMPTS, lockout username trong LOCKOUT_DURATION_MS.
     *
     * @param username username bị failed attempt
     */
    public void recordFailedAttempt(String username) {
        AtomicInteger count = attempts.computeIfAbsent(username, k -> new AtomicInteger(0));
        int failed = count.incrementAndGet();

        if (failed >= MAX_ATTEMPTS) {
            lockouts.put(username, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
            attempts.remove(username);
        }
    }

    /**
     * Reset số lần thử khi login thành công.
     *
     * @param username username được reset
     */
    public void recordSuccessfulLogin(String username) {
        attempts.remove(username);
        lockouts.remove(username);
    }

    /**
     * Lấy số lần thử còn lại trước khi lockout.
     *
     * @param username username
     * @return số lần thử còn lại
     */
    public int getRemainingAttempts(String username) {
        AtomicInteger count = attempts.get(username);
        return count == null ? MAX_ATTEMPTS : Math.max(0, MAX_ATTEMPTS - count.get());
    }

    /**
     * Lấy thời gian lockout còn lại (milliseconds).
     *
     * @param username username
     * @return milliseconds còn lại, hoặc 0 nếu không bị lockout
     */
    public long getRemainingLockoutTime(String username) {
        Long lockoutTime = lockouts.get(username);
        if (lockoutTime == null) return 0;
        long remaining = lockoutTime - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }
}
