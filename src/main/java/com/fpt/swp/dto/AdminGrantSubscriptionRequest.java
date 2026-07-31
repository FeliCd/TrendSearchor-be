package com.fpt.swp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminGrantSubscriptionRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "planCode is required")
    private String planCode;

    private Integer durationDays;
}
