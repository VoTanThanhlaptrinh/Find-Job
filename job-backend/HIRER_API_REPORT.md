# Hirer API Report

Generated from:
- `src/main/java/com/job_web/controller/account/hirer/HirerAuthController.java`
- `src/main/java/com/job_web/controller/account/hirer/HirerAccountController.java`
- `src/main/java/com/job_web/controller/application/HirerApplicationController.java`
- `src/main/java/com/job_web/controller/application/HirerResumeController.java`
- `src/main/java/com/job_web/controller/job/HirerJobController.java`

## 1) Standard Response Wrapper

All Hirer endpoints return `ApiResponse<T>`:

```json
{
  "message": "string",
  "data": "T",
  "status": 200,
  "traceId": "string | null"
}
```

Some endpoints return Spring Data `Page<T>` in `data` (not `PageResponse<T>`).

## 2) Hirer API Inventory

| # | Method | Endpoint | Request | Response (`data`) | HTTP Status |
|---|---|---|---|---|---|
| 1 | POST | `/api/auth/hirer/login` | Body JSON: `LoginDTO` + internal `HttpServletRequest/Response` | `String` (`accessToken`) + `Set-Cookie: refreshToken` | 200 |
| 2 | POST | `/api/auth/hirer/register` | Body JSON: `RegistationForm` | `String` (`username`) | 200 |
| 3 | GET | `/api/account/roles/hirer` | No body; uses `@CurrentUser` | `Boolean` | 200 |
| 4 | GET | `/api/hirer/applications/jobs/{jobId}/candidates/{pageIndex}/{pageSize}` | Path: `jobId: long`, `pageIndex: int`, `pageSize: int` | `Page<CandidateDTO>` | Dynamic from service (`res.getStatus()`) |
| 5 | GET | `/api/hirer/resumes/users/{email}` | Path: `email: String` (`@Email`) | `List<ResumeDTO>` | Dynamic from service (`res.getStatus()`) |
| 6 | GET | `/api/hirer/resumes/{id}/view` | Path: `id: long` | `ResumeUrlDTO` | Dynamic from service (`res.getStatus()`) |
| 7 | GET | `/api/hirer/resumes/{id}/download` | Path: `id: long` | `ResumeUrlDTO` | Dynamic from service (`res.getStatus()`) |
| 8 | POST | `/api/hirer/jobs` | `multipart/form-data`, `@ModelAttribute JobDTO`, `@CurrentUser` | `String` (`null` on success) | 201 |
| 9 | PUT | `/api/hirer/jobs/{id}` | Path: `id: Long`, `multipart/form-data`, `@ModelAttribute JobDTO`, `@CurrentUser` | `String` (`null` on success) | 200 |
| 10 | DELETE | `/api/hirer/jobs/{id}` | Path: `id: Long`, `@CurrentUser` | `String` (`null` on success) | 200 |
| 11 | GET | `/api/hirer/jobs/posted` | Query: `page` (default `0`), `size` (default `10`), `@CurrentUser` | `Page<JobResponse>` | 200 |
| 12 | GET | `/api/hirer/jobs/posted/count` | `@CurrentUser` | `Long` | 200 |

## 3) Request DTO Schemas

### `LoginDTO`
- `username: String` (`@NotBlank`)
- `password: String` (`@NotBlank`)

### `RegistationForm`
- `fullName: String` (`@NotBlank`, `@Size(max=255)`)
- `username: String` (`@NotBlank`, `@Email`, `@EmailExist`)
- `password: String` (`@NotBlank`, `@Size(min=8)`)
- `confirmPassword: String` (`@NotBlank`)
- Rule: `password == confirmPassword`

### `JobDTO` (`multipart/form-data` via `@ModelAttribute`)
- `jobName: String` (`@NotBlank`)
- `addressId: long` (`@Positive`)
- `jobType: String` (`@NotBlank`)
- `salary: String` (`@Size(max=255)`)
- `jobDescription: String` (`@NotBlank`, `@Size(max=5000)`)
- `jobRequirement: String` (`@NotBlank`, `@Size(max=5000)`)
- `jobSkill: String` (`@NotBlank`, `@Size(max=5000)`)
- `deadlineCV: LocalDate` (`@NotNull`, format `yyyy-MM-dd`, must be future date)
- `hirerId: long` (`@Positive`)
- `moreDetail: String` (`@Size(max=5000)`)
- `headcount: Integer` (`@Min(1)`)

## 4) Response DTO Schemas

### `CandidateDTO` (projection)
- `fullName: String`
- `email: String`
- `fileName: String`
- `applyDate: String`

### `ResumeDTO` (projection)
- `id: long`
- `fileName: String`
- `createDate: String`

### `ResumeUrlDTO`
- `resumeId: long`
- `fileName: String`
- `url: String`
- `expiresInMinutes: int`

### `JobResponse`
- `id: Long`
- `title: String`
- `description: String`
- `address: String`
- `salary: String`
- `time: String`
- `applies: int`
- `headcount: int`

## 5) Notes

- Hirer auth uses the same token pattern as user auth:
  - `POST /api/auth/hirer/login` returns `accessToken` in body.
  - `refreshToken` is set in `HttpOnly` cookie (`Set-Cookie`), not returned directly in JSON body.
- Hirer jobs create/update APIs use `multipart/form-data`.
- Endpoints that return `res.getStatus()` can respond with different HTTP status codes based on service logic.
