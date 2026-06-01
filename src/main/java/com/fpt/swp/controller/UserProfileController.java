package com.fpt.swp.controller;

import com.fpt.swp.dto.UserResponse;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.UserRepository;
import com.fpt.swp.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
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
public class UserProfileController {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public UserProfileController(UserRepository userRepository, CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * GET /api/users/profile — Get current user's profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByMail(email)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    /**
     * POST /api/users/profile/avatar — Upload avatar to Cloudinary
     */
    @PostMapping("/profile/avatar")
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

    /**
     * DELETE /api/users/profile/avatar — Delete avatar
     */
    @DeleteMapping("/profile/avatar")
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
}
