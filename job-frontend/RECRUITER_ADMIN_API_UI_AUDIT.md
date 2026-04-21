# Recruiter + Admin API/UI Audit Report

Updated: 2026-04-21
Scope: recruiter/admin pages in current routes and related service contracts.

## Status Legend
- BOUND: UI dang doc du lieu tu API va hien thi duoc.
- PARTIAL: da co API va da bind mot phan, con field/flow chua map.
- MISSING: chua bind API hoac chua co API contract.

## Recruiter Pages

### 1) Recruiter Login (`/recruiter/login`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `POST /auth/hirer/login` | `ApiResponse<string>` (token) | email, password, remember, loading, error | Da submit `email/password`, nhan token, set login state, redirect | `remember` chua duoc dua vao payload API; cac nut social login/forgot password la placeholder |

### 2) Recruiter Register (`/recruiter/register`) - MISSING

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `POST /auth/hirer/register` | `ApiResponse<unknown>` | fullName, companyName, email, phone, password, confirmPassword, agreeTerms, loading, error | Component TS da co reactive form + submit logic | Template hien tai van la form static, chua `formGroup/formControlName`, chua goi submit API |

### 3) Recruiter Dashboard Shell (`/recruiter/dashboard`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /account/roles/hirer` | `ApiResponse<string[]>` | role menu, role badge, phan quyen menu, profile | Roles da load vao signal | Template dashboard van hardcoded profile/metrics/menu; signal roles chua bind vao UI |

### 4) Recruiter Dashboard Overview Component (`recruiter-dashboard-overview`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /hirer/jobs/posted/count` | `ApiResponse<number>` or number-like shape | open jobs, new candidates, interviews, response rate | Da map open jobs count | Cac metric con lai dang "Khong co du lieu" do chua co endpoint; component nay hien chua nam trong route active dashboard |

### 5) Recruiter Job List (`/recruiter/jobs`) - BOUND

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /hirer/jobs/posted?page&size` | `ApiResponse<PagedPayload<HirerJobPostView>>` with `id,title,description,address,salary,time,applies,headcount,status,totalPages,totalElements` | card list, title, address, salary, time, applies, headcount, status, pagination | Da bind day du list + pagination + loading state | Field `description` co trong response nhung chua hien thi tren list |
| `GET /hirer/jobs/posted/count` | count | tong so tin dang co | Da bind | Khong |
| `DELETE /hirer/jobs/{id}` | `ApiResponse<string|null>` | xoa job + feedback | Da bind + thong bao | Khong |

### 6) Recruiter Job Detail (`/recruiter/jobs/detail/:id`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /jobs/{id}` | `ApiResponse<JobDetailViewModel>` with `id,title,address,description,salary,time,requireDetails,skill,expiredDate,headcount` | title, address, salary, time, headcount, skill, expiry, description, requirements | Da bind day du cac field tren | Dang dung endpoint public jobs, khong co thong tin recruiter-specific nhu `status`, `applies`; contract salary detail la number trong khi list salary la string |

### 7) Recruiter Candidate List (`/recruiter/candidates`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /hirer/applications/jobs/{jobId}/candidates/{pageIndex}/{pageSize}` | `ApiResponse<PagedPayload<RecruiterCandidatesApiRaw>>` (raw fields bien the: `id/userId`, `fullName/userName/candidateName`, `email/userEmail`, `role/jobTitle`, `experience/yearsOfExperience`, `status`) | candidate table, totals, status badge, paging | Da map va bind duoc bang mapping/fallback | Contract candidate dang khong on dinh (nhieu ten field thay the), can chot schema backend de UI khong phai map heuristics |
| `GET /hirer/resumes/users/{email}` | `ApiResponse<ResumeReviewInput[]>` (`id,fileName,createDate,...`) | list CV theo candidate | Da bind list + loading + empty | Khong |
| `GET /hirer/resumes/{resumeId}/view` | `ApiResponse<ResumeUrlDTO>` (`url`) | mo CV | Da bind | Khong |
| `GET /hirer/resumes/{resumeId}/download` | `ApiResponse<ResumeUrlDTO>` (`url`) | tai CV | Da bind | Khong |

### 8) Company Address (`/recruiter/company-address`) - MISSING

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| Chua co endpoint recruiter company-address trong services hien tai | N/A | list dia chi, add, delete, default marker, contact info | Da render bang mock data local + kebab menu + delete | Chua co API contract CRUD (list/create/update/delete/default) nen khong the bind backend |

### 9) Recruiter Post Job (`/recruiter/jobs/post-job`) - MISSING

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `POST /hirer/jobs` | `ApiResponse<string|null>` | form tao job, upload image, validation, submit/loading/result | TS da co `FormGroup`, convert FormData, goi `createJob` | Template hien tai van static input/select/textarea, chua bind reactive form, chua noi submit voi API |

## Admin Pages

### 1) Admin Login (`/admin/login`) - MISSING

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `POST /admin/auth/login` | `ApiResponse<AdminLoginData>` (`accessToken,refreshToken,expiresIn,admin`) | email/password/remember, loading, error, redirect | Login page hien tai chi la HTML static; TS component rong | Chua dung `AdminAuthService.login`, chua submit API, chua map loading/error |

### 2) Admin Dashboard Shell (`/admin/*` wrapper) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| Session tu `AdminAuthService` (`adminProfile`) | `AdminLoginProfile` (`id,fullName,email,role,lastLoginAt`) | profile name/role/last login/logout | Shell co router links va layout | Name/role/last login dang hardcoded; chua bind profile that; chua co nut logout call service |

### 3) Admin Overview (`/admin/dashboard`) - BOUND (co gap nho)

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /admin/dashboard/summary` | `AdminDashboardSummary` | KPI cards | Da bind | Khong |
| `GET /admin/dashboard/revenue-trend?range=...` | `AdminRevenueTrend` | trend table | Da bind (default range) | Chua co UI de doi `range` |
| `GET /admin/dashboard/job-distribution` | `AdminJobDistribution` | distribution section | Da bind | Khong |
| `GET /admin/jobs/pending?page&pageSize&search&status` | `AdminListPayload<AdminPendingJobItem>` | pending list + total | Da bind list + total | Chua co UI cho `search/status`; field `subtitle` trong item chua dung |

### 4) Admin Employers (`/admin/employers`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /admin/employers/metrics` | `AdminEmployersMetrics` | metrics cards | Da bind | Khong |
| `GET /admin/employers` | `AdminListPayload<AdminEmployerItem>` | list + pagination + search | Da bind | Query filters `kycStatus/status/sortBy/sortDir` chua co control UI |
| `GET /admin/employers/{id}` | `AdminEmployerDetail` | detail panel | Da bind | Khong |
| `PATCH /admin/employers/{id}/status` | `AdminUpdateEmployerStatusData` | update status action | Da bind suspend/restore | Action `activate` co trong contract nhung chua co nut UI |
| `GET /admin/employers/export` | `AdminEmployersExportData` | export data action | Chua co UI | Missing action hoan toan |

### 5) Admin Job Seekers (`/admin/job-seekers`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /admin/job-seekers/metrics` | `AdminJobSeekersMetrics` | metrics cards | Da bind | Khong |
| `GET /admin/job-seekers` | `AdminListPayload<AdminJobSeekerItem>` | list + pagination + search | Da bind | Query filters `resumeStatus/sortBy/sortDir` chua co control UI |
| `GET /admin/job-seekers/region-distribution` | `AdminRegionDistribution` | region list | Da bind | Khong |
| `POST /admin/job-seekers` | `AdminCreateJobSeekerData` | tao moi seeker | Service da co | Chua co form/UI |

### 6) Admin Jobs Management (`/admin/jobs`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /admin/jobs/metrics` | `AdminJobsMetrics` | metrics cards | Da bind | Khong |
| `GET /admin/jobs` | `AdminListPayload<AdminJobItem>` | list + pagination + search | Da bind | Query filters `category/status/sortBy/sortDir` chua co control UI |
| `PATCH /admin/jobs/{id}/status` | `AdminUpdateJobStatusData` | doi status tung job | Da bind | Select status chua auto preselect theo `job.status` |
| `POST /admin/jobs` | `AdminCreateJobData` | tao moi job | Service da co | Chua co form/UI |
| `POST /admin/jobs/bulk-action` | `AdminBulkJobActionData` | bulk actions | Service da co | Chua co UI |

### 7) Admin Billing (`/admin/billing`) - PARTIAL

| API | Response contract | UI fields can hien thi / xu ly | Hien trang | Gap |
|---|---|---|---|---|
| `GET /admin/billing/summary` | `AdminBillingSummary` | summary cards | Da bind | Khong |
| `GET /admin/billing/tiers` | `AdminBillingTiersData` | tiers cards | Da bind | Khong |
| `PATCH /admin/billing/tiers/{id}` | `AdminUpdateBillingTierData` | update tier | Service da co | Chua co UI cap nhat |
| `GET /admin/billing/transactions` | `AdminListPayload<AdminBillingTransactionItem>` | transactions table + total | Da bind list | Chua co UI filter (`status/from/to`) va pagination controls |

## Priority Gap List (Recommended Order)

1. Bind Admin Login page vao `AdminAuthService.login` (hien dang MISSING).
2. Bind Recruiter Register page vao reactive form + submit API (hien dang MISSING).
3. Bind Recruiter Post Job page vao reactive form + submit/upload API (hien dang MISSING).
4. Chot route/structure cho Recruiter Dashboard: su dung overview API-bound thay vi layout static hardcoded.
5. Bo sung API contract cho Company Address CRUD de thay mock data.
6. Bo sung bo loc/sort/pagination controls cho cac trang admin da PARTIAL.

## Build Validation After Current Binding Pass

- `npm run build`: PASS.
- Build dang clean sau cac sua template + typing.
