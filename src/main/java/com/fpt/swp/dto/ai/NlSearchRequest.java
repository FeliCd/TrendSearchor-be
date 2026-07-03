package com.fpt.swp.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request cho FR-10.1: tìm kiếm bằng ngôn ngữ tự nhiên.
 * VD: "Tìm các bài báo của Vaswani về attention năm 2017"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NlSearchRequest {

    @NotBlank(message = "Query must not be blank")
    private String query;
}
