# 📤 Hướng dẫn FE — Tính năng Upload Paper (bản đầy đủ)

> Tài liệu tích hợp dành cho **FE team**, gói gọn mọi thứ cần để làm màn hình Upload Paper
> và các luồng liên quan (my-uploads, báo cáo bản quyền, hiển thị license/embargo/gỡ bài)
> sau đợt cập nhật backend.
>
> Base URL giữ nguyên. Mọi request đăng nhập gửi header `Authorization: Bearer <token>`.

---

## 0. Tóm tắt việc FE cần làm

| # | Việc | Nếu bỏ sót |
|---|------|-----------|
| 1 | Form upload thêm **4 field bắt buộc**: `license`, `publicationType`, `ownershipConfirmed`, `termsAccepted` | Upload trả **400** |
| 2 | 2 checkbox xác nhận (quyền sở hữu + điều khoản) — disable Submit nếu chưa tick | — |
| 3 | Dropdown `license` (4) và `publicationType` (2); date picker `embargoUntil` (optional) | — |
| 4 | Trang chi tiết bài: hiển thị **badge license**, xử lý **404** cho bài embargo/bị gỡ | Hiện lỗi khó hiểu |
| 5 | Thêm nút "🚩 Báo cáo vi phạm bản quyền" (chỉ khi đã đăng nhập) | — |

---

## 1. Upload bài báo

### `POST /api/papers/upload`
- **Quyền:** role `RESEARCHER` hoặc `ADMIN` (kèm token).
- **Request body:**

```json
{
  "title": "Deep Learning cho phân loại ảnh y tế",   // bắt buộc, ≤ 1000 ký tự
  "abstractText": "Nghiên cứu này đề xuất...",        // bắt buộc, ≤ 10000 ký tự
  "year": 2026,                                        // optional
  "paperUri": "https://doi.org/10.xxxx/yyyy",          // optional, ≤ 500
  "authors": ["Nguyen Van A", "Tran Thi B"],           // optional, ≤ 20 phần tử
  "journals": ["Journal of Medical AI"],               // optional, ≤ 10
  "keywords": ["deep learning", "medical imaging"],    // optional, ≤ 30

  "license": "CC_BY",                                  // BẮT BUỘC
  "publicationType": "ORIGINAL_THESIS",                // BẮT BUỘC
  "ownershipConfirmed": true,                          // BẮT BUỘC, phải = true
  "termsAccepted": true,                               // BẮT BUỘC, phải = true
  "embargoUntil": "2027-01-01"                         // optional, phải là ngày tương lai
}
```

- **Response 201 (Created):** object `PaperDto` đầy đủ:
```json
{
  "id": 12,
  "title": "Deep Learning cho phân loại ảnh y tế",
  "status": "PENDING",
  "license": "CC_BY",
  "publicationType": "ORIGINAL_THESIS",
  "embargoUntil": "2027-01-01",
  "source": "USER_UPLOAD",
  "isSelfPublished": true,
  "uploadStatus": "PENDING",
  "...": "..."
}
```
> Sau upload, hiển thị toast: **"Bài báo của bạn đã được gửi đi và đang chờ duyệt."**

- **Response 400 (Bad Request):** trả map `{ "field": "message" }`, ví dụ:
```json
{ "license": "License is required...", "termsAccepted": "You must accept the Terms of Service to upload a paper" }
```

### Bảng lỗi 400 hay gặp
| Nguyên nhân | Xử lý FE |
|---|---|
| Thiếu `license` / `publicationType` | Bắt chọn dropdown trước khi submit |
| `ownershipConfirmed` / `termsAccepted` ≠ `true` | Disable Submit đến khi tick cả 2 checkbox |
| `embargoUntil` là ngày quá khứ | Date picker chỉ cho chọn ngày tương lai |
| `title`/`abstractText` vượt độ dài | Đếm ký tự, chặn ở client |

---

## 2. Yêu cầu UI cho form upload

1. **2 checkbox bắt buộc** (nút Submit disabled cho tới khi cả hai được tick):
   - ☐ "Tôi là tác giả hoặc có quyền hợp pháp để đăng bài này." → `ownershipConfirmed`
   - ☐ "Tôi đã đọc và đồng ý với **Điều khoản đăng tải**." → `termsAccepted`
     *(nội dung điều khoản lấy từ `TERMS_OF_SERVICE.md`, hiển thị dạng modal/link)*
2. **Dropdown `license`** — 4 lựa chọn (label gợi ý ở Phụ lục).
3. **Dropdown `publicationType`** — 2 lựa chọn. Khi chọn **`PREVIOUSLY_PUBLISHED`**, hiện cảnh báo:
   > ⚠️ "Hãy đảm bảo nhà xuất bản cho phép bạn đăng lại bài này."
4. **Date picker `embargoUntil`** (optional) — kèm tooltip: "Bài sẽ được ẩn khỏi công khai đến hết ngày này, dù đã được duyệt."

---

## 3. Xem bài đã upload của mình

### `GET /api/papers/my-uploads?page=0&size=10`
- **Quyền:** `RESEARCHER` / `ADMIN` (token).
- **Response:** `Page<PaperDto>`. Dùng field `status` (hoặc `uploadStatus` — cùng giá trị) để render nhãn trạng thái:

| `status` | Nhãn gợi ý |
|---|---|
| `PENDING` | 🟡 Đang chờ duyệt |
| `APPROVED` | 🟢 Đã duyệt |
| `REJECTED` | 🔴 Bị từ chối (xem `rejectionReason`) |
| `TAKEN_DOWN` | ⚫ Đã bị gỡ (vi phạm bản quyền) |
| `REVOKED` | ⚫ Đã bị thu hồi (admin gỡ bài đã duyệt) |

> Uploader **vẫn xem được** bài của mình kể cả khi `TAKEN_DOWN` / `REVOKED` / đang embargo (khác với người ngoài — họ nhận 404). Nên hiển thị nhãn "Đang embargo đến {ngày}" nếu `embargoUntil` còn hiệu lực.

---

## 4. Báo cáo vi phạm bản quyền (mọi user đăng nhập)

### `POST /api/papers/{id}/copyright-report`
```json
{ "reason": "Bài này sao chép luận văn của tôi mà không xin phép." }  // ≤ 2000 ký tự
```
- **201:** tạo report thành công.
- **400:** bài đã bị gỡ rồi, **hoặc** bạn đã có 1 report đang chờ xử lý trên bài này (mỗi user 1 report chờ/bài).

> UI: nút "🚩 Báo cáo vi phạm bản quyền" trên trang chi tiết bài, chỉ hiện khi đã đăng nhập. Mở modal nhập lý do.

---

## 5. Hành vi hiển thị FE cần xử lý

### 5.1 Badge license
`PaperDto` có `license` — hiển thị badge trên trang chi tiết:
| `license` | Badge gợi ý |
|---|---|
| `CC_BY` | `CC BY` |
| `CC_BY_NC` | `CC BY-NC` |
| `ALL_RIGHTS_RESERVED` | `© All rights reserved` |
| `AUTHOR_AGREEMENT` | `Author agreement` |
| `null` | (bài nguồn ngoài — không hiện badge) |

### 5.2 Bài bị ẩn (embargo / gỡ)
`GET /api/papers/{id}` trả **404** khi bài đang `embargoUntil` (> hôm nay), `TAKEN_DOWN`, hoặc `REVOKED` — **với mọi người trừ chính uploader**.
- Người ngoài: FE hiển thị trang "Bài viết không tồn tại hoặc không khả dụng".
- Uploader (trong my-uploads): vẫn 200, hiển thị nhãn trạng thái tương ứng.
- Các bài này **không** xuất hiện trong search / danh sách công khai.

---

## Phụ lục — Enum cho dropdown

**`license`**
| Value | Label hiển thị | Nghĩa |
|---|---|---|
| `CC_BY` | Creative Commons BY | Chia sẻ/phân phối lại tự do, phải ghi công tác giả |
| `CC_BY_NC` | Creative Commons BY-NC | Như trên nhưng cấm mục đích thương mại |
| `ALL_RIGHTS_RESERVED` | Giữ toàn bộ quyền | Chỉ hiển thị metadata + abstract |
| `AUTHOR_AGREEMENT` | Thỏa thuận tác giả | Cấp quyền hiển thị cho riêng TrendSearchor |

**`publicationType`**
| Value | Label | Ghi chú |
|---|---|---|
| `ORIGINAL_THESIS` | Nghiên cứu/luận văn gốc | Chưa công bố nơi khác |
| `PREVIOUSLY_PUBLISHED` | Đã xuất bản trước đó | Cần đảm bảo NXB cho phép đăng lại |

**`status` (PaperStatus):** `PENDING` · `APPROVED` · `REJECTED` · `TAKEN_DOWN` · `REVOKED`

---

> Tài liệu liên quan: `FRONTEND_CHANGES.md` (tổng hợp toàn bộ thay đổi API — moderation/AI/legal), `TERMS_OF_SERVICE.md` (nội dung điều khoản).
