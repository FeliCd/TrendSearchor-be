package com.fpt.swp.util;

import com.fpt.swp.model.User;
import com.fpt.swp.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {

    private final UserRepository userRepository;

    public AuthUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        try {
            String email = userDetails.getUsername();
            User user = userRepository.findByMail(email).orElse(null);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
