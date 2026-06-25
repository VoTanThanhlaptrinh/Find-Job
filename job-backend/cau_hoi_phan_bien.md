# 🎓 Bộ Câu Hỏi Phản Biện — Hệ Thống Tuyển Dụng Find-Job

> Được xây dựng dựa trên phân tích mã nguồn thực tế của dự án. Mỗi câu hỏi đều trỏ vào một **quyết định thiết kế cụ thể** trong code, không hỏi lý thuyết chung chung.

---

## PHẦN 1: KIẾN TRÚC TỔNG THỂ (Architecture)

### Câu 1 — Bounded Context & Package Structure
Hệ thống chia thành 5 package chính: `identity`, `recruitment`, `applicationProcess`, `content`, `admin` — mỗi package đều có cấu trúc `api/application/domain/infrastructure/mapper`.

**Câu hỏi:**
- Tại sao bạn chọn cách chia package theo **Bounded Context** thay vì chia theo **Layer** (tất cả controller vào 1 package, tất cả service vào 1 package)?
- Nếu module `applicationProcess` cần dùng `Job` entity từ module `recruitment`, bạn xử lý dependency giữa các Bounded Context thế nào? Có vi phạm nguyên tắc DDD không?

> [!IMPORTANT]
> **Gợi ý đánh giá:** Trong code thực tế, `JobApplication.java` (`applicationProcess`) đang `import` trực tiếp `Job` từ `recruitment`. Hãy hỏi sinh viên biết đây là **trade-off có ý thức** hay **vô tình vi phạm**.

---

### Câu 2 — Tại sao chỉ có 1 ứng dụng Spring Boot duy nhất?
File [JobPortalWebApplication.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/JobPortalWebApplication.java) cho thấy đây là **monolith** — tất cả Bounded Context cùng chạy trong 1 process.

**Câu hỏi:**
- Bạn đã cân nhắc microservices chưa? Tại sao lại chọn monolith?
- Nếu sau này muốn tách `identity` thành service riêng (ví dụ: authentication service), bạn cần thay đổi những gì trong kiến trúc hiện tại?

---

## PHẦN 2: DOMAIN MODEL & BUSINESS LOGIC

### Câu 3 — Validation trong Entity vs Controller
Trong [Job.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/domain/model/Job.java), validation nằm trong **setter** (ví dụ `setTitle()` throw `BadRequestException` nếu rỗng). Nhưng ở [HirerJobController.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/api/HirerJobController.java), bạn cũng dùng `@Valid` trên `@ModelAttribute JobDto`.

**Câu hỏi:**
- Validation đang diễn ra ở **2 nơi** (DTO validation + Entity setter). Đây là thiết kế có chủ đích hay trùng lặp?
- Khi nào thì validation ở Entity quan trọng hơn validation ở DTO? Cho ví dụ cụ thể trong hệ thống của bạn.

---

### Câu 4 — Soft Delete với `@SQLRestriction`
Tất cả entity đều kế thừa [BaseEntity.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/domain/model/BaseEntity.java) và sử dụng `@SQLRestriction("record_status <> 'DELETED'")`.

**Câu hỏi:**
- Soft delete hay hard delete — bạn chọn soft delete vì lý do gì?
- `@SQLRestriction` tự động thêm điều kiện `WHERE record_status <> 'DELETED'` vào mọi query. **Vậy khi Admin muốn xem lại các job đã bị xóa**, bạn làm thế nào?
- Annotation `@SQLRestriction` có ảnh hưởng gì đến performance khi bảng lớn (hàng triệu bản ghi) không?

---

### Câu 5 — Quan hệ `Job ↔ Recruitment ↔ User`
Trong [Job.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/domain/model/Job.java#L51-L54), `Job` liên kết với `Recruitment` (nhà tuyển dụng), và `Recruitment` lại liên kết với `User`. Nhưng khi kiểm tra quyền trong [JobServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/application/impl/JobServiceImpl.java#L137), bạn so sánh `job.getRecruitment().getId() != recruitment.getId()`.

**Câu hỏi:**
- Tại sao dùng `!=` cho `long` thay vì `.equals()` cho `Long`? Khi nào điều này gây bug?
- Phương thức `isOwnedBy()` trong `Job.java` đã tồn tại nhưng chỉ được dùng ở `analyzeJob()` mà không dùng ở `updateJob()` hay `deleteJob()`. Tại sao không thống nhất?

---

## PHẦN 3: SECURITY & AUTHENTICATION

### Câu 6 — JWT Flow & Refresh Token
Trong [refresh-token.interceptor.ts](file:///d:/web-project/job-frontend/src/app/core/interceptors/refresh-token.interceptor.ts), bạn xử lý **silent refresh** khi nhận được HTTP 401.

**Câu hỏi:**
- Giải thích luồng hoạt động: Khi access token hết hạn → browser gửi request → server trả 401 → interceptor làm gì tiếp theo?
- Biến `refreshRequest$` ở dòng 18 là **module-level variable**. Tại sao bạn dùng `shareReplay(1)` kết hợp với biến này? Nếu không có, chuyện gì xảy ra khi 5 request đồng thời đều nhận được 401?
- Refresh token được lưu ở đâu? Cookie hay localStorage? Tại sao chọn cách đó?

---

### Câu 7 — Security Configuration
Trong [SecurityConfig.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/infrastructure/config/SecurityConfig.java), filter chain có 4 filter custom: `RequestLoggingFilter` → `RateLimitFilter` → `JwtFilter` → `VerifyRecoveryFilter`.

**Câu hỏi:**
- **Thứ tự filter** có quan trọng không? Nếu đặt `JwtFilter` trước `RateLimitFilter`, có vấn đề gì?
- CORS chỉ cho phép 2 origin: `localhost:4200` và `find-job-frontend.vercel.app`. Nếu bạn muốn thêm domain mới (ví dụ mobile app), bạn phải sửa code và deploy lại? Có cách nào linh hoạt hơn không?
- `DaoAuthenticationProvider` với `BCryptPasswordEncoder` — tại sao chọn BCrypt thay vì Argon2 hay PBKDF2?

---

## PHẦN 4: MESSAGING & ASYNC PROCESSING

### Câu 8 — RabbitMQ Architecture
Trong [MessageProducer.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/message/MessageProducer.java), bạn có 3 exchange: `mailExchange`, `parsingExchange`, `apiExchange`.

**Câu hỏi:**
- Tại sao tách thành 3 exchange riêng thay vì dùng 1 exchange với nhiều routing key?
- Nếu service xử lý AI (parsing CV) bị crash giữa chừng, message có bị mất không? Bạn xử lý **retry** hay **dead letter queue** thế nào?
- Trong [MessageConsumer.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/message/MessageConsumer.java#L83-L90), khi catch exception ở `parsingRawText`, bạn chỉ gửi SSE event "failed". **Dữ liệu message gốc có được retry không hay mất luôn?**

---

### Câu 9 — SSE (Server-Sent Events) cho Real-time
Bạn sử dụng `SseEmitterService` để push trạng thái xử lý AI về frontend (analyzing → vectorizing → analyzed).

**Câu hỏi:**
- SSE khác WebSocket ở điểm nào? Tại sao chọn SSE thay vì WebSocket cho use case này?
- Nếu user đóng tab browser giữa lúc AI đang xử lý, rồi mở lại — user có biết kết quả không? Cơ chế nào đảm bảo điều đó?
- SSE connection có timeout không? Nếu quá trình AI mất 5 phút, connection có bị ngắt không?

---

## PHẦN 5: AI & CV MATCHING

### Câu 10 — Luồng xử lý AI Matching
Trong [JobServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/application/impl/JobServiceImpl.java#L195-L229), `matchJobs()` query trực tiếp từ database bằng native query (`jobRepository.matchJobs(cvId)`) và trả về `Tuple`.

**Câu hỏi:**
- `matchJobs` trả về `final_dist`, `skill_dist`, `exp_dist`. Đây là **cosine distance** hay **euclidean distance**? Tại sao chọn metric đó?
- Công thức `(1.0 - finalDist) * 100` để tính phần trăm match — giải thích tại sao `1 - distance` lại ra similarity score?
- Vector embedding được tạo ra bởi model nào? Dimension bao nhiêu? Tại sao chọn dimension đó?
- Nếu CV của ứng viên viết bằng tiếng Việt nhưng JD viết bằng tiếng Anh, matching có chính xác không? Tại sao?

---

### Câu 11 — Vectorize JD Flow
Khi tạo job mới có `enableAiAnalysis`, hệ thống gửi `VectorizeJdRequest` qua RabbitMQ. Trong đó, bạn tách dữ liệu thành 2 context:
- `skillsAndProjectsContext` = skill + moreDetail
- `experienceContext` = description + requireDetails

**Câu hỏi:**
- Tại sao tách thành 2 vector riêng (skill vs experience) thay vì gộp thành 1 vector duy nhất?
- `htmlParserService.parseHtml()` — JD được lưu dưới dạng HTML à? Tại sao cần parse HTML trước khi vectorize?

---

## PHẦN 6: FRONTEND ANGULAR

### Câu 12 — Lazy Loading & Route Structure
Trong [app.routes.ts](file:///d:/web-project/job-frontend/src/app/app.routes.ts), bạn sử dụng `loadChildren` cho `recruiter` và `admin`, nhưng dùng `loadComponent` cho hầu hết các route khác.

**Câu hỏi:**
- `loadChildren` vs `loadComponent` — khác nhau thế nào? Tại sao recruiter dùng `loadChildren` còn login/register dùng `loadComponent`?
- Lazy loading giải quyết vấn đề gì? Nếu không lazy load, user sẽ trải nghiệm điều gì khác?

---

### Câu 13 — Guards
Bạn có 3 guard: `admin-guard`, `hirer-guard`, `user-login`.

**Câu hỏi:**
- Guard hoạt động ở **client-side**. Nếu attacker bypass guard bằng cách gọi trực tiếp API, backend có chặn được không? Cơ chế nào ở backend đảm bảo điều đó?
- Guard kiểm tra gì để xác định user là admin/hirer? Thông tin đó lấy từ đâu — JWT token hay API call riêng?

---

## PHẦN 7: DEPLOYMENT & DevOps

### Câu 14 — CSP (Content Security Policy)
Lỗi CSP bạn vừa gặp là ví dụ thực tế.

**Câu hỏi:**
- CSP là gì? Nó bảo vệ ứng dụng khỏi loại tấn công nào?
- Trong [vercel.json](file:///d:/web-project/job-frontend/vercel.json), `script-src 'unsafe-inline'` — bạn biết `unsafe-inline` nguy hiểm thế nào không? Tại sao vẫn phải dùng?
- CSP được set ở **3 chỗ** (vercel.json, server.ts, index.html meta tag). Khi deploy lên Vercel, cái nào có hiệu lực? Có conflict không?

---

### Câu 15 — CORS Configuration
Backend cho phép origin `https://find-job-frontend.vercel.app`, nhưng CSP ở frontend cho phép `connect-src` tới `https://find-job-ctkj.onrender.com`.

**Câu hỏi:**
- CORS và CSP — hai thứ này khác nhau thế nào? Cái nào do server kiểm soát, cái nào do browser kiểm soát?
- Nếu CORS cho phép nhưng CSP chặn, request có đi qua không?
- Nếu CSP cho phép nhưng CORS chặn, kết quả sẽ ra sao?

---

## PHẦN 8: CÂU HỎI "BẪY" — KIỂM TRA HIỂU SÂU

### Câu 16 — Transactional
`JobServiceImpl` được annotate `@Transactional(readOnly = true)` ở class level, nhưng `createJob()`, `updateJob()`, `deleteJob()` lại có `@Transactional` riêng (không readOnly).

**Câu hỏi:**
- Giải thích cơ chế override này. Nếu bỏ `@Transactional` trên `createJob()`, chuyện gì xảy ra?
- `readOnly = true` mang lại lợi ích gì cho Hibernate? (Gợi ý: dirty checking)

---

### Câu 17 — MDC Logging
Bạn dùng `MDC.put(MDC_USER_ID, ...)` và `MDC.remove()` trong `try-finally` block.

**Câu hỏi:**
- MDC là gì? Tại sao phải `remove` trong `finally`?
- Nếu quên `MDC.remove()`, trong môi trường multi-threaded (như thread pool), điều gì xảy ra?

---

### Câu 18 — N+1 Query Problem
Trong `Job.java`, quan hệ `@ManyToOne(fetch = FetchType.LAZY)` với `Recruitment`, `Address`, `Category`.

**Câu hỏi:**
- Khi load danh sách 100 job để hiển thị, mỗi job lại cần `recruitment.companyName`, `address.city`, `category.name`. Tổng cộng bao nhiêu query sẽ được thực thi?
- Bạn giải quyết N+1 problem bằng cách nào? `JOIN FETCH`? `@EntityGraph`? `@BatchSize`?

---

> [!TIP]
> **Cách sử dụng bộ câu hỏi này:**
> - Câu 1-5: Kiểm tra hiểu **kiến trúc và thiết kế domain** (30%)
> - Câu 6-7: Kiểm tra hiểu **bảo mật** (15%)
> - Câu 8-9: Kiểm tra hiểu **messaging & async** (15%)
> - Câu 10-11: Kiểm tra hiểu **tính năng AI** (15%)
> - Câu 12-13: Kiểm tra hiểu **frontend** (10%)
> - Câu 14-15: Kiểm tra hiểu **deployment** (5%)
> - Câu 16-18: Kiểm tra **hiểu sâu kỹ thuật** (10%)
