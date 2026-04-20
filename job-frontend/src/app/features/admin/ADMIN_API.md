# Admin Views - Required API Contract

This document defines required APIs based on current admin views:
- /admin/login
- /admin/dashboard
- /admin/employers
- /admin/job-seekers
- /admin/jobs
- /admin/billing

## 1) Response Envelope (Project Standard)

From shared model:

```ts
class ApiResponse<T> {
  message: string;
  data: T;
  status: number;
  traceId?: string;
}
```

All JSON APIs should return:

```json
{
  "message": "Success",
  "data": {},
  "status": 200,
  "traceId": "d7c6cb59-2f3d-4fb4-84b9-7f88d4f8f8a7"
}
```

Notes:
- Frontend base URL already points to /api in this project setup.
- Endpoint paths below are shown as /admin/... (relative to API base).

---

## 2) Authentication APIs

Used by /admin/login and /admin/admin-login.

### 2.1 POST /admin/auth/login

Request body:

```json
{
  "email": "admin@elitehire.com",
  "password": "string",
  "rememberMe": true
}
```

Response data:

```json
{
  "accessToken": "jwt_access_token",
  "refreshToken": "jwt_refresh_token",
  "expiresIn": 900,
  "admin": {
    "id": "adm_001",
    "fullName": "Admin User",
    "email": "admin@elitehire.com",
    "role": "super_admin",
    "lastLoginAt": "2026-04-20T08:44:00Z"
  }
}
```

### 2.2 POST /admin/auth/refresh

Request body:

```json
{
  "refreshToken": "jwt_refresh_token"
}
```

Response data:

```json
{
  "accessToken": "new_jwt_access_token",
  "refreshToken": "new_jwt_refresh_token",
  "expiresIn": 900
}
```

### 2.3 POST /admin/auth/logout

Request body:

```json
{
  "refreshToken": "jwt_refresh_token"
}
```

Response data:

```json
{
  "loggedOut": true
}
```

---

## 3) Dashboard / Overview APIs (/admin/dashboard)

### 3.1 GET /admin/dashboard/summary

Response data:

```json
{
  "totalEmployers": 1240,
  "totalJobSeekers": 45200,
  "pendingJobs": 86,
  "totalRevenue": 142500,
  "growth": {
    "employersPct": 12,
    "jobSeekersPct": 8,
    "revenuePct": 15
  }
}
```

### 3.2 GET /admin/dashboard/revenue-trend?range=30d

Response data:

```json
{
  "range": "30d",
  "labels": ["Week 1", "Week 2", "Week 3", "Week 4"],
  "current": [7200, 9100, 12600, 15800],
  "previous": [6100, 7900, 9500, 12100]
}
```

### 3.3 GET /admin/dashboard/job-distribution

Response data:

```json
{
  "total": 2400,
  "activePct": 60,
  "pendingPct": 25,
  "expiredPct": 15
}
```

### 3.4 GET /admin/jobs/pending?page=1&pageSize=10&search=&status=

Response data:

```json
{
  "items": [
    {
      "id": "job_001",
      "title": "Senior UX Designer",
      "subtitle": "Product Design - Remote",
      "company": "NexGen Tech",
      "postDate": "2026-04-18",
      "status": "pending"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 10,
    "totalItems": 86,
    "totalPages": 9
  }
}
```

---

## 4) Employers APIs (/admin/employers)

### 4.1 GET /admin/employers/metrics

Response data:

```json
{
  "totalEmployers": 1240,
  "totalEmployersGrowthPct": 12,
  "kycVerified": 980,
  "kycVerifiedPct": 79,
  "pendingKyc": 120,
  "suspended": 140
}
```

### 4.2 GET /admin/employers?page=1&pageSize=10&search=&kycStatus=&status=&sortBy=registrationDate&sortDir=desc

Response data:

```json
{
  "items": [
    {
      "id": "emp_001",
      "name": "NexGen Tech",
      "industry": "SaaS & FinTech",
      "registrationDate": "2023-10-12",
      "activeJobs": 24,
      "kycStatus": "verified",
      "accountStatus": "active",
      "avatarInitials": "NT"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 10,
    "totalItems": 1240,
    "totalPages": 124
  }
}
```

### 4.3 GET /admin/employers/{employerId}

Response data:

```json
{
  "id": "emp_001",
  "name": "NexGen Tech",
  "industry": "SaaS & FinTech",
  "registrationDate": "2023-10-12",
  "activeJobs": 24,
  "kycStatus": "verified",
  "accountStatus": "active",
  "contactEmail": "hr@nexgen.com",
  "contactPhone": "+1-555-0101"
}
```

### 4.4 PATCH /admin/employers/{employerId}/status

Request body:

```json
{
  "action": "suspend",
  "reason": "policy_violation"
}
```

Response data:

```json
{
  "id": "emp_001",
  "accountStatus": "suspended",
  "updatedAt": "2026-04-20T09:15:00Z"
}
```

### 4.5 GET /admin/employers/export?format=csv&search=&kycStatus=&status=

Response data:

```json
{
  "downloadUrl": "https://cdn.example.com/exports/employers-2026-04-20.csv",
  "expiresAt": "2026-04-20T10:15:00Z"
}
```

---

## 5) Job Seekers APIs (/admin/job-seekers)

### 5.1 GET /admin/job-seekers/metrics

Response data:

```json
{
  "totalSeekers": 45200,
  "totalSeekersGrowthPct": 12,
  "activeLast7Days": 12400,
  "placedCandidates": 8900,
  "retentionPct": 94
}
```

### 5.2 GET /admin/job-seekers?page=1&pageSize=10&search=&resumeStatus=&sortBy=lastActive&sortDir=desc

Response data:

```json
{
  "items": [
    {
      "id": "cand_001",
      "fullName": "Sarah Jenkins",
      "email": "sarah.j@email.com",
      "profession": "Senior UX Designer",
      "resumeStatus": "available",
      "lastActiveAt": "2026-04-20T08:30:00Z",
      "avatarInitials": "SJ"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 10,
    "totalItems": 45200,
    "totalPages": 4520
  }
}
```

### 5.3 POST /admin/job-seekers

Request body:

```json
{
  "fullName": "string",
  "email": "string",
  "profession": "string",
  "resumeUrl": "string"
}
```

Response data:

```json
{
  "id": "cand_999",
  "created": true
}
```

### 5.4 GET /admin/job-seekers/region-distribution

Response data:

```json
{
  "regions": [
    { "code": "NA", "count": 12000 },
    { "code": "EU", "count": 17000 },
    { "code": "APAC", "count": 9000 },
    { "code": "LATAM", "count": 5000 },
    { "code": "MEA", "count": 2200 }
  ]
}
```

---

## 6) Jobs Management APIs (/admin/jobs)

### 6.1 GET /admin/jobs/metrics

Response data:

```json
{
  "livePostings": 142,
  "livePostingsGrowthPct": 12,
  "pendingReview": 28,
  "totalApplicants": 12400,
  "avgTimeToHireDays": 18
}
```

### 6.2 GET /admin/jobs?page=1&pageSize=12&search=&category=&status=&sortBy=createdAt&sortDir=desc

Response data:

```json
{
  "items": [
    {
      "id": "job_001",
      "title": "Senior Product Designer",
      "company": "Stripe",
      "location": "Remote",
      "category": "design",
      "applications": 1204,
      "newApplicationsToday": 18,
      "status": "active",
      "expiryDate": "2026-05-12"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 12,
    "totalItems": 142,
    "totalPages": 12
  }
}
```

### 6.3 POST /admin/jobs

Request body:

```json
{
  "title": "string",
  "companyId": "string",
  "category": "engineering",
  "description": "string",
  "location": "string",
  "expiryDate": "2026-05-30"
}
```

Response data:

```json
{
  "id": "job_999",
  "status": "pending",
  "created": true
}
```

### 6.4 PATCH /admin/jobs/{jobId}/status

Request body:

```json
{
  "status": "active"
}
```

Response data:

```json
{
  "id": "job_001",
  "status": "active",
  "updatedAt": "2026-04-20T09:20:00Z"
}
```

### 6.5 POST /admin/jobs/bulk-action

Request body:

```json
{
  "jobIds": ["job_001", "job_002"],
  "action": "archive"
}
```

Response data:

```json
{
  "processed": 2,
  "failed": 0
}
```

---

## 7) Billing APIs (/admin/billing)

### 7.1 GET /admin/billing/tiers

Response data:

```json
{
  "tiers": [
    {
      "id": "basic",
      "name": "Basic",
      "badge": "Standard",
      "priceMonthly": 499,
      "currency": "USD",
      "isPopular": false,
      "usagePct": 15,
      "features": [
        "Up to 10 Job Postings",
        "Standard Talent Pool",
        "Email Support"
      ]
    },
    {
      "id": "pro",
      "name": "Pro",
      "badge": "Advanced",
      "priceMonthly": 1299,
      "currency": "USD",
      "isPopular": true,
      "usagePct": 65,
      "features": [
        "Unlimited Job Postings",
        "AI Talent Matching",
        "Dedicated Account Manager",
        "Analytics Dashboard"
      ]
    }
  ]
}
```

### 7.2 PATCH /admin/billing/tiers/{tierId}

Request body:

```json
{
  "priceMonthly": 1399,
  "features": [
    "Unlimited Job Postings",
    "AI Talent Matching",
    "Dedicated Account Manager",
    "Analytics Dashboard"
  ]
}
```

Response data:

```json
{
  "id": "pro",
  "updated": true,
  "updatedAt": "2026-04-20T09:30:00Z"
}
```

### 7.3 GET /admin/billing/transactions?page=1&pageSize=20&status=&from=&to=

Response data:

```json
{
  "items": [
    {
      "id": "TX-90234",
      "employerId": "emp_001",
      "employerName": "Nexus Core Inc.",
      "package": "pro",
      "amount": 1299,
      "currency": "USD",
      "date": "2026-04-20T07:00:00Z",
      "status": "paid"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 400,
    "totalPages": 20
  }
}
```

### 7.4 GET /admin/billing/summary

Response data:

```json
{
  "monthlyRecurringRevenue": 442890,
  "mrrGrowthPct": 12.4,
  "activeSubscriptions": 2841
}
```

---

## 8) Common Response Types for Lists

Recommended shape for list pagination in data:

```json
{
  "items": [],
  "pagination": {
    "page": 1,
    "pageSize": 10,
    "totalItems": 0,
    "totalPages": 0
  }
}
```

---

## 9) Error Response Shape

Use the same envelope for errors:

```json
{
  "message": "Validation error",
  "data": {
    "errors": [
      {
        "field": "email",
        "message": "Email is invalid"
      }
    ]
  },
  "status": 400,
  "traceId": "3f7fda44-6a9a-4f5b-931e-84f2f314eb11"
}
```

Suggested status usage:
- 200: success
- 201: created
- 400: validation error
- 401: unauthenticated
- 403: forbidden
- 404: not found
- 409: conflict
- 500: internal error
