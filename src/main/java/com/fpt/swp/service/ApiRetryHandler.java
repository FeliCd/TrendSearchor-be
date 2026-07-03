package com.fpt.swp.service;

import com.fpt.swp.service.ApiRateLimiter.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Slf4j
public class ApiRetryHandler {

    private final RestTemplate restTemplate;
    private final ApiRateLimiter rateLimiter;

    @Value("${app.semantic-scholar.retry-max-attempts:3}")
    private int maxAttempts;

    @Value("${app.semantic-scholar.retry-initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${app.semantic-scholar.retry-max-delay-ms:30000}")
    private long maxDelayMs;

    public ApiRetryHandler(RestTemplate restTemplate, ApiRateLimiter rateLimiter) {
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
    }

    public <T> Optional<T> executeWithRetry(String endpoint, String url, Class<T> responseType) {
        return executeWithRetry(endpoint, () -> {
            ResponseEntity<T> response = restTemplate.getForEntity(java.net.URI.create(url), responseType);
            return Optional.ofNullable(response.getBody());
        }).or(() -> Optional.empty());
    }

    public String executeWithRetryRaw(String endpoint, String url) {
        return executeWithRetry(endpoint, () -> {
            ResponseEntity<String> response = restTemplate.getForEntity(java.net.URI.create(url), String.class);
            return Optional.ofNullable(response.getBody());
        }).orElse(null);
    }

    public String executeWithRetryRaw(String endpoint, String url, RestTemplate customRestTemplate) {
        return executeWithRetry(endpoint, () -> {
            ResponseEntity<String> response = customRestTemplate.getForEntity(java.net.URI.create(url), String.class);
            return Optional.ofNullable(response.getBody());
        }).orElse(null);
    }

    public <T> Optional<T> executeWithRetry(String endpoint, Supplier<Optional<T>> request) {
        int attempt = 0;
        long delay = initialDelayMs;

        while (attempt < maxAttempts) {
            attempt++;

            try {
                rateLimiter.awaitPermit(endpoint);
                Optional<T> result = request.get();
                if (result.isPresent()) {
                    return result;
                }
                if (attempt >= maxAttempts) {
                    log.error("All {} retry attempts exhausted for {}", maxAttempts, endpoint);
                    return Optional.empty();
                }
            } catch (RateLimitExceededException e) {
                log.error("Daily rate limit exceeded for {}: {}", e.getEndpoint(), e.getMessage());
                return Optional.empty();
            } catch (HttpServerErrorException e) {
                if (e.getStatusCode().value() == 429) {
                    long retryAfter = parseRetryAfter(e.getResponseHeaders());
                    log.warn("Received 429 from {} (attempt {}/{}). Retry-After: {} ms",
                            endpoint, attempt, maxAttempts, retryAfter);
                    delay = Math.min(retryAfter > 0 ? retryAfter : delay * 2, maxDelayMs);
                } else if (e.getStatusCode().is5xxServerError()) {
                    log.warn("Server error {} from {} (attempt {}/{}). Retrying in {} ms",
                            e.getStatusCode(), endpoint, attempt, maxAttempts, delay);
                    delay = Math.min(delay * 2, maxDelayMs);
                } else {
                    log.error("HTTP error {} from {} on attempt {}/{}",
                            e.getStatusCode(), endpoint, attempt, maxAttempts);
                    return Optional.empty();
                }
            } catch (HttpClientErrorException e) {
                log.error("Client error {} from {}: {}", e.getStatusCode(), endpoint, e.getMessage());
                return Optional.empty();
            } catch (RestClientException e) {
                log.warn("RestClient error for {} (attempt {}/{}): {}",
                        endpoint, attempt, maxAttempts, e.getMessage());
                if (attempt >= maxAttempts) {
                    log.error("Max retry attempts reached for {}", endpoint);
                    return Optional.empty();
                }
                delay = Math.min(delay * 2, maxDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Retry interrupted for {}", endpoint);
                return Optional.empty();
            }

            if (attempt < maxAttempts) {
                try {
                    log.info("Retrying {} in {} ms (attempt {}/{})", endpoint, delay, attempt + 1, maxAttempts);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    private long parseRetryAfter(HttpHeaders headers) {
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter == null) return 0;

        try {
            return Long.parseLong(retryAfter) * 1000;
        } catch (NumberFormatException e) {
            try {
                return Duration.parse("PT" + retryAfter.toUpperCase().replace(" ", "")).toMillis();
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public void setMaxDelayMs(long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }
}
