# VNPay — Hướng dẫn nối Frontend

Backend đã hoàn thiện tích hợp VNPay (ký HMAC-SHA512 + verify callback). Phần còn lại
là FE nối luồng **redirect sang VNPay** và **trang xử lý khi quay về**. Doc này mô tả
đúng những gì FE cần làm.

> Lưu ý: BE đọc credentials từ `.env` (`VNP_TMN_CODE`, `VNP_HASH_SECRET`). Chưa có key
> sandbox thật (đăng ký tại https://sandbox.vnpayment.vn) thì `create-url` trả lỗi 400
> `"VNPay chưa được cấu hình"`.

---

## Luồng thanh toán VNPay

```
FE                          BE                                VNPay
 │  POST /subscriptions/subscribe (planCode=PRO)              │
 │ ───────────────────────────►  tạo txn PENDING             │
 │  ◄─────────────────────────── { transactionId }           │
 │                                                            │
 │  POST /payments/vnpay/create-url { transactionId }         │
 │ ───────────────────────────►  ký HMAC → URL VNPay         │
 │  ◄─────────────────────────── { paymentUrl }              │
 │                                                            │
 │  window.location = paymentUrl ────────────────────────────►  (user nhập thẻ/bank)
 │                                                            │
 │                          GET /payments/vnpay/return ◄──────  redirect kèm kết quả
 │  ◄── 302 → /subscription?status=success  (BE verify + kích hoạt gói)
```

Song song, VNPay gọi **IPN** (server-to-server) `GET /payments/vnpay/ipn` — BE tự verify
và kích hoạt gói (nguồn xác nhận đáng tin cậy; idempotent với return).

---

## FE cần đổi 2 chỗ

### 1) Khi user bấm "Nâng cấp PRO" — thay `mock-confirm` bằng redirect VNPay

Trong `subscriptionService.subscribePlan` (hoặc chỗ xử lý checkout), sau khi có
`transactionId` từ `/subscriptions/subscribe`, gọi tiếp `create-url` rồi redirect:

```js
// 1. tạo giao dịch PENDING
const subRes = await api.post('/api/subscriptions/subscribe', {
  planCode: planId,          // 'PRO'
  paymentMethod: 'VNPAY',
});
const transactionId = subRes.data.transactionId;

// 2. lấy URL VNPay
const urlRes = await api.post('/api/payments/vnpay/create-url', { transactionId });

// 3. chuyển trình duyệt sang trang thanh toán VNPay
window.location.href = urlRes.data.paymentUrl;
```

> `mock-confirm` vẫn giữ để test nhanh không qua cổng — nhưng luồng thật dùng VNPay ở trên.

### 2) Trang `/subscription` đọc kết quả trả về

BE redirect về `VNP_FE_RETURN_URL` (mặc định `http://localhost:3000/subscription`)
kèm query `?status=...`:

| `status` | Ý nghĩa | Gợi ý hiển thị |
|---|---|---|
| `success` | Thanh toán thành công, gói đã kích hoạt | Toast xanh + refetch `/subscriptions/me` |
| `failed`  | User huỷ / thẻ lỗi (`&code=<vnp_ResponseCode>`) | Toast đỏ "Thanh toán thất bại" |
| `invalid` | Chữ ký sai (nghi giả mạo) | Toast đỏ "Giao dịch không hợp lệ" |
| `error`   | Lỗi khi kích hoạt | Toast đỏ "Có lỗi, liên hệ hỗ trợ" |

```js
// trong SubscriptionPage, đọc query khi mount
const params = new URLSearchParams(window.location.search);
const status = params.get('status');
if (status === 'success') { showToast('Nâng cấp PRO thành công!', 'success'); refetchSubscription(); }
else if (status) { showToast('Thanh toán không thành công.', 'error'); }
// rồi xoá query khỏi URL cho sạch
```

---

## Endpoints BE (tham chiếu)

| Method | Path | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/subscriptions/subscribe` | user | Tạo giao dịch PENDING, trả `transactionId` |
| POST | `/api/payments/vnpay/create-url` | user | Body `{transactionId}` → trả `{paymentUrl}` |
| GET  | `/api/payments/vnpay/return` | public | VNPay redirect về; BE verify + 302 về FE |
| GET  | `/api/payments/vnpay/ipn` | public | VNPay IPN server-to-server (BE tự xử lý) |

## Cấu hình BE (.env)
```
VNP_TMN_CODE=<mã merchant sandbox>
VNP_HASH_SECRET=<secret sandbox>
VNP_RETURN_URL=http://localhost:8080/api/payments/vnpay/return
VNP_FE_RETURN_URL=http://localhost:3000/subscription
# (production: đổi sang domain thật + VNP_PAY_URL production của VNPay)
```
