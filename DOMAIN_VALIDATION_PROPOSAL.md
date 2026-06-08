# TÀI LIỆU ĐỀ XUẤT: QUY TRÌNH LỌC VÀ KIỂM DUYỆT DỮ LIỆU THEO LĨNH VỰC (DOMAIN VALIDATION)

## 1. Bối cảnh & Mục tiêu (Context & Objective)
**Vấn đề:** Hiện tại, hệ thống TrendSearchor thu thập (crawl) dữ liệu từ các nguồn mở (OpenAlex, Semantic Scholar) mà không giới hạn chuyên ngành. Điều này dẫn đến nguy cơ dữ liệu bị "rác", loãng thông tin, ảnh hưởng đến độ chính xác của biểu đồ phân tích xu hướng (Trend Analysis), đồng thời gây quá tải cho cơ sở dữ liệu.

**Mục tiêu:**
- Thu hẹp phạm vi thu thập dữ liệu vào **một lĩnh vực cốt lõi duy nhất** (Ví dụ: Computer Science - Khoa học Máy tính).
- Thiết lập một cơ chế **Lọc (Filtering)** tự động ngay tại tầng giao tiếp API bên thứ 3.
- Xây dựng một quy trình **Kiểm duyệt (Validation)** có yếu tố con người (Human-in-the-loop) để đảm bảo chất lượng dữ liệu đầu vào.
- Thiết kế hệ thống phân quyền (RBAC) rõ ràng bằng cách định nghĩa thêm một Role mới chuyên biệt cho nghiệp vụ này.

---

## 2. Đề xuất Giải pháp Kỹ thuật (Technical Solution)

Giải pháp được chia làm 2 chốt chặn (Checkpoints) để đảm bảo dữ liệu sạch 100%:

### Chốt chặn 1: Hard-Filter tại tầng Data Sync (Tự động)
Thay vì kéo toàn bộ dữ liệu về rồi mới phân loại, Backend sẽ chủ động ép bộ lọc chuyên ngành vào chuỗi truy vấn (query parameter) khi gửi request lên OpenAlex/Semantic Scholar.
- **Ví dụ với OpenAlex:** Bổ sung tham số `filter=concepts.id:C41008148` (C41008148 là định danh của lĩnh vực Computer Science trên hệ thống OpenAlex).
- **Kết quả:** Ngăn chặn ngay từ gốc >90% bài báo thuộc các lĩnh vực không liên quan (Y học, Lịch sử, Tôn giáo...).

### Chốt chặn 2: Validation Dashboard & Role MODERATOR (Thủ công)
Bởi vì thuật toán AI của OpenAlex đôi khi vẫn phân loại sai, hệ thống cần một bước nghiệm thu cuối cùng bởi con người trước khi bài báo được phép đưa vào biểu đồ Trend.

**1. Định nghĩa Role mới (`MODERATOR`):**
- Thêm role `MODERATOR` (Kiểm duyệt viên) vào Enum `Role`.
- Role này khác `ADMIN` ở chỗ: Không có quyền xóa sửa tài khoản User, không có quyền can thiệp cấu hình hệ thống, **CHỈ CÓ QUYỀN** vào màn hình Kiểm duyệt dữ liệu bài báo.

**2. Cấu trúc Database (Entity `ResearchPaper`):**
Thêm trường `validationStatus` (Enum: `PENDING`, `APPROVED`, `REJECTED`) vào bảng bài báo.
- `PENDING`: Bài báo mới được Cronjob kéo về, chờ kiểm duyệt.
- `APPROVED`: Bài báo hợp lệ, được phép xuất hiện trên biểu đồ Trend và kết quả Search của người dùng.
- `REJECTED`: Bài báo lạc đề, bị ẩn khỏi hệ thống.

---

## 3. Workflow Vận hành Thực tế (Operational Flow)

### Kịch bản 1: Đồng bộ dữ liệu ngầm (Cronjob)
1. Đến giờ hẹn (vd: 2h sáng), `DataSyncService` kích hoạt.
2. Hệ thống gọi OpenAlex API kèm filter `Computer Science`.
3. Lưu 1000 bài báo mới vào Database với trạng thái mặc định: `validation_status = 'PENDING'`.

### Kịch bản 2: MODERATOR làm nhiệm vụ (Validation)
1. Người dùng có Role `MODERATOR` đăng nhập vào hệ thống.
2. Điều hướng đến màn hình **"Validation Workspace"**.
3. Hệ thống gọi API `GET /api/moderator/papers/pending` trả về danh sách bài báo chờ duyệt.
4. Moderator đọc nhanh Title và Abstract.
5. Nếu hợp lý -> Bấm **Approve**. Hệ thống gọi API `PATCH /api/moderator/papers/{id}/status` -> Update status thành `APPROVED`.
6. Nếu lạc đề -> Bấm **Reject** -> Update status thành `REJECTED`.

### Kịch bản 3: Người dùng bình thường truy cập
1. Sinh viên / Nhà nghiên cứu (Role `USER`) vào xem biểu đồ Trend.
2. Mọi câu query SQL tính toán Trend Count, Citation Count từ Backend giờ đây sẽ được nối thêm điều kiện: `WHERE validation_status = 'APPROVED'`.
3. Đảm bảo 100% dữ liệu hiển thị ra ngoài đều đã được kiểm định chất lượng nghiêm ngặt.

---

## 4. Kế hoạch Implement Backend (Implementation Plan)

Nếu ý tưởng này được thông qua, dưới đây là các Task cần thực hiện ở phía Backend:

- [ ] **Task 1:** Thêm Role `MODERATOR` vào `com.fpt.swp.model.Role`.
- [ ] **Task 2:** Thêm Enum `ValidationStatus` và trường `validationStatus` vào Entity `ResearchPaper`.
- [ ] **Task 3:** Cập nhật Flyway Migration (Viết file `.sql` thêm cột vào DB).
- [ ] **Task 4:** Sửa lại DataSyncService để truyền thêm Hard-Filter `concepts.id` khi call API ngoài. Set default status là `PENDING`.
- [ ] **Task 5:** Tạo `ModeratorController` với các API dành riêng cho Role MODERATOR:
      - Lấy danh sách PENDING (Phân trang).
      - API Cập nhật status (Duyệt hàng loạt hoặc duyệt lẻ từng bài).
- [ ] **Task 6:** Cập nhật lại các câu lệnh Query JPA trong `TrendService` và `SearchService` (Chỉ query bài `APPROVED`).

---
*Tài liệu này dùng để trình bày giải pháp thiết kế kiến trúc phần mềm với Giảng viên / Hội đồng bảo vệ đồ án nhằm chứng minh tư duy phản biện và khả năng kiểm soát luồng dữ liệu của hệ thống.*
