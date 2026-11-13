# Bảng Thông Số Các GET API Có Thể Cache Bằng Redis (`job-backend`)

> Tài liệu này tổng hợp và phân loại toàn bộ **59 HTTP `GET` endpoint** hiện có trên 26 Controller thuộc 7 bounded context của `job-backend` (`recruitment`, `content`, `applicationProcess`, `identity`, `payment`, `admin`, `shared`), đồng thời xác định **ngưỡng đếm kích hoạt (`Threshold`)**, **số lượng request được phép cache tối đa (`Max Keys`)**, **ước tính dung lượng RAM Redis**, **thời gian tự xóa (`TTL`)** và **sự kiện xóa chủ động (`Invalidation`)**.

---

## 1. Tổng Quan Phân Loại 59 GET API

Qua rà soát toàn bộ mã nguồn Controller và cấu hình bảo mật tại [SecurityConfig.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/infrastructure/config/SecurityConfig.java) và [ApiConstants.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/domain/vo/ApiConstants.java), 59 GET API được chia làm **4 nhóm**:

| Nhóm | Phạm vi (`Scope`) | Số lượng API | Đặc điểm & Chiến lược áp dụng |
| :--- | :--- | :---: | :--- |
| **Nhóm A** | `PUBLIC` (dùng chung toàn hệ thống) | **11** | Không cần xác thực hoặc đọc công khai. Tần suất gọi rất cao, tỷ lệ Đọc/Ghi lớn. **Ưu tiên số 1 để cache.** |
| **Nhóm B** | `ROLE_ADMIN` (dùng chung giữa các Admin) | **16** | Yêu cầu quyền `ROLE_ADMIN` / `ADMIN`. Chứa nhiều query `COUNT(...)` thống kê và phân trang. Cache chung theo role với TTL ngắn (`60s–180s`). |
| **Nhóm C** | `USER:{userId}` (riêng từng người dùng) | **15** | Có `@CurrentUser` và/hoặc kiểm tra quyền sở hữu (Ownership). Chỉ có lợi khi **cùng một user** gọi lặp lại (reload trang, chuyển tab, hoặc query `pgvector` nặng). |
| **Nhóm D** | **CẤM CACHE (Blacklist)** | **17** | API `GET` nhưng làm thay đổi DB/Redis/gửi email, streaming `SseEmitter`, sinh URL ký sẵn thời hạn ngắn (presigned URL), hoặc kiểm tra tức thời đã tối ưu sẵn. |

> [!IMPORTANT]
> Ngoài 59 endpoint trên, dự án còn có các endpoint Actuator (`GET /actuator/health`, `GET /actuator/info` cấu hình trong [application.yml](file:///d:/web-project/job-backend/src/main/resources/application.yml#L110-L117)) và 1 endpoint đang comment (`GET /api/auth/status` trong [AuthController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/api/AuthController.java#L63-L70)). Tất cả đều không đưa vào cache HTTP Filter.
>
> Lưu ý thêm về đa ngôn ngữ (`Accept-Language`): [I18nConfig](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/config/I18nConfig.java) hỗ trợ 2 ngôn ngữ `vi` (mặc định) và `en`, làm thay đổi trường `message` trong `ApiResponse<T>`. Vì vậy, các `Max Keys` dưới đây đã nhân hệ số biến thể ngôn ngữ (`vi` / `en`) khi cần thiết.

---

## 2. Bảng 1 (Nhóm A): Các GET API Public & Master Data (Ưu Tiên Số 1)

Đây là nhóm mang lại hiệu quả giảm tải DB cao nhất vì 1 entry cache phục vụ cho hàng nghìn người dùng khác nhau.

| STT | Endpoint (`GET`) | Controller | Scope | Ngưỡng đếm kích hoạt (`Threshold / Window`) | Kích thước 1 Response (TB) | Giới hạn tối đa (`Max Keys`) | RAM Redis tối đa (`JSON / Gzip`) | `TTL` (Tự xóa) | Sự kiện xóa chủ động (`Invalidation Triggers`) |
| :---: | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| 1 | `/api/v1/categories` | `PublicCategoryController` | `PUBLIC` | **3 lần / 300s** | `~4.0 KB` | **4** (`vi`, `en`, dự phòng) | `~17 KB` / `~8 KB` | **3,600s** (60 phút) | `POST/PUT/DELETE /api/admin/categories/**` |
| 2 | `/api/home/init` | `HomeFeedController` | `PUBLIC` | **5 lần / 60s** | `~2.0 KB` | **4** (`vi`, `en`) | `~9 KB` / `~5 KB` | **600s** (10 phút) | Tạo/sửa/xóa/duyệt Job (`POST/PUT/DELETE /api/hirer/jobs/**`, `/api/admin/jobs/**`) *(đang dùng `CategoryCacheService`)* |
| 3 | `/api/addresses/address-count` | `AddressController` | `PUBLIC` | **5 lần / 60s** | `~1.5 KB` | **4** (`vi`, `en`) | `~7 KB` / `~4 KB` | **600s** (10 phút) | Tạo/sửa/xóa/duyệt Job hoặc sửa city Address *(đang dùng `CategoryCacheService`)* |
| 4 | `/api/jobs/newest` (`?page&size`) | `PublicJobController` | `PUBLIC` | **5 lần / 60s** | `~4.0 KB` | **30** (`page=0..9`, `size=5/10/20`) | `~128 KB` / `~40 KB` | **180s** (3 phút) | Tạo/sửa/xóa/duyệt Job; khóa Employer (`PATCH /api/admin/employers/{id}/status`) |
| 5 | `/api/jobs/count` | `PublicJobController` | `PUBLIC` | **5 lần / 60s** | `~0.15 KB` | **2** (`vi`, `en`) | `~0.8 KB` | **180s** (3 phút) | Tạo/xóa/duyệt trạng thái Job (`ACTIVE` thay đổi) |
| 6 | `/api/jobs/{id}` | `PublicJobController` | `PUBLIC` | **5 lần / 300s** | `~10.0 KB` | **500** (Top 500 Job hot) | `~5.1 MB` / `~1.3 MB` | **600s** (10 phút) | Xóa riêng `{id}` khi `PUT/DELETE /api/hirer/jobs/{id}`, `PATCH /api/admin/jobs/{id}/status`, `PUT /api/addresses/{id}` |
| 7 | `/api/jobs/{id}/exists` | `PublicJobController` | `PUBLIC` | **5 lần / 120s** | `~0.15 KB` | **500** | `~200 KB` | **300s** (5 phút) | Xóa riêng `{id}` khi Job `{id}` bị xóa hoặc đổi trạng thái |
| 8 | `/api/blogs` (`?pageIndex&pageSize&title&dateOrder`) | `BlogController` | `PUBLIC` | **5 lần / 60s** | `~6.0 KB` | **50** (Trang đầu & từ khóa hot) | `~312 KB` / `~90 KB` | **300s** (5 phút) | `POST/PUT/DELETE /api/blogs/**`, `POST /api/blogs/likes/**` |
| 9 | `/api/blogs/{blogId}` | `BlogController` | `PUBLIC` | **5 lần / 300s** | `~18.0 KB` | **200** (Top 200 bài viết hot) | `~3.6 MB` / `~900 KB` | **900s** (15 phút) | Xóa riêng `{blogId}` khi `PUT/DELETE /api/blogs/{blogId}` hoặc `likes` |
| 10 | `/api/blogs/comments/{pageIndex}/{pageSize}` | `BlogController` | `PUBLIC` | **5 lần / 60s** | `~3.5 KB` | **30** | `~112 KB` / `~40 KB` | **120s** (2 phút) | `POST /api/blogs/comments` |
| 11 | `/api/language` | `LanguageController` | `PUBLIC` | **5 lần / 300s** | `~0.3 KB` | **2** (`vi`, `en`) | `~1 KB` | **86,400s** (24 giờ) | Dữ liệu tĩnh không thay đổi lúc runtime |

**Tổng hạn mức Nhóm A:** Tối đa **1,326 keys** — RAM tối đa **~9.58 MB** (JSON thuần) hoặc **~2.55 MB** (khi nén Gzip payload $> 2\text{ KB}$).

> [!NOTE]
> Tại [PublicCategoryController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/api/PublicCategoryController.java#L23), `GET /api/v1/categories` là master data ít thay đổi nhất nhưng hiện tại lại đang query trực tiếp `categoryRepository.findByRecordStatus(EntityStatus.ACTIVE)` ở mỗi request (mặc dù dự án có [CategoryCacheService.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/infrastructure/cache/CategoryCacheService.java)). Ngưỡng kích hoạt ở đây nên đặt thấp (`3 lần / 300s`) để cache sớm.

---

## 3. Bảng 2 (Nhóm B): Các GET API Quản Trị (`ROLE_ADMIN` Shared Scope)

Các API thuộc [AdminDashboardController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/api/AdminDashboardController.java), [AdminJobsController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/api/AdminJobsController.java), [AdminEmployersController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/api/AdminEmployersController.java), [AdminJobSeekersController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/api/AdminJobSeekersController.java), [AdminBillingController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/api/AdminBillingController.java) và [UserCrudController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/api/UserCrudController.java) dùng chung dữ liệu giữa các tài khoản Admin. Vì trang quản trị cần độ tươi dữ liệu cao, `TTL` được giữ ngắn (`60s–180s`).

| STT | Endpoint (`GET`) | Controller | Scope | Ngưỡng đếm (`Threshold / Window`) | Kích thước 1 Response (TB) | Giới hạn tối đa (`Max Keys`) | RAM Redis tối đa | `TTL` (Tự xóa) | Sự kiện xóa chủ động (`Invalidation Triggers`) |
| :---: | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| 12 | `/api/admin/dashboard/summary` | `AdminDashboardController` | `ROLE:ADMIN` | **5 lần / 60s** | `~0.3 KB` | **2** | `~1.1 KB` | **60s** (1 phút) | Đăng ký Hirer, tạo Job Seeker, tạo/duyệt/xóa Job |
| 13 | `/api/admin/dashboard/revenue-trend` (`?range`) | `AdminDashboardController` | `ROLE:ADMIN` | **5 lần / 60s** | `~0.3 KB` | **6** (`7d`, `30d`, `90d` $\times$ `vi/en`) | `~3.3 KB` | **300s** (5 phút) | Tạo thanh toán mới (`POST /api/payments`) *(hiện đang mock)* |
| 14 | `/api/admin/dashboard/job-distribution` | `AdminDashboardController` | `ROLE:ADMIN` | **5 lần / 60s** | `~0.25 KB` | **2** | `~1.0 KB` | **60s** (1 phút) | Mọi thao tác tạo/sửa/xóa/duyệt Job |
| 15 | `/api/admin/jobs/pending` (`?page&pageSize`) | `AdminDashboardController` | `ROLE:ADMIN` | **5 lần / 60s** | `~2.5 KB` | **10** | `~27 KB` | **60s** (1 phút) | `POST /api/admin/jobs`, `PATCH /api/admin/jobs/{id}/status`, `POST /api/admin/jobs/bulk-action` |
| 16 | `/api/admin/jobs/metrics` | `AdminJobsController` | `ROLE:ADMIN` | **5 lần / 60s** | `~0.25 KB` | **2** | `~1.0 KB` | **60s** (1 phút) | Tạo/xóa/duyệt Job, nộp đơn ứng tuyển |
| 17 | `/api/admin/jobs` (`?page&pageSize&search&category&status`) | `AdminJobsController` | `ROLE:ADMIN` | **5 lần / 60s** | `~3.5 KB` | **30** | `~112 KB` | **60s** (1 phút) | Thay đổi Job hoặc ứng tuyển mới |
| 18 | `/api/admin/employers/metrics` | `AdminEmployersController` | `ROLE:ADMIN` | **5 lần / 60s** | `~0.25 KB` | **2** | `~1.0 KB` | **120s** (2 phút) | `POST /api/auth/hirer/register`, `PATCH /api/admin/employers/{id}/status` |
| 19 | `/api/admin/employers` (`?page&pageSize&search&kycStatus&status`) | `AdminEmployersController` | `ROLE:ADMIN` | **5 lần / 60s** | `~3.0 KB` | **30** | `~97 KB` | **90s** | Đăng ký Hirer, duyệt trạng thái Employer, tạo/xóa Job |
| 20 | `/api/admin/employers/{id}` | `AdminEmployersController` | `ROLE:ADMIN` | **5 lần / 120s** | `~0.45 KB` | **50** | `~35 KB` | **180s** (3 phút) | `PATCH /api/admin/employers/{id}/status`, cập nhật user |
| 21 | `/api/admin/job-seekers/metrics` | `AdminJobSeekersController` | `ROLE:ADMIN` | **5 lần / 60s** | `~0.25 KB` | **2** | `~1.0 KB` | **120s** (2 phút) | `POST /api/admin/job-seekers`, đăng ký User |
| 22 | `/api/admin/job-seekers` (`?page&pageSize&search&resumeStatus`) | `AdminJobSeekersController` | `ROLE:ADMIN` | **5 lần / 60s** | `~3.0 KB` | **30** | `~97 KB` | **90s** | `POST /api/admin/job-seekers` |
| 23 | `/api/admin/job-seekers/region-distribution` | `AdminJobSeekersController` | `ROLE:ADMIN` | **5 lần / 120s** | `~0.25 KB` | **2** | `~1.0 KB` | **300s** (5 phút) | Thêm/sửa Job Seeker *(hiện đang mock)* |
| 24 | `/api/admin/billing/tiers` | `AdminBillingController` | `ROLE:ADMIN` | **5 lần / 300s** | `~0.6 KB` | **2** | `~1.7 KB` | **600s** (10 phút) | `PATCH /api/admin/billing/tiers/{id}` |
| 25 | `/api/admin/billing/summary` | `AdminBillingController` | `ROLE:ADMIN` | **5 lần / 60s** | `~0.2 KB` | **2** | `~0.9 KB` | **120s** (2 phút) | Thanh toán mới *(hiện đang mock)* |
| 26 | `/api/admin/billing/transactions` (`?page&pageSize&status`) | `AdminBillingController` | `ROLE:ADMIN` | **5 lần / 60s** | `~4.0 KB` | **20** | `~85 KB` | **60s** (1 phút) | Thanh toán mới *(hiện đang mock)* |
| 27 | `/api/admin/users/page` (`?page&size`) | `UserCrudController` | `ROLE:ADMIN` | **5 lần / 60s** | `~4.0 KB` | **20** | `~85 KB` | **60s** (1 phút) | CRUD User (`POST/PUT/DELETE /api/admin/users/**`), đăng ký/kích hoạt |

**Tổng hạn mức Nhóm B:** Tối đa **212 keys** — RAM tối đa **~0.55 MB** (rất nhẹ).

---

## 4. Bảng 3 (Nhóm C): Các GET API Cá Nhân Hóa Theo User (`USER:{userId}` Scope)

Nhóm này chứa dữ liệu riêng của từng người dùng (Candidate, Hirer, hoặc Admin hiện tại). Nếu chỉ dùng 1 key chung theo URL sẽ gây **lộ dữ liệu giữa các user**, nên khóa Redis bắt buộc gắn phạm vi `user:{userId}`. Với ngưỡng đếm `5 lần` (riêng query AI `pgvector` rất nặng đề xuất `3 lần`), cache chỉ kích hoạt cho các user đang thao tác lặp lại nhiều lần trong phiên (F5 liên tục, chuyển tab qua lại).

| STT | Endpoint (`GET`) | Controller | Scope | Ngưỡng đếm (`Threshold / Window`) | Kích thước 1 Response (TB) | Giới hạn tối đa (`Max Keys`) | RAM Redis tối đa | `TTL` (Tự xóa) | Sự kiện xóa chủ động (`Invalidation Triggers`) |
| :---: | :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| 28 | `/api/user/jobs/match/{cvId}` | `UserApplicationController` | `USER:{id}` | **3 lần / 120s** *(query `pgvector` nặng)* | `~5.0 KB` | **300** | `~1.57 MB` | **600s** (10 phút) | Phân tích xong CV `{cvId}`, xóa CV, hoặc có JD mới được vectorize |
| 29 | `/api/hirer/jobs/posted` (`?page&size`) | `HirerJobController` | `USER:{id}` | **5 lần / 60s** | `~12.0 KB` | **150** | `~1.84 MB` | **120s** (2 phút) | Hirer tạo/sửa/xóa/analyze Job, Admin duyệt Job, hoặc có ứng viên nộp đơn |
| 30 | `/api/hirer/jobs/posted/count` | `HirerJobController` | `USER:{id}` | **5 lần / 60s** | `~0.15 KB` | **150** | `~60 KB` | **120s** (2 phút) | Hirer tạo/xóa Job, Admin đổi trạng thái Job |
| 31 | `/api/hirer/applications/jobs/{jobId}/candidates/{pageIndex}/{pageSize}` | `HirerApplicationController` | `USER:{id}` | **5 lần / 60s** | `~2.5 KB` | **100** | `~275 KB` | **60s** (1 phút) | Ứng viên nộp hồ sơ vào `jobId` |
| 32 | `/api/user/applied` (`?page&size`) | `UserApplicationController` | `USER:{id}` | **5 lần / 60s** | `~3.5 KB` | **150** | `~562 KB` | **120s** (2 phút) | User nộp đơn ứng tuyển (`submit-existing`, `submit-upload`) |
| 33 | `/api/user/applications/{jobId}/status` | `UserApplicationController` | `USER:{id}` | **5 lần / 60s** | `~0.15 KB` | **300** | `~120 KB` | **120s** (2 phút) | User nộp đơn vào `jobId` |
| 34 | `/api/user/resumes` | `UserResumeController` | `USER:{id}` | **5 lần / 60s** | `~1.2 KB` | **150** | `~217 KB` | **120s** (2 phút) | Upload xong CV, xóa CV, bắt đầu/hoàn tất phân tích AI CV |
| 35 | `/api/user/resumes/analyzed` | `UserResumeController` | `USER:{id}` | **5 lần / 60s** | `~1.0 KB` | **150** | `~187 KB` | **120s** (2 phút) | Phân tích AI CV hoàn tất (`READY`) hoặc xóa CV |
| 36 | `/api/user/resumes/{id}` | `UserResumeController` | `USER:{id}` | **5 lần / 60s** | `~0.3 KB` | **150** | `~82 KB` | **120s** (2 phút) | Xóa CV `{id}`, bấm `analyze`, hoặc worker phân tích xong |
| 37 | `/api/blogs/me` (`?pageIndex&pageSize&title&dateOrder`) | `BlogController` | `USER:{id}` | **5 lần / 60s** | `~5.0 KB` | **100** | `~525 KB` | **180s** (3 phút) | `POST/PUT/DELETE /api/blogs/**`, like/unlike |
| 38 | `/api/addresses` | `AddressController` | `USER:{id}` | **5 lần / 60s** | `~0.8 KB` | **100** | `~105 KB` | **300s** (5 phút) | `POST/PUT/DELETE /api/addresses/**` |
| 39 | `/api/payments/me` | `PaymentController` | `USER:{id}` | **5 lần / 60s** | `~2.0 KB` | **100** | `~225 KB` | **180s** (3 phút) | `POST /api/payments` thành công của user đó |
| 40 | `/api/payments/{id}` | `PaymentController` | `USER:{id}` | **5 lần / 120s** | `~0.4 KB` | **200** | `~130 KB` | **600s** (10 phút) | Dữ liệu Payment sau khi tạo là bất biến (`COMPLETED`) |
| 41 | `/api/account/profile` | `AccountController` | `USER:{id}` | **5 lần / 60s** | `~0.35 KB` | **200** | `~120 KB` | **300s** (5 phút) | `PUT /api/account/profile`, `PUT /api/admin/users/{id}` |
| 42 | `/api/admin/users` | `UserCrudController` | `USER:{id}` | **5 lần / 60s** | `~0.45 KB` | **20** | `~14 KB` | **300s** (5 phút) | `PUT /api/admin/users/{id}`, `PUT /api/account/profile` |

**Tổng hạn mức Nhóm C (15 APIs):** Tối đa **2,320 keys** — RAM tối đa **~6.03 MB** (JSON thuần) hoặc **~2.1 MB** (nén Gzip payload $> 2\text{ KB}$).

> [!WARNING]
> **Cần lưu ý đặc biệt cho `GET /api/user/resumes`:** Khi người dùng chưa có CV nào, `ResumeServiceImpl.getListResumeOfUser` hiện ném `ResourceNotFoundException` (`404 Not Found`). Bộ lọc Cache chỉ được phép lưu các response có HTTP status **`2xx` (chủ yếu `200 OK`)** để tránh cache nhầm trạng thái lỗi tạm thời.

---

## 5. Bảng 4 (Nhóm D): Danh Sách 17 GET API Tuyệt Đối Không Cache (Blacklist)

| STT | Endpoint (`GET`) | Controller | Lý do kỹ thuật không được phép cache |
| :---: | :--- | :--- | :--- |
| 1 | `/api/auth/activation-links/{email}` | `AuthController` | **Có side-effect:** Tạo JWT kích hoạt mới và gửi email qua RabbitMQ. |
| 2 | `/api/auth/activate/{token}` | `AuthController` | **Thay đổi trạng thái DB:** Kích hoạt `user.setActive(true)`. |
| 3 | `/api/auth/refreshToken` | `AuthController` | **Thay đổi token & Cookie:** Xoay vòng refresh token JTI trong Redis và trả header `Set-Cookie`. |
| 4 | `/api/auth/logout` | `AuthController` | **Thay đổi session/token:** Xóa refresh token family trong Redis và xóa Cookie. |
| 5 | `/api/auth/password/code/{email}` | `AuthController` | **Có side-effect:** Tăng bộ đếm chống spam email, sinh OTP trong Redis và gửi email. |
| 6 | `/api/account/oauth2` | `AccountController` | **Có side-effect:** Nếu tài khoản OAuth2 chưa có mật khẩu, hệ thống tạo token vào Redis và gửi email đặt mật khẩu qua RabbitMQ. |
| 7 | `/api/sse/connect` | `SseNotificationController` | **Kết nối Streaming (`text/event-stream`):** Trả về `SseEmitter` bất đồng bộ dài hạn, không thể buffer hay replay. |
| 8 | `/api/user/resumes/{id}/view` | `UserResumeController` | **Sinh Presigned URL có chữ ký hết hạn (15–30 phút):** Cache response lâu hơn hạn URL sẽ làm client nhận link hỏng `403`. |
| 9 | `/api/user/resumes/{id}/download` | `UserResumeController` | **Sinh Presigned Download URL có hạn:** Tương tự mục 8. |
| 10 | `/api/hirer/resumes/{id}/view` | `HirerResumeController` | **Sinh Presigned URL có hạn:** Tương tự mục 8, kèm kiểm tra quyền sở hữu ứng tuyển. |
| 11 | `/api/hirer/resumes/{id}/download` | `HirerResumeController` | **Sinh Presigned Download URL có hạn:** Tương tự mục 8. |
| 12 | `/api/auth/password/check-random/{random}` | `AuthController` | Đã là thao tác kiểm tra 1 key TTL 5 phút trực tiếp trong Redis (`verifyService.containsKey`), cache thêm là dư thừa. |
| 13 | `/api/account/roles/hirer` | `HirerAccountController` | Chỉ kiểm tra `currentUser != null` trong RAM sau `JwtFilter`, không tốn query riêng; cache sẽ làm sai khi vừa đổi/thu hồi role. |
| 14 | `/api/account/roles/user` | `UserAccountController` | Tương tự mục 13. |
| 15 | `/api/auth/google/url` | `AuthController` | Chỉ ghép chuỗi từ host hiện tại (`ServletUriComponentsBuilder`), không chạm DB/Redis; không nên cache theo domain khác nhau. |
| 16 | `/api/home` | `HomeFeedController` | Trả về hằng số `"Hello world"` (11 bytes) trong RAM, tốc độ nhanh hơn đọc Redis qua mạng. |
| 17 | `/api/admin/employers/export` | `AdminEmployersController` | Endpoint xuất file (hiện trả URL tải CSV), nếu sau này sinh file động hoặc URL có hạn thì không phù hợp cache response. |

---

## 6. Tài Liệu Tiếp Theo

- Xem chi tiết công thức tính RAM Redis, cơ chế đếm `5` lần, thuật toán giới hạn `Max Keys` bằng `ZSET` và chiến lược `TTL` tại [02-tinh-toan-dung-luong-va-gioi-han-redis.md](file:///d:/web-project/docs/cache/02-tinh-toan-dung-luong-va-gioi-han-redis.md).
- Xem phân tích vướng mắc `Authenticate / Authorize` và danh sách 5 giải pháp kiến trúc tại [03-danh-sach-giai-phap-kien-truc-va-filter.md](file:///d:/web-project/docs/cache/03-danh-sach-giai-phap-kien-truc-va-filter.md).
