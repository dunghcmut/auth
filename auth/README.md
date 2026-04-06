# Spring Boot JWT Authentication Service

Authentication service hoàn chỉnh bằng Spring Boot, Spring Security, JPA/Hibernate, MySQL và JWT với kiến trúc tách lớp rõ ràng, ưu tiên dễ maintain và production-friendly.

Tóm tắt tính năng: [AUTH_FEATURES.md](/home/dungne/up/isCode/localJava/project/auth/AUTH_FEATURES.md)

## 1. Project Tree

```text
auth
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java/com/example/auth
│   │   │   ├── AuthApplication.java
│   │   │   ├── config
│   │   │   │   ├── JwtProperties.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller
│   │   │   │   └── AuthController.java
│   │   │   ├── dto
│   │   │   │   ├── request
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   │   └── RegisterRequest.java
│   │   │   │   └── response
│   │   │   │       ├── ApiResponse.java
│   │   │   │       ├── AuthResponse.java
│   │   │   │       ├── TokenResponse.java
│   │   │   │       └── UserProfileResponse.java
│   │   │   ├── entity
│   │   │   │   ├── Role.java
│   │   │   │   ├── User.java
│   │   │   │   └── UserStatus.java
│   │   │   ├── exception
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ...
│   │   │   ├── mapper
│   │   │   │   ├── AuthMapper.java
│   │   │   │   └── UserMapper.java
│   │   │   ├── repository
│   │   │   │   └── UserRepository.java
│   │   │   ├── security
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtToken.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── SecurityUserPrincipal.java
│   │   │   │   ├── TokenHashProvider.java
│   │   │   │   └── TokenType.java
│   │   │   ├── service
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── RefreshTokenService.java
│   │   │   │   └── impl
│   │   │   │       ├── AuthServiceImpl.java
│   │   │   │       └── RefreshTokenServiceImpl.java
│   │   │   └── validator
│   │   │       ├── StrongPassword.java
│   │   │       └── StrongPasswordValidator.java
│   │   └── resources
│   │       ├── application.yml
│   │       └── sql/mysql-schema.sql
│   └── test
│       ├── java/com/example/auth
│       │   ├── AuthApplicationTests.java
│       │   └── AuthIntegrationTest.java
│       └── resources/application.yml
└── mvnw
```

## 2. Kiến trúc và SOLID

- `controller` chỉ nhận request, validate DTO, trả response thống nhất.
- `service` chứa toàn bộ business logic cho register/login/logout/refresh/me.
- `repository` chỉ truy cập dữ liệu JPA.
- `security` cô lập JWT generation/validation, hash refresh token, `UserDetailsService`, filter và security entry point.
- `mapper` tách conversion entity <-> DTO để controller/service không tự map dữ liệu.
- `validator` chứa custom password policy thay vì nhồi regex vào service/controller.
- `exception` gom custom exception và global handler để API trả lỗi nhất quán.

Áp dụng SOLID ngắn gọn:

- `S`: mỗi lớp một trách nhiệm rõ ràng, ví dụ `JwtTokenProvider` chỉ xử lý JWT, `RefreshTokenServiceImpl` chỉ quản lý refresh token state.
- `O`: thêm role, thêm auth provider hoặc thay đổi response mapper không phải sửa controller.
- `L`: interface `AuthService`, `RefreshTokenService` có thể thay implementation mà không đổi nơi sử dụng.
- `I`: không ép controller/repository phụ thuộc logic dư thừa.
- `D`: service phụ thuộc abstraction (`AuthService`, `RefreshTokenService`) và bean injected qua constructor.

## 3. Authentication Flow

- `register`: tạo user mới, hash password bằng BCrypt, phát access token + refresh token.
- `login`: authenticate bằng Spring Security `AuthenticationManager`, sau đó phát token mới.
- `refresh`: verify refresh JWT, so khớp `refreshTokenHash` trong DB, rồi rotate sang refresh token mới.
- `logout`: xoá `refreshTokenHash` trong DB để refresh token đang giữ mất hiệu lực.
- `me`: dùng access token đã được filter xác thực để lấy profile hiện tại.

### Vì sao chọn logout + refresh token rotation như hiện tại

- Access token là stateless nên không blacklist trong DB để tránh tăng chi phí I/O mỗi request.
- Refresh token được rotate và chỉ giữ hash của token mới nhất trong DB, nên token cũ tự mất hiệu lực ngay sau refresh hoặc logout.
- Thiết kế này đơn giản, đủ production-friendly cho single-session flow. Nếu cần multi-device/session management, nên tách refresh token sang bảng riêng.

## 4. User Entity

`User` dùng chuẩn JPA/Hibernate với:

- `id`
- `name`
- `email` unique
- `password`
- `role`
- `status`
- `refreshTokenHash`
- `createdAt`
- `updatedAt`

`createdAt` và `updatedAt` được set qua `@PrePersist` và `@PreUpdate`.

## 5. API Endpoints

### `POST /api/auth/register`

Request:

```json
{
  "name": "Dung Nguyen",
  "email": "dung@example.com",
  "password": "Password123"
}
```

Response `201 Created`:

```json
{
  "success": true,
  "message": "User registered successfully.",
  "data": {
    "user": {
      "id": 1,
      "name": "Dung Nguyen",
      "email": "dung@example.com",
      "role": "USER",
      "status": "ACTIVE",
      "createdAt": "2026-04-05T08:10:00",
      "updatedAt": "2026-04-05T08:10:00"
    },
    "tokens": {
      "tokenType": "Bearer",
      "accessToken": "<jwt-access-token>",
      "accessTokenExpiresAt": "2026-04-05T08:25:00Z",
      "refreshToken": "<jwt-refresh-token>",
      "refreshTokenExpiresAt": "2026-04-12T08:10:00Z"
    }
  },
  "timestamp": "2026-04-05T08:10:00Z"
}
```

### `POST /api/auth/login`

Request:

```json
{
  "email": "dung@example.com",
  "password": "Password123"
}
```

Response `200 OK`: cùng format `AuthResponse`.

### `POST /api/auth/refresh`

Request:

```json
{
  "refreshToken": "<jwt-refresh-token>"
}
```

Response `200 OK`: trả access token mới và refresh token mới.

### `POST /api/auth/logout`

Headers:

```http
Authorization: Bearer <jwt-access-token>
```

Response `200 OK`:

```json
{
  "success": true,
  "message": "Logout successful.",
  "timestamp": "2026-04-05T08:20:00Z"
}
```

### `GET /api/auth/me`

Headers:

```http
Authorization: Bearer <jwt-access-token>
```

Response `200 OK`:

```json
{
  "success": true,
  "message": "Current user profile fetched successfully.",
  "data": {
    "id": 1,
    "name": "Dung Nguyen",
    "email": "dung@example.com",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": "2026-04-05T08:10:00",
    "updatedAt": "2026-04-05T08:10:00"
  },
  "timestamp": "2026-04-05T08:21:00Z"
}
```

### Validation/Error example

Response `400 Bad Request`:

```json
{
  "success": false,
  "message": "Validation failed.",
  "errors": {
    "password": "Password must contain at least one letter and one number."
  },
  "timestamp": "2026-04-05T08:09:00Z"
}
```

## 6. application.yml

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: update

app:
  jwt:
    secret: ${JWT_SECRET:}
    secret-format: ${JWT_SECRET_FORMAT:BASE64}
    allow-raw-secret: ${JWT_ALLOW_RAW_SECRET:false}
    issuer: ${JWT_ISSUER:auth-service}
    access-token-expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:15m}
    refresh-token-expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:7d}
```

Lưu ý:

- `JWT_SECRET_FORMAT` hỗ trợ `RAW`, `BASE64`, `BASE64URL`. Mặc định production là `BASE64`.
- `JWT_ALLOW_RAW_SECRET=false` theo mặc định để tránh dùng RAW ngoài local/dev.
- `JWT_SECRET` phải tương ứng với format đã chọn và có entropy tối thiểu 32 bytes sau khi decode.
- Với production thực tế, nên đổi `ddl-auto` sang `validate` và quản lý schema bằng Flyway/Liquibase.

## 7. MySQL Schema Mẫu

File mẫu: `src/main/resources/sql/mysql-schema.sql`

```sql
CREATE DATABASE IF NOT EXISTS auth_db;

USE auth_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    refresh_token_hash VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);
```

## 8. Hướng dẫn chạy project

### Yêu cầu

- Java 21+
- MySQL 8+
- Maven wrapper (`./mvnw`)

### Tạo JWT secret

Khuyến nghị cho production:

- Production: dùng `BASE64` hoặc `BASE64URL`, không dùng `RAW`.
- Local/dev: chỉ dùng `RAW` khi profile local bật hoặc khi chủ động set `JWT_ALLOW_RAW_SECRET=true`.

Ví dụ tạo secret Base64 bằng OpenSSL:

```bash
openssl rand -base64 64
```

### Export env

```bash
export DB_URL='jdbc:mysql://localhost:3306/auth_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USERNAME='root'
export DB_PASSWORD='your_password'
export JWT_SECRET='your_base64_secret_here'
export JWT_SECRET_FORMAT='BASE64'
export JWT_ALLOW_RAW_SECRET='false'
export JWT_ISSUER='auth-service'
export JWT_ACCESS_TOKEN_EXPIRATION='15m'
export JWT_REFRESH_TOKEN_EXPIRATION='7d'
```

Nếu chạy local với RAW secret:

```bash
export SPRING_PROFILES_ACTIVE='local'
export JWT_SECRET='your_local_raw_secret_with_more_than_32_bytes'
export JWT_SECRET_FORMAT='RAW'
export JWT_ALLOW_RAW_SECRET='true'
```

### Run

```bash
./mvnw spring-boot:run
```

### Test

```bash
./mvnw test
```

## 9. Curl Mẫu

### Register

```bash
curl --request POST 'http://localhost:8080/api/auth/register' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "name": "Dung Nguyen",
    "email": "dung@example.com",
    "password": "Password123"
  }'
```

### Login

```bash
curl --request POST 'http://localhost:8080/api/auth/login' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "email": "dung@example.com",
    "password": "Password123"
  }'
```

### Refresh

```bash
curl --request POST 'http://localhost:8080/api/auth/refresh' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "refreshToken": "<jwt-refresh-token>"
  }'
```

### Logout

```bash
curl --request POST 'http://localhost:8080/api/auth/logout' \
  --header 'Authorization: Bearer <jwt-access-token>'
```

### Me

```bash
curl --request GET 'http://localhost:8080/api/auth/me' \
  --header 'Authorization: Bearer <jwt-access-token>'
```

## 10. Curl Smoke Test Script

Script có sẵn:

- `scripts/curl-auth-smoke.sh`: chạy full flow `register -> login -> me -> refresh -> logout -> verify refresh revoked`
- `scripts/curl-auth-examples.sh`: in ra curl examples đơn lẻ

Chạy smoke test:

```bash
chmod +x scripts/curl-auth-smoke.sh
BASE_URL='http://localhost:8080' ./scripts/curl-auth-smoke.sh
```

Tuỳ chọn:

```bash
TEST_NAME='Dung Nguyen'
TEST_EMAIL='dung.test@example.com'
TEST_PASSWORD='Password123'
BASE_URL='http://localhost:8080'
./scripts/curl-auth-smoke.sh
```

Lưu ý:

- Script smoke test cần `jq`.
- `TEST_EMAIL` mặc định sẽ tự sinh theo timestamp để tránh trùng email.
