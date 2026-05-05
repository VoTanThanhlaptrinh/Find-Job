# CV Key Features - Job Portal

Ghi chu:
Uu tien chon 4-6 bullet dau tien neu CV can gon. Cac bullet ben duoi dung khi ban can mo rong phan project experience hoac interview talking points.

## Tech Stack

- Frontend: Angular 20, TypeScript, Angular SSR, Angular Material, Tailwind CSS, RxJS, ngx-quill, Express.
- Backend: Java 17, Spring Boot 3.3, Spring Web, Spring Security, Spring Data JPA, Spring Validation, Spring Mail, OAuth2 Client/Resource Server.
- Database va truy van: PostgreSQL, JPA/Hibernate, QueryDSL.
- Authentication va security: JWT (JJWT), refresh token rotation, RBAC, Google OAuth2 login, BCrypt.
- Infrastructure va async processing: Redis, RabbitMQ, Docker.
- File/AI/document processing: Apache Tika, LangChain4j, OpenAI-compatible integration, jsoup, Cloud/S3 SDK.
- Testing va tooling: JUnit, Spring Boot Test, Spring Security Test, Mockito, Selenium, Maven, npm.

## Ban Ngan Gon De Dua Vao CV

- Xay dung nen tang tuyen dung full-stack voi 3 luong nguoi dung tach biet: Candidate, Recruiter va Admin; bao gom dang ky/dang nhap, quan ly ho so, dang tin tuyen dung, theo doi ung vien va dashboard quan tri.
- Thiet ke he thong xac thuc va phan quyen da lop voi JWT access token, refresh token xoay vong trong HttpOnly cookie, RBAC theo role, kich hoat tai khoan qua email, quen/dat lai mat khau va Google OAuth2 login.
- Trien khai pipeline xu ly CV bat dong bo: upload CV, trich xuat text bang Apache Tika kem OCR fallback, luu file tren Cloudflare R2, tao pre-signed URL de xem/tai xuong va day trang thai phan tich realtime qua SSE.
- Tich hop AI resume parsing va job matching: phan tich CV bang LangChain4j + DeepSeek, vector hoa CV/JD qua AI service rieng, va goi y cong viec dua tren similarity query.
- Phat trien bo chuc nang recruiter end-to-end: dang ky nha tuyen dung, CRUD job posting, quan ly danh sach job da dang, xem danh sach ung vien va truy cap CV cua ung vien theo quyen.
- Xay dung frontend Angular SSR theo feature-based architecture, lazy-loaded routes, Angular Signals, i18n VI/EN va cac dashboard rieng cho candidate, recruiter, admin.
- Thiet ke backend huong production voi Spring Boot, Redis caching + rate limiting, RabbitMQ cho mail/upload/AI jobs, request tracing bang correlation ID va masking payload nhay cam trong logs.

## Ban Day Du Hon Cho Project Description

- Ho tro day du candidate journey: dang ky, dang nhap, kich hoat email, cap nhat profile, doi mat khau, quan ly nhieu CV, ung tuyen bang CV co san hoac upload CV moi, va theo doi lich su ung tuyen.
- Ho tro recruiter journey rieng: onboarding recruiter, quan ly thong tin cong ty/dia chi, tao-sua-xoa job post, thong ke job da dang, xem danh sach candidate theo tung job va review/download CV.
- Xay dung public job discovery flow gom trang home, newest jobs, job detail, bo loc theo muc luong/khu vuc/loai hinh lam viec, va dem so luong job theo dia diem.
- Phat trien module blog co tao/sua/xoa bai viet, rich-text editing, comment, like/unlike, phu hop cho noi dung employer branding hoac career content.
- Su dung xu ly bat dong bo bang RabbitMQ de tach mail sending, cloud upload va AI parsing khoi request-response, giup giam do tre va on dinh trai nghiem nguoi dung.
- Ap dung Redis cho cache home/category data va rate limiting theo IP, giup toi uu response time va han che abuse tren public APIs.
- Trien khai logging co trace/correlation ID, sanitize body/query de mask thong tin nhay cam, ho tro debug va quan sat request flow tot hon trong moi truong production.
- Xay dung co che refresh token theo token family/JTI rotation de giam rui ro replay token va cai thien session security.
- Phat trien admin modules cho employer management, job seeker management, pending job moderation, bulk job actions va billing dashboard/screens.
- Bo sung test coverage cho backend voi nhieu WebMvcTest/SpringBootTest cho auth, jobs, admin, blog, resume, application va notification flows.

## Phien Ban Tieng Anh Neu Can Dan Thang Vao CV

- Built a full-stack recruitment platform with separate Candidate, Recruiter, and Admin journeys, covering authentication, profile management, job posting, applicant tracking, and admin operations.
- Designed a layered authentication system using JWT access tokens, rotating refresh tokens in HttpOnly cookies, role-based authorization, email activation, password recovery, and Google OAuth2 login.
- Implemented an asynchronous resume-processing pipeline with CV upload, Apache Tika text extraction plus OCR fallback, Cloudflare R2 storage, pre-signed file URLs, and SSE-based real-time status updates.
- Integrated AI-assisted resume parsing and job matching using LangChain4j + DeepSeek, with external CV/JD vectorization and similarity-based job recommendation.
- Built the system with production-oriented concerns in mind, including Redis caching and rate limiting, RabbitMQ-based background jobs, structured request tracing, and sensitive payload masking in logs.
