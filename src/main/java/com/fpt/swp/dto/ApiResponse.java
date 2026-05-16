package com.fpt.swp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private String message;

    public static ApiResponse of(String message) {
        return new ApiResponse(message);
    }
}
