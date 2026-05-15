package com.fpt.swp.dto;

import com.fpt.swp.model.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {
    @NotNull(message = "Role is required")
    private Role role;
}
