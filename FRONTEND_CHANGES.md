# 📋 Thay đổi API cho Frontend — Đợt cập nhật (Moderation · AI · Bản quyền)

> Tài liệu này liệt kê **mọi thay đổi backend ảnh hưởng tới FE** trong đợt vừa rồi,
> kèm việc FE **bắt buộc phải làm** để tránh lỗi 400/401. Đọc mục ⚠️ trước.
>
> Backend đã test runtime end-to-end (9/9 pass). Base URL: giữ nguyên. Auth: vẫn `Authorization: Bearer <token>`.

---

## ⚠️ TL;DR — Việc FE BẮT BUỘC làm

| # | Việc | Nếu không làm |
|---|------|---------------|
| 1 | Form **upload paper** thêm 4 field bắt buộc (`license`, `publicationType`, `ownershipConfirmed`, `termsAccepted`) | Upload trả **400** |
| 2 | Gọi `POST /api/ai/summarize` và `POST /api/ai/rerank` **kèm token** | Trả **401** |
| 3 | Bỏ dùng endpoint `PATCH /api/admin/users/{id}/moderator` (đã xoá) → cấp moderator bằng `/role` | Trả **404/405** |
| 4 | Xử lý mã **429** cho mọi endpoint `/api/ai/*` (rate limit) | User spam sẽ bị chặn, cần hiển thị thông báo |
| 5 | Trang chi tiết bài xử lý **404** cho bài bị gỡ / đang embargo | Hiện lỗi không rõ ràng |

---

## A. Upload Paper — thêm khai báo pháp lý (BREAKING)

### `POST /api/papers/upload`
Quyền: `RESEARCHER` hoặc `ADMIN`. Header `Authorization: Bearer <token>`.

**Body mới:**
```json
{
  "title": "Tên bài (bắt buộc, ≤1000 ký tự)",
  "abstractText": "Tóm tắt (bắt buộc, ≤10000 ký tự)",
  "year": 2026,
  "paperUri": "https://doi.org/... (optional, ≤500)",
  "authors": ["Nguyen Van A"],
  "journals": ["Journal of X"],
  "keywords": ["ai", "nlp"],

  "license": "CC_BY",                 // BẮT BUỘC — xem bảng enum bên dưới
  "publicationType": "ORIGINAL_THESIS", // BẮT BUỘC
  "ownershipConfirmed": true,         // BẮT BUỘC = true
  "termsAccepted": true,              // BẮT BUỘC = true
  "embargoUntil": "2027-01-01"        // optional, phải là ngày tương lai
}
```

**Response 201:** object `PaperDto` (có `id`, `status: "PENDING"`, `license`, `publicationType`, `embargoUntil`).

**Response 400** (kèm map `{field: message}`) khi:
- Thiếu `license` / `publicationType`
- `ownershipConfirmed` hoặc `termsAccepted` không phải `true`
- `embargoUntil` là ngày quá khứ
- Vượt giới hạn độ dài

### UI cần có
1. **2 checkbox bắt buộc** (disable nút Submit nếu chưa tick cả hai):
   - ☐ "Tôi là tác giả hoặc có quyền hợp pháp để đăng bài này" → `ownershipConfirmed`
   - ☐ "Tôi đồng ý với [Điều khoản đăng tải](TERMS_OF_SERVICE.md)" → `termsAccepted`
2. **Dropdown `license`** (4 lựa chọn) và **`publicationType`** (2 lựa chọn).
3. Khi chọn `PREVIOUSLY_PUBLISHED` → hiện cảnh báo: *"Hãy đảm bảo nhà xuất bản cho phép đăng lại bài này."*
4. **Date picker `embargoUntil`** (tùy chọn) — giải thích: bài sẽ ẩn khỏi công khai đến hết ngày này.

Nội dung điều khoản để hiển thị: file `TERMS_OF_SERVICE.md` (version 1.0).

---

## B. Bản quyền & Gỡ bài (Notice-and-Takedown) — TÍNH NĂNG MỚI

### B.1 User báo cáo vi phạm — `POST /api/papers/{id}/copyright-report`
Quyền: mọi user đã đăng nhập.
```json
// Request body
{ "reason": "Bài này sao chép luận văn của tôi (≤2000 ký tự)" }
```
- **201:** `CopyrightReportDto`
- **400:** bài đã bị gỡ, hoặc user đã có report PENDING trên bài này (chống spam — mỗi user 1 report chờ/bài)

**UI gợi ý:** nút "🚩 Báo cáo vi phạm bản quyền" trên trang chi tiết bài, chỉ hiện khi đã đăng nhập.

### B.2 Moderator xem hàng đợi — `GET /api/moderation/copyright-reports`
Quyền: `MODERATOR` / `ADMIN`.
- Query: `status` = `PENDING` | `DISMISSED` | `ACTION_TAKEN` (mặc định PENDING), `page`, `size`
- **Response:** `Page<CopyrightReportDto>`

### B.3 Moderator xử lý — `PATCH /api/moderation/copyright-reports/{id}/resolve`
```json
{ "action": "TAKE_DOWN", "notes": "Xác nhận vi phạm (optional, ≤2000)" }
// action: "DISMISS" | "TAKE_DOWN"
```
- `TAKE_DOWN`: bài → `TAKEN_DOWN` (ẩn khỏi search + truy cập trực tiếp trả 404); uploader và mọi người đã report đều nhận notification; các report chờ khác của cùng bài tự đóng.
- `DISMISS`: bài giữ nguyên, report đóng lại.
- **400** nếu report đã được xử lý trước đó.

### `CopyrightReportDto`
```json
{
  "id": 1, "paperId": 12, "paperTitle": "...", "paperStatus": "TAKEN_DOWN",
  "reportedByMail": "user@mail.com", "reason": "...",
  "status": "ACTION_TAKEN", "resolutionNotes": "...",
  "reviewedByMail": "mod@mail.com", "reviewedAt": "...", "createdAt": "..."
}
```

---

## C. Endpoint AI — siết auth + rate limit + giới hạn độ dài

### C.1 `summarize` và `rerank` giờ CẦN đăng nhập (BREAKING)
| Endpoint | Trước | Giờ |
|---|---|---|
| `POST /api/ai/summarize` | public | **cần token** (401 nếu thiếu) |
| `POST /api/ai/rerank` | public | **cần token** (401 nếu thiếu) |
| `POST /api/ai/search` | public | public (không đổi) |
| `POST /api/ai/trend-qa` | public | public (không đổi) |
| `POST /api/ai/abstract`, `GET /api/ai/recommendations` | cần token | cần token (không đổi) |

→ Nếu FE gọi summarize/rerank ở trang cho khách chưa login: chỉ gọi khi đã đăng nhập, hoặc gắn token.

### C.2 Rate limit — tất cả `/api/ai/*`
- Giới hạn **20 request/phút** cho mỗi user (hoặc mỗi IP nếu chưa login).
- Vượt → **HTTP 429** `{ "message": "Too many AI requests. Please wait a moment before trying again." }`
- FE nên bắt 429 và hiển thị toast "Bạn thao tác AI quá nhanh, thử lại sau chút nhé".

### C.3 Giới hạn độ dài input (400 nếu vượt)
| Field | Endpoint | Max |
|---|---|---|
| `text` | /abstract | 6000 |
| `query` | /search | 500 |
| `question` / `keyword` | /trend-qa | 1000 / 200 |
| `title` / `abstractText` / `authors` | /summarize | 500 / 6000 / 1000 |
| `query` / `papers` (list) | /rerank | 500 / **tối đa 20 bài** |

→ `rerank` chỉ nhận **tối đa 20 papers/lần**. Nếu FE đang gửi cả trang kết quả, cắt còn ≤20.

---

## D. Kiểm duyệt & Phân quyền Moderator

### D.1 Endpoint cấp moderator ĐÃ XOÁ (BREAKING)
- ❌ `PATCH /api/admin/users/{id}/moderator` — **không còn tồn tại**.
- ✅ Cấp moderator bằng: `PATCH /api/admin/users/{id}/role` body `{ "role": "MODERATOR" }`.
- Field `isModerator` trong `UserResponse` vẫn còn nhưng giờ **suy ra từ** `role === "MODERATOR"` (không phải cờ riêng nữa).

### D.2 Moderation giờ chạy đúng
Trước đây hàng đợi kiểm duyệt đọc nhầm field nên **luôn rỗng** — giờ đã sửa. Không đổi cách gọi API, chỉ là dữ liệu hiển thị đúng.
- `GET /api/moderation/papers?status=` — `status` giờ nhận thêm **`TAKEN_DOWN`** (ngoài PENDING/APPROVED/REJECTED).

### D.3 `GET /api/moderation/stats` — thêm field
```json
{
  "pendingCount": 5, "approvedCount": 10, "rejectedCount": 2, "totalUploads": 17,
  "takenDownCount": 1,            // MỚI
  "pendingCopyrightReports": 3    // MỚI — badge cho tab report bản quyền
}
```

---

## E. `PaperDto` — field mới (mọi API trả bài báo)

Thêm 3 field, FE dùng để hiển thị badge & trạng thái:
```jsonc
{
  // ... các field cũ ...
  "license": "CC_BY",                 // hoặc null (bài từ nguồn ngoài)
  "publicationType": "ORIGINAL_THESIS",
  "embargoUntil": "2027-01-01"        // hoặc null
}
```

### Hành vi hiển thị FE cần biết
- **Badge license** trên trang chi tiết bài (VD: `CC BY`, `All Rights Reserved`).
- Bài **đang embargo** (`embargoUntil` > hôm nay) hoặc **`TAKEN_DOWN`**: `GET /api/papers/{id}` trả **404** với mọi người — **trừ chính uploader** (vẫn xem được bài của mình trong my-uploads / link trực tiếp). FE nên hiển thị nhãn "Đang embargo đến {ngày}" hoặc "Đã bị gỡ" cho uploader.
- Các bài này cũng không xuất hiện trong search / danh sách công khai.

---

## Phụ lục — Bảng enum (dùng cho dropdown)

**`license`**
| Giá trị | Nghĩa |
|---|---|
| `CC_BY` | Chia sẻ/phân phối lại tự do, phải ghi công tác giả |
| `CC_BY_NC` | Như CC_BY nhưng cấm mục đích thương mại |
| `ALL_RIGHTS_RESERVED` | Giữ toàn quyền; chỉ hiển thị metadata + abstract |
| `AUTHOR_AGREEMENT` | Cấp quyền hiển thị cho riêng TrendSearchor |

**`publicationType`**: `ORIGINAL_THESIS` (nghiên cứu gốc) · `PREVIOUSLY_PUBLISHED` (đã xuất bản nơi khác)

**`status` của bài (PaperStatus)**: `PENDING` · `APPROVED` · `REJECTED` · `TAKEN_DOWN`

**`CopyrightReportStatus`**: `PENDING` · `DISMISSED` · `ACTION_TAKEN`
