# Đánh giá hệ thống & các điểm cần cải thiện

> Phạm vi: `job-backend` (Spring Boot 3.3, Java 17) và `job-frontend` (Angular 20 SSR), nhánh `thanh` tại commit `365d5fd`.
> Ngày đánh giá: 2026-11-08. Phương pháp: đọc mã nguồn (không chạy ứng dụng). Các mục ghi **[Đã xác minh]** đã được dò theo luồng code; các mục còn lại là đánh giá thiết kế.

---

## 0. Tóm tắt

Hệ thống đã có nền khá tốt: chia bounded context rõ (`identity`, `recruitment`, `applicationProcess`, `content`, `admin`), refresh token xoay vòng theo "family", publish RabbitMQ sau commit (`@TransactionalEventListener(AFTER_COMMIT)`), upload CV qua presigned URL, frontend dùng standalone component + lazy route + signal.

Tuy vậy có **một số lỗi logic/phân quyền nghiêm trọng** cần sửa trước mọi tối ưu khác:

| # | Mức | Vấn đề | Vị trí |
|---|-----|--------|--------|
| 1 | 🔴 Nghiêm trọng | Mọi user đã đăng nhập có thể CRUD **toàn bộ user** (kể cả tự nâng role lên ADMIN) | `UserCrudController` (`/api/users`) |
| 2 | 🔴 Nghiêm trọng | Mọi user đã đăng nhập có thể tạo/sửa/xóa **danh mục** | `AdminCategoryController` |
| 3 | 🔴 Nghiêm trọng | HIRER bất kỳ xem được **CV và danh sách ứng viên** của job không thuộc mình (IDOR) | `HirerResumeController`, `HirerApplicationController` |
| 4 | 🔴 Nghiêm trọng | Stored XSS: mô tả job (HTML) render bằng `bypassSecurityTrustHtml`, backend không sanitize | `job-single.component.html`, `safe-html.pipe.ts` |
| 5 | 🔴 Nghiêm trọng | Link kích hoạt tài khoản bị hỏng từ commit `ed99fd6`; trường `active`/`enabled`/`accountLocked` **không được kiểm tra khi login** | `AuthServiceImpl#createLink`, `#loginByRole` |
| 6 | 🟠 Cao | Rate limit luôn dùng hạn mức "public" và có thể bypass bằng header giả `X-Forwarded-For`/`CF-Connecting-IP` | `RateLimitFilter`, `IpUtils` |
| 7 | 🟠 Cao | AI matching trả về job đã xóa/hết hạn; endpoint `/api/jobs/match/{cvId}` public, không kiểm tra chủ CV | `JobRepository#matchJobs` |
| 8 | 🟠 Cao | Prod không cấu hình retry/DLQ RabbitMQ → message lỗi có thể requeue vô hạn (tốn tiền gọi AI) | `application-prod.yml`, `RabbitMQConfig` |
| 9 | 🟡 Trung bình | Frontend SSR không bật hydration, deploy Vercel dạng SPA → SSR không đem lại lợi ích | `app.config.ts`, `vercel.json` |
| 10 | 🟡 Trung bình | Không có migration DB (Flyway/Liquibase), thiếu index cho bảng `job`, `job_application` | `application*.yml`, entity |

---

## 1. Logic & phân quyền (ưu tiên cao nhất)

### 1.1 🔴 `/api/users` – ai đăng nhập cũng quản trị được user  **[Đã xác minh]**
- [UserCrudController.java](../job-backend/src/main/java/com/nlu/admin/api/UserCrudController.java) map `/api/users`, không có `@PreAuthorize`, và path này **không nằm trong** `ADMIN_ENDPOINTS` ([ApiConstants.java](../job-backend/src/main/java/com/nlu/identity/domain/vo/ApiConstants.java)) → rơi vào `anyRequest().authenticated()`.
- `UserCrudServiceImpl#applyDtoToUser` gán thẳng `role`, `enabled`, `accountLocked` từ request.
- **Kịch bản**: user thường gọi `PUT /api/users/{idCuaMinh}` với `role=ROLE_ADMIN` → thành admin; hoặc `DELETE /api/users/{id}` để xóa người khác; `GET /api/users/page` để dump toàn bộ email/SĐT.
- **Đề xuất**:
  - Chuyển controller sang `/api/admin/users`, thêm vào `ADMIN_ENDPOINTS`.
  - Bật `@EnableMethodSecurity` và đặt `@PreAuthorize("hasRole('ADMIN')")` ở mức class cho mọi controller admin (phòng thủ 2 lớp, không phụ thuộc danh sách path).
  - Đổi quy tắc cuối cùng thành `anyRequest().denyAll()` để endpoint mới quên khai báo sẽ bị chặn thay vì mở.

### 1.2 🔴 `/api/admin/categories` không được bảo vệ  **[Đã xác minh]**
- `ADMIN_ENDPOINTS` chỉ liệt kê `dashboard`, `employers`, `job-seekers`, `jobs`, `billing` → [AdminCategoryController.java](../job-backend/src/main/java/com/nlu/admin/api/AdminCategoryController.java) chỉ cần đăng nhập.
- Ngoài ra pattern `"/api/*/categories/**"` trong `PUBLIC_GET_ENDPOINTS` làm `GET /api/admin/categories/**` thành public.
- **Đề xuất**: dùng một pattern duy nhất `/api/admin/**` cho admin (trừ `/api/auth/admin/**`), đổi pattern public thành `/api/v1/categories/**` cụ thể. Viết test bảo mật dạng bảng: *mỗi endpoint × mỗi role → mã HTTP mong đợi*.

### 1.3 🔴 IDOR phía nhà tuyển dụng  **[Đã xác minh]**
- [HirerResumeController.java](../job-backend/src/main/java/com/nlu/applicationProcess/api/HirerResumeController.java) → `ResumeServiceImpl#getResumeViewUrlForHirer` ([dòng 176](../job-backend/src/main/java/com/nlu/applicationProcess/application/impl/ResumeServiceImpl.java#L176)) chỉ `findResumeById(id)` rồi cấp presigned URL. HIRER nào cũng tải được **mọi CV** bằng cách duyệt id.
- [HirerApplicationController.java](../job-backend/src/main/java/com/nlu/applicationProcess/api/HirerApplicationController.java) trả danh sách ứng viên của **bất kỳ** `jobId`, không kiểm tra `job.isOwnedBy(recruitment)`.
- **Đề xuất**: chỉ cho phép khi tồn tại `JobApplication` nối `resume → job → recruitment` của hirer hiện tại (1 query `EXISTS`). Thêm test regression cho cả hai endpoint.

### 1.4 🔴 Stored XSS ở trang chi tiết job  **[Đã xác minh]**
- [job-single.component.html](../job-frontend/src/app/features/jobs/pages/job-single/job-single.component.html#L179) dùng `[innerHTML]="jobDetail.description | safeHtml"`, mà [safe-html.pipe.ts](../job-frontend/src/app/shared/pipes/safe-html.pipe.ts) gọi `bypassSecurityTrustHtml` → **tắt** sanitizer của Angular. Backend lưu HTML thô (không có Jsoup `Safelist`).
- Một HIRER có thể chèn `<img src=x onerror=...>` vào mô tả job → chạy JS trên trình duyệt ứng viên, đọc access token trong bộ nhớ.
- CSP hiện tại có `script-src 'unsafe-inline'` ([server.ts](../job-frontend/src/server.ts), [index.html](../job-frontend/src/index.html)) nên không chặn được.
- **Đề xuất**:
  - Backend: `Jsoup.clean(html, Safelist.relaxed())` khi tạo/cập nhật job (jsoup đã có trong `pom.xml`).
  - Frontend: bỏ `safeHtml`, dùng `[innerHTML]` trực tiếp (Angular tự sanitize) hoặc sanitize bằng DOMPurify trước khi bypass.
  - Bỏ `'unsafe-inline'` và `cdn.tailwindcss.com` khỏi `script-src` (Tailwind đã build qua PostCSS).

### 1.5 🔴 Luồng kích hoạt tài khoản bị hỏng & trạng thái tài khoản không được kiểm tra  **[Đã xác minh]**
- `AuthServiceImpl#createLink` ([dòng 406](../job-backend/src/main/java/com/nlu/identity/application/impl/AuthServiceImpl.java#L406)) gọi `jwtService.generateToken(email + "|activate")`. Từ commit `ed99fd6`, overload `generateToken(String)` lại `loadUserByUsername("email|activate")` → `UsernameNotFoundException` → **đăng ký thất bại/rollback** (hoặc gửi lại link thất bại).
- Kể cả khi tạo được token, `activeAccount` so `isTokenValid(token, user)` giữa subject `"email|activate"` và `"email"` → luôn sai.
- `loginByRole` ([dòng 173](../job-backend/src/main/java/com/nlu/identity/application/impl/AuthServiceImpl.java#L173)) tự so `encoder.matches` mà **không kiểm tra** `active`, `enabled`, `accountLocked`. `JwtFilter` cũng không kiểm tra. Hệ quả: admin khóa/xóa mềm tài khoản nhưng access/refresh token cũ vẫn dùng được; email chưa kích hoạt vẫn đăng nhập được.
- **Đề xuất**:
  - Tạo token kích hoạt riêng: claim `purpose=activate`, TTL 24h, không load user; hoặc dùng token ngẫu nhiên lưu Redis (giống luồng quên mật khẩu).
  - Kiểm tra `isEnabled() && isAccountNonLocked() && active` khi login, khi refresh, và trong `JwtFilter` (hoặc dùng `AuthenticationManager` + `AccountStatusUserDetailsChecker`).
  - Khi khóa/xóa user hoặc đổi/reset mật khẩu → thu hồi mọi refresh-token family của user (lưu `auth:user-families:{userId}` dạng Set).

### 1.6 🟠 Token: refresh token dùng được như access token
- Access và refresh token ký chung một khóa, cùng `subject=email`; `JwtFilter` chỉ kiểm tra chữ ký + hạn → refresh token (7 ngày) dùng được làm `Bearer`.
- **Đề xuất**: thêm claim `typ` (`access`/`refresh`) và kiểm tra trong `JwtFilter`; tốt hơn nữa là dùng 2 khóa ký khác nhau.

### 1.7 🟠 Xoay vòng refresh token không nguyên tử
- `refreshToken()` kiểm tra `isValid` rồi mới `reGenerateRefreshToken` (2 bước). Hai tab cùng refresh → cả hai hợp lệ, JTI cuối trong Redis có thể không khớp với cookie cuối cùng trình duyệt nhận → lần refresh sau bị đá ra ngoài. Frontend lại gọi refresh **mỗi lần tải trang** (`provideAppInitializer`), nên mở nhiều tab dễ gặp.
- Khi phát hiện tái sử dụng token (JTI lệch), code chỉ log warning, không thu hồi family → mất ý nghĩa "reuse detection".
- **Đề xuất**: dùng Lua script/`WATCH` để compare-and-set JTI; cho phép "grace window" ~10–30 giây với JTI cũ; khi phát hiện reuse ngoài grace window → xóa family.

### 1.8 🟠 Luồng ứng tuyển
[JobApplicationServiceImpl.java](../job-backend/src/main/java/com/nlu/applicationProcess/application/impl/JobApplicationServiceImpl.java)
- Không kiểm tra job đã hết hạn (`expiredDate`) hoặc job không ở trạng thái `ACTIVE` → ứng tuyển được vào job đã đóng.
- Chống nộp trùng là check-then-act (`findByJobAndUser` rồi `save`), không có unique constraint `(job_id, user_id)` → 2 request đồng thời tạo 2 bản ghi.
- Upload lên R2 **bên trong** transaction DB: giữ connection trong lúc gọi mạng; nếu `save` lỗi sau khi upload → file mồ côi trên R2.
- **Đề xuất**: thêm unique index + bắt `DataIntegrityViolationException` → 409; kiểm tra `job.isExpired()`/status; upload trước khi mở transaction và dọn file khi rollback (hoặc tái dùng luồng presigned upload đã có của `ResumeUploadService`).

### 1.9 🟠 Tìm kiếm & AI matching trả kết quả sai
- `JobQuery#findJobsBySalaryAddressAndEmploymentTypes` ([dòng 118](../job-backend/src/main/java/com/nlu/recruitment/infrastructure/query/JobQuery.java#L118)) **không lọc** `recordStatus = ACTIVE` (các query khác có) → job `PENDING`/`SUSPENDED`/`REJECTED` lộ ra ở trang tìm kiếm. Không query public nào lọc job hết hạn.
- Bộ lọc lương bị comment vì `salary` là `String` → UI có slider lương nhưng backend bỏ qua `min/max`.
- `JobRepository#matchJobs` là native SQL nên `@SQLRestriction` **không áp dụng** → trả cả job đã xóa/hết hạn. Endpoint `/api/jobs/match/{cvId}` thuộc `PUBLIC_GET_ENDPOINTS` (`/api/jobs/**`) → ai cũng gọi được với `cvId` bất kỳ, và đây là query vector nặng (dễ bị lạm dụng làm DoS).
- `updateJob` không reset `isAnalyzed`/không vector hóa lại → vector JD cũ lệch với nội dung mới; xóa job không xóa `jd_vectors`.
- **Đề xuất**: thêm `j.record_status = 'ACTIVE' AND (j.expired_date IS NULL OR j.expired_date > now())` vào native query; chuyển endpoint sang `/api/user/jobs/match/{cvId}` và kiểm tra chủ CV; tách lương thành `salary_min`, `salary_max`, `currency` (số); khi JD thay đổi → set `isAnalyzed=false` và phát lại event.

### 1.10 🟡 Kiểm duyệt job (moderation) chưa thực sự hoạt động
- `BaseEntity#onPrePersist` mặc định `ACTIVE` → job mới **không bao giờ** vào `PENDING`; hàng đợi duyệt trên dashboard admin luôn rỗng trừ khi admin tự chuyển.
- `AdminDashboardServiceImpl#getJobDistribution` đếm `DELETED` làm "expired" nhưng `@SQLRestriction("record_status <> 'DELETED'")` làm số này **luôn bằng 0**; `jobRepository.count()` cũng không tính bản ghi đã xóa.
- `totalRevenue`, `growth`, `revenueTrend`, billing tiers/transactions đều là **số liệu cứng** ([AdminDashboardServiceImpl.java:38](../job-backend/src/main/java/com/nlu/admin/application/impl/AdminDashboardServiceImpl.java#L38), `AdminBillingServiceImpl`).
- **Đề xuất**: thêm cờ cấu hình `app.moderation.enabled`; khi bật thì job mới = `PENDING`. Thống kê "hết hạn" dựa trên `expiredDate`, không dựa trên `DELETED`. Đánh dấu rõ trên UI các widget đang dùng dữ liệu mẫu cho đến khi có module thanh toán.

### 1.11 🟡 Blog
[BlogServiceImpl.java](../job-backend/src/main/java/com/nlu/content/application/impl/BlogServiceImpl.java)
- `getBlogById` không lọc `status = PUBLIC` → blog nháp/riêng tư đọc được qua id.
- `getComments` trả **toàn bộ comment của mọi blog** (không theo `blogId`) và trả thẳng entity `Comment` (lộ quan hệ/lazy loading).
- `comment(Comment comment, …)` nhận entity làm request body → mass assignment.
- Điều kiện `blog.getAuthor() != null && …` → blog không có tác giả thì ai cũng sửa/xóa được.
- Response public chứa **email tác giả** (rò rỉ PII) – nên trả tên hiển thị.

### 1.12 🟡 Các lỗi logic nhỏ khác
- `JwtFilter` ([dòng 71](../job-backend/src/main/java/com/nlu/identity/infrastructure/filter/JwtFilter.java#L71)): `extractUsername` nằm **ngoài** `try` → token hết hạn ném exception thô; nhánh `userEmail == null` gọi `filterChain.doFilter` rồi lại `throw` (xử lý request 2 lần). Token sai trả **400** trong khi frontend chỉ refresh khi gặp **401**.
- `GET /api/auth/activation-links/{email}` public, không rate-limit → có thể dùng để spam email bất kỳ địa chỉ nào; thao tác có side-effect không nên là `GET`. Tương tự `GET /auth/refreshToken`, `GET /auth/logout` nên là `POST`.
- `logout` gọi `request.getSession()` → tạo session mới nếu chưa có; nên dùng `getSession(false)`.
- Login trả thông báo khác nhau cho "email không tồn tại" và "sai mật khẩu" → lộ email nào đã đăng ký; email không tồn tại cũng không bị tính vào bộ đếm spam.
- `forgotPassword`: không giới hạn số lần nhập mã OTP; reset mật khẩu không thu hồi phiên cũ.
- OAuth2 ([CustomOAuth2SuccessHandler.java:72](../job-backend/src/main/java/com/nlu/identity/infrastructure/filter/CustomOAuth2SuccessHandler.java#L72)): access token đặt trong **URL** (`/login-callback?token=…`) → lộ qua history/Referer/log. Cookie refresh đã được set nên chỉ cần redirect rồi để frontend gọi refresh. Nếu `FRONTEND_URL` chứa nhiều origin (CORS hỗ trợ) thì URL redirect bị hỏng. Facebook có thể không trả `email` → NPE.
- Frontend [error.interceptor.ts](../job-frontend/src/app/core/interceptors/error.interceptor.ts): **mọi** 403 đều buộc logout (vd. hirer bấm sửa job không phải của mình → bị đăng xuất). Chỉ nên logout khi 401 không refresh được; 403 nên hiển thị thông báo. Interceptor cũng toast cho mọi lỗi → trùng với toast trong component.
- `userLoginGuard`: `token.getToken() !== null` luôn đúng vì token mặc định là `''`.
- Không có route `**` (trang 404) trong [app.routes.ts](../job-frontend/src/app/app.routes.ts).

---

## 2. Hiệu năng

### 2.1 Backend

| Vấn đề | Chi tiết | Đề xuất |
|---|---|---|
| **Mỗi request có token = 1 query DB** | `JwtFilter` gọi `loadUserByUsername` mỗi request | Đưa `userId`, `roles`, `tokenVersion` vào claim; chỉ load user khi cần (`@CurrentUser`) hoặc cache user trong Redis/Caffeine 30–60 giây |
| **Parse JWT 3–4 lần/request** | `extractUsername` + `isTokenValid` (parse lại) + `isTokenExpired` (parse lại); `getSignInKey()` decode Base64 mỗi lần | Parse 1 lần lấy `Claims`; cache `SecretKey` trong field `final` (khởi tạo ở `@PostConstruct`) |
| **Rate limit 4–5 round-trip Redis/request** | `isBlocked` (2 lần), `INCR`, `EXPIRE`, `GET` remaining, `TTL` | Một Lua script trả về `{allowed, remaining, ttl}`; `INCR`+`EXPIRE` không nguyên tử → nếu crash giữa chừng key không bao giờ hết hạn. Comment ghi "sliding window" nhưng thực tế là fixed window |
| **Rate limit sai hạn mức** | `RateLimitFilter` được add **trước** `JwtFilter` ([SecurityConfig.java:70](../job-backend/src/main/java/com/nlu/identity/infrastructure/config/SecurityConfig.java#L70)) → `SecurityContext` chưa có auth → luôn dùng `PUBLIC_RATE_LIMIT` | Đặt sau `JwtFilter`, hoặc key theo `userId` khi có token |
| **IP giả mạo** | `IpUtils` tin `CF-Connecting-IP`/`X-Forwarded-For` từ bất kỳ client nào | Đã có `server.forward-headers-strategy: framework` → chỉ dùng `request.getRemoteAddr()`, và cấu hình trusted proxy ở tầng hạ tầng |
| **Thiếu index** | Bảng `job` không index `created_at`, `record_status`, `category_id`, `hirer_id`, `address_id` (PostgreSQL **không** tự index FK); `job_application` không index `(job_id, user_id)` | Thêm qua migration: `job(record_status, created_at DESC)`, `job(category_id)`, `job(hirer_id)`, unique `job_application(job_id, user_id)` |
| **Tìm theo tiêu đề full scan** | `title.lower().containsIgnoreCase()` → `LIKE '%x%'` (vừa thừa `lower()`) | Dùng `pg_trgm` + GIN index, hoặc full-text `tsvector` có hỗ trợ tiếng Việt không dấu (`unaccent`) |
| **Count thừa** | `JobQuery#fetchPage` luôn chạy `COUNT` | `PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne)` → bỏ qua count khi trang đầu chưa đầy; cân nhắc keyset pagination cho feed "mới nhất" |
| **N+1** | `BlogServiceImpl#getBlogs` gọi `b.getAuthor()` từng dòng; `getPendingJobs` gọi `job.getRecruitment()` từng dòng | Projection DTO hoặc `@EntityGraph`/`JOIN FETCH` |
| **Cache trang chủ không invalidate** | `CategoryCacheService` TTL 10 phút, không xóa khi tạo/sửa/xóa job; không chống cache stampede | Evict khi job thay đổi (qua event `AFTER_COMMIT`); dùng Spring Cache + `@Cacheable(sync = true)`. Đổi tên lớp cho đúng nội dung (đang cache home feed + thống kê theo thành phố, không phải category) |
| **Message chứa raw text CV** | `ResumeParsingMessage` mang nguyên `rawText` qua RabbitMQ | Chỉ gửi `cvId`, consumer tự đọc từ DB → message nhỏ, retry an toàn |
| **Không có DLQ, retry chỉ ở dev** | `application-prod.yml` không có `listener.simple.retry`/`default-requeue-rejected=false`; `ResumeParsingConsumer` ném lại exception → mặc định requeue vô hạn → gọi LLM lặp lại | Khai báo DLX/DLQ cho từng queue, cấu hình retry có backoff ở `application.yml` (dùng chung mọi profile), idempotency key cho consumer |
| **Gửi email đồng bộ trong transaction** | `registerUser` gửi message trong transaction | Publish event và gửi sau `AFTER_COMMIT` như đã làm với AI |
| **`open-in-view` mặc định bật** | Giữ connection DB suốt request, che giấu lazy-loading | `spring.jpa.open-in-view: false` |
| **Chưa nén response** | | `server.compression.enabled: true` (JSON danh sách job nén rất tốt) |
| **JVM** | Dockerfile cố định `-Xmx400m` | Dùng `-XX:MaxRAMPercentage=75` để bám theo giới hạn container |

### 2.2 Frontend

| Vấn đề | Chi tiết | Đề xuất |
|---|---|---|
| **SSR không có hydration** | Không có `provideClientHydration()` → client vẽ lại toàn bộ DOM (nhấp nháy, CLS), mọi HTTP gọi 2 lần (server + client) | `provideClientHydration(withEventReplay(), withHttpTransferCacheOptions(...))` |
| **SSR không được deploy** | [vercel.json](../job-frontend/vercel.json) rewrite mọi route về `index.html` → chạy như SPA; SEO trang job/blog không được hưởng SSR | Hoặc deploy SSR thật (Vercel Node function / Render), hoặc dùng `RenderMode.Prerender` cho trang tĩnh (`about`, `contact`) + `RenderMode.Client` cho trang tài khoản/admin/recruiter trong [app.routes.server.ts](../job-frontend/src/app/app.routes.server.ts) |
| **Bundle khởi tạo lớn** | Bản build trong `dist/` có initial ≈ **770 KB** (chưa nén); Prism + 11 ngôn ngữ, `ngx-markdown`, DOMPurify, ClipboardJS import trong `app.config.ts` → nằm trong `main` dù chỉ trang blog dùng; budget cho phép tới 4–5 MB | Chuyển `provideMarkdown` và Prism vào providers của route blog; hạ budget về ~600 KB warning / 1 MB error để CI bắt hồi quy |
| **Thư viện trùng chức năng** | `swiper` và `keen-slider`; `prismjs` và `highlight.js`; Angular Material + Tailwind | Giữ một thư viện mỗi loại |
| **Hai lockfile** | `package-lock.json` và `pnpm-lock.yaml` cùng tồn tại | Chọn một package manager (AGENTS.md dùng `npm ci`) |
| **0/82 component dùng `OnPush`** | Zone.js chạy change detection toàn cây mỗi sự kiện; header có `@HostListener('window:scroll')` không throttle | Bật `ChangeDetectionStrategy.OnPush` (đã dùng signal nên chuyển dễ), cân nhắc zoneless (`provideZonelessChangeDetection`) |
| **Pipe dịch `impure`** | [translate.pipe.ts](../job-frontend/src/app/shared/pipes/translate.pipe.ts) `pure: false` → mỗi chu kỳ CD tách chuỗi key và duyệt cây cho **mọi** chỗ dùng | Dùng `computed()` theo `language` signal, hoặc pipe pure nhận thêm tham số ngôn ngữ; cache theo key |
| **File dịch 2.000 dòng** | [translations.ts](../job-frontend/src/app/core/i18n/translations.ts) chứa cả `vi` và `en` trong main bundle | Tách JSON theo ngôn ngữ, lazy load |
| **Rò rỉ subscription** | 113 lời gọi `.subscribe(` nhưng chỉ 2 file dùng `takeUntilDestroyed`/`DestroyRef` | `takeUntilDestroyed()`, `toSignal()`, hoặc `async` pipe |
| **Ảnh** | Không dùng `NgOptimizedImage`, không có `@defer` | `ngSrc` cho logo công ty; `@defer (on viewport)` cho các section dưới màn hình đầu ở trang chủ |
| **Log debug ở production** | `AuthService#logAuthDebug` luôn `console.info` | Bọc theo `isDevMode()` |

---

## 3. Thiết kế & kiến trúc

### 3.1 Backend
- **Migration DB**: dev dùng `ddl-auto: update`, prod `none`, không có Flyway/Liquibase → schema prod lệch dev, index/constraint không được quản lý. Nên thêm Flyway, baseline từ schema hiện tại; `jd_vectors`/`cv_vectors` (pgvector) cũng cần migration và index HNSW/IVFFlat.
- **Phân quyền dựa trên danh sách path** ([ApiConstants.java](../job-backend/src/main/java/com/nlu/identity/domain/vo/ApiConstants.java)) dễ sót (đã sót 2 chỗ). Kết hợp method security và `denyAll()` mặc định. Chuẩn hóa role: hiện chấp nhận cả `ROLE_HIRER` lẫn `HIRER` → nên migrate dữ liệu về một dạng.
- **Ranh giới bounded context bị xuyên thủng**: `User` (identity) giữ `List<JobApplication>`, `List<Resume>` (applicationProcess); `Job` giữ `List<JobApplication>` với `cascade = ALL`; `AuthServiceImpl` dùng `RecruitmentRepository`. Nên tham chiếu bằng id giữa các context và giao tiếp qua application service/event. Tài liệu `job-backend/ddd_analysis.md` đã có — nên cập nhật theo.
- **Hai controller cùng `@RequestMapping("/api/account/roles")`** (`HirerAccountController`, `UserAccountController`) → dễ nhầm lẫn; tách path rõ ràng.
- **Trạng thái dùng chung một enum** `EntityStatus` (ACTIVE/DELETED/PENDING/REJECTED/SUSPENDED) cho mọi entity, trong khi Job cần vòng đời riêng (DRAFT → PENDING → PUBLISHED → EXPIRED/CLOSED) và soft-delete là khái niệm khác. Đề xuất tách `deletedAt` (soft delete) khỏi trạng thái nghiệp vụ.
- **Kiểu dữ liệu**: `salary` là `String`; `Job.time` lưu `EmploymentType` nhưng native query parse lại bằng `valueOf` thủ công; thuộc tính `isAnalyzed` có tiền tố `is` gây getter `isAnalyzed()`/tên cột khó đoán.
- **Validate trong setter entity ném `BadRequestException`** kèm `MessageUtils` → domain phụ thuộc HTTP/i18n. Dùng Bean Validation ở DTO + exception domain riêng, ánh xạ sang HTTP ở `GlobalExceptionHandler`.
- **Exception chung chung**: nhiều chỗ `throw new RuntimeException(...)` (kèm thông báo tiếng Việt cứng) → client nhận 500. Thay bằng `AppException` có mã lỗi.
- **Dead code/config cũ**: `JobSpecifications` (không dùng, gọi `between` trên cột String), queue `cloudUploadQueue` "legacy", cấu hình `deepseek`, `ocr.api-key`, `HomeFeedController#home()` trả "Hello world", `DefaultHttpFirewall` (yếu hơn `StrictHttpFirewall` mặc định – nên bỏ trừ khi có lý do cụ thể).
- **SSE lưu trong RAM** (`ConcurrentHashMap` trong [SseEmitterServiceImpl.java](../job-backend/src/main/java/com/nlu/shared/application/impl/SseEmitterServiceImpl.java#L29)): chỉ chạy được 1 instance; mỗi user chỉ 1 emitter → mở tab thứ hai sẽ ngắt tab đầu, sự kiện bị mất nếu user offline. Khi scale ngang: dùng Redis Pub/Sub (hoặc RabbitMQ fanout) để phát sự kiện tới instance đang giữ kết nối, map `userId → List<SseEmitter>`; lưu trạng thái xử lý CV trong DB để client poll bù khi kết nối lại.
- **Actuator**: `/actuator/**` public + `health.show-details: always` → lộ thông tin DB/Redis/RabbitMQ. Đặt `show-details: when-authorized`.
- **Nhất quán API**: lẫn `/{pageIndex}/{pageSize}` trong path và `?page=&size=`; lẫn `Page<T>` của Spring (serialize không ổn định) với `PageResponse` tự định nghĩa; lẫn `/api/v1/...` với `/api/...`. Đề xuất chuẩn: query param, `PageResponse` cho mọi API, versioning thống nhất, và sinh tài liệu OpenAPI (springdoc).

### 3.2 Frontend
- **Kiểu `any`** ở các service lõi (`AuthService.login(body: any)`, `http.get<any>`) → mất lợi ích TypeScript; định nghĩa interface request/response (có thể sinh từ OpenAPI).
- **Code chết**: `AuthService#hirerLogin` điều hướng tới `/hirer` (route không tồn tại); route alias trùng (`blog`/`blogHome`, `blogCreation`/`blog-creation`, `jobs`/`category`) → chọn một URL chuẩn và redirect 301 các URL cũ.
- **Component quá lớn**: `jobs-management` (610 dòng TS + 885 dòng HTML), `employers`, `billing`, `category`… → tách presentational component (bảng, toolbar, drawer) và đưa state vào service/store theo signal.
- **Ghi đè API base URL qua `localStorage`** ([utilities.service.ts](../job-frontend/src/app/core/services/utilities.service.ts)) – đảm bảo `allowLocalApiOverride` luôn `false` ở `environment.production.ts`.
- **CSP lặp ở hai nơi** (`index.html` meta và `server.ts`) và có domain cứng (`localhost:8080`, bucket R2) → sinh từ biến môi trường, một nguồn duy nhất.
- **Phân quyền chỉ ở client** qua guard đọc role trong JWT là đúng cho UX, nhưng cần đảm bảo backend luôn kiểm tra (xem mục 1).

---

## 4. Kiểm thử, vận hành, quy trình

- **Thiếu test bảo mật theo ma trận role** – các lỗ hổng ở mục 1.1–1.3 lẽ ra bị bắt bởi một test `@WebMvcTest`/`@SpringBootTest` duy nhất duyệt mọi endpoint × role.
- **Không có CI** (`.github/workflows` không tồn tại): tối thiểu chạy `./mvnw test` và `npm test -- --watch=false` + `npm run build` (có budget) cho mỗi PR. Dockerfile đang `-DskipTests`.
- **Integration test với hạ tầng thật**: dùng Testcontainers (PostgreSQL + pgvector, Redis, RabbitMQ) cho các query native và luồng consumer.
- **Observability**: đã có `RequestLoggingFilter` + MDC; nên bổ sung Micrometer metrics (`/actuator/prometheus`), thời gian xử lý AI, độ dài queue, tỷ lệ lỗi consumer; tracing (OpenTelemetry) xuyên HTTP → RabbitMQ.
- **Dọn repo**: `bash.exe.stackdump` đang được commit; thư mục `outputs/` rỗng; README còn dòng "replace both links above with your real deployed URLs".

---

## 5. Đề xuất tính năng

Nhóm theo giá trị cho từng đối tượng; ưu tiên các tính năng tận dụng hạ tầng đã có (vector matching, SSE, RabbitMQ).

### 5.1 Ứng viên
1. **Theo dõi trạng thái hồ sơ (Application Tracking)** – hiện `JobApplication` chưa có trạng thái nghiệp vụ. Thêm vòng đời `SUBMITTED → VIEWED → SHORTLISTED → INTERVIEW → OFFERED/REJECTED`, timeline trong trang "Lịch sử ứng tuyển", thông báo SSE + email khi đổi trạng thái.
2. **Job Alert / Saved Search** – lưu bộ lọc tìm kiếm, job mới khớp được gửi email tổng hợp hằng ngày (job scheduler + RabbitMQ mail queue sẵn có). Kết hợp vector matching: "job mới phù hợp với CV của bạn".
3. **Đồng bộ "Việc làm đã lưu" lên server** – hiện chỉ lưu `localStorage` ([saved-jobs.service.ts](../job-frontend/src/app/core/services/saved-jobs.service.ts)), mất khi đổi thiết bị.
4. **Giải thích độ phù hợp (Explainable match)** – đã có `skill_dist`/`exp_dist`; bổ sung danh sách kỹ năng khớp/thiếu và gợi ý cải thiện CV (LLM đã tích hợp sẵn).
5. **Trung tâm thông báo** – lưu thông báo vào DB (đã đọc/chưa đọc), SSE chỉ là kênh đẩy; giải quyết luôn vấn đề mất sự kiện khi offline.

### 5.2 Nhà tuyển dụng
1. **Pipeline ứng viên dạng Kanban** cho mỗi job (kéo thả giữa các trạng thái), ghi chú nội bộ, đánh giá sao.
2. **Xếp hạng ứng viên bằng AI cho từng job** – chiều ngược lại của `matchJobs`: với một JD, xếp hạng các CV đã nộp theo độ tương đồng vector.
3. **Lên lịch phỏng vấn** – chọn khung giờ, gửi lời mời qua email/ICS, ứng viên xác nhận.
4. **Trang công ty công khai** (logo, mô tả, danh sách job đang tuyển, SEO-friendly URL) – dữ liệu `Recruitment` đã có.
5. **Thống kê tin tuyển dụng** – lượt xem, lượt ứng tuyển, tỷ lệ chuyển đổi theo ngày (cần thêm bảng `job_view_event` hoặc counter Redis flush định kỳ).
6. **Vòng đời tin**: nháp, lên lịch đăng, gia hạn, đóng tin sớm, nhân bản tin cũ.

### 5.3 Quản trị
1. **Kiểm duyệt job thực sự** (xem 1.10) + hàng đợi báo cáo vi phạm từ người dùng.
2. **Audit log** cho các thao tác quản trị (ai khóa tài khoản nào, khi nào) – `BaseEntity` đã có `createdBy/updatedBy` nhưng chưa cấu hình `AuditorAware`.
3. **Billing thật**: gói dịch vụ cho nhà tuyển dụng (giới hạn số tin, tin nổi bật, quyền dùng AI matching), tích hợp cổng thanh toán (VNPay/MoMo/Stripe) thay cho dữ liệu cứng hiện tại.
4. **Xuất báo cáo CSV** cho danh sách nhà tuyển dụng/ứng viên/job.

### 5.4 Nền tảng
1. **SEO cho trang job**: slug URL (`/viec-lam/{slug}-{id}`), `JobPosting` JSON-LD (Google for Jobs), sitemap tự sinh, meta/OG tag động – chỉ phát huy khi SSR được deploy (mục 2.2).
2. **Tìm kiếm nâng cao**: full-text tiếng Việt không dấu, gợi ý autocomplete, tìm theo bán kính địa lý.
3. **2FA (TOTP) cho tài khoản admin và nhà tuyển dụng.**
4. **PWA/Web Push** để nhận thông báo khi không mở trang.

---

## 6. Lộ trình gợi ý

| Giai đoạn | Nội dung | Ước lượng |
|---|---|---|
| **Sprint 1 – Vá khẩn** | 1.1, 1.2, 1.3, 1.4, 1.5 + test ma trận phân quyền; chặn `/api/jobs/match/{cvId}` | 3–5 ngày |
| **Sprint 2 – Ổn định** | Flyway + index + unique constraint; DLQ/retry RabbitMQ cho prod; sửa rate limit & IP; token `typ`; refresh nguyên tử; lọc status/hết hạn ở search & matching | 1–1,5 tuần |
| **Sprint 3 – Hiệu năng** | JWT claims thay query DB; cache invalidation; hydration + chiến lược render route; lazy-load markdown/Prism; OnPush; hạ budget; CI | 1–1,5 tuần |
| **Sprint 4+ – Tính năng** | Application tracking + thông báo lưu DB → Job alert → Pipeline Kanban & xếp hạng ứng viên bằng AI → SEO trang job | theo backlog |
