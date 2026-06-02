# TrendSearchor — Backend

> Spring Boot 3 + MySQL 8 + Flyway | Java 21

## Yêu cầu

- [Java 21](https://adoptium.net/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (đang chạy)

## Setup & Chạy local

### 1. Khởi tạo MySQL bằng Docker

```bash
docker run -d --name trendsearchor_mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=trendsearchor mysql:8
```

### 2. Tạo file `.env`

```bash
copy .env.example .env
```

Mở `.env`, sửa lại:

```properties
MYSQL_URL=jdbc:mysql://localhost:3306/trendsearchor
MYSQL_USER=root
MYSQL_PASSWORD=root123
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=<tạo bằng lệnh bên dưới>
OPENALEX_MAILTO=your_email@example.com
```

Tạo JWT secret (PowerShell):

```powershell
$b = New-Object byte[] 32; (New-Object System.Security.Cryptography.RNGCryptoServiceProvider).GetBytes($b); [Convert]::ToBase64String($b)
```

### 3. Chạy ứng dụng

```bash
mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev"
```

> Flyway sẽ tự động migrate DB khi khởi động.

### 4. Truy cập

| Endpoint | URL |
|----------|-----|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Admin mặc định | `admin` / `admin123` |

## Lệnh Docker hữu ích

```bash
docker stop trendsearchor_mysql    # Dừng
docker start trendsearchor_mysql   # Chạy lại
docker rm -f trendsearchor_mysql   # Xóa (mất dữ liệu)
```
