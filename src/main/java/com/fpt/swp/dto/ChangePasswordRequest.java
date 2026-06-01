package com.fpt.swp.dto;

import com.fpt.swp.util.AppConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Pattern(regexp = AppConstants.PASSWORD_PATTERN, message = AppConstants.MSG_PASSWORD_INVALID)
    private String newPassword;
}
