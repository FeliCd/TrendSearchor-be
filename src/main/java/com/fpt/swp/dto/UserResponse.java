package com.fpt.swp.dto;

import com.fpt.swp.model.Gender;
import com.fpt.swp.model.Role;
import com.fpt.swp.model.User;
import com.fpt.swp.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String mail;
    private LocalDate dob;
    private String phone;
    private Gender gender;
    private String workplace;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Boolean receiveNotifications;

    public static UserResponse fromUser(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .mail(user.getMail())
                .dob(user.getDob())
                .phone(user.getPhone())
                .gender(user.getGender())
                .workplace(user.getWorkplace())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .receiveNotifications(user.getReceiveNotifications())
                .build();
    }
}
