# 🚀 API Documentation (Dành cho Frontend) — Tính Năng Upload & Duyệt Bài

Tài liệu này hướng dẫn team FE cách gọi và xử lý dữ liệu cho 4 FR mới được thêm vào hệ thống (Upload bài, Duyệt bài, Phân quyền Moderator, Thông báo).

---

## 1. 📝 Tính năng Upload Bài (Dành cho Role: `RESEARCHER`, `LECTURER`, `ADMIN`)

### 1.1. Upload bài báo mới (⚠️ ĐÃ CẬP NHẬT — có field pháp lý bắt buộc)
- **Endpoint:** `POST /api/papers/upload`
- **Quyền yêu cầu:** Phải kèm Header `Authorization: Bearer <token>` (role RESEARCHER/ADMIN)
- **Body (JSON):**
```json
{
  "title": "Tên bài báo (BẮT BUỘC, ≤1000 ký tự)",
  "abstractText": "Tóm tắt bài báo (BẮT BUỘC, ≤10000 ký tự)",
  "year": 2026,
  "paperUri": "https://doi.org/... (optional, ≤500 ký tự)",
  "authors": ["Nguyen Van A"],
  "journals": ["Tên tạp chí (optional)"],
  "keywords": ["ai", "machine learning"],

  "license": "CC_BY | CC_BY_NC | ALL_RIGHTS_RESERVED | AUTHOR_AGREEMENT (BẮT BUỘC)",
  "publicationType": "ORIGINAL_THESIS | PREVIOUSLY_PUBLISHED (BẮT BUỘC)",
  "ownershipConfirmed": true,
  "termsAccepted": true,
  "embargoUntil": "2027-01-01 (optional, phải là ngày tương lai)"
}
```
- **Response (201 Created):** object `PaperDto` đầy đủ (có `id`, `status: "PENDING"`, `license`, `publicationType`, `embargoUntil`...).
- **Response (400):** thiếu `license`/`publicationType`, hoặc `ownershipConfirmed`/`termsAccepted` khác `true` → body trả map `{field: message}`.

> **Lưu ý FE (QUAN TRỌNG):**
> 1. Form upload phải có **2 checkbox bắt buộc**: "Tôi là tác giả / có quyền đăng bài này" (`ownershipConfirmed`) và "Tôi đồng ý với [Điều khoản đăng tải](TERMS_OF_SERVICE.md)" (`termsAccepted`). Không tick → disable nút submit.
> 2. Dropdown chọn `license` (4 giá trị) và `publicationType` (2 giá trị). Nếu user chọn `PREVIOUSLY_PUBLISHED`, nên hiện cảnh báo "Hãy đảm bảo nhà xuất bản cho phép đăng lại".
> 3. `embargoUntil` là date picker tùy chọn — bài sẽ ẩn khỏi công khai đến hết ngày này.
> 4. Sau khi upload thành công, hiển thị toast: "Bài báo của bạn đã được gửi đi và đang chờ duyệt".

### 1.2. Xem danh sách bài đã upload của chính mình
- **Endpoint:** `GET /api/papers/my-uploads`
- **Query Params:**
  - `page` (mặc định: 0)
  - `size` (mặc định: 10)
- **Response (200 OK):** Trả về object `Page<ResearchPaper>`. Chú ý field `uploadStatus` sẽ có 1 trong 3 giá trị: `PENDING`, `APPROVED`, hoặc `REJECTED`. Nếu bị từ chối, sẽ có thêm field `rejectionReason`.

---

## 2. 🛡️ Tính năng Duyệt Bài (Dành cho Role: `MODERATOR`, `ADMIN`)

Dành cho giao diện **Moderation Dashboard**.

### 2.1. Lấy thông số thống kê (Stats)
- **Endpoint:** `GET /api/moderation/stats`
- **Response (200 OK):**
```json
{
  "pendingCount": 5,
  "approvedCount": 10,
  "rejectedCount": 2,
  "totalUploads": 17,
  "takenDownCount": 1,
  "revokedCount": 0,
  "pendingCopyrightReports": 3
}
```

### 2.2. Xem danh sách bài theo trạng thái
- **Endpoint:** `GET /api/moderation/papers`
- **Query Params:**
  - `status`: `PENDING` | `APPROVED` | `REJECTED` | `TAKEN_DOWN` | `REVOKED` (Mặc định là `PENDING`)
  - `page`, `size`
- **Response (200 OK):** `Page<ResearchPaper>`

### 2.3. Lấy chi tiết 1 bài chờ duyệt
- **Endpoint:** `GET /api/moderation/papers/{id}`
- **Response (200 OK):** Trả về toàn bộ chi tiết bài báo (Model: `ResearchPaper`).

### 2.4. Duyệt bài (Approve)
- **Endpoint:** `PATCH /api/moderation/papers/{id}/approve`
- **Body:** Bỏ trống
- **Response (200 OK):**
```json
{
  "message": "Paper approved successfully",
  "paperId": 12,
  "status": "APPROVED"
}
```

### 2.5. Từ chối bài (Reject)
- **Endpoint:** `PATCH /api/moderation/papers/{id}/reject`
- **Body (JSON):**
```json
{
  "reason": "Bài báo không hợp lệ vì..."
}
```
- **Response (200 OK):**
```json
{
  "message": "Paper rejected",
  "paperId": 12,
  "status": "REJECTED",
  "reason": "Bài báo không hợp lệ vì..."
}
```

---

## 3. 🔍 Tính năng Search (Đã Cập Nhật)

- **Endpoint:** `GET /api/papers/search`
- **Sự thay đổi:** FE **không cần thay đổi cách gọi API**. Backend đã tự động merge các bài báo Local (các bài được Moderator duyệt) lên **trên cùng** kết quả tìm kiếm của OpenAlex.
- Các bài báo do user upload sẽ có field: `source: "USER_UPLOAD"`, các bài từ nguồn ngoài sẽ là `source: "OPENALEX"`. FE có thể dùng field này để gắn badge (VD: `[Local Upload]`) để làm nổi bật.

---

## 4. 🔔 Thông báo (Notification System)

Hệ thống thông báo vẫn gọi bằng cụm API `/api/notifications/…` như cũ. Tuy nhiên backend đã tự động sinh ra 3 loại `NotificationType` mới. FE cần chú ý để render Icon/Màu sắc cho phù hợp:

1. `PAPER_UPLOADED`: (Gửi cho Mod/Admin) - Báo có bài mới đang chờ duyệt.
2. `PAPER_APPROVED`: (Gửi cho Researcher) - Báo tin vui là bài của họ đã được duyệt.
3. `PAPER_REJECTED`: (Gửi cho Researcher) - Báo tin buồn bài bị từ chối kèm lý do.
4. `NEW_PAPER`: (Gửi cho User thường) - Thông báo public có 1 bài mới vừa ra mắt.

---

## 5. 👑 Cấp quyền Moderator (Role Management)

Nếu Frontend làm giao diện Admin quản lý User, để cấp quyền Moderator cho 1 user bất kỳ:

- **Endpoint:** `PATCH /api/admin/users/{userId}/role`
- **Body:**
```json
{
  "role": "MODERATOR"
}
```
> Ngay sau khi gọi API này, User đó (nếu đang đăng nhập) cần F5 lại để nhận quyền truy cập vào các API Moderation.
> ⚠️ Endpoint cũ `PATCH /api/admin/users/{id}/moderator` (cờ isModerator) **đã bị xoá** — chỉ dùng `/role`. Field `isModerator` trong `UserResponse` vẫn tồn tại nhưng giờ được suy ra từ `role === "MODERATOR"`.

---

## 6. ⚖️ Bản quyền & Gỡ bài (Notice-and-Takedown) — MỚI

### 6.1. User báo cáo bài vi phạm bản quyền
- **Endpoint:** `POST /api/papers/{id}/copyright-report`
- **Quyền:** cần đăng nhập (mọi role)
- **Body:** `{ "reason": "Bài này sao chép luận văn của tôi... (≤2000 ký tự)" }`
- **Response (201):** object `CopyrightReportDto`
- **Response (400):** bài đã bị gỡ rồi, hoặc user này đã có report đang chờ trên cùng bài (chống spam)

### 6.2. Moderator xem hàng đợi report (Role: MODERATOR/ADMIN)
- **Endpoint:** `GET /api/moderation/copyright-reports?status=PENDING&page=0&size=10`
- `status`: `PENDING` | `DISMISSED` | `ACTION_TAKEN`
- **Response:** `Page<CopyrightReportDto>` — mỗi item có `paperId`, `paperTitle`, `reportedByMail`, `reason`, `createdAt`...

### 6.3. Moderator xử lý report
- **Endpoint:** `PATCH /api/moderation/copyright-reports/{id}/resolve`
- **Body:** `{ "action": "DISMISS" | "TAKE_DOWN", "notes": "ghi chú (optional, ≤2000)" }`
- `TAKE_DOWN`: bài chuyển `TAKEN_DOWN` (biến mất khỏi search + truy cập trực tiếp trả 404), uploader và mọi người report đều nhận notification; các report chờ khác của cùng bài tự đóng.
- `DISMISS`: bài giữ nguyên, report đóng lại.

### 6.4. Hành vi hiển thị FE cần biết
- `PaperDto` giờ có thêm: `license`, `publicationType`, `embargoUntil` → FE nên hiển thị **badge license** (VD: `CC BY`, `All Rights Reserved`) trên trang chi tiết bài.
- Bài đang **embargo** (`embargoUntil` > hôm nay): không xuất hiện trong search/list công khai; truy cập trực tiếp trả **404** — trừ chính uploader (vẫn thấy bài của mình trong my-uploads và qua link trực tiếp, hãy hiển thị nhãn "Đang embargo đến {date}").
- Bài `TAKEN_DOWN`: tương tự — 404 với mọi người trừ uploader.
- Nên đặt link "🚩 Báo cáo vi phạm bản quyền" trên trang chi tiết bài (chỉ hiện khi đã đăng nhập).
- Nội dung điều khoản để hiển thị trong form upload: xem file `TERMS_OF_SERVICE.md` (version 1.0).
