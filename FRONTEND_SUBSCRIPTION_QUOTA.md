# 💳 Hướng dẫn — Gói dịch vụ & Hạn mức AI (Subscription / Quota)

> Tài liệu tích hợp cho **FE team** về mô hình freemium mới, kèm phần ghi chú backend cuối file.
> Mọi request (trừ `GET /api/plans`) gửi header `Authorization: Bearer <token>`.

---

## 0. Mô hình & việc FE cần làm

| Tier | Hạn mức AI | Giá |
|---|---|---|
| **FREE** | 3 lượt / 24h | 0đ (mặc định mọi user) |
| **PRO** | 50 lượt / 24h | 199.000đ / 30 ngày |
| ADMIN | không giới hạn | — |

- **"1 lượt"** = 1 lần gọi bất kỳ tính năng AI nào. Cửa sổ **trượt 24h** (không reset lúc nửa đêm).
- Quota **chỉ bị trừ khi AI thật sự chạy** — nếu model lỗi/offline (trả fallback) thì **không trừ lượt**.

| # | Việc FE bắt buộc | Nếu bỏ sót |
|---|------|-----------|
| 1 | **Mọi tính năng AI giờ cần đăng nhập** (kể cả search & trend-qa — trước đây public) | 401 |
| 2 | Bắt **HTTP 402** trên mọi call AI → hiện popup "Hết lượt, nâng cấp PRO" | User bối rối khi hết lượt |
| 3 | Hiển thị quota còn lại (gọi `GET /api/ai/quota`) | — |
| 4 | Trang pricing + luồng subscribe → confirm | — |

---

## 1. Kiểm tra quota — `GET /api/ai/quota`
```json
{ "tier": "FREE", "dailyLimit": 3, "used": 1, "remaining": 2, "unlimited": false, "nextAvailableAt": null }
```
- Khi hết lượt: `remaining: 0`, `nextAvailableAt: "2026-07-24T10:30:00"` (thời điểm có lượt trở lại).
- ADMIN: `{ "tier": "ADMIN", "dailyLimit": -1, "remaining": -1, "unlimited": true }`.

> Gợi ý: gọi khi vào app + sau mỗi lần dùng Ai thành công, hiển thị badge "Còn {remaining}/{dailyLimit} lượt".

## 2. Khi hết lượt — mọi endpoint AI trả **402**
Bất kỳ call nào tới `/api/ai/*` khi hết quota:
```json
{
  "code": "QUOTA_EXCEEDED",
  "message": "Daily AI quota exceeded (3/3 on plan FREE). Upgrade to PRO for a higher limit.",
  "tier": "FREE",
  "dailyLimit": 3,
  "used": 3,
  "nextAvailableAt": "2026-07-24T10:30:00"
}
```
→ FE: bắt status **402**, hiện modal nâng cấp (kèm `nextAvailableAt` để báo "Bạn sẽ có lượt lại lúc …").

> Vẫn giữ **429** riêng cho chống spam (quá 20 request/phút) — thông điệp khác, xử lý riêng.

## 3. Trang Pricing — `GET /api/plans` (công khai)
```json
[
  { "id": 1, "code": "FREE", "name": "Free", "price": 0.00, "durationDays": 0, "dailyPromptLimit": 3, "description": "..." },
  { "id": 2, "code": "PRO",  "name": "Pro Researcher", "price": 199000.00, "durationDays": 30, "dailyPromptLimit": 50, "description": "..." }
]
```

## 4. Luồng đăng ký PRO (mock payment)

### Bước 1 — `POST /api/subscriptions/subscribe`
```json
// Request
{ "planCode": "PRO", "paymentMethod": "MOCK" }
```
```json
// Response 201
{
  "message": "Subscription created. Confirm the payment to activate.",
  "subscriptionId": 5,
  "transactionId": "TXN-8f3a...",
  "amount": 199000.00,
  "status": "PENDING",
  "mockConfirmEndpoint": "/api/payments/mock-confirm"
}
```
- **400** nếu `planCode` là FREE (gói miễn phí không cần mua).

### Bước 2 — `POST /api/payments/mock-confirm` *(mô phỏng thanh toán thành công)*
```json
// Request
{ "transactionId": "TXN-8f3a..." }
```
```json
// Response 200
{
  "message": "Payment confirmed. Your subscription is now active.",
  "transactionId": "TXN-8f3a...",
  "paymentStatus": "SUCCESS",
  "subscriptionStatus": "ACTIVE",
  "endDate": "2026-08-22T10:30:00"
}
```
- **400** nếu giao dịch đã xử lý, hoặc không phải của user hiện tại.

> Sau bước 2: gọi lại `GET /api/ai/quota` → tier chuyển **PRO**, limit 50. FE cập nhật UI.
> *(Cổng thật sau này: thay bước 2 bằng redirect cổng + webhook; luồng FE gần như giữ nguyên.)*

## 5. Gói hiện tại — `GET /api/subscriptions/me`
```json
{
  "tier": "PRO",
  "proActive": true,
  "status": "ACTIVE",
  "planName": "Pro Researcher",
  "startDate": "2026-07-23T10:30:00",
  "endDate": "2026-08-22T10:30:00",
  "quota": { "tier": "PRO", "dailyLimit": 50, "used": 4, "remaining": 46, "unlimited": false, "nextAvailableAt": null }
}
```
- User FREE: `tier: "FREE"`, `proActive: false`, `status: null`, `endDate: null`.

---

## Ghi chú backend (nội bộ)

- **Nguồn hạn mức:** `subscription_plans.daily_prompt_limit` (FREE=3, PRO=50). Sửa quota = sửa dữ liệu, không phải code.
- **Đếm cửa sổ trượt:** bảng `ai_usage_log` (1 dòng / lượt AI thành công). `AiQuotaService.checkQuota` đếm số dòng của user trong 24h gần nhất; `≥ limit` → ném `QuotaExceededException` (→402).
- **Chỉ trừ khi LLM chạy:** `OpenRouterClient` set cờ ThreadLocal khi call thành công; `AiController.afterAiCall` chỉ ghi `ai_usage_log` khi cờ true. Fallback (model lỗi) không trừ lượt.
- **Tier resolution:** `UserSubscriptionRepository.findActiveByUserId` lọc `status=ACTIVE AND endDate>now` → PRO còn hạn tự rớt về FREE khi hết hạn **mà không cần cron** (cron chỉ để dọn `status=EXPIRED`, chưa làm).
- **Kích hoạt:** `PaymentService.confirmMockPayment` verify txn PENDING + đúng chủ → SUCCESS + subscription ACTIVE (end = now + durationDays).
- **Lên cổng thật:** thay `POST /api/payments/mock-confirm` bằng webhook có verify chữ ký (VNPay/MoMo/PayOS); giữ nguyên phần kích hoạt subscription.
- **Migration:** `V22__add_subscription_and_ai_usage.sql` (3 bảng subscription + `ai_usage_log` + seed 2 plan).
- **Chưa làm (optional):** G6 — admin quản lý plan + cron hạ EXPIRED.
