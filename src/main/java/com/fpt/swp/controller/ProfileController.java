package com.fpt.swp.controller;

import com.fpt.swp.dto.UpdateUserRequest;
import com.fpt.swp.dto.UserResponse;
import com.fpt.swp.model.User;
import com.fpt.swp.repository.UserRepository;
import com.fpt.swp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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

    public ProfileController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.fromUser(user));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateUserRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

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
        if (req.getDob() != null) user.setDob(req.getDob());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getWorkplace() != null) user.setWorkplace(req.getWorkplace());

        return ResponseEntity.ok(UserResponse.fromUser(userRepository.save(user)));
    }
}
