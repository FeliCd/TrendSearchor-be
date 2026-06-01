package com.fpt.swp.dto;

import com.fpt.swp.model.Gender;
import com.fpt.swp.model.Role;
import com.fpt.swp.model.UserStatus;
import com.fpt.swp.util.AppConstants;
import com.fpt.swp.validation.ValidDob;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String mail;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = AppConstants.PASSWORD_PATTERN, message = AppConstants.MSG_PASSWORD_INVALID)
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, message = "Full name must be at least 3 characters")
    private String fullName;

    @NotNull(message = "Role is required")
    private Role role;

    private UserStatus status;

    @ValidDob
    private LocalDate dob;

    @Pattern(regexp = AppConstants.PHONE_PATTERN, message = AppConstants.MSG_PHONE_INVALID)
    private String phone;

    private Gender gender;

    private String workplace;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatarUrl;
}
