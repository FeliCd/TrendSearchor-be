package com.fpt.swp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ApiRateLimiter {

    public enum LimitType { ALLOWED, PER_SECOND_LIMIT, DAILY_LIMIT }

    public record RateLimitResult(
            LimitType limitType,
            boolean allowed,
            long retryAfterMs,
            int remainingSecond,
            long remainingDaily
    ) {}

    @Value("${app.semantic-scholar.rate-limit-per-second:5}")
    private int rateLimitPerSecond;

    @Value("${app.semantic-scholar.rate-limit-per-day:10000}")
    private int rateLimitPerDay;

    @Value("${app.openalex.rate-limit-per-second:10}")
    private int openAlexRateLimitPerSecond;

    @Value("${app.openalex.rate-limit-per-day:100000}")
    private int openAlexRateLimitPerDay;

    private final Map<String, RateWindow> perSecondCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> dailyCounters = new ConcurrentHashMap<>();
    private volatile long dailyResetTime = getDailyResetTime();
    private volatile long openAlexDailyResetTime = getDailyResetTime();

    public ApiRateLimiter() {
        Thread dailyResetThread = new Thread(this::dailyResetLoop, "api-rate-limit-reset");
        dailyResetThread.setDaemon(true);
        dailyResetThread.start();
    }

    private long getDailyResetTime() {
        long now = System.currentTimeMillis();
        long secondsUntilMidnight = 86400 - ((now / 1000) % 86400);
        return now + (secondsUntilMidnight * 1000L);
    }

    private void dailyResetLoop() {
        while (true) {
            try {
                long now = System.currentTimeMillis();
                long sleepTime = Math.max(1000, dailyResetTime - now);
                Thread.sleep(sleepTime);
                dailyCounters.clear();
                dailyResetTime = getDailyResetTime();
                openAlexDailyResetTime = getDailyResetTime();
                perSecondCounters.clear();
                log.info("API rate limiter daily counters reset");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public RateLimitResult acquire(String endpoint) {
        boolean isOpenAlex = endpoint.startsWith("openalex");

        int dailyLimit = isOpenAlex ? openAlexRateLimitPerDay : rateLimitPerDay;
        long resetTime = isOpenAlex ? openAlexDailyResetTime : dailyResetTime;

        AtomicLong dailyCounter = dailyCounters.computeIfAbsent(endpoint, k -> new AtomicLong(0));

        if (dailyCounter.get() >= dailyLimit) {
            long msUntilReset = Math.max(0, resetTime - System.currentTimeMillis());
            log.warn("Daily API rate limit reached for {} ({}/{}). Retry after {} ms",
                    endpoint, dailyCounter.get(), dailyLimit, msUntilReset);
            return new RateLimitResult(LimitType.DAILY_LIMIT, false, msUntilReset, 0, 0);
        }

        dailyCounter.incrementAndGet();
        return new RateLimitResult(LimitType.ALLOWED, true, 0, 0, dailyLimit - dailyCounter.get());
    }

    public void awaitPermit(String endpoint) throws InterruptedException, RateLimitExceededException {
        RateLimitResult result = acquire(endpoint);
        switch (result.limitType()) {
            case ALLOWED -> { }
            case PER_SECOND_LIMIT -> {
                Thread.sleep(result.retryAfterMs());
                awaitPermit(endpoint);
            }
            case DAILY_LIMIT -> {
                throw new RateLimitExceededException(endpoint, result.retryAfterMs(),
                        "Daily API rate limit reached for " + endpoint);
            }
        }
    }

    public boolean canMakeRequest(String endpoint) {
        boolean isOpenAlex = endpoint.startsWith("openalex");
        int dailyLimit = isOpenAlex ? openAlexRateLimitPerDay : rateLimitPerDay;
        AtomicLong counter = dailyCounters.get(endpoint);
        return counter == null || counter.get() < dailyLimit;
    }

    public long getDailyRemaining(String endpoint) {
        boolean isOpenAlex = endpoint.startsWith("openalex");
        int dailyLimit = isOpenAlex ? openAlexRateLimitPerDay : rateLimitPerDay;
        AtomicLong counter = dailyCounters.get(endpoint);
        return Math.max(0, dailyLimit - (counter != null ? counter.get() : 0));
    }

    public int getDailyLimit(String endpoint) {
        boolean isOpenAlex = endpoint.startsWith("openalex");
        return isOpenAlex ? openAlexRateLimitPerDay : rateLimitPerDay;
    }

    private static class RateWindow {
        final AtomicInteger count = new AtomicInteger(0);
    }

    public static class RateLimitExceededException extends Exception {
        private final String endpoint;
        private final long retryAfterMs;

        public RateLimitExceededException(String endpoint, long retryAfterMs, String message) {
            super(message);
            this.endpoint = endpoint;
            this.retryAfterMs = retryAfterMs;
        }

        public String getEndpoint() { return endpoint; }
        public long getRetryAfterMs() { return retryAfterMs; }
    }
}
