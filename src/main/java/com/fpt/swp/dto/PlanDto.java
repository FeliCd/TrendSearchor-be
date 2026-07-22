package com.fpt.swp.dto;

import com.fpt.swp.model.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Thông tin một gói dịch vụ cho trang pricing. */
@Data
@Builder
public class PlanDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private Integer dailyPromptLimit;

    public static PlanDto fromEntity(SubscriptionPlan p) {
        return PlanDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .durationDays(p.getDurationDays())
                .dailyPromptLimit(p.getDailyPromptLimit())
                .build();
    }
}
