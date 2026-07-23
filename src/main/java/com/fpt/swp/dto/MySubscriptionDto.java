package com.fpt.swp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Gói hiện tại của user + trạng thái quota — cho màn "Tài khoản/Gói dịch vụ". */
@Data
@Builder
public class MySubscriptionDto {
    private String tier;            // FREE / PRO / ADMIN
    private boolean proActive;      // đang có PRO còn hạn
    private String status;          // ACTIVE / null
    private String planName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private QuotaStatusDto quota;
}
