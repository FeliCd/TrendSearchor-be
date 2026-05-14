package com.fpt.swp.journaltrendtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fpt.swp.journaltrendtracker.model.Gender;
import com.fpt.swp.journaltrendtracker.model.Role;
import com.fpt.swp.journaltrendtracker.validation.ValidDob;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{9,}$", 
             message = "Password must be at least 9 characters long, contain at least 1 uppercase letter, 1 number, and 1 special character")
    private String password;

    @NotNull(message = "Date of birth is required")
    @ValidDob
    private LocalDate dob;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String mail;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Phone must start with 09, 03, 05, 07, 08 and have exactly 10 digits")
    private String phone;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Workplace is required")
    private String workplace;

    private Role role; // Role can be optional in request
}
