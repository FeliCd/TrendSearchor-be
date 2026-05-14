package com.fpt.swp.controller;

import com.fpt.swp.dto.ChangePasswordRequest;
import com.fpt.swp.dto.ForgotPasswordRequest;
import com.fpt.swp.dto.JwtAuthResponse;
import com.fpt.swp.dto.LoginRequest;
import com.fpt.swp.dto.RegisterRequest;
import com.fpt.swp.model.InvalidatedToken;
import com.fpt.swp.model.Role;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.InvalidatedTokenRepository;
import com.fpt.swp.repository.UserRepository;
import com.fpt.swp.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvalidatedTokenRepository invalidatedTokenRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          InvalidatedTokenRepository invalidatedTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.invalidatedTokenRepository = invalidatedTokenRepository;
    }

    // ─────────────────────────────────────────────
    // FR-01.2 – Đăng nhập
    // ─────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        return ResponseEntity.ok(new JwtAuthResponse(token));
    }

    // ─────────────────────────────────────────────
    // FR-01.1 – Đăng ký tài khoản
    // ─────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        // Kiểm tra xem username đã tồn tại chưa
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return new ResponseEntity<>("Username is already taken!", HttpStatus.BAD_REQUEST);
        }

        // Kiểm tra xem email đã tồn tại chưa
        if (userRepository.existsByMail(registerRequest.getMail())) {
            return new ResponseEntity<>("Email is already taken!", HttpStatus.BAD_REQUEST);
        }

        // Sử dụng Lombok Builder để tạo đối tượng User dễ dàng và gọn gàng
        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword())) // Mã hóa mật khẩu
                .dob(registerRequest.getDob())
                .mail(registerRequest.getMail())
                .phone(registerRequest.getPhone())
                .gender(registerRequest.getGender())
                .workplace(registerRequest.getWorkplace())
                .role(registerRequest.getRole() != null ? registerRequest.getRole() : Role.USER) // Default to USER
                .build();

        userRepository.save(user);

        return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
    }

    // ─────────────────────────────────────────────
    // FR-01.3 – Đăng xuất
    // Hủy session hiện tại, invalidate access token bằng cách đưa vào blacklist.
    // ─────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String token = extractToken(request);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.badRequest().body("Invalid or missing token.");
        }

        String jti = jwtTokenProvider.getJti(token);

        // Token chưa bị invalidate → thêm vào blacklist
        if (!invalidatedTokenRepository.existsByJti(jti)) {
            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .jti(jti)
                    .expiresAt(jwtTokenProvider.getExpiration(token))
                    .build();
            invalidatedTokenRepository.save(invalidatedToken);
        }

        // Xóa SecurityContext phía server
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("Logged out successfully.");
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // ─────────────────────────────────────────────
    // FR-01.5 – Đổi mật khẩu
    // ─────────────────────────────────────────────
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // Kiểm tra xem mật khẩu mới và xác nhận mật khẩu có khớp nhau không
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("New password and confirm password do not match.");
        }

        // Lấy thông tin user hiện tại từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Incorrect old password.");
        }

        // Mã hóa và lưu mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Password changed successfully.");
    }

    // ─────────────────────────────────────────────
    // FR-01.6 – Quên mật khẩu (Reset Password - API Mock)
    // ─────────────────────────────────────────────
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByMail(request.getMail())
                .orElse(null);

        // Bảo mật: Không nên báo lỗi tường minh nếu email không tồn tại để tránh rò rỉ thông tin
        if (user == null) {
            return ResponseEntity.ok("If the email exists, a new password will be provided.");
        }

        // Tạo mật khẩu mới ngẫu nhiên (đáp ứng điều kiện: >= 9 ký tự, có Hoa, số, ký tự đặc biệt)
        // Ví dụ: Trend@ + 6 ký tự random
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String newRandomPassword = "Trend@" + randomSuffix + "1";

        // Mã hóa và lưu vào DB
        user.setPassword(passwordEncoder.encode(newRandomPassword));
        userRepository.save(user);

        // Trong thực tế sẽ gửi email. Ở đây trả thẳng về màn hình để test.
        return ResponseEntity.ok("Password has been reset successfully. Your new password is: " + newRandomPassword);
    }
}
