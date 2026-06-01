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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken!");
        }
        if (userRepository.existsByMail(req.getMail())) {
            throw new IllegalArgumentException("Email already taken!");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .mail(req.getMail())
                .role(req.getRole())
                .status(req.getStatus() != null ? req.getStatus() : UserStatus.ACTIVE)
                .dob(req.getDob())
                .phone(req.getPhone())
                .gender(req.getGender())
                .workplace(req.getWorkplace())
                .build();

        return UserResponse.fromUser(userRepository.save(user));
    }

    // ─── 3.4 PUT /api/admin/users/{id} — Update ───────────────────────────
    public UserResponse updateUser(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (req.getUsername() != null && !req.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(req.getUsername())) {
                throw new IllegalArgumentException("Username already taken!");
            }
            user.setUsername(req.getUsername());
        }
        if (req.getMail() != null && !req.getMail().equals(user.getMail())) {
            if (userRepository.existsByMail(req.getMail())) {
                throw new IllegalArgumentException("Email already taken!");
            }
            user.setMail(req.getMail());
        }
        if (req.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getRole() != null) user.setRole(req.getRole());
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        if (req.getDob() != null) user.setDob(req.getDob());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getWorkplace() != null) user.setWorkplace(req.getWorkplace());

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
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("mail")), pattern)
                );
            });
        }

        return spec;
    }
}
