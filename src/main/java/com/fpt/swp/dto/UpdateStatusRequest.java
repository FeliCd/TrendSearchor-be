package com.fpt.swp.dto;

import com.fpt.swp.model.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    @NotNull(message = "Status is required")
    private UserStatus status;
}
