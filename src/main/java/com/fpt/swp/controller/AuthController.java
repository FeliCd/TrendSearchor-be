package com.fpt.swp.controller;

import com.fpt.swp.dto.*;
import com.fpt.swp.exception.AccountDisabledException;
import com.fpt.swp.exception.RateLimitExceededException;
import com.fpt.swp.model.InvalidatedToken;
import com.fpt.swp.model.Role;
import com.fpt.swp.model.User;
import com.fpt.swp.model.UserStatus;
import com.fpt.swp.repository.InvalidatedTokenRepository;
import com.fpt.swp.repository.UserRepository;
import com.fpt.swp.security.JwtTokenProvider;
import com.fpt.swp.security.LoginRateLimiter;
import com.fpt.swp.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://localhost:5173",
                "https://trend-searchor-fe.vercel.app",
                "https://trend-searchor-fe-*.vercel.app",
                "https://trendsearchor-be-production.up.railway.app"
        },
        allowCredentials = "true",
        allowedHeaders = {"Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"}
)
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final LoginRateLimiter loginRateLimiter;
    private final EmailService emailService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          InvalidatedTokenRepository invalidatedTokenRepository,
                          LoginRateLimiter loginRateLimiter,
                          EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
        this.loginRateLimiter = loginRateLimiter;
        this.emailService = emailService;
    }

    // ─────────────────────────────────────────────
    // FR-01.2 – Đăng nhập
    // ─────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        log.info("[AUTH] Login attempt for username: {}", username);
        log.debug("[AUTH] Origin: {}", /* TODO: get from request if needed */ "N/A");

        // Rate limiting – kiểm tra lockout
        if (loginRateLimiter.isLockedOut(username)) {
            long remainingMs = loginRateLimiter.getRemainingLockoutTime(username);
            long minutes = remainingMs / 60_000;
            log.warn("[AUTH] Login LOCKED OUT for username: {}, remaining: {} minutes", username, minutes);
            throw new RateLimitExceededException(
                    "Account temporarily locked due to too many failed attempts. Try again in " + minutes + " minutes.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Kiểm tra user status sau khi authenticate thành công
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            checkUserStatus(user);

            // Cập nhật lastLogin
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Reset rate limit
            loginRateLimiter.recordSuccessfulLogin(username);

            String token = jwtTokenProvider.generateToken(authentication);
            log.info("[AUTH] Login SUCCESS for username: {}", username);

            return ResponseEntity.ok(new JwtAuthResponse(token, UserResponse.fromUser(user)));

        } catch (BadCredentialsException | DisabledException | LockedException e) {
            loginRateLimiter.recordFailedAttempt(username);
            int remaining = loginRateLimiter.getRemainingAttempts(username);
            log.warn("[AUTH] Login FAILED for username: {}, attempts remaining: {}, error: {}",
                    username, remaining, e.getClass().getSimpleName());
            if (remaining > 0) {
                throw new BadCredentialsException(
                        "Invalid username or password. " + remaining + " attempts remaining.");
            }
            throw new RateLimitExceededException(
                    "Too many failed attempts. Account is temporarily locked for 15 minutes.");
        } catch (Exception e) {
            log.error("[AUTH] Login ERROR for username: {}, exception: {}", username, e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────
    // FR-01.1 – Đăng ký tài khoản
    // ─────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest req) {
        if (req.getRole() != null && req.getRole() != Role.STUDENT && req.getRole() != Role.RESEARCHER && req.getRole() != Role.USER) {
            return ResponseEntity.badRequest().body(
                    Map.of("role", "Invalid role selected. Only STUDENT or RESEARCHER are allowed."));
        }

        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest().body(
                    Map.of("username", "Username is already taken"));
        }

        if (userRepository.existsByMail(req.getMail())) {
            return ResponseEntity.badRequest().body(
                    Map.of("mail", "Email is already taken"));
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .dob(req.getDob())
                .mail(req.getMail())
                .phone(req.getPhone())
                .gender(req.getGender())
                .workplace(req.getWorkplace())
                .role(req.getRole() != null ? req.getRole() : Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("User registered successfully"));
    }

    // ─────────────────────────────────────────────
    // FR-01.4 – Lấy thông tin user hiện tại
    // ─────────────────────────────────────────────
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    // ─────────────────────────────────────────────
    // FR-01.3 – Đăng xuất
    // ─────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String token = extractToken(request);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.of("Invalid or missing token"));
        }

        String jti = jwtTokenProvider.getJti(token);

        if (!invalidatedTokenRepository.existsByJti(jti)) {
            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .jti(jti)
                    .expiresAt(jwtTokenProvider.getExpiration(token))
                    .build();
            invalidatedTokenRepository.save(invalidatedToken);
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(ApiResponse.of("Logged out successfully"));
    }

    // ─────────────────────────────────────────────
    // FR-01.5 – Đổi mật khẩu
    // ─────────────────────────────────────────────
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(
                    Map.of("confirmPassword", "Passwords do not match"));
        }

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(
                    Map.of("oldPassword", "Incorrect old password"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.of("Password changed successfully"));
    }

    // ─────────────────────────────────────────────
    // FR-01.6 – Quên mật khẩu
    // ─────────────────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByMail(request.getMail()).orElse(null);

        // Không tiết lộ user có tồn tại hay không
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.of(
                    "If the email exists, a new password will be sent to it."));
        }

        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Gửi email thực sự thay vì trả về response
        emailService.sendPasswordReset(user.getMail(), newPassword);

        return ResponseEntity.ok(ApiResponse.of(
                "If the email exists, a new password will be sent to it."));
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────
    private void checkUserStatus(User user) {
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccountDisabledException("Account is inactive. Please contact support.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountDisabledException("Account is suspended. Please contact support.");
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String generateRandomPassword() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "Trend@" + uuid.substring(0, 6) + "1";
    }
}
