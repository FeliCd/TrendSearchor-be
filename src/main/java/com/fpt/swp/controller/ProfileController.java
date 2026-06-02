package com.fpt.swp.controller;

import com.fpt.swp.dto.UpdateUserRequest;
import com.fpt.swp.dto.UserResponse;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.UserRepository;
import com.fpt.swp.service.CloudinaryService;
import com.fpt.swp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()")
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
public class ProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public ProfileController(UserService userService,
                             UserRepository userRepository,
                             CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(@RequestBody UpdateUserRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        if (req.getMail() != null) {
            if (!req.getMail().equals(user.getMail())) {
                if (userRepository.existsByMail(req.getMail())) {
                    throw new IllegalArgumentException("Email already taken!");
                }
            }
            if (!req.getMail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new IllegalArgumentException("Invalid email format");
            }
            user.setMail(req.getMail());
        }

        if (req.getFullName() != null) {
            if (req.getFullName().trim().length() < 3) {
                throw new IllegalArgumentException("Full name must be at least 3 characters");
            }
            user.setFullName(req.getFullName().trim());
        }

        if (req.getDob() != null) {
            user.setDob(req.getDob());
        }

        if (req.getPhone() != null && !req.getPhone().isEmpty()) {
            if (!req.getPhone().matches("^0[35789][0-9]{8}$")) {
                throw new IllegalArgumentException("Phone must start with 03, 05, 07, 08, 09 and have exactly 10 digits");
            }
            user.setPhone(req.getPhone());
        }

        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getWorkplace() != null) user.setWorkplace(req.getWorkplace());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());

        return ResponseEntity.ok(UserResponse.fromUser(userRepository.save(user)));
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        String publicId = "trendsearchor/avatars/user" + user.getId();

        Map<String, Object> uploadResult = cloudinaryService.upload(file, "trendsearchor/avatars", publicId);
        String secureUrl = (String) uploadResult.get("secure_url");

        user.setAvatarUrl(secureUrl);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Avatar updated successfully",
                "data", UserResponse.fromUser(user)
        ));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Map<String, Object>> deleteAvatar() throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        if (user.getAvatarUrl() != null) {
            String publicId = "trendsearchor/avatars/user" + user.getId();
            cloudinaryService.delete(publicId);
            user.setAvatarUrl(null);
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Avatar deleted successfully",
                "data", UserResponse.fromUser(user)
        ));
    }

    @PostMapping("/change-role")
    public ResponseEntity<UserResponse> changeRole(@Valid @RequestBody com.fpt.swp.dto.UpdateRoleRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        user.setRole(req.getRole());
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        return ResponseEntity.ok(UserResponse.fromUser(userRepository.save(user)));
    }

    @PatchMapping("/notification-settings")
    public ResponseEntity<UserResponse> updateNotificationSettings(@RequestBody java.util.Map<String, Boolean> request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        if (request.containsKey("receiveNotifications")) {
            user.setReceiveNotifications(request.get("receiveNotifications"));
        }
        return ResponseEntity.ok(UserResponse.fromUser(userRepository.save(user)));
    }
}
