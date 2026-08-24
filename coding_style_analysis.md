# 📋 Phân Tích Chi Tiết Phong Cách & Thói Quen Viết Code

> **Dự án**: Job Portal Web (Find-Job)
> **Công nghệ**: Angular 20 (Frontend) + Spring Boot 3 (Backend)
> **Ngày phân tích**: 05/08/2026
> **Phạm vi**: Toàn bộ source code trong `job-frontend/` và `job-backend/`

---

## Mục Lục

1. [Tổng Quan Kiến Trúc](#1-tổng-quan-kiến-trúc)
2. [Backend — Java/Spring Boot](#2-backend--javaspring-boot)
3. [Frontend — Angular/TypeScript](#3-frontend--angulartypescript)
4. [Quy Ước Đặt Tên](#4-quy-ước-đặt-tên)
5. [Xử Lý Lỗi & Logging](#5-xử-lý-lỗi--logging)
6. [Quốc Tế Hóa (i18n)](#6-quốc-tế-hóa-i18n)
7. [Bảo Mật & Authentication](#7-bảo-mật--authentication)
8. [Design System & UI](#8-design-system--ui)
9. [Quản Lý State](#9-quản-lý-state)
10. [Thói Quen Đáng Chú Ý](#10-thói-quen-đáng-chú-ý)
11. [Tổng Kết Điểm Mạnh & Điểm Cần Cải Thiện](#11-tổng-kết-điểm-mạnh--điểm-cần-cải-thiện)

---

## 1. Tổng Quan Kiến Trúc

### 1.1. Cấu Trúc Monorepo

Dự án được tổ chức thành 2 thư mục chính trong cùng một repository:

```
web-project/
├── job-backend/        ← Spring Boot 3 (Java)
├── job-frontend/       ← Angular 20 (TypeScript)
├── README.md
├── DESIGN.md
└── chuc_nang_hoat_dong.md
```

> [!NOTE]
> Bạn viết documentation bằng cả tiếng Việt (`chuc_nang_hoat_dong.md`) lẫn tiếng Anh (`README.md`, `DESIGN.md`), thể hiện thói quen **ghi chép chi tiết** và có ý thức về việc duy trì tài liệu cho dự án.

### 1.2. Backend — Domain-Driven Design (DDD)

Backend được chia thành các **Bounded Context** rõ ràng, mỗi context đều tuân theo cùng một cấu trúc phân lớp nhất quán:

```
com.nlu/
├── identity/           ← Quản lý user, auth, JWT
├── recruitment/        ← Quản lý job, company, category
├── applicationProcess/ ← Quản lý apply, resume, CV parsing
├── content/            ← Blog, comment, like
├── admin/              ← Dashboard admin
└── shared/             ← Shared kernel: exception, model, utils, config
```

Mỗi bounded context đều có **5 layer** giống nhau:

| Layer | Chức năng | Ví dụ |
|-------|-----------|-------|
| `api/` | REST Controller + DTO | `AuthController.java`, `LoginDTO.java` |
| `application/` | Service interface + impl | `AuthService.java`, `AuthServiceImpl.java` |
| `domain/` | Entity, Repository, Value Object, Event | `User.java`, `EmailAddress.java` |
| `infrastructure/` | Config, Filter, Validation | `SecurityConfig.java`, `JwtFilter.java` |
| `mapper/` | Object mapping (DTO ↔ Entity) | `RegistrationFormMapper.java` |

### 1.3. Frontend — Feature-Based Architecture

Frontend theo mô hình **Feature-Based** chuẩn Angular:

```
src/app/
├── core/               ← Singleton services, guards, interceptors, layout, i18n
├── features/           ← Feature modules (auth, home, jobs, recruiter, admin, blog, candidate, site)
└── shared/             ← Shared components, models, pipes, utils
```

Mỗi feature có cấu trúc con riêng:

```
features/<feature-name>/
├── pages/              ← Page-level components
├── services/           ← Feature-specific services
└── components/         ← Feature-specific reusable components (nếu cần)
```

---

## 2. Backend — Java/Spring Boot

### 2.1. Controller Pattern

Bạn có một **mẫu controller cực kỳ nhất quán** xuyên suốt toàn bộ project:

```java
@RestController
@RequestMapping(path = "/api/<path>", produces = "application/json")
@RequiredArgsConstructor
public class SomeController {
    private final SomeService someService;

    @GetMapping("/endpoint")
    public ResponseEntity<ApiResponse<T>> methodName(...) {
        T data = someService.doSomething(...);
        return ResponseEntity.ok(
            new ApiResponse<>(MessageUtils.getMessage("message.key"), data, HttpStatus.OK.value())
        );
    }
}
```

**Đặc điểm nổi bật:**

| Pattern | Mô tả |
|---------|-------|
| `@RequiredArgsConstructor` | **Luôn dùng** constructor injection qua Lombok, không bao giờ `@Autowired` |
| `produces = "application/json"` | Khai báo rõ ràng content type ở `@RequestMapping` |
| `ResponseEntity<ApiResponse<T>>` | **Mọi endpoint** đều trả về cùng wrapper `ApiResponse` |
| `MessageUtils.getMessage()` | **Tất cả message** đều thông qua i18n, không hardcode chuỗi |
| `@Valid` | Luôn validate DTO đầu vào với Jakarta Validation |
| `@CurrentUser User` | Custom annotation để inject user hiện tại |

### 2.2. Service Layer Pattern

Service layer luôn theo mô hình **Interface + Implementation**:

```java
// Interface (application/)
public interface JobService {
    void createJob(JobDto jobDTO, User user);
    void updateJob(Long id, JobDto jobDTO, User user);
    void deleteJob(Long id, User user);
}

// Implementation (application/impl/)
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)      // ← Class-level read-only
public class JobServiceImpl implements JobService {

    @Override
    @Transactional                    // ← Method-level write transaction
    public void createJob(JobDto jobDTO, User user) {
        try {
            MDC.put("userId", String.valueOf(user.getId()));
            // ... business logic ...
            log.info("Job post created — job: {}, title: {}", job.getId(), job.getTitle());
        } finally {
            MDC.remove("userId");
        }
    }
}
```

**Thói quen trong service:**

- `@Transactional(readOnly = true)` ở class-level, `@Transactional` ở method-level cho write operations
- **MDC context** (`try { MDC.put(...) } finally { MDC.remove(...) }`) để tracking user/entity trong logs
- `@Slf4j` + structured logging với `log.info()` / `log.warn()` / `log.error()`
- Tách **query** (read) và **command** (write) service: `JobQueryService` vs `JobService`

### 2.3. Entity / Domain Model Pattern

Entities kế thừa từ `BaseEntity` với auditing tự động:

```java
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "job")
@SQLRestriction("record_status <> 'DELETED'")  // ← Soft delete filter
public class Job extends BaseEntity {
    @Id
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    // ...

    // Domain behavior — validation trong setter
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new BadRequestException(MessageUtils.getMessage("validation.job.name.required"));
        }
        this.title = title;
    }

    // Domain behavior — business rule
    public boolean isOwnedBy(Recruitment h) {
        return recruitment != null && h != null && recruitment.getId() == h.getId();
    }
}
```

**Đặc điểm entity:**

| Pattern | Mô tả |
|---------|-------|
| `BaseEntity` | `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `recordStatus` — audit tự động |
| `@SQLRestriction` | Soft delete — filter record `DELETED` ở JPA level |
| Validation trong setter | Domain model tự validate, không phụ thuộc service |
| `markDeleted()` | Soft delete behavior trong entity |
| `@Embedded` Value Objects | `EmailAddress`, `Password`, `PhoneNumber`, `ExperienceYears`, `SocialLink` |

### 2.4. Value Object Pattern

Bạn sử dụng Value Objects (VOs) cho dữ liệu có constraint:

```java
@Embeddable
@Getter
@EqualsAndHashCode
public class EmailAddress {
    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@...");

    @Column(name = "email", length = 255)
    private String value;

    protected EmailAddress() {} // JPA

    public EmailAddress(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(MessageUtils.getMessage("validation.email.required"));
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new BadRequestException("Invalid email format");
        }
        this.value = value;
    }
}
```

**VOs được sử dụng**: `EmailAddress`, `Password` (ẩn value khi toString), `PhoneNumber`, `ExperienceYears`, `SocialLink`

### 2.5. DTO Pattern

DTOs dùng **Java Record** — bạn ưa thích kiểu immutable, concise:

```java
public record LoginDTO(
    @NotBlank(message = "{validation.username.required}") String username,
    @NotBlank(message = "{validation.password.required}") String password
) {
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
```

> [!NOTE]
> Bạn thêm getter method tường minh trong record mặc dù record đã có accessor method sẵn. Đây có thể là thói quen để tương thích ngược hoặc framework binding.

### 2.6. API Response Wrapper

Mọi API response đều qua một wrapper duy nhất:

```java
public record ApiResponse<T>(
    String message,
    T data,
    int status,
    String traceId
) { ... }
```

### 2.7. Exception Hierarchy

Exception hierarchy rõ ràng, mỗi loại lỗi map đến HTTP status riêng:

```
AppException (base) ← HttpStatus tùy ý
├── BadRequestException    ← 400
├── UnauthorizedException  ← 401
├── ForbiddenException     ← 403
└── ResourceNotFoundException ← 404
```

Global handler `@RestControllerAdvice` catch tất cả, trả về `ApiResponse` + `traceId` từ MDC.

### 2.8. Configuration & Environment

- Dùng **dotenv** (`Dotenv.configure().ignoreIfMissing().load()`) để load biến môi trường
- Phân tách profile: `application.yml` (common) → `application-dev.yml` / `application-prod.yml`
- Tất cả secrets qua environment variables: `${DB_URL}`, `${SECRET_KEY}`, etc.
- Bật **Spring Features**: `@EnableAsync`, `@EnableScheduling`, `@EnableJpaAuditing`

---

## 3. Frontend — Angular/TypeScript

### 3.1. Component Pattern

Tất cả component đều dùng **Standalone Component** (Angular 14+):

```typescript
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  standalone: true,
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  // ...
}
```

**Đặc điểm:**

| Pattern | Mô tả |
|---------|-------|
| `standalone: true` | 100% standalone components, không dùng NgModule |
| `inject()` function | Ưu tiên `inject()` + `private readonly` cho DI mới, nhưng cũng dùng constructor injection |
| Reactive Forms | `FormBuilder.nonNullable.group()` cho form xử lý |
| `@Input({ required: true })` | Sử dụng required input decorator |
| Angular new control flow | Dùng `@if`, `@for` (Angular 17+ syntax) thay vì `*ngIf`, `*ngFor` |

### 3.2. Service Pattern

Services đều `providedIn: 'root'` (singleton) và sử dụng **Signals** rất tích cực:

```typescript
@Injectable({ providedIn: 'root' })
export class JobService {
  // Private writable signals
  private jobDetail = signal<JobDetailViewModel | null>(null);
  private isLoadingJobDetail = signal<boolean>(false);

  // Public read-only computed signals
  readonly jobDetail$ = computed(() => this.jobDetail());
  readonly isLoadingJobDetail$ = computed(() => this.isLoadingJobDetail());

  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  getDetailJob(id: string): void {
    this.isLoadingJobDetail.set(true);
    this.http.get<JobDetailApiResponse>(`${this.url}/jobs/${id}`).pipe(
      take(1),
      finalize(() => this.isLoadingJobDetail.set(false))
    ).subscribe({
      next: (response) => { this.jobDetail.set(response.data); },
      error: (error) => { /* handle error */ }
    });
  }
}
```

**Pattern rõ ràng:**

- **Signal-based state**: `signal()` cho private state, `computed()` cho public read-only
- Hậu tố `$` cho computed signals (convention riêng)
- `take(1)` trên hầu hết HTTP calls để tự unsubscribe
- `finalize()` để reset loading state
- `subscribe({ next, error })` object pattern
- URL base từ `UtilitiesService.getURLDev()` — centralized API URL management

### 3.3. Routing Pattern

**Lazy loading** triệt để:

```typescript
// Component lazy load
{ path: 'login', loadComponent: () =>
    import('./features/auth/pages/login/login.component')
      .then(c => c.LoginComponent)
}

// Module lazy load
{ path: 'recruiter',
  loadChildren: () =>
    import('./features/recruiter/recruiter.routes')
      .then(m => m.recruiterRoutes)
}
```

**Đặc điểm routing:**
- `loadComponent` cho single page, `loadChildren` cho feature module lớn
- **Legacy redirect support**: giữ redirect cho URL cũ → URL mới
- `canActivateChild` + custom guards cho protected routes
- Comment bằng tiếng Việt giải thích từng nhóm route

### 3.4. Interceptor Pattern

Dùng **functional interceptors** (Angular 15+):

```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifyMessageService = inject(NotifyMessageService);
  // ...
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // handle error
      return throwError(() => error);
    })
  );
};
```

**3 interceptors theo thứ tự:**
1. `loggerInterceptor` — log request
2. `errorInterceptor` — hiển thị lỗi, force logout khi 403
3. `refreshTokenInterceptor` — auto refresh token khi 401, retry request

### 3.5. Guard Pattern

Guards dùng **functional guard** pattern:

```typescript
export const hirerGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthReady()) return true; // allow while restoring
  return hasHirerRole(tokenService) ? true : buildUnauthorizedRedirect(router, state);
};
```

**Đặc điểm guard:**
- Helper functions thuần (`hasHirerRole`, `buildUnauthorizedRedirect`) thay vì logic inline
- `logGuardDecision()` — luôn log quyết định guard để debug
- `isAuthReady()` check — cho phép navigation tạm khi auth đang restoring

### 3.6. Model / Type Pattern

Frontend models dùng **TypeScript interface** + **type alias**:

```typescript
// Interface cho data shape
export interface JobCardModel {
  id: string | number;
  title: string;
  address: string;
  salary: number;
  time: string;
}

// Type alias cho API response
export type JobListApiResponse = ApiResponse<PagedPayload<JobCardModel>>;
export type JobDetailApiResponse = ApiResponse<JobDetailViewModel>;
```

**Pattern:** Interface cho data model, Type alias cho composed response types.

---

## 4. Quy Ước Đặt Tên

### 4.1. Backend (Java)

| Phần tử | Quy ước | Ví dụ |
|---------|---------|-------|
| Package | camelCase (bounded context) | `applicationProcess`, `identity`, `recruitment` |
| Controller | `<Scope><Feature>Controller` | `UserAuthController`, `HirerJobController`, `PublicJobController` |
| Service interface | `<Feature>Service` | `AuthService`, `JobService`, `JobQueryService` |
| Service impl | `<Feature>ServiceImpl` | `AuthServiceImpl`, `JobServiceImpl` |
| DTO (immutable) | Java Record | `LoginDTO`, `RegistrationForm`, `ForgotPassDTO` |
| DTO (mutable) | Lombok class | `UserInfo`, `JobDto` |
| Entity | Singular noun | `User`, `Job`, `Resume`, `Blog` |
| Value Object | Descriptive noun | `EmailAddress`, `Password`, `PhoneNumber` |
| Repository | `<Entity>Repository` | `UserRepository`, `JobRepository` |
| Mapper | `<Feature>Mapper` | `RegistrationFormMapper`, `JobMapper` |
| Constants | `<Feature>Constants` | `RoleConstants`, `RateLimitConstants`, `ApiConstants` |
| Exception | `<Type>Exception` | `BadRequestException`, `ResourceNotFoundException` |

### 4.2. Frontend (TypeScript)

| Phần tử | Quy ước | Ví dụ |
|---------|---------|-------|
| Component | kebab-case files, PascalCase class | `login.component.ts` → `LoginComponent` |
| Service | kebab-case file | `auth.service.ts` → `AuthService` |
| Model file | kebab-case | `job-card.model.ts`, `api-response.model.ts` |
| Interface | PascalCase | `JobCardModel`, `ApiResponse`, `PagedPayload` |
| Type alias | PascalCase | `JobListApiResponse`, `ApplyCvResponse` |
| Pipe | short name | `TranslatePipe` → `name: 't'` |
| Guard | kebab-case | `hirer-guard.guard.ts` |
| Interceptor | kebab-case | `error.interceptor.ts` |
| Signal (private) | camelCase | `private jobDetail = signal<...>(null)` |
| Signal (public) | camelCase + `$` suffix | `readonly jobDetail$ = computed(...)` |
| Route file | `<feature>.routes.ts` | `recruiter.routes.ts` |

### 4.3. API Endpoint Naming

RESTful conventions, grouped by role:

| Prefix | Audience | Ví dụ |
|--------|----------|-------|
| `/api/auth/` | Public auth | `/api/auth/user/login`, `/api/auth/refreshToken` |
| `/api/jobs/` | Public job listing | `/api/jobs/newest`, `/api/jobs/{id}` |
| `/api/user/` | Authenticated user | `/api/user/applications/submit-existing` |
| `/api/hirer/` | Recruiter only | `/api/hirer/jobs/posted` |
| `/api/account/` | Account management | `/api/account/profile` |
| `/api/blogs/` | Blog system | `/api/blogs/{blogId}` |

---

## 5. Xử Lý Lỗi & Logging

### 5.1. Backend — Structured Logging

```java
// Pattern: MDC context → structured log → finally cleanup
try {
    MDC.put("userId", String.valueOf(user.getId()));
    MDC.put("jobId", String.valueOf(job.getId()));

    log.info("Job post created — job: {}, title: {}, by hirer: {}",
            job.getId(), job.getTitle(), recruitment.getId());
} finally {
    MDC.remove("userId");
    MDC.remove("jobId");
}
```

**Log message convention:**
- `log.info("Action description — key: value, key: value")` — mô tả + context
- `log.warn("Action failed — reason description")` — failure cases
- `log.error("Unhandled exception [traceId={}]: ", traceId, ex)` — với stack trace
- Luôn log operation result, không log nhạy cảm (password, PII)

### 5.2. Frontend — Debug Logging

```typescript
private logAuthDebug(message: string, context?: Record<string, unknown>): void {
  console.info(`[AuthService] ${message}`, context ?? {});
}

// Guard logging
function logGuardDecision(message, state, authReady, roles): void {
  console.info('[HirerGuard]', { message, url: state.url, authReady, roles });
}
```

**Frontend logging convention:**
- `[ServiceName]` prefix cho mỗi log entry
- Object context thay vì string concatenation
- `console.info` cho operational logs, `console.warn` cho warnings, `console.error` cho errors

### 5.3. Global Error Handling

- **Backend**: `@RestControllerAdvice` catch all → `ApiResponse` + `traceId`
- **Frontend**: `errorInterceptor` catch HTTP errors → toast notification + auto logout khi 403

---

## 6. Quốc Tế Hóa (i18n)

### 6.1. Backend i18n

- `messages_vi.properties` + `messages_en.properties` trong `resources/`
- `MessageUtils.getMessage("key")` — static utility class, inject `MessageSource`
- `I18nConfig` + `Accept-Language` header detection
- **Mọi exception message** đều dùng i18n key, không hardcode string

### 6.2. Frontend i18n

- Custom `I18nService` với `signal()` cho reactive language switching
- `TranslatePipe` (name: `'t'`) dùng trong template
- `TRANSLATIONS` object chứa translation tree
- Auto-detect ngôn ngữ: localStorage → navigator locale → timezone (Ho_Chi_Minh → vi)

---

## 7. Bảo Mật & Authentication

### 7.1. Authentication Flow

```
User Login → JWT Access Token (response body) + Refresh Token (HttpOnly Cookie)
    ↓
API Request → Bearer Token in Authorization header
    ↓
Token Expired (401) → Auto refresh via interceptor → Retry original request
    ↓
Refresh Failed → Force logout → Redirect to login
```

### 7.2. Security Patterns

| Feature | Implementation |
|---------|---------------|
| JWT Access Token | Trả về trong response body, lưu trong `signal()` (memory only) |
| Refresh Token | `HttpOnly`, `Secure`, `SameSite=Lax` cookie, 7 ngày |
| Token Refresh | Interceptor auto-refresh, `shareReplay(1)` cho concurrent requests |
| OAuth2 | Google login, Facebook login support |
| Password | BCrypt encoding, `Password` VO hide value khi `toString()` |
| Rate Limiting | IP-based spam protection (`SpamService` + Redis) |
| Anti-CSRF | `SameSite=Lax` cookie policy |
| Soft Delete | `@SQLRestriction("record_status <> 'DELETED'")` |
| SSR Safety | `isPlatformBrowser()` checks trước mọi browser-only API |

---

## 8. Design System & UI

### 8.1. Tailwind CSS

- Dùng **Tailwind CSS 4** (`@import "tailwindcss"` + `@theme {}` block)
- Design token system qua CSS custom properties trong `@theme {}`
- Separate color palette cho **Admin** (`ad-*`) và **Recruiter** (`re-*`)
- **Font system**: Manrope (headlines) + Inter (body/labels)
- Custom animations: `animate-flow`, `animate-shimmer` (skeleton loading)

### 8.2. UI Patterns

- **Google Material Symbols Outlined** cho icons
- Angular Material components (`provideNativeDateAdapter`)
- `ngx-toastr` cho notifications
- `ngx-quill` cho rich text editor
- Skeleton loading components (`skeleton-job-card`, `skeleton-blog-card`, etc.)
- Gradient buttons, glassmorphism, hover effects, micro-animations

### 8.3. Template Style

```html
<!-- Angular 17+ control flow syntax -->
@if (usernameControl.hasError("required") && (usernameControl.touched || usernameControl.dirty)) {
  <p class="mt-1 text-xs text-red-600">Vui lòng nhập tên người dùng hoặc email.</p>
}

<!-- Tailwind utility-first approach -->
<button class="w-full rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 px-4 py-3 ...">
```

---

## 9. Quản Lý State

### 9.1. Angular Signals (Primary)

Bạn đã adopt **Angular Signals** triệt để thay cho RxJS Subject/BehaviorSubject:

```typescript
// Private mutable signal
private readonly loggedIn = signal(false);

// Public read-only computed
public isLoggedIn = computed(() => this.loggedIn());

// Update
this.loggedIn.set(true);

// Effect (in component)
effect(() => {
  this.showHeader = this.layoutVisibilityService.headerComputed();
});
```

### 9.2. RxJS (HTTP & Event Streams)

RxJS vẫn dùng cho:
- HTTP calls: `this.http.get().pipe(take(1), map(), catchError())`
- SSE event handling: `fetchEventSource` + internal Signal publish
- Token refresh flow: `switchMap`, `shareReplay(1)`

### 9.3. SSE (Server-Sent Events)

Custom `SseService` phức tạp:
- Exponential backoff reconnection (1s → 30s max, 5 retries)
- Event-driven architecture: `fromEvent<T>('event-name')` → `Signal<T>`
- Auto token refresh khi SSE connection bị 401
- `AbortController` cho clean disconnect

---

## 10. Thói Quen Đáng Chú Ý

### 10.1. Những Pattern Tích Cực ✅

| # | Thói quen | Chi tiết |
|---|-----------|----------|
| 1 | **DDD nhất quán** | 6 bounded contexts, mỗi cái đều 5 layers giống nhau |
| 2 | **Interface-first** | Service luôn có interface trước, impl sau |
| 3 | **Signal-first state** | Đã chuyển triệt để sang Signals thay vì BehaviorSubject |
| 4 | **Structured logging** | MDC context, formatted messages, severity phân biệt |
| 5 | **i18n từ đầu** | Cả backend (properties) và frontend (custom service) đều hỗ trợ đa ngôn ngữ |
| 6 | **Soft delete** | `BaseEntity` + `@SQLRestriction`, không xóa cứng |
| 7 | **SSR-safe code** | `isPlatformBrowser()` check mọi nơi trước khi dùng browser API |
| 8 | **API response chuẩn** | Một `ApiResponse<T>` duy nhất cho toàn bộ API |
| 9 | **Security-conscious** | HttpOnly cookie, refresh token rotation, rate limiting, spam protection |
| 10 | **Lazy loading** | 100% routes đều lazy load (component hoặc children) |
| 11 | **Value Objects** | Dùng VOs cho domain concepts (`EmailAddress`, `Password`) |
| 12 | **Separation of concerns** | Query vs Command service (`JobQueryService` vs `JobService`) |
| 13 | **Functional patterns** | Functional interceptors, functional guards (Angular modern style) |
| 14 | **Documentation** | README, DESIGN.md, chuc_nang_hoat_dong.md — nhiều tài liệu |

### 10.2. Comment & Documentation Style

```java
// Vietnamese inline comments
@PrePersist
protected void onPrePersist() {
    // Lưu ý: Không cần gán this.createdAt = LocalDateTime.now() ở đây
    // vì @CreatedDate và AuditingEntityListener đã tự động làm việc đó.
}
```

```typescript
// Vietnamese route group comments
// Lazy load Children - Các nhóm module lớn đã được chia file routes riêng
// Lazy load Components - Tính năng Site / Blog
```

**Đặc điểm:**
- Comment bằng **tiếng Việt** cho internal notes, giải thích lý do "tại sao"
- `// TODO Auto-generated method stub` từ IDE — đôi khi chưa dọn
- JavaDoc cho utility classes (`MessageUtils`) — bằng tiếng Anh
- Ít unit test (chỉ có spec files placeholder)

### 10.3. Dependency/Library Choices

| Category | Backend | Frontend |
|----------|---------|----------|
| Core | Spring Boot 3, Spring Security | Angular 20, Angular SSR |
| Database | PostgreSQL, Spring Data JPA | — |
| Cache/Queue | Redis, RabbitMQ | — |
| Auth | JWT, OAuth2 (Google, Facebook) | @auth0/angular-jwt |
| AI | LangChain4j, Gemini, Apache Tika | — |
| Storage | Cloudflare R2 | — |
| UI | — | Tailwind CSS 4, Angular Material, ngx-quill, ngx-toastr |
| Build | Maven | npm/pnpm, Angular CLI |
| Deploy | Docker, Render.com | Vercel |
| Real-time | SSE (SseEmitter) | @microsoft/fetch-event-source |

### 10.4. Thói Quen Coding Nhỏ

- **`take(1)`**: Gần như mọi HTTP call đều có `.pipe(take(1))` — đảm bảo auto-unsubscribe
- **`withCredentials: true`**: Luôn gửi cookie cho authenticated endpoints
- **`finalize()`**: Dùng để reset loading state bất kể success hay error
- **Constructor injection mix**: Dùng cả `inject()` function và constructor DI trong cùng component
- **`LinkedList` preference**: Dùng `LinkedList` thay `ArrayList` cho một số collection (ví dụ: `applies`)
- **Null-safe getters**: `return email != null ? email.getValue() : null;`
- **Private helper methods**: Tách logic thành nhiều private methods nhỏ (`extractSkillsContext`, `extractExperienceContext`, `getClientIP`, `buildVectorizeRequest`)
- **Java Records cho DTO**: Ưu tiên record cho immutable DTOs, Lombok cho mutable objects

---

## 11. Tổng Kết Điểm Mạnh & Điểm Cần Cải Thiện

### ⭐ Điểm Mạnh

1. **Kiến trúc chặt chẽ**: DDD backend + Feature-based frontend, cấu trúc thư mục rất nhất quán
2. **Coding convention nhất quán**: Cùng pattern xuyên suốt mọi module — dễ đoán, dễ maintain
3. **Modern Angular**: Signals, standalone components, functional guards/interceptors, new control flow
4. **Security depth**: Không chỉ JWT đơn giản mà có refresh token rotation, rate limiting, anti-spam, SSR-safe
5. **Production-ready mindset**: Docker, env-based config, soft delete, structured logging, traceId tracking
6. **Design system**: Color tokens, typography system, skeleton loading — UI chuyên nghiệp
7. **Full i18n**: Hỗ trợ đa ngôn ngữ từ cả hai phía (backend + frontend)

### 🔧 Điểm Có Thể Cải Thiện

1. **Unit test coverage**: Hầu hết spec files là placeholder, cần bổ sung test thực tế
2. **TODO comments**: Một số `// TODO Auto-generated method stub` chưa được dọn dẹp
3. **Record getters**: DTOs dùng Java Record nhưng lại thêm getter thủ công (không cần thiết)
4. **Error message inconsistency**: Đôi khi dùng i18n key (`"auth.user.not_found"`), đôi khi hardcode (`"Invalid email format"`)
5. **SSR handling**: Nhiều `isPlatformBrowser()` check rải rác — có thể centralize
6. **Type safety**: Một số chỗ dùng `any` trong TypeScript (`body: any`, `Observable<any>`)
7. **Naming inconsistency nhỏ**: Hỗn hợp `DTO` suffix (LoginDTO) và không suffix (RegistrationForm) cho cùng vai trò

---

> [!TIP]
> **Tổng kết**: Code của bạn thể hiện tư duy kiến trúc vững, consistency cao, và đã áp dụng nhiều best practices hiện đại. Đặc biệt ấn tượng ở sự nhất quán trong DDD layers, Signal-based state management, và security implementation depth. Project này vượt xa mức "CRUD student project" thông thường.
