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

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Pattern(regexp = AppConstants.PASSWORD_PATTERN, message = AppConstants.MSG_PASSWORD_INVALID)
    private String password;

    @Email(message = "Invalid email format")
    private String mail;

    private Role role;
    private UserStatus status;

    @ValidDob
    private LocalDate dob;

    @Pattern(regexp = AppConstants.PHONE_PATTERN, message = AppConstants.MSG_PHONE_INVALID)
    private String phone;

    private Gender gender;
    private String workplace;
}
