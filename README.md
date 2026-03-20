# Job Portal Web

A full-stack recruitment platform built with `job-frontend` and `job-backend`, covering both candidate and recruiter journeys: registration, account verification, job discovery, CV management, job application, job posting, and applicant tracking.

## Live Demo

- Frontend-Seeker: `https://findjob-neon.vercel.app/`
- Frontend-Recruiter: `https://findjob-neon.vercel.app/recruiter/login`
- Backend API: `https://find-job-ctkj.onrender.com`
- Note: replace both links above with your real deployed URLs so HR can access the project directly.

## Key Strengths

- Clear and realistic product scope: separate flows for `Candidate` and `Recruiter`, not just a landing page or basic CRUD.
- Modern Angular frontend with feature-based structure, SSR support, and domain separation across `auth`, `jobs`, `employer`, `blog`, and `site`.
- Spring Boot 3 backend organized with `controller -> service -> repository`, making the codebase easier to maintain and extend.
- Stronger-than-basic authentication flow with `JWT access token`, `refresh token` in `HttpOnly cookie`, email activation, password recovery, and OAuth2 login.
- Good system design thinking for performance and scale: Redis for temporary state and anti-spam protection, RabbitMQ for asynchronous email processing.
- More technical depth than a typical student CRUD project: job filtering with JPA Specification, CV upload and management, application history, blog module, and AI-oriented resume parsing/verification.
- Production-oriented setup already considered: `application-prod.yml`, environment-based configuration, backend `Dockerfile`, and frontend API URL switching by environment.

## Main Features

### Candidate

- Register, log in, and activate an account via email
- Forgot password and password reset flow
- Manage profile information and CVs
- Apply with an existing CV or upload a new one
- View application history

### Recruiter

- Dedicated recruiter login and registration flow
- Create, update, and delete job postings
- View posted jobs and applicant volume
- Dedicated recruiter dashboard

### Public Content

- Home page with job feed data
- Newest jobs, job details, and filtering experience
- Blog and informational pages such as `about` and `contact`

## Tech Stack

- Frontend: Angular 20, Angular SSR, Angular Material, Tailwind CSS, RxJS
- Backend: Spring Boot 3, Spring Security, Spring Data JPA, MySQL
- Auth/Security: JWT, Refresh Token, OAuth2, BCrypt
- Infrastructure: Redis, RabbitMQ, Docker
- AI/Document Processing: LangChain4j, Gemini, Apache Tika

## Project Structure

- `job-frontend`: user interface, SSR, and candidate/recruiter/public pages
- `job-backend`: REST API, authentication, business logic, database, Redis, RabbitMQ, and email flow

## Run Locally

### Frontend

```bash
cd job-frontend
npm install
npm start
```

### Backend

```bash
cd job-backend
./mvnw spring-boot:run
```

The backend also requires supporting services and environment variables such as MySQL, Redis, RabbitMQ, Gmail App Password, and OAuth client credentials if you want to run the full application flow locally.
