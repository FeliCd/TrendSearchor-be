package com.fpt.swp.dto;

import java.math.BigDecimal;

/**
 * Projection cho doanh thu theo tháng (native query).
 * Alias cột trong query phải khớp tên getter: ym / revenue / cnt.
 */
public interface MonthlyRevenueRow {
    String getYm();            // định dạng "YYYY-MM"
    BigDecimal getRevenue();   // tổng doanh thu tháng đó
    long getCnt();             // số giao dịch thành công tháng đó
}
