# Điều khoản Đăng tải Nội dung (Upload Agreement)

**Phiên bản: 1.0** — áp dụng từ 2026-07-18
*(khớp với `app.legal.terms-version` trong cấu hình backend; khi sửa nội dung file này phải bump version ở cả hai nơi)*

> ⚠️ Tài liệu này là bản mẫu phục vụ đồ án sinh viên. Trước khi vận hành thật,
> cần được bộ phận pháp chế / sở hữu trí tuệ rà soát.

---

## 1. Phạm vi

Điều khoản này áp dụng cho mọi hành vi đăng tải bài báo khoa học, luận văn,
hoặc công trình nghiên cứu ("Nội dung") lên nền tảng TrendSearchor thông qua
chức năng Upload Paper. Bằng việc tích chọn **"Tôi đồng ý với Điều khoản"**
(`termsAccepted`) khi upload, bạn xác nhận đã đọc, hiểu và chấp nhận toàn bộ
điều khoản dưới đây.

## 2. Cam kết của người đăng

Khi tích chọn **xác nhận quyền sở hữu** (`ownershipConfirmed`), bạn cam kết:

1. Bạn là **tác giả** của Nội dung, hoặc có **quyền hợp pháp** (được đồng tác giả
   / chủ sở hữu quyền ủy quyền) để đăng tải Nội dung lên nền tảng.
2. Nội dung là **nguyên gốc**, không sao chép, đạo văn từ công trình của người khác.
3. Việc đăng tải không vi phạm hợp đồng chuyển nhượng bản quyền nào bạn đã ký
   (ví dụ với nhà xuất bản, tạp chí, trường đại học hoặc đơn vị tài trợ).
4. Thông tin khai báo (tác giả, năm, nguồn xuất bản...) là chính xác và trung thực.

## 3. Phân loại công bố (`publicationType`)

| Giá trị | Ý nghĩa | Lưu ý pháp lý |
|---|---|---|
| `ORIGINAL_THESIS` | Luận văn / nghiên cứu gốc của chính bạn, chưa công bố nơi khác | Bạn giữ toàn quyền tác giả |
| `PREVIOUSLY_PUBLISHED` | Bài đã xuất bản ở hội nghị / tạp chí | **Bạn phải tự đảm bảo** nhà xuất bản cho phép đăng lại (kiểm tra chính sách self-archiving / Sherpa Romeo) |

## 4. Giấy phép hiển thị (`license`)

Bạn chọn một trong các giấy phép sau, quyết định người đọc được làm gì với Nội dung:

| Giá trị | Ý nghĩa |
|---|---|
| `CC_BY` | Người khác được chia sẻ, phân phối lại, kể cả mục đích thương mại, **phải ghi công tác giả** |
| `CC_BY_NC` | Như CC_BY nhưng **cấm mục đích thương mại** |
| `ALL_RIGHTS_RESERVED` | Bạn giữ toàn bộ quyền; nền tảng chỉ hiển thị metadata + abstract |
| `AUTHOR_AGREEMENT` | Bạn cấp cho TrendSearchor quyền không độc quyền để hiển thị Nội dung trong phạm vi nền tảng |

Trong mọi trường hợp, bạn cấp cho TrendSearchor quyền tối thiểu, không độc quyền,
để lưu trữ và hiển thị Nội dung theo giấy phép đã chọn.

## 5. Embargo (`embargoUntil`)

Nếu Nội dung thuộc diện hạn chế công bố có thời hạn (quy định của trường, dự án,
hoặc nhà tài trợ), bạn có thể đặt ngày embargo. Nội dung đã được duyệt sẽ **không
hiển thị công khai** (không xuất hiện trong tìm kiếm, truy cập trực tiếp trả 404)
cho đến hết ngày này. Chỉ bạn (người đăng) và đội kiểm duyệt xem được trong thời gian đó.

## 6. Kiểm duyệt và gỡ bỏ (Notice-and-Takedown)

1. Mọi Nội dung upload đều ở trạng thái **chờ duyệt** (`PENDING`) và chỉ hiển thị
   công khai sau khi được kiểm duyệt viên phê duyệt.
2. Bất kỳ người dùng nào cũng có thể **báo cáo vi phạm bản quyền** đối với một bài
   (`POST /api/papers/{id}/copyright-report`).
3. Khi báo cáo được xác nhận, Nội dung sẽ bị **gỡ khỏi hiển thị công khai**
   (trạng thái `TAKEN_DOWN`) và bạn sẽ nhận được thông báo kèm lý do.
4. Nền tảng có quyền gỡ bỏ Nội dung vi phạm mà không cần sự đồng ý của người đăng.

## 7. Dữ liệu được lưu làm bằng chứng

Khi bạn upload, hệ thống lưu kèm bản ghi: phiên bản điều khoản bạn đã đồng ý
(`termsVersion`), thời điểm đồng ý (`termsAcceptedAt`), và **địa chỉ IP**
(`uploadedByIp`). Dữ liệu này chỉ dùng cho mục đích đối chiếu khi có tranh chấp
pháp lý về quyền đăng tải.

## 8. Trách nhiệm

- Người đăng chịu **toàn bộ trách nhiệm pháp lý** về Nội dung mình đăng tải,
  bao gồm mọi khiếu nại về bản quyền, đạo văn hoặc thông tin sai lệch.
- TrendSearchor là nền tảng trung gian lưu trữ/hiển thị, xử lý khiếu nại theo
  quy trình notice-and-takedown ở Mục 6, và không chịu trách nhiệm thay cho
  người đăng về các vi phạm do người đăng gây ra.

## 9. Thay đổi điều khoản

Khi điều khoản thay đổi, phiên bản mới sẽ được áp dụng cho các lượt upload **sau
thời điểm cập nhật**. Các bản ghi cũ vẫn gắn với phiên bản điều khoản tại thời
điểm người dùng đã đồng ý.
