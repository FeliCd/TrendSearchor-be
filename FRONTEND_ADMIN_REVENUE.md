# 📊 FE — Dashboard Doanh thu Admin

> Số liệu doanh thu được **gộp vào** endpoint admin stats sẵn có (không thêm endpoint mới).
> Tiền tính bằng **VND**; mốc thời gian theo **giờ Việt Nam (UTC+7)**.

## Endpoint
`GET /api/dashboard/admin/stats` — quyền **ADMIN** (header `Authorization: Bearer <token>`).

Response cũ giữ nguyên (totalPapers, totalUsers, apiSyncStatuses...), **thêm object `revenue`**:

```json
{
  "totalUsers": 1, "totalPapers": 0, "...": "...",
  "revenue": {
    "totalRevenue": 199000,          // tổng doanh thu all-time
    "todayRevenue": 199000,          // doanh thu hôm nay (giờ VN)
    "thisMonthRevenue": 199000,      // doanh thu tháng này (giờ VN)
    "activeProSubscribers": 1,       // số gói PRO đang active
    "mrr": 199000,                   // doanh thu định kỳ tháng
    "conversionRate": 100.0,         // % user đã từng trả tiền / tổng user
    "successCount": 1,               // số giao dịch theo trạng thái
    "pendingCount": 0,
    "failedCount": 0,
    "monthlyChart": [
      { "month": "2025-08", "revenue": 0, "transactions": 0 },
      "... đủ 12 tháng ...",
      { "month": "2026-07", "revenue": 199000, "transactions": 1 }
    ]
  }
}
```

## Gợi ý hiển thị

**KPI cards:**
| Card | Field | Ghi chú |
|---|---|---|
| Tổng doanh thu | `totalRevenue` | format VND (199.000 ₫) |
| Doanh thu tháng này | `thisMonthRevenue` | |
| Doanh thu hôm nay | `todayRevenue` | |
| Subscriber PRO | `activeProSubscribers` | số gói đang active |
| MRR | `mrr` | doanh thu định kỳ/tháng |
| Tỷ lệ chuyển đổi | `conversionRate` | hiển thị `%` (VD "100%") |

**Biểu đồ cột/đường — doanh thu theo tháng:**
- Dữ liệu: `monthlyChart` (luôn đủ **12 tháng gần nhất**, tháng không có giao dịch = 0 → khỏi lo khoảng trống).
- Trục X: `month` ("YYYY-MM"). Trục Y: `revenue`. Có thể chồng thêm `transactions` (số giao dịch) làm đường phụ.

**Trạng thái giao dịch:** `successCount / pendingCount / failedCount` — làm mini-stat hoặc donut.

## Lưu ý
- Số tiền là `BigDecimal` → FE nhận dạng number; nên format bằng `Intl.NumberFormat('vi-VN', {style:'currency', currency:'VND'})`.
- Doanh thu chỉ tính giao dịch **SUCCESS** (thanh toán đã xác nhận). Giao dịch PENDING/FAILED không cộng vào doanh thu.
- Hiện thanh toán là **mock** → doanh thu = các lần mock-confirm thành công.
