package com.fpt.swp.dto;

import com.fpt.swp.model.Gender;
import com.fpt.swp.model.Role;
import com.fpt.swp.util.AppConstants;
import com.fpt.swp.validation.ValidDob;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = AppConstants.PASSWORD_PATTERN, message = AppConstants.MSG_PASSWORD_INVALID)
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotNull(message = "Date of birth is required")
    @ValidDob
    private LocalDate dob;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String mail;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = AppConstants.PHONE_PATTERN, message = AppConstants.MSG_PHONE_INVALID)
    private String phone;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Workplace is required")
    private String workplace;

    private Role role;

    public Role getValidatedRole() {
        if (role == null) {
            return Role.STUDENT;
        }
        switch (role) {
            case STUDENT:
            case LECTURER:
            case RESEARCHER:
                return role;
            default:
                return Role.STUDENT;
        }
    }
}
