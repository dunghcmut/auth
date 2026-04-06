# Auth Module Features

Tài liệu này chỉ liệt kê tính năng của module authentication, không lặp lại toàn bộ hướng dẫn setup hay source tree.

## 1. Core Features

- Đăng ký tài khoản qua `POST /api/auth/register`
- Đăng nhập qua `POST /api/auth/login`
- Đăng xuất qua `POST /api/auth/logout`
- Làm mới token qua `POST /api/auth/refresh`
- Lấy thông tin người dùng hiện tại qua `GET /api/auth/me`

## 2. Authentication Model

- Dùng JWT stateless cho access token
- Tách riêng access token và refresh token
- Access token sống ngắn
- Refresh token sống dài hơn
- Hỗ trợ refresh token rotation
- Logout làm refresh token hiện tại mất hiệu lực

## 3. Security Features

- Hash password bằng `BCryptPasswordEncoder`
- Không trả password hoặc password hash trong response
- Lưu `refreshTokenHash` thay vì lưu plain refresh token
- Có `JwtAuthenticationFilter` để xác thực JWT trên mỗi request protected
- Có `CustomUserDetailsService` cho Spring Security
- Có `JwtAuthenticationEntryPoint` để trả lỗi unauthorized thống nhất
- Public route: `register`, `login`, `refresh`
- Protected route: `logout`, `me`

## 4. JWT Secret Policy

- Mặc định production ưu tiên `BASE64`
- Hỗ trợ `BASE64URL`
- Chỉ cho phép `RAW` khi bật rõ `allow-raw-secret=true`
- Fail fast nếu:
- thiếu `JWT_SECRET`
- secret sai format
- secret ngắn dưới 32 bytes

## 5. User Domain Features

- `User` entity chuẩn JPA/Hibernate
- `email` là unique
- Có `role` enum
- Có `status` enum
- Có audit fields `createdAt`, `updatedAt`
- Có `refreshTokenHash` để kiểm soát phiên refresh token

## 6. Validation Features

- Validate `name` không rỗng
- Validate `email` đúng format
- Validate password tối thiểu 8 ký tự
- Validate password có cả chữ và số
- Dùng Bean Validation và custom validator cho password policy

## 7. API Response Features

- Response JSON thống nhất qua `ApiResponse`
- Trả HTTP status code đúng semantics
- Có DTO riêng cho request và response
- Có global exception handler cho validation, unauthorized, conflict, not found và unexpected error

## 8. Architecture Features

- Tách layer rõ ràng: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `security`, `validator`, `exception`, `config`
- Business logic nằm ở `service`
- Controller không chứa logic nghiệp vụ
- Repository chỉ xử lý truy cập dữ liệu
- Mapper tách conversion entity và DTO
- Áp dụng SOLID theo hướng dễ mở rộng và dễ maintain

## 9. Database Features

- Dùng Spring Data JPA + Hibernate
- Dùng MySQL làm database chính
- Có schema mẫu cho bảng `users`
- Query tìm user theo email
- Query lấy user active theo email

## 10. Test Coverage

- Có context load test
- Có integration test cho flow auth chính
- Có unit test cho JWT signing key policy và validation
