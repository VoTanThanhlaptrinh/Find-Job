# Tổng hợp thay đổi & Các điểm quan trọng — Hệ thống Hot-GET Cache (Giải pháp 2 + 5)

Tài liệu này tổng hợp toàn bộ những thành phần đã thêm mới và chỉnh sửa trong `job-backend` khi hiện thực **Giải pháp 2 (2-Tier Filter + `UserDetails` Redis Cache)** kết hợp **Giải pháp 5 (Single-Flight Lock + Nén Gzip + `X-Cache` Header)** để bạn preview nhanh.

---

## 1. Luồng xử lý tổng thể đã hiện thực

```mermaid
flowchart TD
    Req["HTTP Request"] --> RL["RateLimitFilter"]
    RL --> PubCache["PublicHotGetCacheFilter (PUBLIC GET)"]
    PubCache -->|HIT| Fast200["Trả 200 ngay từ Redis (bỏ qua JwtFilter & DB)"]
    PubCache -->|COUNTING / MISS / Không thuộc PUBLIC| Jwt["JwtFilter"]

    Jwt --> UCache{"UserDetailsCacheService\n(auth:user:{email})"}
    UCache -->|HIT Redis| AuthCtx["Set SecurityContext (không query DB)"]
    UCache -->|MISS Redis| DBUser["UserRepositoryDetailsService -> Cache 300s"]

    AuthCtx --> Authz["Spring AuthorizationFilter (check 401/403)"]
    DBUser --> Authz

    Authz --> ProtCache["ProtectedHotGetCacheFilter"]
    ProtCache -->|GET ROLE/USER + HIT| FastAuth200["Trả 200 từ Redis"]
    ProtCache -->|GET < 5 lần /60s| Counting["Header X-Cache: COUNTING -> Controller"]
    ProtCache -->|GET >= 5 lần (MISS)| Lock{"Single-Flight Lock\nSET NX EX 5s"}
    Lock -->|Có lock| Ctrl["Chạy Controller -> Gzip (>2KB) -> Lưu Redis + ZSET quota"]
    Lock -->|Không có lock| Wait["Chờ tối đa 3x50ms đọc lại Redis -> HIT hoặc BYPASS"]
    ProtCache -->|POST/PUT/PATCH/DELETE 2xx| Inval["Invalidate rule/tag tương ứng + Event"]
```

---

## 2. Những điểm quan trọng cần chú ý khi preview

### 2.1. An toàn bảo mật & Cô lập dữ liệu (Chống Auth Bypass & IDOR)
1. **Tách đôi Filter theo đúng ranh giới Security** trong [SecurityConfig.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/infrastructure/config/SecurityConfig.java#L80-L87):
   - `PublicHotGetCacheFilter` chạy **sau `RateLimitFilter` và trước `JwtFilter`**: chỉ phục vụ rule có `scope: PUBLIC`. Request public hot sẽ trả về ngay mà không tốn chi phí parse JWT hay hit Controller.
   - `ProtectedHotGetCacheFilter` chạy **sau `AuthorizationFilter`**: chỉ phục vụ rule `ROLE` và `USER`, đồng thời hứng các write request (`POST`, `PUT`, `PATCH`, `DELETE`) trả về `2xx` để tự động xóa cache. Nhờ đứng sau `AuthorizationFilter`, mọi request `401 Unauthorized` / `403 Forbidden` đều bị Spring Security chặn trước, không bao giờ đọc được cache của người khác.
2. **Không bao giờ dùng chung cache giữa các user (`USER` scope)**:
   - Key cache cho `USER` gắn `:user_{userId}:` lấy từ `Authentication.getPrincipal()` ([HotCacheKeyBuilder.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheKeyBuilder.java#L98-L110)).
   - Key cho `ROLE` gắn `:role_{sortedAuthorities}:` (ví dụ quản trị viên `ROLE_ADMIN`).
3. **Tách biệt ngôn ngữ i18n**: Key luôn gắn `LocaleContextHolder.getLocale().getLanguage()` (`vi`, `en`...) nên đổi ngôn ngữ không bị trả nhầm bản dịch cũ ([HotCacheKeyBuilder.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheKeyBuilder.java#L66-L74)).
4. **Blacklist ưu tiên tuyệt đối**: `excluded-patterns` (17 pattern nhạy cảm như `/api/account/oauth2`, `/api/admin/employers/export`, `/api/user/resumes/*/view`, `/api/sse/**`, `/api/auth/active/**`, ...) luôn được kiểm tra **trước** danh sách `rules` ([HotCacheKeyBuilder.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheKeyBuilder.java#L38-L41)). Các rule cụ thể (`/api/blogs/me`, `/api/jobs/newest`, `/api/admin/employers/metrics`, ...) được đặt **trước** rule wildcard `{id}` trong [application.yml](file:///d:/web-project/job-backend/src/main/resources/application.yml).

### 2.2. `UserDetails` Cache trong `JwtFilter` & Lưu ý Hibernate Detached Entity
1. **Không lưu Password Hash lên Redis**: `UserDetailsCacheServiceImpl` chỉ serialize record [CachedUserDetails.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/impl/CachedUserDetails.java) (bỏ qua `password`, để `null`) dưới key `auth:user:{email}` (TTL mặc định `300s`). Khi HIT Redis, `toDetachedUser()` dựng lại `User` entity để tương thích `@CurrentUser` (`CurrentUserArgumentResolver`) và check `isEnabled()`, `isAccountLocked()`, `isActive()` trong [JwtFilter.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/infrastructure/filter/JwtFilter.java#L100-L110).
2. **Bảo vệ luồng sửa Account khỏi password `null` / collection rỗng**: Vì `User` từ Redis có `password = null` và `applies/resumes` rỗng, [AccountServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/impl/AccountServiceImpl.java#L60-L108) đã được sửa để **luôn load lại `User persisted = userRepository.findById(user.getId()).orElse(user)`** trước khi kiểm tra mật khẩu hoặc save thay đổi, đồng thời gọi `evictByEmail`. `DaoAuthenticationProvider` (login bằng mật khẩu) vẫn dùng trực tiếp `UserRepositoryDetailsService` để giữ nguyên xác thực DB.
3. **Evict `auth:user:{email}` tức thời khi trạng thái user thay đổi**:
   - Đổi mật khẩu / cập nhật profile ([AccountServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/impl/AccountServiceImpl.java))
   - Kích hoạt tài khoản / reset password ([AuthServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/impl/AuthServiceImpl.java))
   - Admin sửa/xóa user ([UserCrudServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/application/impl/UserCrudServiceImpl.java))
   - Admin suspend/activate employer ([AdminEmployerServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/application/impl/AdminEmployerServiceImpl.java#L83-L112))

### 2.3. Tối ưu RAM Redis, Chống Stampede & Tránh phá `@WebMvcTest`
1. **Ngưỡng đếm + Quota ZSET**:
   - Chỉ bắt đầu cache sau khi endpoint+param đạt `>= 5` request trong `60s` (`INCR` + `EXPIRE` trên key `http_cache:counter:...`).
   - Mỗi rule có chỉ mục ZSET (`http_cache:index:{ruleId}`) lưu `entryKey` theo timestamp hết hạn; khi vượt `max-entries` sẽ tự dọn key hết hạn (`removeRangeByScore`) rồi đẩy key cũ nhất (`ZPOPMIN`) và xóa payload khỏi Redis ([HotCacheService.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheService.java#L79-L167)).
2. **Nén Gzip & Trần kích thước body**:
   - [HotCachePayloadCodec.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCachePayloadCodec.java) lưu binary header (`4 byte status` + `1 byte flag gzip` + `2 byte length` + `Content-Type` + `body`), tự nén Gzip nếu body `> 2048 bytes` và thực sự nhỏ hơn bản gốc; body `> 65536 bytes` sẽ tự động `BYPASS`.
   - `cacheByteRedisTemplate` dùng `RedisSerializer.byteArray()` thay cho JSON serializer để tránh double-escape JSON ([RedisConfig.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/config/RedisConfig.java#L80-L94)).
3. **Single-Flight Lock (Giải pháp 5)**:
   - Khi vừa chạm ngưỡng miss ở lần thứ 5, chỉ 1 request lấy được lock `SET NX EX 5s` đi gọi Controller; các request trùng tới cùng lúc chờ tối đa `3 x 50ms` rồi đọc lại cache vừa ghi (`HIT`) ([HotGetCacheSupport.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotGetCacheSupport.java#L76-L130)).
4. **Không gắn `@Component` trực tiếp lên 2 Filter**:
   - `PublicHotGetCacheFilter` và `ProtectedHotGetCacheFilter` được tạo trong [HotCacheFilterConfig.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheFilterConfig.java) với `@ConditionalOnBean(HotCacheService.class)` và tắt `FilterRegistrationBean` để không tự chạy 2 lần ở servlet container và **không làm gãy các `@WebMvcTest` slice test**.
5. **Tích hợp Invalidation cho Legacy Cache & Async RabbitMQ Consumer**:
   - [CategoryCacheService.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/infrastructure/cache/CategoryCacheService.java#L48-L56) lắng nghe `HotCacheInvalidatedEvent` để xóa `cache:home:init` và `cache:home:category` đồng bộ với HTTP cache.
   - [ResumeParsingConsumer.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/applicationProcess/infrastructure/message/ResumeParsingConsumer.java#L80-L95) xóa cache CV (`user_resumes`, `user_resume_detail`, `user_jobs_match`) khi phân tích xong `READY` hoặc `ANALYSIS_FAILED`.
   - [JobVectorizationConsumer.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/infrastructure/message/JobVectorizationConsumer.java#L47-L88) xóa `hirer_jobs_posted` và `jobs_search` / `jobs_newest` sau khi vectorize JD xong.

---

## 3. Danh sách file MỚI được thêm vào project

### Core Hot-GET HTTP Cache (`com.nlu.shared.infrastructure.cache`)
| File | Chức năng chính |
| :--- | :--- |
| [HotCacheConstants.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheConstants.java) | Hằng số header `X-Cache` (`HIT`, `MISS-STORED`, `COUNTING`, `BYPASS`), prefix Redis key, thông số mặc định và danh sách query param tracking bị bỏ qua (`utm_*`, `fbclid`, `_ts`...) |
| [HotCacheScope.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheScope.java) | Enum phạm vi cache: `PUBLIC`, `ROLE`, `USER` |
| [HotCacheProperties.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheProperties.java) | Mapping `@ConfigurationProperties(prefix = "app.hot-cache")` (cấu hình toàn cục, `excludedPatterns`, danh sách `rules`) |
| [CachedHttpResponse.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/CachedHttpResponse.java) | Record `(int httpStatus, String contentType, byte[] body)` |
| [HotCachePayloadCodec.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCachePayloadCodec.java) | Encode/decode nhị phân + tự động nén Gzip khi body `> gzipThresholdBytes` |
| [HotCacheKeyContext.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheKeyContext.java) | Giữ trọn bộ `entryKey`, `counterKey`, `lockKey`, `indexKey`, `scopeSegment` cho 1 request khớp rule |
| [HotCacheInvalidatedEvent.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheInvalidatedEvent.java) | Spring ApplicationEvent `(String ruleId)` phát khi rule bị evict |
| [HotCacheKeyBuilder.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheKeyBuilder.java) | Match URL theo thứ tự blacklist -> rules, chuẩn hóa query param (sort + bỏ tracking param), kiểm tra ngưỡng `page`/`size`, tách `:r[var=val]` & locale |
| [HotCacheService.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheService.java) | Quản lý counter `INCR/EXPIRE`, Single-Flight lock `SET NX EX`, ghi cache kèm ZSET quota eviction, invalidation theo `[var=val]`, scope user hoặc `[all]` |
| [HotGetCacheSupport.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotGetCacheSupport.java) | Engine dùng chung cho 2 Filter: đọc cache, tăng counter, giữ Single-Flight lock, bọc/tái sử dụng `ContentCachingResponseWrapper` |
| [PublicHotGetCacheFilter.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/PublicHotGetCacheFilter.java) | Filter tầng 1 cho GET có `scope == PUBLIC` |
| [ProtectedHotGetCacheFilter.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/ProtectedHotGetCacheFilter.java) | Filter tầng 2 cho GET `ROLE`/`USER` và auto-invalidation khi `POST/PUT/PATCH/DELETE` trả về `2xx` |
| [HotCacheFilterConfig.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/cache/HotCacheFilterConfig.java) | Khai báo bean Filter an toàn cho `@WebMvcTest` và tắt double-registration ngoài Spring Security |

### `UserDetails` Redis Cache (`com.nlu.identity`)
| File | Chức năng chính |
| :--- | :--- |
| [UserDetailsCacheProperties.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/infrastructure/config/UserDetailsCacheProperties.java) | Cấu hình `app.user-details-cache` (`enabled = true`, `ttlSeconds = 300`) |
| [UserDetailsCacheService.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/UserDetailsCacheService.java) | Interface `loadUserByUsername(email)` và `evictByEmail(email)` |
| [CachedUserDetails.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/impl/CachedUserDetails.java) | DTO không chứa password lưu Redis và tái tạo detached `User` |
| [UserDetailsCacheServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/impl/UserDetailsCacheServiceImpl.java) | Read-through Redis cache `auth:user:{email}` sử dụng `cacheByteRedisTemplate` |

### Unit / Integration Test mới tạo
| File | Nội dung kiểm thử |
| :--- | :--- |
| [HotGetCacheFilterTest.java](file:///d:/web-project/job-backend/src/test/java/com/nlu/shared/infrastructure/cache/HotGetCacheFilterTest.java) | Threshold = 5, query normalization, locale separation, `page/size` & blacklist bypass, Gzip + `maxBodyBytes`, ZSET `ZPOPMIN`, bảo mật `ROLE`/`USER` (401/403 & chống leak chéo user), write auto-invalidation (`[id]` & `[all]`), Single-Flight lock |
| [UserDetailsCacheServiceImplTest.java](file:///d:/web-project/job-backend/src/test/java/com/nlu/identity/application/impl/UserDetailsCacheServiceImplTest.java) | Cache HIT/MISS `auth:user:{email}`, xác nhận không lưu password hash, `evictByEmail`, bật/tắt cờ `enabled`, `JwtFilter` ưu tiên `UserDetailsCacheService` |

---

## 4. Danh sách file ĐÃ CHỈNH SỬA trong project

| File | Nội dung chỉnh sửa cụ thể |
| :--- | :--- |
| [RedisConfig.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/shared/infrastructure/config/RedisConfig.java#L75-L94) | Thêm bean `stringRedisTemplate` (cho counter, lock, ZSET) và `cacheByteRedisTemplate` (lưu binary/Gzip byte array không double-serialize) |
| [SecurityConfig.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/infrastructure/config/SecurityConfig.java#L54-L114) | Đăng ký `publicHotGetCacheFilter` sau `RateLimitFilter` (trước `JwtFilter`), `protectedHotGetCacheFilter` sau `AuthorizationFilter`, và thêm `X-Cache` vào CORS `exposedHeaders` |
| [JwtFilter.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/infrastructure/filter/JwtFilter.java#L38-L105) | Thêm `@Autowired` constructor 3 tham số nhận `@Nullable UserDetailsCacheService` (giữ nguyên constructor 2 tham số cho test cũ), ưu tiên load user từ Redis cache |
| [AccountServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/impl/AccountServiceImpl.java#L37-L108) | Inject `@Autowired(required = false) UserDetailsCacheService`, reload entity từ `userRepository.findById` trước khi đổi mật khẩu/sửa profile/check OAuth2, và evict cache `auth:user:{email}` |
| [AuthServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/identity/application/impl/AuthServiceImpl.java) | Evict `UserDetailsCacheService` khi `activeAccount` và `resetPassword`; đồng thời evict `admin_users_page` qua `HotCacheService` khi kích hoạt tài khoản |
| [UserCrudServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/application/impl/UserCrudServiceImpl.java) | Evict `UserDetailsCacheService` (kể cả email cũ nếu admin đổi email) khi `updateUser` hoặc `deleteUser` |
| [AdminEmployerServiceImpl.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/admin/application/impl/AdminEmployerServiceImpl.java#L84-L112) | Evict `UserDetailsCacheService` khi admin `suspend` hoặc `activate`/`restore` tài khoản nhà tuyển dụng |
| [CategoryCacheService.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/infrastructure/cache/CategoryCacheService.java#L48-L56) | Thêm `@EventListener onHotCacheInvalidated` để xóa đồng bộ `cache:home:init` / `cache:home:category` |
| [ResumeParsingConsumer.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/applicationProcess/infrastructure/message/ResumeParsingConsumer.java#L80-L95) | Tự động evict cache `user_resumes`, `user_resume_detail`, `user_jobs_match` khi RabbitMQ parse CV xong hoặc lỗi |
| [JobVectorizationConsumer.java](file:///d:/web-project/job-backend/src/main/java/com/nlu/recruitment/infrastructure/message/JobVectorizationConsumer.java#L47-L88) | Tự động evict `hirer_jobs_posted`, `jobs_search`, `jobs_newest` sau khi vectorize JD hoàn tất |
| [application.yml](file:///d:/web-project/job-backend/src/main/resources/application.yml) | Bổ sung cấu hình `app.user-details-cache` và `app.hot-cache` với 17 `excluded-patterns` và 42 `rules` |
| [SecurityConfigCorsTest.java](file:///d:/web-project/job-backend/src/test/java/web_application/security/SecurityConfigCorsTest.java) | Cập nhật constructor `new SecurityConfig(...)` bổ sung mock `PublicHotGetCacheFilter` và `ProtectedHotGetCacheFilter` |

---

## 5. Tóm tắt cấu hình đang bật trong `application.yml`

- **`app.user-details-cache`**: `enabled: true`, `ttl-seconds: 300` (5 phút).
- **`app.hot-cache` thông số toàn cục**:
  - `enabled: true`
  - `default-threshold: 5` lần trong `default-window-seconds: 60` giây
  - `max-body-bytes: 65536` (64 KB), `gzip-threshold-bytes: 2048` (nén Gzip khi `> 2 KB`)
  - `lock-ttl-seconds: 5`, `single-flight-wait-ms: 50`, `single-flight-max-wait-retries: 3`
  - `max-page: 10`, `max-size: 50` (quá ngưỡng phân trang sâu sẽ `BYPASS`)
- **Phạm vi rule**: Tổng cộng **42 rules** (11 nhóm A `PUBLIC`, 16 nhóm B `ROLE` Admin, 15 nhóm C `USER` cá nhân) + **17 `excluded-patterns`** (blacklist).

---

## 6. Trạng thái kiểm chứng & Cách tự kiểm tra nhanh khi preview

- **Trạng thái compile/test hiện tại**: Mã nguồn và 2 file unit/integration test mới ([HotGetCacheFilterTest.java](file:///d:/web-project/job-backend/src/test/java/com/nlu/shared/infrastructure/cache/HotGetCacheFilterTest.java), [UserDetailsCacheServiceImplTest.java](file:///d:/web-project/job-backend/src/test/java/com/nlu/identity/application/impl/UserDetailsCacheServiceImplTest.java)) **chưa được chạy** lệnh Maven nào trong phiên này (đang giữ đúng quy tắc an toàn lệnh chờ xác nhận từ bạn).
- **Lệnh kiểm thử sau khi bạn sẵn sàng chạy** (tại thư mục `d:\web-project\job-backend`):
  ```powershell
  .\mvnw.cmd test
  ```
- **Cách quan sát khi chạy API thực tế (Postman / Browser DevTools)**:
  - Kiểm tra response header `X-Cache`:
    - Lần 1 → 4: `COUNTING`
    - Lần 5: `MISS-STORED`
    - Lần 6 trở đi (trong TTL & quota): `HIT`
    - Vượt `page > 10` / `size > 50` hoặc body `> 64KB`: `BYPASS`
    - Endpoint nằm trong blacklist hoặc chưa đủ điều kiện cache: không có header `X-Cache`
  - Kiểm tra key trong Redis: `http_cache:counter:*`, `http_cache:entry:*`, `http_cache:index:*`, `http_cache:lock:*`, `auth:user:*`.

