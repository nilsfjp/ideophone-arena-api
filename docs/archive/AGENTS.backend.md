# AGENTS.md - Ideophone Arena API

## Repository role

This repository is the Spring Boot backend for Ideophone Arena.

The backend is the grading-critical part of the project. It must demonstrate correct Spring Boot architecture, security, persistence, DTO usage, validation, CORS, and a working game API.

The project may continue after the original June 5, 2026 course checkpoint, but the current priority is still correctness and documentation before scope expansion.

Do not treat this as a thesis experiment recreation. It is a small playable web app inspired by Japanese ideophones and cross-modal iconicity.

## Required reading before code changes

Before changing implementation code, inspect these files:

```text
docs/project_guidelines.md
docs/backend-contract.md
docs/backend-grading-checklist.md
docs/demo-runbook.md
docs/progress-log.md
```

Use `docs/project_guidelines.md` as the teacher-facing architecture authority.

Use `docs/backend-contract.md` as the backend/frontend API authority.

Use `docs/backend-grading-checklist.md` as the review checklist before declaring backend work complete.

Use `docs/demo-runbook.md` for demo proof steps.

Update `docs/progress-log.md` after meaningful work sessions.

## Current local contract

Backend base URL:

```text
http://localhost:8081
```

Frontend development origin:

```text
http://localhost:5174
```

It is acceptable for CORS to also allow:

```text
http://localhost:5173
```

Current public/authenticated API contract:

```text
POST /api/auth/register                         public
POST /api/auth/login                            public

POST /api/game/sessions                         authenticated
GET  /api/game/sessions/{sessionUuid}/rounds/next  authenticated
POST /api/game/sessions/{sessionUuid}/answers      authenticated

GET  /api/game/me/attempts                      authenticated
GET  /api/leaderboard                           public

GET  /stimuli/**                                public or frontend-accessible
```

Protected endpoints require:

```text
Authorization: Bearer <token>
```

Do not reintroduce stale endpoint paths such as:

```text
/api/rounds/next
/api/rounds/{roundId}/answer
```

Do not reintroduce stale port assumptions such as:

```text
http://localhost:8080
```

unless documenting historical examples.

## Course requirements

The backend must use and visibly demonstrate:

```text
Spring Boot
Spring Web
Spring Data JPA
Spring Security
MySQL
Explicit CORS configuration
Three-layer architecture
DTO request and response boundaries
Controller, service, repository, entity/model, DTO, mapper, config/security, and exception separation
```

## Architecture rules

Controllers:

- Return `ResponseEntity`.
- Use request DTOs and response DTOs.
- Use `@Valid` on request bodies where validation exists.
- Delegate to services.
- Do not call repositories.
- Do not expose entities.
- Do not contain business logic.

Services:

- Contain business logic.
- Own scoring, answer evaluation, session rules, and transactional work.
- Use repositories for persistence.
- Use mappers for entity/DTO conversion.
- Throw named project exceptions for expected failures.
- Do not return `ResponseEntity`.
- Do not know HTTP status codes.

Repositories:

- Are Spring Data JPA repositories.
- Use derived query methods where possible.
- Use bound parameters for custom queries.
- Do not contain business logic.
- Are never injected into controllers.

Entities/models:

- Represent database structure.
- Define database-related constraints.
- Are not returned directly by controllers.
- Do not contain API presentation logic.
- Do not expose password hashes through DTO mapping.

DTOs:

- Separate request and response shapes where appropriate.
- Use Bean Validation for required fields and basic constraints.
- Must not contain JPA annotations.
- Must not expose internal-only or sensitive fields.

Mappers:

- Keep entity/DTO conversion out of controllers.
- Do not call repositories.
- Do not contain business rules.
- Keep API response shape deliberate.

Exceptions:

- Use named project exceptions for expected failures.
- Use global error handling with `@ControllerAdvice` or equivalent.
- Do not leak stack traces in normal API responses.
- Map expected client errors to clear 400, 401, 403, or 404 responses as appropriate.

## Security rules

Use Spring Security with JWT for the separate frontend.

Use BCrypt for password hashing.

Never store raw passwords.

Never return password hashes.

Use Spring Security-compatible role names such as:

```text
ROLE_USER
ROLE_ADMIN
```

Disable CSRF only because the API uses stateless JWT authentication.

Keep protected game and personal-history endpoints authenticated.

Keep public endpoints intentionally public.

CORS must be configured explicitly for the frontend origin. Do not rely on browser workarounds or a frontend proxy as the only solution.

## Game-domain rules

The MVP game loop is:

1. Register or log in.
2. Start an authenticated game session.
3. Fetch the next round.
4. Present two ideophone options.
5. Submit one selected ideophone.
6. Evaluate correctness on the backend.
7. Store the answer.
8. Return immediate feedback.
9. Show recent attempts or leaderboard.

Current supported demo settings:

```json
{
  "conditionName": "CONDITION_1_SOKUON",
  "difficultyLevel": 1
}
```

Other condition enum values may exist, but do not expand condition or difficulty behavior unless `docs/backend-contract.md` is updated.

Only difficulty level 1 is currently safe for the demo unless the seed data and service logic prove otherwise.

Reject unsupported difficulty or condition values clearly, or document the current behavior as a known limitation.

Normal game completion should be intentional. Prefer a clear completion response or `204 No Content` over treating completion as an accidental server error. If the implementation currently uses `404` for "no next round", document and handle it deliberately.

## Database and seed-data rules

Use MySQL.

Do not switch databases.

Keep credentials out of git.

It is acceptable to commit safe defaults, but local usernames, passwords, ports, and JWT secrets must stay in ignored local properties or environment variables.

Project data and course resources may exist under:

```text
course-docs/
project-resources/
stimuli/
scripts/
```

Do not move large media or course-resource files unless the task explicitly requires it.

Do not edit generated build output under:

```text
target/
```

Do not add Flyway, Liquibase, Docker, CI, or deployment work unless there is a concrete project need and the grading checklist is already in good shape.

## Scope control

Current priority order:

1. Satisfy the backend grading checklist.
2. Keep the documented backend contract accurate.
3. Prove the full register, login, session, round, answer, storage, leaderboard/history flow.
4. Fix architecture violations.
5. Improve documentation.
6. Only then consider feature expansion.

Avoid until the core is correct:

```text
admin panels
unlockable levels
rich statistics
new difficulty systems
new condition systems
thesis-style counterbalancing
large security rewrites
frontend-driven mock game logic
dataset redesign
deployment work
infrastructure work
```

If a proposed change does not improve grading readiness, API correctness, or the demonstrable MVP, defer it.

## Code style

Keep Java code simple and course-level.

Use constructor injection.

Avoid Lombok unless already used consistently.

Avoid new frameworks unless already present or explicitly requested.

Do not introduce unnecessary abstractions.

Prefer explicit classes and methods over clever generic designs.

Use ASCII-only comments and shell snippets.

Avoid broad rewrites. First inspect the current code. Patch the smallest coherent area.

## Proof commands

From the repository root, prefer:

```sh
./mvnw test
```

```sh
./mvnw spring-boot:run
```

If Maven wrapper is not usable:

```sh
mvn test
```

```sh
mvn spring-boot:run
```

Useful curl proof shape:

```sh
curl -i http://localhost:8081/api/leaderboard
```

Register:

```sh
curl -i -X POST http://localhost:8081/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","email":"testuser@example.com","password":"password123"}'
```

Login:

```sh
curl -i -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"password123"}'
```

Start session:

```sh
curl -i -X POST http://localhost:8081/api/game/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1}'
```

Next round:

```sh
curl -i http://localhost:8081/api/game/sessions/$SESSION_UUID/rounds/next \
  -H "Authorization: Bearer $TOKEN"
```

Submit answer:

```sh
curl -i -X POST http://localhost:8081/api/game/sessions/$SESSION_UUID/answers \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"roundId":1,"selectedIdeophoneId":1,"responseTimeMs":1200}'
```

Recent attempts:

```sh
curl -i http://localhost:8081/api/game/me/attempts \
  -H "Authorization: Bearer $TOKEN"
```

Adjust request bodies only after inspecting the actual DTOs and `docs/backend-contract.md`.

## Documentation rules

Keep docs aligned with implementation.

When endpoint paths, ports, DTOs, auth behavior, CORS origins, or completion behavior change, update:

```text
docs/backend-contract.md
docs/backend-grading-checklist.md
docs/demo-runbook.md
```

When a work session ends, append to:

```text
docs/progress-log.md
```

Use this format:

```text
## YYYY-MM-DD

Session goal:
Changed:
Proof:
Result:
Commit:
Blocker:
Next single task:
```

## Agent completion rule

A backend change is not complete until the response includes:

1. What changed.
2. Why it was necessary.
3. What files were changed.
4. A proof command and result, or the exact blocker.
5. The next single task.

At least one of these must exist before claiming success:

```text
passing ./mvnw test
successful app startup
working curl/Postman proof
updated checklist item with evidence
commit hash
written blocker with exact error output
```

Do not claim a feature is done because the code looks plausible.
