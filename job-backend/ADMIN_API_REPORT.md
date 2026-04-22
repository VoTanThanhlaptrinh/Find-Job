# Admin API Report

Generated from:
- `src/main/java/com/job_web/controller/admin`
- `src/main/java/com/job_web/dto/admin`
- `src/main/java/com/job_web/dto/common`

## 1) Standard Response Wrapper

All admin endpoints return `ApiResponse<T>`:

```json
{
  "message": "string",
  "data": "T",
  "status": 200,
  "traceId": "string | null"
}
```

`PageResponse<T>` shape (when paginated):

```json
{
  "items": ["T"],
  "pagination": {
    "page": 1,
    "pageSize": 10,
    "totalItems": 0,
    "totalPages": 0
  }
}
```

## 2) API Inventory (Admin Controllers)

| # | Method | Endpoint | Request | Response (`data`) | HTTP Status |
|---|---|---|---|---|---|
| 1 | POST | `/admin/auth/login` | Body: `AdminLoginRequest` | `String` (`accessToken`) + `Set-Cookie: refreshToken` (HttpOnly) | 200 |
| 2 | POST | `/admin/auth/refresh` | Body: `AdminRefreshRequest` | `AdminLoginResponse` | 200 |
| 3 | POST | `/admin/auth/logout` | Body: `AdminLogoutRequest` | `Map<String, Boolean>` (`loggedOut`) | 200 |
| 4 | GET | `/admin/billing/tiers` | None | `List<BillingTierDTO>` | 200 |
| 5 | PATCH | `/admin/billing/tiers/{id}` | Path: `id` (`String`), Body: `BillingTierDTO` | `Boolean` (`true` when updated) | 200 |
| 6 | GET | `/admin/billing/transactions` | Query: `page` (default `1`), `pageSize` (default `20`), `status` (optional) | `PageResponse<TransactionListItem>` | 200 |
| 7 | GET | `/admin/billing/summary` | None | `BillingSummaryResponse` | 200 |
| 8 | GET | `/admin/dashboard/summary` | None | `DashboardSummaryResponse` | 200 |
| 9 | GET | `/admin/dashboard/revenue-trend` | Query: `range` (default `30d`) | `RevenueTrendResponse` | 200 |
| 10 | GET | `/admin/dashboard/job-distribution` | None | `JobDistributionResponse` | 200 |
| 11 | GET | `/admin/jobs/pending` | Query: `page` (default `1`), `pageSize` (default `10`) | `PageResponse<PendingJobItem>` | 200 |
| 12 | GET | `/admin/employers/metrics` | None | `EmployerMetricsResponse` | 200 |
| 13 | GET | `/admin/employers` | Query: `page` (default `1`), `pageSize` (default `10`), `search` (optional), `kycStatus` (optional), `status` (optional) | `PageResponse<EmployerListItem>` | 200 |
| 14 | GET | `/admin/employers/{id}` | Path: `id` (`long`) | `EmployerDetail` | 200 |
| 15 | PATCH | `/admin/employers/{id}/status` | Path: `id` (`long`), Body: `EmployerStatusRequest` | `Map<String, Object>` (`id`, `updated`) | 200 |
| 16 | GET | `/admin/employers/export` | Query: `format` (default `csv`) | `Map<String, String>` (`downloadUrl`) | 200 |
| 17 | GET | `/admin/jobs/metrics` | None | `JobMetricsResponse` | 200 |
| 18 | GET | `/admin/jobs` | Query: `page` (default `1`), `pageSize` (default `12`), `search` (optional), `category` (optional), `status` (optional) | `PageResponse<AdminJobListItem>` | 200 |
| 19 | POST | `/admin/jobs` | Body: `AdminJobRequest` | `Map<String, Object>` (`id`, `created`) | 201 |
| 20 | PATCH | `/admin/jobs/{id}/status` | Path: `id` (`long`), Body: `Map<String, String>` (`status`) | `Map<String, Object>` (`id`, `status`) | 200 |
| 21 | POST | `/admin/jobs/bulk-action` | Body: `BulkActionRequest` | `Map<String, Object>` (`processed`, `failed`) | 200 |
| 22 | GET | `/admin/job-seekers/metrics` | None | `JobSeekerMetricsResponse` | 200 |
| 23 | GET | `/admin/job-seekers` | Query: `page` (default `1`), `pageSize` (default `10`), `search` (optional), `resumeStatus` (optional) | `PageResponse<JobSeekerListItem>` | 200 |
| 24 | POST | `/admin/job-seekers` | Body: `JobSeekerRequest` | `Map<String, Object>` (`id`, `created`) | 201 |
| 25 | GET | `/admin/job-seekers/region-distribution` | None | `RegionDistributionResponse` | 200 |

## 3) Request DTO Schemas

### `AdminLoginRequest`
- `email: String`
- `password: String`
- `rememberMe: boolean`

### `AdminRefreshRequest`
- `refreshToken: String`

### `AdminLogoutRequest`
- `refreshToken: String`

### `BillingTierDTO`
- `id: String`
- `name: String`
- `badge: String`
- `priceMonthly: double`
- `currency: String`
- `isPopular: boolean`
- `usagePct: int`
- `features: List<String>`

### `EmployerStatusRequest`
- `action: String`
- `reason: String`

### `AdminJobRequest`
- `title: String`
- `companyId: String`
- `category: String`
- `description: String`
- `location: String`
- `expiryDate: LocalDateTime`

### `BulkActionRequest`
- `jobIds: List<String>`
- `action: String`

### `JobSeekerRequest`
- `fullName: String`
- `email: String`
- `profession: String`
- `resumeUrl: String`

## 4) Response DTO Schemas

### `AdminLoginResponse`
- `accessToken: String`
- `refreshToken: String`
- `expiresIn: long`
- `admin: AdminInfo`

`AdminInfo`:
- `id: String`
- `fullName: String`
- `email: String`
- `role: String`
- `lastLoginAt: LocalDateTime`

### `BillingSummaryResponse`
- `monthlyRecurringRevenue: double`
- `mrrGrowthPct: double`
- `activeSubscriptions: long`

### `TransactionListItem`
- `id: String`
- `employerId: String`
- `employerName: String`
- `packageName: String`
- `amount: double`
- `currency: String`
- `date: LocalDateTime`
- `status: String`

### `DashboardSummaryResponse`
- `totalEmployers: long`
- `totalJobSeekers: long`
- `pendingJobs: long`
- `totalRevenue: double`
- `growth: Growth`

`Growth`:
- `employersPct: double`
- `jobSeekersPct: double`
- `revenuePct: double`

### `RevenueTrendResponse`
- `range: String`
- `labels: List<String>`
- `current: List<Double>`
- `previous: List<Double>`

### `JobDistributionResponse`
- `total: long`
- `activePct: double`
- `pendingPct: double`
- `expiredPct: double`

### `PendingJobItem`
- `id: String`
- `title: String`
- `subtitle: String`
- `company: String`
- `postDate: LocalDate`
- `status: String`

### `EmployerMetricsResponse`
- `totalEmployers: long`
- `totalEmployersGrowthPct: double`
- `kycVerified: long`
- `kycVerifiedPct: double`
- `pendingKyc: long`
- `suspended: long`

### `EmployerListItem`
- `id: String`
- `name: String`
- `industry: String`
- `registrationDate: LocalDate`
- `activeJobs: int`
- `kycStatus: String`
- `accountStatus: String`
- `avatarInitials: String`

### `EmployerDetail`
- `id: String`
- `name: String`
- `industry: String`
- `registrationDate: LocalDate`
- `activeJobs: int`
- `kycStatus: String`
- `accountStatus: String`
- `contactEmail: String`
- `contactPhone: String`

### `JobMetricsResponse`
- `livePostings: long`
- `livePostingsGrowthPct: double`
- `pendingReview: long`
- `totalApplicants: long`
- `avgTimeToHireDays: int`

### `AdminJobListItem`
- `id: String`
- `title: String`
- `company: String`
- `location: String`
- `category: String`
- `applications: int`
- `newApplicationsToday: int`
- `status: String`
- `expiryDate: LocalDateTime`

### `JobSeekerMetricsResponse`
- `totalSeekers: long`
- `totalSeekersGrowthPct: double`
- `activeLast7Days: long`
- `placedCandidates: long`
- `retentionPct: double`

### `JobSeekerListItem`
- `id: String`
- `fullName: String`
- `email: String`
- `profession: String`
- `resumeStatus: String`
- `lastActiveAt: LocalDateTime`
- `avatarInitials: String`

### `RegionDistributionResponse`
- `regions: List<RegionCount>`

`RegionCount`:
- `code: String`
- `count: long`

## 5) Notes

- Every listed endpoint uses `produces = "application/json"` at class level.
- Response `message` is generated from `MessageUtils.getMessage("message.success")`.
- `POST /admin/auth/login` does not return refresh token in JSON body; refresh token is stored in `refreshToken` cookie (`HttpOnly`, `SameSite=Lax`, max-age 7 days).
- Endpoints returning `Map` currently provide lightweight payloads, often with generated or mock values (for example export URL and created IDs).
