package com.fpt.swp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter đơn giản (in-memory, fixed-window) cho các endpoint AI gọi LLM trả phí.
 * Key theo user (nếu đã đăng nhập) hoặc IP client. Mỗi key được phép tối đa
 * {@code maxRequestsPerWindow} request trong mỗi cửa sổ {@code windowMs}.
 *
 * <p>Giống {@link com.fpt.swp.security.LoginRateLimiter}, đây là state cục bộ theo
 * instance — production nhiều instance nên dùng Redis. Đủ dùng cho phạm vi đồ án.
 */
@Service
public class AiRateLimiter {

    private static final long WINDOW_MS = 60_000L;
    /** Ngưỡng map size để kích hoạt dọn dẹp các cửa sổ đã hết hạn. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxRequestsPerWindow;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AiRateLimiter(@Value("${app.ai.rate-limit.max-per-minute:20}") int maxPerMinute) {
        this.maxRequestsPerWindow = maxPerMinute;
    }

    /**
     * Thử ghi nhận một request cho {@code key}.
     *
     * @return true nếu còn quota, false nếu đã vượt giới hạn trong cửa sổ hiện tại.
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> now - w.windowStart >= WINDOW_MS);
        }

        Window window = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (window) {
            if (now - window.windowStart >= WINDOW_MS) {
                window.windowStart = now;
                window.count = 0;
            }
            if (window.count >= maxRequestsPerWindow) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    private static final class Window {
        long windowStart;
        int count;

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
