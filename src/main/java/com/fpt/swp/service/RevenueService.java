package com.fpt.swp.service;

import com.fpt.swp.dto.MonthlyRevenueRow;
import com.fpt.swp.dto.RevenueStatsDto;
import com.fpt.swp.model.PaymentStatus;
import com.fpt.swp.repository.PaymentTransactionRepository;
import com.fpt.swp.repository.UserRepository;
import com.fpt.swp.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tổng hợp số liệu doanh thu từ payment_transactions + user_subscriptions.
 * Mốc "hôm nay / tháng này" tính theo múi giờ Việt Nam (UTC+7).
 */
@Service
@RequiredArgsConstructor
public class RevenueService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int CHART_MONTHS = 12;

    private final PaymentTransactionRepository paymentRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public RevenueStatsDto getRevenueStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate todayVn = LocalDate.now(VN);
        LocalDate firstOfMonth = todayVn.withDayOfMonth(1);

        LocalDateTime startToday = todayVn.atStartOfDay();
        LocalDateTime startTomorrow = todayVn.plusDays(1).atStartOfDay();
        LocalDateTime startMonth = firstOfMonth.atStartOfDay();
        LocalDateTime startNextMonth = firstOfMonth.plusMonths(1).atStartOfDay();
        LocalDateTime chartFrom = firstOfMonth.minusMonths(CHART_MONTHS - 1L).atStartOfDay();

        long totalUsers = userRepository.count();
        long payingUsers = paymentRepository.countPayingUsers();
        double conversion = totalUsers > 0 ? (payingUsers * 100.0 / totalUsers) : 0.0;

        // Doanh thu theo tháng (fill các tháng không có giao dịch = 0)
        Map<String, MonthlyRevenueRow> byMonth = paymentRepository.monthlyRevenueSince(chartFrom).stream()
                .collect(Collectors.toMap(MonthlyRevenueRow::getYm, r -> r, (a, b) -> a));
        List<RevenueStatsDto.MonthlyPoint> chart = new ArrayList<>();
        for (int i = CHART_MONTHS - 1; i >= 0; i--) {
            String ym = firstOfMonth.minusMonths(i).format(YM);
            MonthlyRevenueRow row = byMonth.get(ym);
            chart.add(RevenueStatsDto.MonthlyPoint.builder()
                    .month(ym)
                    .revenue(row != null ? row.getRevenue() : BigDecimal.ZERO)
                    .transactions(row != null ? row.getCnt() : 0L)
                    .build());
        }

        return RevenueStatsDto.builder()
                .totalRevenue(paymentRepository.totalRevenue())
                .todayRevenue(paymentRepository.revenueBetween(startToday, startTomorrow))
                .thisMonthRevenue(paymentRepository.revenueBetween(startMonth, startNextMonth))
                .activeProSubscribers(subscriptionRepository.countActive(now))
                .mrr(subscriptionRepository.activeMrr(now))
                .conversionRate(Math.round(conversion * 100.0) / 100.0)
                .successCount(paymentRepository.countByStatus(PaymentStatus.SUCCESS))
                .pendingCount(paymentRepository.countByStatus(PaymentStatus.PENDING))
                .failedCount(paymentRepository.countByStatus(PaymentStatus.FAILED))
                .monthlyChart(chart)
                .build();
    }
}
