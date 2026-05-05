# Agent Guide — Job Portal

This file provides concise, actionable guidance for AI coding agents working in this repository.

## Quick start
- **Frontend dev**: `cd job-frontend && npm install && npm start` (dev server: http://localhost:4200)
- **Backend dev**: `cd job-backend && ./mvnw spring-boot:run` (uses `application-dev.yml` for local config)

## Where to read first
- Overview: [README.md](README.md)
- UI design system: [DESIGN.md](DESIGN.md)
- Frontend entry points: [job-frontend/package.json](job-frontend/package.json) and [job-frontend/src/app](job-frontend/src/app)
- Backend entry points: [job-backend/pom.xml](job-backend/pom.xml) and [job-backend/src/main/java](job-backend/src/main/java)

## Key responsibilities for agents
- **Frontend component work**: follow `job-frontend/SKILL.md` and `DESIGN.md`; place new feature modules under `job-frontend/src/app/features`.
- **Backend API work**: use Controller → Service → Repository pattern; prefer QueryDSL for complex filters in `job-backend/src/main/java/**/queryDSL`.
- **Infrastructure-aware changes**: check env vars and `application-*.yml` before touching DB, cache, or message queues.

## Common commands
- Frontend tests: `cd job-frontend && npm test`
- Backend build: `cd job-backend && mvn clean install`
- Run backend tests: `cd job-backend && mvn test`

## Hotspots (read before editing)
- Security / auth: [job-backend/src/main/java/com/job_web/security](job-backend/src/main/java/com/job_web/security)
- CV parsing & AI: [job-backend/src/main/java/com/job_web/service/application](job-backend/src/main/java/com/job_web/service/application)
- Complex queries: [job-backend/src/main/java/com/job_web/data/queryDSL](job-backend/src/main/java/com/job_web/data/queryDSL)
- Frontend interceptors & auth: [job-frontend/src/app/core/services](job-frontend/src/app/core/services)

## Testing conventions
- Backend: use `@WebMvcTest` for controller slices and `@SpringBootTest` for integrations; see `job-backend/src/test` for patterns.
- Frontend: Karma + Jasmine; mock HTTP with `HttpClientTestingModule`.

## AI integration notes
- Resume parsing pipeline: Apache Tika → LangChain4j/DeepSeek; set `DEEP_SEEK_API_KEY` in env for local testing.
- Job matching uses vector services — read connect code before modifying matching logic.

## Next suggestions
- Consider adding targeted agent skills: `frontend-component`, `backend-endpoint`, and `infra-check` to automate common workflows.

---
If you want, I can now create the suggested skills (`/create-skill frontend-component`, `/create-skill backend-endpoint`).
