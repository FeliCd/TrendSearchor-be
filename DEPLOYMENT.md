# Hướng dẫn Deploy — TrendSearchor

Kiến trúc: **FE → Vercel**, **BE → Railway**, **MySQL → Railway**. (Vercel không chạy được
Java nên BE bắt buộc dùng Railway/Render.)

```
Vercel (FE React)  ──►  Railway (BE Spring Boot)  ──►  Railway MySQL
trend-searchor-fe       trendsearchor-be-production      (plugin database)
   .vercel.app             .up.railway.app
```

> Config sẵn có: BE có `Dockerfile`, FE có `vercel.json`, CORS đã cho phép domain Vercel.
> Đã thêm `server.port=${PORT:8080}` để bind port động của Railway.

---

## Phần A — Deploy BE + MySQL lên Railway

### A1. Tạo project + database
1. Vào https://railway.app → **New Project** → **Deploy from GitHub repo** → chọn `FeliCd/TrendSearchor-be`.
2. Railway tự phát hiện `Dockerfile` và build. (Lần đầu build ~3-5 phút.)
3. Trong project, bấm **+ New** → **Database** → **Add MySQL**. Railway tạo MySQL và sinh sẵn các biến: `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`.

### A2. Set biến môi trường cho service BE
Vào service BE → tab **Variables** → thêm:

| Biến | Giá trị |
|---|---|
| `MYSQL_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}` |
| `MYSQL_USER` | `${{MySQL.MYSQLUSER}}` |
| `MYSQL_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `JWT_SECRET` | chuỗi base64 ≥ 32 bytes (tạo mới, KHÔNG dùng của dev) |
| `OPENROUTER_API_KEY` | key OpenRouter thật |
| `OPENROUTER_MODEL` | `google/gemini-2.5-flash` |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | key Cloudinary thật (nếu dùng upload file) |
| `VNP_TMN_CODE` / `VNP_HASH_SECRET` | key VNPay (sandbox đã có default trong code) |
| `VNP_RETURN_URL` | `https://<BE-domain>.up.railway.app/api/payments/vnpay/return` |
| `VNP_FE_RETURN_URL` | `https://<FE-domain>.vercel.app/researcher/subscription` |
| `SEMANTIC_SCHOLAR_API_KEY` | (tùy chọn) key để search ổn định khi OpenAlex hết budget |

> `${{MySQL.XXX}}` là cú pháp Railway tham chiếu biến của service MySQL — gõ đúng như vậy.
> Flyway sẽ **tự chạy migration** tạo bảng khi BE khởi động lần đầu.

### A3. Public domain
Service BE → **Settings → Networking → Generate Domain**. Ghi lại domain (VD
`trendsearchor-be-production.up.railway.app`).
> Nếu domain KHÁC `trendsearchor-be-production...`, phải thêm nó vào CORS
> (`SecurityConfig.corsConfigurationSource`) và vào `VNP_RETURN_URL`.

---

## Phần B — Deploy FE lên Vercel

1. Vào https://vercel.com → **Add New → Project** → import `FeliCd/TrendSearchor-fe`.
2. Vercel tự nhận **Vite** (Build `npm run build`, Output `dist`). Không cần đổi.
3. **Environment Variables** → thêm:
   | Biến | Giá trị |
   |---|---|
   | `VITE_API_BASE_URL` | `https://<BE-domain>.up.railway.app` (domain Railway ở A3, KHÔNG có `/` cuối) |
   | `VITE_OPENROUTER_API_KEY` | (nếu FE còn gọi AI client-side) |
4. **Project name** đặt là `trend-searchor-fe` để domain khớp CORS (`trend-searchor-fe.vercel.app`).
   Nếu tên khác → thêm domain thật vào CORS của BE.
5. **Deploy**. Xong Vercel cho domain `https://<project>.vercel.app`.

---

## Phần C — Sau khi cả 2 đã lên

1. **Kiểm tra kết nối**: mở FE Vercel → đăng ký/đăng nhập. Nếu lỗi CORS → domain FE chưa khớp
   list trong `SecurityConfig`; nếu lỗi mạng → `VITE_API_BASE_URL` sai.
2. **VNPay**: vào Merchant Admin sandbox, cập nhật **Return URL / IPN URL** thành domain Railway
   (`https://<BE>/api/payments/vnpay/return` và `/ipn`). Localhost không nhận callback được.
3. **Admin mặc định**: `admin@mail.com` / `1` (đổi qua biến `admin.mail` / `admin.password` nếu cần).

---

## Lưu ý / bẫy thường gặp
- **Secrets**: KHÔNG commit key thật lên git. Set hết qua Variables của Railway/Vercel.
  (Riêng VNPay sandbox đã baked default trong code cho tiện demo — production thì override.)
- **server.port**: đã bind `${PORT:8080}` cho Railway. Đừng hardcode 8080 lại.
- **Java 21**: Dockerfile dùng `eclipse-temurin-21`. Đảm bảo `pom.xml` compile ở Java 21.
- **DB rỗng lúc đầu**: DB production mới toanh, chỉ có bảng (Flyway) + admin mặc định. Muốn có
  bài báo thì gọi API sync hoặc để user search (OpenAlex).
- **OpenAlex budget**: nhỏ, dễ 429. Nên thêm `SEMANTIC_SCHOLAR_API_KEY` làm nguồn dự phòng.
- **ddl-auto=validate**: nếu migration lệch schema, BE sẽ fail start — đọc log Railway để sửa.
