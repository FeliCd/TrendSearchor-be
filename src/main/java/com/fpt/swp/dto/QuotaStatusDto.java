package com.fpt.swp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Trạng thái hạn mức AI của user — cho FE hiển thị "còn X/limit lượt". */
@Data
@Builder
public class QuotaStatusDto {
    private String tier;            // FREE / PRO
    private int dailyLimit;
    private long used;
    private long remaining;
    private boolean unlimited;      // true cho ADMIN
    private LocalDateTime nextAvailableAt;  // khi đã hết: thời điểm có lượt trở lại (null nếu còn lượt)
}
