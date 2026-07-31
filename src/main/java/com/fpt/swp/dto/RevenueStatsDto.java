package com.fpt.swp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Số liệu doanh thu cho dashboard admin (gộp vào /api/dashboard/admin/stats). */
@Data
@Builder
public class RevenueStatsDto {

    private BigDecimal totalRevenue;        // tổng doanh thu all-time (SUCCESS)
    private BigDecimal todayRevenue;        // doanh thu hôm nay (giờ VN)
    private BigDecimal thisMonthRevenue;    // doanh thu tháng này (giờ VN)
    private long activeProSubscribers;      // số gói PRO đang active
    private BigDecimal mrr;                 // doanh thu định kỳ tháng (tổng giá gói active)
    private double conversionRate;          // % user đã từng trả tiền / tổng user

    private long successCount;              // số giao dịch theo trạng thái
    private long pendingCount;
    private long failedCount;

    private List<MonthlyPoint> monthlyChart; // 12 tháng gần nhất (đã fill tháng trống)

    @Data
    @Builder
    public static class MonthlyPoint {
        private String month;               // "YYYY-MM"
        private BigDecimal revenue;
        private long transactions;
    }
}
