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
public class UpdateUserRequest {

    @Email(message = "Invalid email format")
    private String mail;

    @Pattern(regexp = AppConstants.PASSWORD_PATTERN, message = AppConstants.MSG_PASSWORD_INVALID)
    private String password;

    private String fullName;

    private Role role;
    private UserStatus status;

    @ValidDob
    private LocalDate dob;

    @Pattern(regexp = "^$|" + AppConstants.PHONE_PATTERN, message = AppConstants.MSG_PHONE_INVALID)
    private String phone;

    private Gender gender;
    private String workplace;

    @Size(max = 500, message = "Avatar URL must be at most 500 characters")
    private String avatarUrl;
}
