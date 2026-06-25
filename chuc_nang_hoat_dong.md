# Tổng hợp các chức năng đã hoạt động ổn định

Dựa trên việc đọc và phân tích cấu trúc 2 project `job-frontend` (Angular 19) và `job-backend` (Spring Boot 3), hệ thống tuyển dụng này đã triển khai thành công một tập hợp phong phú các tính năng từ phía người tìm việc (Candidate), nhà tuyển dụng (Recruiter), và quản trị viên (Admin). 

Dưới đây là các chức năng đã được kết nối hoàn chỉnh từ Frontend tới Backend và chạy ổn định:

## 1. Dành cho Ứng viên (Candidate / User)
Hệ thống cho phép người tìm việc tạo hồ sơ, quản lý CV, ứng tuyển và sử dụng công nghệ AI để tìm việc:

*   **Xác thực và Tài khoản (Authentication & Profile):**
    *   Đăng ký tài khoản và đăng nhập (sử dụng JWT Token).
    *   Xác thực email đăng ký.
    *   Đăng nhập nhanh qua Google (OAuth2).
    *   Quên mật khẩu và khôi phục mật khẩu.
    *   Cập nhật thông tin cá nhân (Profile) và thay đổi mật khẩu.
*   **Quản lý Hồ sơ (Resume/CV Management):**
    *   Tải lên CV cá nhân.
    *   Xem danh sách các CV đã tải lên.
*   **Gợi ý việc làm bằng AI (AI Matching):**
    *   Người dùng có thể upload file CV của mình và sử dụng chức năng AI để tìm các job phù hợp dựa trên nội dung CV đó (Hệ thống tính toán độ phù hợp - Match Score).
*   **Quản lý Ứng tuyển (Job Application):**
    *   Ứng tuyển công việc bằng CV có sẵn hoặc tải CV mới lên.
    *   Xem lịch sử ứng tuyển (các công việc đã nộp).

## 2. Dành cho Nhà tuyển dụng (Recruiter / Hirer)
Các tính năng hỗ trợ nhà tuyển dụng quản lý thông tin công ty và quá trình tuyển dụng:

*   **Tài khoản & Hồ sơ Công ty:**
    *   Luồng đăng ký và đăng nhập dành riêng cho Nhà tuyển dụng.
    *   Quản lý thông tin hồ sơ của Công ty / Nhà tuyển dụng.
*   **Quản lý Công việc (Job Posting):**
    *   Tạo tin tuyển dụng mới.
    *   Chỉnh sửa, cập nhật và xóa tin tuyển dụng.
    *   Xem danh sách các tin đã đăng và thống kê số lượng tin.
*   **Quản lý Ứng viên (Applicant Tracking):**
    *   Xem danh sách hồ sơ ứng tuyển vào từng tin tuyển dụng *(Lưu ý: Chức năng phân tích ứng viên bằng AI dành cho Recruiter hiện chưa có)*.
*   **Quản lý Địa điểm:**
    *   Thêm, sửa, xóa các địa chỉ / cơ sở làm việc của công ty.

## 3. Chức năng Chung và Public (Guest)
Dành cho người dùng chưa đăng nhập tham khảo:

*   **Tìm kiếm & Duyệt Công việc:**
    *   Xem danh sách công việc mới nhất.
    *   Lọc và tìm kiếm công việc nâng cao (sử dụng JPA Specification ở Backend).
    *   Xem chi tiết một công việc.
*   **Phân loại (Categories):**
    *   Xem các danh mục nghề nghiệp hiện có.

## 4. Nội dung & Tương tác (Blog System)
Hệ thống tin tức và tương tác của người dùng:

*   **Bài viết:** Xem, tạo mới, chỉnh sửa và xóa bài viết (Blog).
*   **Tương tác:** Bình luận (Comment) và Thích (Like) các bài viết.

## 5. Quản trị viên (Admin)
Phần dành cho quản trị hệ thống:

*   **Dashboard:** Xem các thông số thống kê tổng quan của hệ thống.
*   **Quản lý Người dùng:** Xem và quản lý người tìm việc (Job Seekers) và Nhà tuyển dụng (Employers).
*   **Quản lý Công việc:** Quản lý các tin tuyển dụng trên hệ thống.
*   **Quản lý Giao dịch:** Quản lý lịch sử thanh toán / billing.

---

> [!TIP]
> **Điểm nổi bật về mặt kiến trúc hệ thống:**
> *   **Domain-Driven Design (DDD):** Code backend được chia tách rõ ràng thành các `Bounded Contexts` như *Identity, Recruitment, ApplicationProcess, Content, Admin*.
> *   **Xử lý Bất đồng bộ & Mở rộng:** Đã áp dụng `Redis` và `RabbitMQ` để tăng hiệu suất cho các tác vụ như gửi email, bộ nhớ tạm.
> *   **AI & Parsing:** Hệ thống có áp dụng LangChain4j và LLM (OpenAI) kết hợp cùng Apache Tika để bóc tách thông tin CV và đối chiếu với công việc phục vụ người tìm việc.
