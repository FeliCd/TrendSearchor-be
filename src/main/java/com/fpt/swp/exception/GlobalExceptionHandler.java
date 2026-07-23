package com.fpt.swp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── Validation ──────────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[400] Malformed request body: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("error", "Bad Request");
        error.put("message", "Malformed JSON request body");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ─── Authentication / Authorization ──────────────────────────────────────────
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<Map<String, String>> handleAccountDisabled(AccountDisabledException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(RateLimitExceededException ex) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceeded(QuotaExceededException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        body.put("code", "QUOTA_EXCEEDED");
        body.put("tier", ex.getTier());
        body.put("dailyLimit", ex.getDailyLimit());
        body.put("used", ex.getUsed());
        body.put("nextAvailableAt", ex.getNextAvailableAt());
        return new ResponseEntity<>(body, HttpStatus.PAYMENT_REQUIRED);
    }

    // ─── Access Control ─────────────────────────────────────────────────────────
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource");
    }

    // ─── Entity ─────────────────────────────────────────────────────────────────
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(
            jakarta.persistence.EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ─── Business Logic ──────────────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ─── Catch-all ──────────────────────────────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("[404] Resource not found: {}", ex.getResourcePath());
        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("error", "Not Found");
        error.put("message", "The requested resource was not found");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("[EXCEPTION] Unhandled exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        Map<String, Object> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ─── Helper ─────────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("message", message);
        return new ResponseEntity<>(error, status);
    }
}
