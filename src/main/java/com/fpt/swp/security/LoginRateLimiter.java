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
     * Checks if an identifier (email) is locked out.
     *
     * @param identifier email used as lockout key
     * @return true if locked out
     */
    public boolean isLockedOut(String identifier) {
        Long lockoutTime = lockouts.get(identifier);
        if (lockoutTime == null) return false;

        if (System.currentTimeMillis() > lockoutTime) {
            lockouts.remove(identifier);
            attempts.remove(identifier);
            return false;
        }
        return true;
    }

    /**
     * Records a failed login attempt. Increments attempt counter.
     * Locks out the identifier after MAX_ATTEMPTS failures.
     *
     * @param identifier email of the failed attempt
     */
    public void recordFailedAttempt(String identifier) {
        AtomicInteger count = attempts.computeIfAbsent(identifier, k -> new AtomicInteger(0));
        int failed = count.incrementAndGet();

        if (failed >= MAX_ATTEMPTS) {
            lockouts.put(identifier, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
            attempts.remove(identifier);
        }
    }

    /**
     * Resets attempt counter on successful login.
     *
     * @param identifier email that logged in successfully
     */
    public void recordSuccessfulLogin(String identifier) {
        attempts.remove(identifier);
        lockouts.remove(identifier);
    }

    /**
     * Returns remaining attempts before lockout.
     *
     * @param identifier email
     * @return remaining attempts
     */
    public int getRemainingAttempts(String identifier) {
        AtomicInteger count = attempts.get(identifier);
        return count == null ? MAX_ATTEMPTS : Math.max(0, MAX_ATTEMPTS - count.get());
    }

    /**
     * Returns remaining lockout time in milliseconds.
     *
     * @param identifier email
     * @return milliseconds remaining, or 0 if not locked out
     */
    public long getRemainingLockoutTime(String identifier) {
        Long lockoutTime = lockouts.get(identifier);
        if (lockoutTime == null) return 0;
        long remaining = lockoutTime - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }
}
