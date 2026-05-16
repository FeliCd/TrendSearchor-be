package com.fpt.swp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ApiCacheService {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl;

    public ApiCacheService(ObjectMapper objectMapper,
                            @Value("${app.semantic-scholar.cache-ttl-minutes:15}") int cacheTtlMinutes) {
        this.objectMapper = objectMapper;
        this.defaultTtl = Duration.ofMinutes(cacheTtlMinutes);
        startCleanupThread();
    }

    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                    cleanup();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "api-cache-cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return Optional.empty();
        }
        try {
            T value = objectMapper.readValue(entry.value(), type);
            log.debug("Cache HIT for key: {}", key);
            return Optional.of(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cached value for key {}: {}", key, e.getMessage());
            cache.remove(key);
            return Optional.empty();
        }
    }

    public <T> T getOrCompute(String key, Class<T> type, java.util.function.Supplier<T> compute) {
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }
        T result = compute.get();
        if (result != null) {
            put(key, result);
        }
        return result;
    }

    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            cache.put(key, new CacheEntry(json, Instant.now().plus(ttl)));
            log.debug("Cached entry for key: {} (TTL: {}s)", key, ttl.getSeconds());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value for caching, key {}: {}", key, e.getMessage());
        }
    }

    public void evict(String key) {
        cache.remove(key);
    }

    public void evictByPrefix(String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public void cleanup() {
        int before = cache.size();
        cache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                log.trace("Evicting expired cache entry: {}", entry.getKey());
                return true;
            }
            return false;
        });
        int removed = before - cache.size();
        if (removed > 0) {
            log.info("Cache cleanup removed {} expired entries (remaining: {})", removed, cache.size());
        }
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
        log.info("API cache cleared");
    }

    public record CacheEntry(String value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
