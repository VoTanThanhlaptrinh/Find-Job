# Cloudflare R2 Bucket Configuration Guide for CV Presigned Upload

> **IMPORTANT**: Cloudflare R2 bucket CORS policies and Object Lifecycle rules **cannot** be configured through Spring Boot `application.yml`. They must be applied directly via the Cloudflare Dashboard, Cloudflare API, or Terraform/Wrangler.

---

## 1. CORS Configuration (Cross-Origin Resource Sharing)

The frontend uploads CV files directly from the browser to Cloudflare R2 using presigned HTTP `PUT` URLs. Without proper CORS rules, browsers will block these requests with a CORS error.

### Production CORS Policy (Cloudflare Dashboard > R2 > Bucket Settings > CORS Policy)

```json
[
  {
    "AllowedOrigins": [
      "https://job-frontend-gray.vercel.app"
    ],
    "AllowedMethods": [
      "GET",
      "PUT",
      "HEAD"
    ],
    "AllowedHeaders": [
      "Content-Type",
      "x-amz-date",
      "x-amz-content-sha256",
      "Authorization"
    ],
    "ExposeHeaders": [
      "ETag"
    ],
    "MaxAgeSeconds": 3600
  }
]
```

### Local Development CORS Policy

```json
[
  {
    "AllowedOrigins": [
      "http://localhost:4200",
      "http://127.0.0.1:4200"
    ],
    "AllowedMethods": [
      "GET",
      "PUT",
      "HEAD"
    ],
    "AllowedHeaders": [
      "Content-Type",
      "x-amz-date",
      "x-amz-content-sha256",
      "Authorization"
    ],
    "ExposeHeaders": [
      "ETag"
    ],
    "MaxAgeSeconds": 3600
  }
]
```

> [!CAUTION]
> Do NOT use wildcard `"AllowedOrigins": ["*"]` in production because presigned upload tokens and authentication context should remain restricted to verified application domains.

---

## 2. Object Lifecycle Rules (Automatic Temporary File Deletion)

Client files are uploaded directly to the `temp/resumes/{userId}/{uploadId}` prefix. Although `ResumeUploadCleanupJob` on backend attempts best-effort deletion of temporary files upon completion or expiry, network interruptions or client abandons might leave unpromoted files in `temp/`.

An automated Cloudflare R2 Object Lifecycle rule guarantees cleanup:

### Lifecycle Rule Specification

| Setting | Value | Description |
| :--- | :--- | :--- |
| **Rule Name** | `delete-temp-resumes-after-24h` | Descriptive name |
| **Prefix / Scope** | `temp/resumes/` | Targets only temporary files |
| **Action** | Delete object | Automatically deletes matching objects |
| **Age** | `1 day` (24 hours) | Ensures sufficient window for active upload sessions (max 30 mins) while preventing storage bloat |

### Setup via Wrangler CLI (Optional)

```bash
wrangler r2 bucket lifecycle add job-portal-resume \
  --name "delete-temp-resumes-after-24h" \
  --prefix "temp/resumes/" \
  --expire-days 1
```
