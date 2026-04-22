# Hirer Frontend API Gap Report

Generated date: 2026-04-22

Scope:
- Backend contract: HIRER_API_REPORT.md
- Frontend scope: src/app/features/recruiter/pages + src/app/features/recruiter/services
- Common API check used by recruiter pages: src/app/features/jobs/services/job.service.ts

## 1) Endpoint Coverage Matrix (Hirer Report vs Frontend)

| # | Endpoint | Frontend status | Evidence | Result |
|---|---|---|---|---|
| 1 | POST /api/auth/hirer/login | Called and bound from UI | src/app/features/recruiter/services/recruiter-auth.service.ts:52, src/app/features/recruiter/pages/recruiter-login/recruiter-login.component.ts:91, src/app/features/recruiter/pages/recruiter-login/recruiter-login.component.html (form has [formGroup] + (ngSubmit)) | OK |
| 2 | POST /api/auth/hirer/register | Service exists but UI is not wired + payload mismatch | src/app/features/recruiter/services/recruiter-auth.service.ts:74, src/app/features/recruiter/services/recruiter-auth.service.ts:12, src/app/features/recruiter/pages/recruiter-register/recruiter-register.component.ts:62, src/app/features/recruiter/pages/recruiter-register/recruiter-register.component.html:140 | NOT OK |
| 3 | GET /api/account/roles/hirer | Called but response type mismatch | src/app/features/recruiter/services/recruiter-auth.service.ts:93, HIRER_API_REPORT.md:31 | NOT OK |
| 4 | GET /api/hirer/applications/jobs/{jobId}/candidates/{pageIndex}/{pageSize} | Called from candidate page | src/app/features/recruiter/services/recruiter-account.service.ts:65, src/app/features/recruiter/pages/candidate-list/candidate-list.component.ts:63 | PARTIAL (data mapping mismatch, see section 3) |
| 5 | GET /api/hirer/resumes/users/{email} | Called and bound | src/app/features/recruiter/services/recruiter-resume.service.ts:36, src/app/features/recruiter/pages/candidate-list/candidate-list.component.ts:129 | OK |
| 6 | GET /api/hirer/resumes/{id}/view | Called and bound | src/app/features/recruiter/services/recruiter-resume.service.ts:57, src/app/features/recruiter/pages/candidate-list/candidate-list.component.ts:151 | OK |
| 7 | GET /api/hirer/resumes/{id}/download | Called and bound | src/app/features/recruiter/services/recruiter-resume.service.ts:67, src/app/features/recruiter/pages/candidate-list/candidate-list.component.ts:162 | OK |
| 8 | POST /api/hirer/jobs (multipart) | Service exists, page TS has submit logic, but HTML is not wired to form | src/app/features/recruiter/services/recruiter-jobs.service.ts:140, src/app/features/recruiter/pages/recruiter-post-job/recruiter-post-job.component.ts:73, src/app/features/recruiter/pages/recruiter-post-job/recruiter-post-job.component.html:203 | NOT OK |
| 9 | PUT /api/hirer/jobs/{id} (multipart) | Service method exists, no page flow uses it | src/app/features/recruiter/services/recruiter-jobs.service.ts:160, (search in recruiter pages: no updateJob call) | NOT OK |
| 10 | DELETE /api/hirer/jobs/{id} | Called from list page | src/app/features/recruiter/services/recruiter-jobs.service.ts:116, src/app/features/recruiter/pages/recruiter-job-list/recruiter-job-list.component.ts:133 | OK |
| 11 | GET /api/hirer/jobs/posted | Called from job list and candidate list | src/app/features/recruiter/services/recruiter-jobs.service.ts:57, src/app/features/recruiter/pages/recruiter-job-list/recruiter-job-list.component.ts:27, src/app/features/recruiter/pages/candidate-list/candidate-list.component.ts:34 | PARTIAL (model/UI expect extra status field not in report) |
| 12 | GET /api/hirer/jobs/posted/count | Called | src/app/features/recruiter/services/recruiter-jobs.service.ts:78, src/app/features/recruiter/pages/recruiter-job-list/recruiter-job-list.component.ts:28 | OK |

## 2) APIs/Flows Missing Or Need Frontend Implementation

These are the concrete API tasks still needed in frontend:

1. Register flow wiring (API #2)
   - Current issue:
     - Register template does not bind to reactive form submit.
     - Payload contract does not match RegistationForm in report.
   - Required frontend contract:
     - fullName
     - username (from email input)
     - password
     - confirmPassword
   - Evidence:
     - HIRER_API_REPORT.md:30, HIRER_API_REPORT.md:50, HIRER_API_REPORT.md:52
     - src/app/features/recruiter/pages/recruiter-register/recruiter-register.component.html:140
     - src/app/features/recruiter/pages/recruiter-register/recruiter-register.component.ts:62

2. Post job create flow wiring + payload alignment (API #8)
   - Current issue:
     - Post job template is static and not bound to postJobFG.
     - FormData keys do not match JobDTO contract.
   - Required frontend contract keys:
     - jobName, addressId, jobType, salary, jobDescription, jobRequirement, jobSkill, deadlineCV, hirerId, moreDetail, headcount
   - Evidence:
     - HIRER_API_REPORT.md:36, HIRER_API_REPORT.md:56, HIRER_API_REPORT.md:57, HIRER_API_REPORT.md:64, HIRER_API_REPORT.md:66
     - src/app/features/recruiter/pages/recruiter-post-job/recruiter-post-job.component.ts:30
     - src/app/features/recruiter/pages/recruiter-post-job/recruiter-post-job.component.ts:33
     - src/app/features/recruiter/pages/recruiter-post-job/recruiter-post-job.component.ts:38
     - src/app/features/recruiter/pages/recruiter-post-job/recruiter-post-job.component.ts:40
     - src/app/features/recruiter/pages/recruiter-post-job/recruiter-post-job.component.ts:41
     - src/app/features/recruiter/pages/recruiter-post-job/recruiter-post-job.component.html:203

3. Job update flow using API #9
   - Current issue:
     - updateJob exists in service but no UI flow calls it.
   - Required implementation:
     - Edit job screen/modal with multipart payload mapping to JobDTO.
   - Evidence:
     - HIRER_API_REPORT.md:37
     - src/app/features/recruiter/services/recruiter-jobs.service.ts:160

## 3) Data Binding/Contract Mismatch Audit (Including Common APIs)

### 3.1 Roles API data type mismatch
- Backend report: data is Boolean.
- Frontend service expects string[] and stores to hirerRoles signal.
- Evidence:
  - HIRER_API_REPORT.md:31
  - src/app/features/recruiter/services/recruiter-auth.service.ts:93

Impact:
- Runtime binding risk and wrong state meaning in dashboard auth-role check flow.

### 3.2 Register DTO mismatch + validation mismatch
- Backend register contract uses username/email + confirmPassword and password min length 8.
- Frontend payload currently sends companyName, email, phone and does not send confirmPassword to API layer.
- Frontend validator uses minLength(6).
- Evidence:
  - HIRER_API_REPORT.md:50, HIRER_API_REPORT.md:51, HIRER_API_REPORT.md:52
  - src/app/features/recruiter/services/recruiter-auth.service.ts:14
  - src/app/features/recruiter/services/recruiter-auth.service.ts:16
  - src/app/features/recruiter/pages/recruiter-register/recruiter-register.component.ts:23

Impact:
- API 400/validation failure even if UI looks valid.

### 3.3 CandidateDTO projection mismatch
- Backend CandidateDTO fields: fullName, email, fileName, applyDate.
- Frontend candidate VM expects role, experience, status and builds status chips.
- Evidence:
  - HIRER_API_REPORT.md:70, HIRER_API_REPORT.md:73, HIRER_API_REPORT.md:74
  - src/app/features/recruiter/services/recruiter-account.service.ts:29
  - src/app/features/recruiter/services/recruiter-account.service.ts:31
  - src/app/features/recruiter/services/recruiter-account.service.ts:33
  - src/app/features/recruiter/pages/candidate-list/candidate-list.component.html (columns include role/experience/status)

Impact:
- UI displays fallback values and does not expose fileName/applyDate from API #4.

### 3.4 Posted jobs model expects status not present in report JobResponse
- Backend JobResponse in report does not list status.
- Frontend model and UI read job.status for badge rendering.
- Evidence:
  - HIRER_API_REPORT.md:87
  - src/app/shared/models/jobs/job-api-response.model.ts:64
  - src/app/features/recruiter/pages/recruiter-job-list/recruiter-job-list.component.html:44

Impact:
- Status badge can become empty/incorrect without defensive handling.

### 3.5 Common API usage in recruiter: job detail page uses shared job endpoint
- Recruiter detail page calls common JobService.getDetailJob(id), not hirer-specific endpoint.
- Evidence:
  - src/app/features/recruiter/pages/recruiter-job-detail/recruiter-job-detail.component.ts:5
  - src/app/features/recruiter/pages/recruiter-job-detail/recruiter-job-detail.component.ts:64

Impact:
- If public job detail schema diverges from recruiter needs, recruiter detail page can drift.

## 4) Recommended Priority Plan

P0 (must do first):
1. Wire register page template to reactive form and align payload with RegistationForm.
2. Wire post-job page template to reactive form submit and map FormData keys exactly to JobDTO.
3. Fix roles API data type contract (boolean) in recruiter auth service.

P1:
1. Implement update job UI flow (edit + submit PUT /hirer/jobs/{id}).
2. Align candidate list VM/table with CandidateDTO projection returned by API #4.
3. Make job status optional in recruiter job list model/UI.

P2:
1. Re-check recruiter job detail dependency on shared /jobs/{id} endpoint after P0/P1 are done.
