# 🚀 API Documentation (Dành cho Frontend) — Tính Năng Upload & Duyệt Bài

Tài liệu này hướng dẫn team FE cách gọi và xử lý dữ liệu cho 4 FR mới được thêm vào hệ thống (Upload bài, Duyệt bài, Phân quyền Moderator, Thông báo).

---

## 1. 📝 Tính năng Upload Bài (Dành cho Role: `RESEARCHER`, `LECTURER`, `ADMIN`)

### 1.1. Upload bài báo mới
- **Endpoint:** `POST /api/papers/upload`
- **Quyền yêu cầu:** Phải kèm Header `Authorization: Bearer <token>`
- **Body (JSON):**
```json
{
  "title": "Tên bài báo (Bắt buộc)",
  "abstractText": "Tóm tắt bài báo (Optional)",
  "year": 2026,
  "keywords": ["ai", "machine learning"],
  "pdfUrl": "https://link-to-pdf.com/file.pdf"
}
```
- **Response (201 Created):**
```json
{
  "message": "Paper submitted for review",
  "paperId": 12,
  "status": "PENDING"
}
```
> **Lưu ý FE:** Sau khi upload thành công, hãy hiển thị thông báo toast: "Bài báo của bạn đã được gửi đi và đang chờ duyệt".

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
  "totalUploads": 17
}
```

### 2.2. Xem danh sách bài theo trạng thái
- **Endpoint:** `GET /api/moderation/papers`
- **Query Params:**
  - `status`: `PENDING` | `APPROVED` | `REJECTED` (Mặc định là `PENDING`)
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
