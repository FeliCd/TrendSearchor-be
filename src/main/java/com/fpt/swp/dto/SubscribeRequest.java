package com.fpt.swp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body cho POST /api/subscriptions/subscribe. */
@Data
public class SubscribeRequest {

    @NotBlank(message = "planCode is required")
    private String planCode;

    /** Phương thức thanh toán (mock). Mặc định "MOCK" nếu bỏ trống. */
    @Size(max = 50)
    private String paymentMethod;
}
