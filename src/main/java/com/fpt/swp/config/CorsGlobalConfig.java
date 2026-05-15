package com.fpt.swp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Dedicated CORS configuration as a backup layer on top of SecurityConfig.
 * This ensures CORS headers are set even before Spring Security processes the request.
 */
@Configuration
public class CorsGlobalConfig implements WebMvcConfigurer {

    private static final List<String> ALLOWED_METHODS = Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
    );

    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Cache-Control"
    );

    private static final List<String> EXPOSED_HEADERS = Arrays.asList(
            "Authorization",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
    );

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "https://trend-searchor-fe.vercel.app",
                        "https://trend-searchor-fe-*.vercel.app"
                )
                .allowedMethods(ALLOWED_METHODS.toArray(new String[0]))
                .allowedHeaders(ALLOWED_HEADERS.toArray(new String[0]))
                .exposedHeaders(EXPOSED_HEADERS.toArray(new String[0]))
                .allowCredentials(true)
                .maxAge(3600L);
    }
}
