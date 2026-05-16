package com.fpt.swp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check controller – handles the root "/" endpoint and provides API info.
 * This prevents NoResourceFoundException from being thrown for the root path.
 */
@RestController
@RequestMapping("/")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "TrendSearchor API",
                "version", "1.0.0",
                "timestamp", LocalDateTime.now().toString(),
                "message", "TrendSearchor Scientific Journal Trend Monitoring API is running"
        ));
    }
}
