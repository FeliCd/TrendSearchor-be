package com.fpt.swp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CORS filter that runs BEFORE Spring Security's filter chain.
 * This ensures CORS headers are set even for requests that fail early
 * (e.g., preflight OPTIONS, blocked by security, etc.).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:5173",
            "https://trend-searchor-fe.vercel.app",
            "https://trend-searchor-fe-*.vercel.app",
            "https://trendsearchor-be-production.up.railway.app"
    };

    private static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
    };

    private static final String[] ALLOWED_HEADERS = {
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
    };

    private static final String[] EXPOSED_HEADERS = {
            "Authorization",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");

        if (isAllowedOrigin(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", String.join(",", ALLOWED_METHODS));
            response.setHeader("Access-Control-Allow-Headers", String.join(",", ALLOWED_HEADERS));
            response.setHeader("Access-Control-Expose-Headers", String.join(",", EXPOSED_HEADERS));
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");
        }

        // Always pass preflight OPTIONS through
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        for (String allowed : ALLOWED_ORIGINS) {
            if (allowed.contains("*")) {
                String regex = allowed
                        .replace(".", "\\.")
                        .replace("*", ".*");
                if (origin.matches(regex)) {
                    return true;
                }
            } else if (allowed.equalsIgnoreCase(origin)) {
                return true;
            }
        }
        return false;
    }
}
