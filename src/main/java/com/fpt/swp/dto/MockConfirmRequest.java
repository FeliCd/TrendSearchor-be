package com.fpt.swp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Body cho POST /api/payments/mock-confirm — mô phỏng webhook xác nhận thanh toán. */
@Data
public class MockConfirmRequest {

    @NotBlank(message = "transactionId is required")
    private String transactionId;
}
