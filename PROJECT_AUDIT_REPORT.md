# 📋 BÁO CÁO PHÂN TÍCH TOÀN DIỆN VÀ DANH SÁCH CẦN CẢI THIỆN (FIND-JOB)

> Tài liệu này tổng hợp toàn bộ các điểm **logic nhầm**, **bottleneck hiệu năng**, và **code thừa/kém tối ưu** trong hệ thống Find-Job (Spring Boot Backend + Angular Frontend).  
> Các mục được gắn kèm checkbox `- [ ]` để bạn có thể theo dõi và đánh dấu hoàn thành trong quá trình sửa dần dần.

---

## 📊 Bảng Tổng Hợp Theo Mức Độ Ưu Tiên

| Check |  #  | Vấn đề                                            | Phân loại              |    Mức độ    | File chính                                          |
| :---: | :-: | :------------------------------------------------ | :--------------------- | :----------: | :-------------------------------------------------- |
|  [X]  |  1  | `containsKey()` sai key prefix                    | 🐛 Logic               | **CRITICAL** | `VerificationServiceImpl.java`                      |
|  [ ]  |  2  | `deleteIpSpamLogin()` logic đảo ngược             | 🐛 Logic               |   **HIGH**   | `SpamServiceImpl.java`                              |
|  [X]  |  3  | `activeAccount()` không lưu trạng thái active     | 🐛 Logic               |   **HIGH**   | `AuthServiceImpl.java`                              |
|  [ ]  |  4  | Search job thiếu lọc `recordStatus` và địa chỉ    | 🐛 Logic + ⚡ Perf     |   **HIGH**   | `JobQuery.java`                                     |
|  [X]  |  5  | Log Authority mức INFO trên mỗi request           | ⚡ Bottleneck          |   **HIGH**   | `JwtFilter.java`                                    |
|  [ ]  |  6  | Vòng lặp `Thread.sleep()` chặn worker thread      | ⚡ Bottleneck          |   **HIGH**   | `ResumeUploadServiceImpl.java`                      |
|  [ ]  |  7  | `RateLimitFilter` gọi Redis lặp lại nhiều lần     | ⚡ Bottleneck          |   **HIGH**   | `RateLimitFilter.java`, `RateLimitServiceImpl.java` |
|  [X]  |  8  | Nuốt ngoại lệ trong `bulkJobAction`               | 🐛 Logic               |   **HIGH**   | `AdminJobServiceImpl.java`                          |
|  [ ]  |  9  | `getClientIP()` nhận diện sai trên proxy/CDN      | 🐛 Logic               |  **MEDIUM**  | `AuthServiceImpl.java`                              |
|  [ ]  | 10  | Hardcode URL `localhost:4200` trong email OAuth2  | 🐛 Logic               |  **MEDIUM**  | `AccountServiceImpl.java`                           |
|  [X]  | 11  | `checkExistJob()` tải cả entity graph             | ⚡ Perf                |  **MEDIUM**  | `JobServiceImpl.java`                               |
|  [ ]  | 12  | Đọc lặp `InputStream` của file resume             | ⚡ Perf                |  **MEDIUM**  | `ResumeServiceImpl.java`                            |
|  [ ]  | 13  | Admin Dashboard chứa fake data và query N+1       | 🗑️ Thừa + ⚡ Perf      |  **MEDIUM**  | `AdminDashboardServiceImpl.java`                    |
|  [ ]  | 14  | SSE Emitter lưu in-memory không scale ngang       | ⚡ Bottleneck          |  **MEDIUM**  | `SseEmitterServiceImpl.java`                        |
|  [ ]  | 15  | `cloudUploadQueue` cấu hình non-durable           | ⚡ Reliability         |  **MEDIUM**  | `RabbitMQConfig.java`                               |
|  [ ]  | 16  | `getBlogs()` trả entity thô và không lọc status   | 🐛 Logic + 🔒 Security |  **MEDIUM**  | `BlogServiceImpl.java`                              |
|  [ ]  | 17  | `VerifyRecoveryFilter` thiếu `final` ở dependency | 🐛 Logic               |  **MEDIUM**  | `VerifyRecoveryFilter.java`                         |
|  [X]  | 18  | Dead code `getInvalidRequestRoleMessage()`        | 🗑️ Code thừa           |   **LOW**    | `AuthServiceImpl.java`                              |
|  [X]  | 19  | Unused injection `DefaultRepositoryTagsProvider`  | 🗑️ Code thừa           |   **LOW**    | `JobServiceImpl.java`                               |
|  [ ]  | 20  | Chuỗi gọi method thừa `gerenateToken`             | 🗑️ Code thừa           |   **LOW**    | `JwtServiceImpl.java`                               |
|  [ ]  | 21  | Lỗi chính tả và thiếu xóa key spam email          | 🐛 Logic               |   **LOW**    | `SpamServiceImpl.java`                              |
|  [X]  | 22  | Sử dụng `e.printStackTrace()` thay vì Logger      | 🗑️ Code thừa           |   **LOW**    | `RefreshTokenServiceImpl.java`                      |
|  [X]  | 23  | Sử dụng `System.err.println()` thay vì Logger     | 🗑️ Code thừa           |   **LOW**    | `JobServiceImpl.java`                               |
|  [ ]  | 24  | Frontend `login()` thiếu xử lý lỗi                | 🐛 Logic               |   **LOW**    | `auth.service.ts`                                   |
|  [X]  | 25  | Duplicate hàm `getLoginUrl()` ở 3 file            | 🗑️ Code thừa           |   **LOW**    | `auth.service.ts`, `error.interceptor.ts`,...       |

---

## 🚨 PHẦN I: CÁC LỖI LOGIC (LOGIC BUGS)

### - [ ] 1. `VerificationServiceImpl.containsKey()` sai prefix gây hỏng quên mật khẩu

- **Vị trí:** `job-backend/src/main/java/com/nlu/shared/application/impl/VerificationServiceImpl.java` (dòng 18-21)
- **Hiện trạng:**
  ```java
  @Override
  public boolean containsKey(String key) {
      return redisTemplate.opsForValue().get("recovery:" + key) != null;
  }
  ```
- **Vấn đề:**
  - Phương thức `add()` lưu key trực tiếp (ví dụ: `"ref-email:user@mail.com"` hoặc `"random:token-abc"`).
  - Nhưng `containsKey()` lại tự ý ghép thêm `"recovery:"` phía trước (`"recovery:ref-email:user@mail.com"`).
  - Kết quả: `containsKey()` **luôn luôn trả về false**. Toàn bộ luồng xác thực mã forgot password/reset password bị chặn hoặc sinh lỗi logic không thể validate.
- **Cách khắc phục đề xuất:**
  ```java
  @Override
  public boolean containsKey(String key) {
      return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
  ```

---

### - [ ] 2. `SpamServiceImpl.deleteIpSpamLogin()` logic kiểm tra ngược

- **Vị trí:** `job-backend/src/main/java/com/nlu/shared/application/impl/SpamServiceImpl.java` (dòng 57-62)
- **Hiện trạng:**
  ```java
  public void deleteIpSpamLogin(String ip) {
      String blockKey = String.format("block_ip_login_%s", ip);
      if (blockIP.hasKey(blockKey)) {
          spamIp.delete(String.format("ip_spam_login_%s", ip));
      }
  }
  ```
- **Vấn đề:**
  - Khi user đăng nhập thành công, hệ thống cần xóa số lần thử sai (`ip_spam_login_`).
  - Tuy nhiên, code lại kiểm tra: nếu `blockIP.hasKey(...)` (tức là đã bị block) thì mới xóa `spamIp`.
  - Hậu quả:
    1. Người dùng gõ sai 1-2 lần rồi gõ đúng -> `hasKey` trả về `false` -> **không xóa số lần thử sai** -> lần sau gõ sai tiếp sẽ bị cộng dồn và bị block oan.
    2. Nếu đã bị block rồi: chỉ xóa `spamIp`, **không xóa `blockIP`** -> IP vẫn tiếp tục bị chặn.
- **Cách khắc phục đề xuất:**
  ```java
  @Override
  public void deleteIpSpamLogin(String ip) {
      spamIp.delete(String.format("ip_spam_login_%s", ip));
      blockIP.delete(String.format("block_ip_login_%s", ip));
  }
  ```

---

### - [ ] 3. `AuthServiceImpl.activeAccount()` không lưu trạng thái active

- **Vị trí:** `job-backend/src/main/java/com/nlu/identity/application/impl/AuthServiceImpl.java` (dòng 137-155)
- **Hiện trạng:**
  ```java
  @Override
  public void activeAccount(String token) {
      final String activate = jwtService.extractUsername(token);
      String[] values = StringUtils.delimitedListToStringArray(activate, "|");
      User user = userRepository.findByEmail_Value(values[0])
              .orElseThrow(() -> new BadRequestException("auth.user.not_found"));
      // ...
      if (!jwtService.isTokenValid(token, user)) {
          throw new BadRequestException("auth.token.expired");
      }
      log.info("Account activated successfully for user: {}", user.getId());
  }
  ```
- **Vấn đề:**
  - Trong `RegistrationFormMapper`, khi user mới đăng ký, tài khoản được gán `user.setActive(false);`.
  - Hàm `activeAccount` chỉ kiểm tra token hợp lệ rồi kết thúc mà **không hề gọi `user.setActive(true)` hay `userRepository.save(user)`**.
  - Tài khoản vẫn vĩnh viễn ở trạng thái `active = false`.
- **Cách khắc phục đề xuất:**
  ```java
  user.setActive(true);
  userRepository.save(user);
  log.info("Account activated successfully for user: {}", user.getId());
  ```

---

### - [ ] 4. `JobQuery` tìm kiếm job bị thiếu lọc `recordStatus = ACTIVE`

- **Vị trí:** `job-backend/src/main/java/com/nlu/recruitment/infrastructure/query/JobQuery.java` (dòng 118-151)
- **Hiện trạng:**
  ```java
  BooleanBuilder filterBuilder = new BooleanBuilder();
  // filterBuilder.and(job.salary.between((double) min, (double) max));
  if (cities != null && !cities.isEmpty()) {
      filterBuilder.and(address.city.in(cities));
  }
  ```
- **Vấn đề:**
  - Tất cả các query khác đều lọc `job.recordStatus.eq(ACTIVE_STATUS)` và `addressIsActiveOrMissing(address)`.
  - Riêng method tìm kiếm theo filter này bỏ quên điều kiện trạng thái -> Trả về cả các công việc đã bị xóa mềm (soft-deleted).
  - Điều kiện lọc lương (`job.salary.between`) bị comment lại khiến người dùng lọc khoảng lương không hoạt động.
- **Cách khắc phục đề xuất:**
  ```java
  filterBuilder.and(job.recordStatus.eq(ACTIVE_STATUS));
  filterBuilder.and(addressIsActiveOrMissing(address));
  if (max > 0 && max >= min) {
      filterBuilder.and(job.salary.between((double) min, (double) max));
  } else if (min > 0) {
      filterBuilder.and(job.salary.goe((double) min));
  }
  ```

---

### - [ ] 5. `AccountServiceImpl.checkOauth2()` hardcode URL `localhost:4200`

- **Vị trí:** `job-backend/src/main/java/com/nlu/identity/application/impl/AccountServiceImpl.java` (dòng 93)
- **Hiện trạng:**
  ```java
  String link = String.format("http://localhost:4200/reset-pass/%s", random);
  ```
- **Vấn đề:**
  - Khi chạy trên môi trường production, link reset mật khẩu gửi qua email vẫn trỏ về `localhost:4200` thay vì domain thực tế.
  - Trong khi đó, file `application-prod.yml` đã cấu hình `app.frontend-url`.
- **Cách khắc phục đề xuất:**
  Inject biến cấu hình:
  ```java
  @Value("${app.frontend-url:http://localhost:4200}")
  private String frontendUrl;
  ```
  Và tạo link:
  ```java
  String link = String.format("%s/reset-pass/%s", frontendUrl, random);
  ```

---

### - [ ] 6. `AuthServiceImpl.getClientIP()` nhận diện sai IP khi đứng sau Proxy

- **Vị trí:** `job-backend/src/main/java/com/nlu/identity/application/impl/AuthServiceImpl.java` (dòng 429-435)
- **Hiện trạng:**
  ```java
  private String getClientIP(HttpServletRequest request) {
      String xfHeader = request.getHeader("X-Forwarded-For");
      if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
          return request.getRemoteAddr();
      }
      return xfHeader.split(",")[0];
  }
  ```
- **Vấn đề:**
  - Logic bắt buộc `X-Forwarded-For` phải chứa `request.getRemoteAddr()` là sai đối với cấu hình Reverse Proxy (Nginx, Cloudflare) tiêu chuẩn.
  - Không đồng nhất với `RateLimitFilter` (nơi đang kiểm tra `CF-Connecting-IP`, `X-Real-IP`, v.v.).
- **Cách khắc phục đề xuất:**
  Đồng bộ hàm trích xuất IP giống như `RateLimitFilter`: ưu tiên `CF-Connecting-IP`, `X-Forwarded-For` (lấy IP đầu tiên), `X-Real-IP` rồi mới fallback về `getRemoteAddr()`.

---

### - [ ] 7. `VerifyRecoveryFilter` khai báo thiếu `final`

- **Vị trí:** `job-backend/src/main/java/com/nlu/identity/infrastructure/filter/VerifyRecoveryFilter.java` (dòng 22)
- **Hiện trạng:**
  ```java
  @RequiredArgsConstructor
  public class VerifyRecoveryFilter extends OncePerRequestFilter {
      private VerificationService verifyService;
  ```
- **Vấn đề:** Biến `verifyService` không có từ khóa `final`, khiến Lombok `@RequiredArgsConstructor` không tạo constructor inject bean này -> `verifyService` luôn bị `null`. Nếu có request đi vào URL bắt đầu bằng `/recovery`, server sẽ văng `NullPointerException`.
- **Cách khắc phục đề xuất:** Đổi thành `private final VerificationService verifyService;`.

---

## ⚡ PHẦN II: CÁC BOTTLENECK HIỆU NĂNG

### - [ ] 8. `JwtFilter` ghi log INFO cho mỗi request được xác thực

- **Vị trí:** `job-backend/src/main/java/com/nlu/identity/infrastructure/filter/JwtFilter.java` (dòng 74)
- **Hiện trạng:**
  ```java
  userDetails.getAuthorities().forEach(a -> log.info(a.getAuthority()));
  ```
- **Vấn đề:** Với mỗi request đi qua filter, dòng này lặp qua toàn bộ role của user và in log ở mức `INFO`. Nếu hệ thống có 1000 req/s và user có 2 roles, sẽ có 2000 dòng log ghi liên tục ra console/file, gây nghẽn CPU và I/O đĩa.
- **Cách khắc phục đề xuất:** Đổi thành `log.debug(...)` hoặc bỏ hoàn toàn dòng này trong môi trường production.

---

### - [ ] 9. `RateLimitFilter` & `RateLimitServiceImpl` thừa các round-trip Redis

- **Vị trí:** `RateLimitFilter.java` và `RateLimitServiceImpl.java`
- **Vấn đề:**
  - Trong `RateLimitFilter.java`:
    1. Gọi `rateLimitService.isBlocked(clientIp)` (1 lần check Redis `EXISTS`)
    2. Gọi `rateLimitService.isAllowed(clientIp, rateLimit)`
  - Bên trong `RateLimitServiceImpl.isAllowed()`:
    1. Lại gọi lại `isBlocked(clientId)` một lần nữa (lãng phí 1 round-trip)
    2. Thực hiện `redisTemplate.opsForValue().increment(key)`
    3. Có thể gọi `redisTemplate.expire(...)`
    4. Khi thêm header, gọi `getRemainingRequests()` (1 lần `GET`)
  - Tổng cộng 4-5 network round-trips tới Redis trên **mỗi HTTP request**.
- **Cách khắc phục đề xuất:**
  - Bỏ lời gọi `isBlocked` thừa bên trong `isAllowed`.
  - Tối ưu bằng cách viết 1 đoạn script Lua chạy nguyên khối trên Redis: kiểm tra block, tăng counter và đặt TTL chỉ trong 1 round-trip duy nhất.

---

### - [ ] 10. `ResumeUploadServiceImpl` dùng `Thread.sleep()` chặn worker thread

- **Vị trí:** `job-backend/src/main/java/com/nlu/applicationProcess/application/impl/ResumeUploadServiceImpl.java` (dòng 234-248 và completeUpload)
- **Hiện trạng:**
  ```java
  for (int i = 0; i < 10; i++) {
      Optional<ResumeUploadSession> session = sessionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
      if (session.isPresent()) {
          return session;
      }
      Thread.sleep(100); // Block luồng 100ms * 10 = tối đa 1s
  }
  ```
- **Vấn đề:**
  - Worker thread của Tomcat bị tạm dừng (sleep).
  - Với thread pool mặc định của Tomcat là 200 luồng, chỉ cần một đợt request concurrent upload cùng lúc sẽ làm cạn kiệt toàn bộ thread pool, gây treo cả hệ thống (Denial of Service).
- **Cách khắc phục đề xuất:**
  - Bỏ vòng lặp sleep. Nếu xảy ra race condition hoặc lock đang bị chiếm, trả về ngay mã lỗi `409 Conflict` (kèm header `Retry-After: 1`) để client phía frontend tự retry, giải phóng thread của server ngay lập tức.

---

### - [ ] 11. `JobServiceImpl.checkExistJob()` nạp toàn bộ entity graph

- **Vị trí:** `job-backend/src/main/java/com/nlu/recruitment/application/impl/JobServiceImpl.java` (dòng 66-68)
- **Hiện trạng:**
  ```java
  @Override
  public Boolean checkExistJob(Long id) {
      return jobRepository.findById(id).isPresent();
  }
  ```
- **Vấn đề:** `findById()` sinh ra câu lệnh `SELECT *` và khởi tạo đối tượng Java đầy đủ cùng các quan hệ. Trong khi mục đích chỉ là kiểm tra tồn tại.
- **Cách khắc phục đề xuất:**
  ```java
  @Override
  public Boolean checkExistJob(Long id) {
      return jobRepository.existsById(id);
  }
  ```
  _(JPA sẽ thực hiện truy vấn tối ưu: `SELECT COUNT(1) WHERE id = ?`)_

---

### - [ ] 12. `ResumeServiceImpl.createResume()` đọc trùng `InputStream`

- **Vị trí:** `job-backend/src/main/java/com/nlu/applicationProcess/application/impl/ResumeServiceImpl.java` (dòng 96-114)
- **Hiện trạng:**
  ```java
  data = fileService.toByteArray(resumeUploadDTO.getFile().getInputStream());
  // ...
  rawText = fileService.extractTextFromFile(resumeUploadDTO.getFile().getInputStream());
  ```
- **Vấn đề:** Gọi `getInputStream()` 2 lần trên cùng 1 `MultipartFile`. Một số servlet container hoặc file lớn sẽ không cho phép đọc lại stream lần thứ hai (stream đã ở vị trí EOF) hoặc phải mở lại file từ ổ đĩa tạm.
- **Cách khắc phục đề xuất:** Tái sử dụng mảng byte `data` đã đọc ở lần 1:
  ```java
  rawText = fileService.extractTextFromFile(new ByteArrayInputStream(data));
  ```

---

### - [ ] 13. Admin Dashboard chứa dữ liệu mẫu và truy vấn đếm riêng lẻ

- **Vị trí:** `AdminDashboardServiceImpl.java` và `AdminJobServiceImpl.java`
- **Vấn đề:**
  - Phương thức `getJobMetrics()` và `getDashboardSummary()` đang hardcode nhiều giá trị (doanh thu `142500.0`, tốc độ tăng trưởng `12.0%`, số ngày tuyển `18 ngày`).
  - `getJobDistribution()` gọi hàm `jobRepository.countByRecordStatus(...)` 3 lần riêng rẽ thay vì gom thành 1 câu `SELECT record_status, COUNT(*) GROUP BY record_status`.
- **Cách khắc phục đề xuất:**
  - Viết câu query `GROUP BY` trong Repository để giảm số lượng query xuống database từ 3 câu thành 1 câu.
  - Kết nối số liệu doanh thu/tăng trưởng thật hoặc ghi chú rõ ràng đây là mock data nếu hệ thống chưa có module thanh toán.

---

### - [ ] 14. `SseEmitterServiceImpl` lưu kết nối trong RAM máy cục bộ

- **Vị trí:** `job-backend/src/main/java/com/nlu/shared/application/impl/SseEmitterServiceImpl.java`
- **Vấn đề:**
  - Dùng `ConcurrentHashMap<Long, SseEmitter>` trong JVM.
  - Khi scale chạy 2 instance backend trở lên, client kết nối SSE ở Instance A sẽ không nhận được thông báo nếu sự kiện được kích hoạt từ Instance B.
- **Cách khắc phục đề xuất:** Khi cần chạy nhiều server (production cluster), cần tích hợp Redis Pub/Sub để broadcast thông báo qua tất cả các server node.

---

## 🗑️ PHẦN III: CODE THỪA & CHẤT LƯỢNG MÃ NGUỒN

### - [ ] 15. `cloudUploadQueue` cấu hình non-durable

- **Vị trí:** `job-backend/src/main/java/com/nlu/shared/infrastructure/config/RabbitMQConfig.java` (dòng 97)
- **Hiện trạng:**
  ```java
  @Bean
  Queue cloudUploadQueue() {
      return new Queue(CLOUD_UPLOAD_QUEUE, false); // false = non-durable
  }
  ```
- **Vấn đề:** Tất cả queue khác đều `durable = true`. Riêng queue upload cloud này lại không durable. Nếu broker RabbitMQ bị restart trong lúc đang có file chờ tải, toàn bộ message trong queue sẽ biến mất.
- **Cách khắc phục đề xuất:** Đổi `false` thành `true`.

---

### - [ ] 16. `BlogServiceImpl.getBlogs()` trả Entity thô và không lọc trạng thái

- **Vị trí:** `job-backend/src/main/java/com/nlu/content/application/impl/BlogServiceImpl.java` (dòng 68-72)
- **Hiện trạng:**
  ```java
  public Page<Blog> getBlogs(final int pageIndex, final int pageSize) {
      Pageable pageable = PageRequest.of(pageIndex, pageSize);
      return blogRepository.findAll(pageable);
  }
  ```
- **Vấn đề:** Trả trực tiếp đối tượng Entity `Blog` ra ngoài API (dễ làm lộ thông tin nhạy cảm của tác giả trong quan hệ LAZY/EAGER), đồng thời không sắp xếp theo ngày tạo và không lọc theo trạng thái `ACTIVE`.
- **Cách khắc phục đề xuất:** Thêm mapper chuyển sang `BlogResponseDTO`, lọc theo `recordStatus = ACTIVE` và sắp xếp `createdAt DESC`.

---

### - [ ] 17. Nuốt ngoại lệ trong `AdminJobServiceImpl.bulkJobAction()`

- **Vị trí:** `job-backend/src/main/java/com/nlu/admin/application/impl/AdminJobServiceImpl.java` (dòng 72-79)
- **Hiện trạng:**
  ```java
  public void bulkJobAction(BulkActionRequest request) {
      for (String jobId : request.getJobIds()) {
          try {
              updateJobStatus(Long.parseLong(jobId), request.getAction());
          } catch (Exception ignored) { // Nuốt sạch lỗi
          }
      }
  }
  ```
- **Vấn đề:** Khi cập nhật hàng loạt thất bại (ví dụ: action không hợp lệ, ID không đúng định dạng), ngoại lệ bị bỏ qua hoàn toàn. Client vẫn nhận mã `200 OK` khiến quản trị viên tưởng rằng hành động đã thành công.
- **Cách khắc phục đề xuất:**
  Thêm log cảnh báo tối thiểu:
  ```java
  log.warn("Failed to apply action {} for job {}: {}", request.getAction(), jobId, e.getMessage());
  ```
  Hoặc trả về danh sách các ID cập nhật thành công và các ID thất bại cho client.

---

### - [ ] 18. Dead code `getInvalidRequestRoleMessage()`

- **Vị trí:** `job-backend/src/main/java/com/nlu/identity/application/impl/AuthServiceImpl.java` (dòng 399-403)
- **Hiện trạng:** Hàm `private String getInvalidRequestRoleMessage(String expectedRole)` không có bất kỳ lời gọi nào trong toàn bộ dự án.
- **Cách khắc phục đề xuất:** Xóa bỏ hàm thừa.

---

### - [ ] 19. Unused dependency trong `JobServiceImpl`

- **Vị trí:** `job-backend/src/main/java/com/nlu/recruitment/application/impl/JobServiceImpl.java` (dòng 49)
- **Hiện trạng:**
  ```java
  private final DefaultRepositoryTagsProvider repositoryTagsProvider;
  ```
- **Vấn đề:** Bean của Spring Actuator này được inject nhưng không hề được sử dụng ở bất kỳ dòng nào trong service.
- **Cách khắc phục đề xuất:** Xóa khai báo biến này để code gọn gàng, tránh inject thừa.

---

### - [ ] 20. Chuỗi forward method thừa trong `JwtServiceImpl`

- **Vị trí:** `job-backend/src/main/java/com/nlu/identity/application/impl/JwtServiceImpl.java` (dòng 113-119)
- **Hiện trạng:**
  ```java
  private String gerenateToken(HashMap<String, Object> claims, String username) {
      return gerenateToken(claims, username, jwtExpiration);
  }
  private String gerenateToken(HashMap<String, Object> claims, String username, long expirationMillis) {
      return buildToken(claims, username, expirationMillis);
  }
  ```
- **Vấn đề:** Method vừa sai chính tả (`gerenate` thay vì `generate`), vừa chỉ làm nhiệm vụ chuyển tiếp gọi sang `buildToken`.
- **Cách khắc phục đề xuất:** Gom gọn trực tiếp vào `buildToken` và sửa tên hàm chuẩn.

---

### - [ ] 21. `SpamServiceImpl` sai chính tả và thiếu xóa block key khi xóa spam email

- **Vị trí:** `job-backend/src/main/java/com/nlu/shared/application/impl/SpamServiceImpl.java` (dòng 91-93)
- **Hiện trạng:**
  ```java
  @Override
  public void deleteInSpamEmail(String ip) {
      spamIp.delete(String.format("ip_spam_email_%s", ip));
  }
  ```
- **Vấn đề:**
  - Tên method bị sai chính tả: `deleteInSpamEmail` thay vì `deleteIpSpamEmail`.
  - Chưa xóa `block_ip_email_%s`.
- **Cách khắc phục đề xuất:** Xóa cả key đếm lẫn key block, đồng thời sửa tên hàm đồng bộ trong Interface.

---

### - [ ] 22. Dùng `e.printStackTrace()` trong `RefreshTokenServiceImpl`

- **Vị trí:** `job-backend/src/main/java/com/nlu/identity/application/impl/RefreshTokenServiceImpl.java` (dòng 54)
- **Hiện trạng:** Gọi `e.printStackTrace()` in trực tiếp ra console chuẩn mà không qua cấu hình định dạng log của Logback.
- **Cách khắc phục đề xuất:** Đổi thành `log.error("Refresh token validation failed: {}", e.getMessage(), e);`.

---

### - [ ] 23. Dùng `System.err.println()` trong `JobServiceImpl`

- **Vị trí:** `job-backend/src/main/java/com/nlu/recruitment/application/impl/JobServiceImpl.java` (dòng 205)
- **Hiện trạng:**
  ```java
  System.err.println("Unrecognized EmploymentType in DB: " + timeStr);
  ```
- **Cách khắc phục đề xuất:** Đổi thành `log.warn("Unrecognized EmploymentType in DB: {}", timeStr);`.

---

### - [ ] 24. Frontend `auth.service.ts` hàm `login()` thiếu xử lý lỗi

- **Vị trí:** `job-frontend/src/app/core/services/auth.service.ts` (dòng 72-85)
- **Hiện trạng:**
  ```typescript
  login(body: any) {
      this.http.post<ApiResponse<string>>(`${this.url}/auth/login`, body, {
          withCredentials: true,
      }).pipe(
          take(1),
          map((res) => {
              this.tokenService.setToken(res.data);
              this.setLoggedIn(true);
          })
      ).subscribe();
  }
  ```
- **Vấn đề:** Dùng `.subscribe()` dạng fire-and-forget mà không có callback `error: (err) => ...`, không trả về `Observable` cho component gọi. Trong khi hàm `hirerLogin()` ngay phía dưới lại có xử lý đầy đủ.
- **Cách khắc phục đề xuất:** Trả về `Observable` hoặc bắt lỗi trong pipe để component hiển thị thông báo lỗi lên giao diện đăng nhập khi mật khẩu sai.

---

### - [ ] 25. Duplicate logic `getLoginUrl()` ở 3 vị trí trên Frontend

- **Vị trí:**
  - `job-frontend/src/app/core/interceptors/refresh-token.interceptor.ts` (dòng 46-55)
  - `job-frontend/src/app/core/interceptors/error.interceptor.ts` (dòng 19-28)
  - `job-frontend/src/app/core/services/auth.service.ts` (dòng 231-243)
- **Vấn đề:** Cả 3 nơi đều cùng kiểm tra URL hiện tại xem là người tìm việc hay nhà tuyển dụng để redirect về đúng trang đăng nhập tương ứng:
  ```typescript
  return currentUrl.includes("/hirer") ? "/auth/login-hirer" : "/auth/login";
  ```
- **Cách khắc phục đề xuất:** Đưa logic này thành 1 hàm dùng chung trong `auth.service.ts` hoặc 1 helper function và export ra để tái sử dụng.

---

_Tài liệu được khởi tạo ngày 21/09/2026 bởi Antigravity Code Audit System._
