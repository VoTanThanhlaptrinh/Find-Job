# Repository Guidelines

## Project Structure & Module Organization

This repository contains two applications. `job-frontend/` is an Angular 20 SSR client; place domain pages and services under `src/app/features/`, cross-cutting guards, interceptors, and layout code under `src/app/core/`, and reusable UI, models, and pipes under `src/app/shared/`. Static files live in `src/assets/` and `public/`.

`job-backend/` is a Java 17 Spring Boot service. Production code is organized by bounded context in `src/main/java/com/nlu/` (`identity`, `recruitment`, `applicationProcess`, `content`, `admin`, and `shared`), then by `api`, `application`, `domain`, `infrastructure`, and `mapper`. Configuration and message bundles are in `src/main/resources/`. Backend tests are in `src/test/java/`; frontend specs are colocated with implementation files. Project-level design notes belong in `docs/`, `DESIGN.md`, or the relevant application README.

## Build, Test, and Development Commands

Run commands from the application directory.

- `npm ci` installs the frontend exactly from `package-lock.json`.
- `npm start` serves Angular locally on port 4200.
- `npm run build` creates the production/SSR bundle in `job-frontend/dist/`.
- `npm test -- --watch=false` runs the Jasmine/Karma suite once.
- `./mvnw spring-boot:run` (or `.\mvnw.cmd spring-boot:run` on Windows) starts the API.
- `./mvnw test` runs Spring Boot, JUnit, and Mockito tests.
- `./mvnw clean package` produces the deployable backend JAR.

Full local flows require the configured database plus Redis and RabbitMQ services.

## Coding Style & Naming Conventions

Frontend files follow `.editorconfig`: UTF-8, two-space indentation, final newlines, and single quotes in TypeScript. Use kebab-case filenames with Angular suffixes such as `job-card.component.ts` and `resume.service.ts`. Keep feature-specific code inside its feature.

For Java, use four-space indentation, PascalCase types, camelCase members, and descriptive suffixes such as `Controller`, `Service`, `Repository`, `Request`, and `Response`. Preserve the existing bounded-context and layer boundaries. No repository-wide lint command is configured, so treat successful builds and tests as the minimum formatting check.

## Testing Guidelines

Name frontend tests `*.spec.ts` beside their subject. Name backend tests `*Test.java` and mirror the behavior or layer under test. Add regression coverage for bug fixes and cover authorization, validation, and failure paths for API changes. There is no enforced coverage threshold; avoid reducing meaningful coverage.

## Commit & Pull Request Guidelines

History uses Conventional Commits: `feat(recruiter): add job preview`, `fix(blog): correct authorization`, and `refactor(backend): ...`. Keep subjects imperative and scoped when practical. Pull requests should summarize behavior changes, identify affected frontend/backend areas, link issues, list verification commands, and include screenshots for UI work. Call out new environment variables, schema changes, or deployment impacts.

## Security & Configuration

Configuration uses environment variables such as `DB_URL`, `SECRET_KEY`, OAuth credentials, mail credentials, and storage keys. Never commit real secrets or generated logs/build output. Keep environment-specific values out of `application*.yml` and frontend source files.
