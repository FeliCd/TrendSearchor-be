package com.fpt.swp.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class RequestUtils {

    private RequestUtils() {}

    /**
     * Lấy IP thật của client, ưu tiên header X-Forwarded-For khi chạy sau
     * reverse proxy (Railway/Vercel). Header dạng "client, proxy1, proxy2"
     * — phần tử đầu tiên là IP gốc.
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
