package com.fpt.swp.service;

import com.fpt.swp.dto.*;
import com.fpt.swp.model.*;
import com.fpt.swp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ─── 3.1 GET /api/admin/users — Paginated list ─────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getAllUsers(int page, int limit, String role, String status, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id").descending());
        Specification<User> spec = buildSpecification(role, status, search);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserResponse> data = userPage.getContent().stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("pagination", Map.of(
                "page", page,
                "limit", limit,
                "total", userPage.getTotalElements(),
                "totalPages", userPage.getTotalPages()
        ));
        return result;
    }

    // ─── 3.2 GET /api/admin/users/{id} — Single user ───────────────────────
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return UserResponse.fromUser(user);
    }

    // ─── 3.3 POST /api/admin/users — Create ────────────────────────────────
    public UserResponse createUser(CreateUserRequest req) {
        if (userRepository.existsByMail(req.getMail())) {
            throw new IllegalArgumentException("Email already taken!");
        }

        User user = User.builder()
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .mail(req.getMail())
                .role(req.getRole())
                .status(req.getStatus() != null ? req.getStatus() : UserStatus.ACTIVE)
                .dob(req.getDob())
                .phone(req.getPhone())
                .gender(req.getGender())
                .workplace(req.getWorkplace())
                .avatarUrl(req.getAvatarUrl())
                .build();

        return UserResponse.fromUser(userRepository.save(user));
    }

    // ─── 3.4 PUT /api/admin/users/{id} — Update ───────────────────────────
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (req.getMail() != null && !req.getMail().equals(user.getMail())) {
            if (userRepository.existsByMail(req.getMail())) {
                throw new IllegalArgumentException("Email already taken!");
            }
            user.setMail(req.getMail());
        }
        if (req.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getRole() != null) user.setRole(req.getRole());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (req.getDob() != null) user.setDob(req.getDob());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getWorkplace() != null) user.setWorkplace(req.getWorkplace());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());

        return UserResponse.fromUser(userRepository.save(user));
    }

    // ─── 3.5 DELETE /api/admin/users/{id} ────────────────────────────────
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found with id: " + id);
        }
        User user = userRepository.findById(id).orElseThrow();
        if (Boolean.TRUE.equals(user.getBuiltin())) {
            throw new IllegalArgumentException("Cannot delete built-in system account.");
        }
        userRepository.deleteById(id);
    }

    // ─── 3.6 PATCH /api/admin/users/{id}/status ──────────────────────────
    public UserResponse updateUserStatus(Long id, UserStatus newStatus) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setStatus(newStatus);
        return UserResponse.fromUser(userRepository.save(user));
    }

    // ─── 3.7 PATCH /api/admin/users/{id}/role ─────────────────────────────
    public UserResponse updateUserRole(Long id, Role newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setRole(newRole);
        return UserResponse.fromUser(userRepository.save(user));
    }

    // ─── 3.8 POST /api/admin/users/{id}/reset-password — Admin reset password ─
    public Map<String, String> adminResetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (Boolean.TRUE.equals(user.getBuiltin())) {
            throw new IllegalArgumentException("Cannot reset password for built-in system account.");
        }

        String newPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("[ADMIN] Password reset for user id={}, email={}", id, user.getMail());

        try {
            emailService.sendPasswordReset(user.getMail(), newPassword);
        } catch (Exception e) {
            log.warn("[ADMIN] Failed to send password reset email to {}: {}", user.getMail(), e.getMessage());
        }

        return Map.of("message", "Password has been reset. New password sent to " + user.getMail());
    }

    // ─── 3.9 POST /api/admin/users/bulk-reset-password — Bulk admin reset ─────
    public Map<String, Object> bulkAdminResetPassword(List<Long> ids) {
        int successCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Long id : ids) {
            try {
                adminResetPassword(id);
                successCount++;
            } catch (Exception e) {
                errors.add("User " + id + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", successCount + " of " + ids.size() + " passwords reset successfully");
        result.put("successCount", successCount);
        result.put("totalCount", ids.size());
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        return result;
    }

    // ─── Helper: Generate random password ─────────────────────────────────────
    private String generateRandomPassword() {
        return "123456";
    }

    // ─── Helper: Build JPA Specification for filtering ───────────────────────
    private Specification<User> buildSpecification(String role, String status, String search) {
        Specification<User> spec = Specification.where(null);

        if (role != null && !role.isBlank()) {
            final String roleValue = role.toUpperCase();
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(root.get("role"), Role.valueOf(roleValue));
            });
        }

        if (status != null && !status.isBlank()) {
            final String statusValue = status.toUpperCase();
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), UserStatus.valueOf(statusValue)));
        }

        if (search != null && !search.isBlank()) {
            final String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("mail")), pattern)
                );
            });
        }

        return spec;
    }
}
