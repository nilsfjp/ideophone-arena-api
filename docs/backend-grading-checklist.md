# Backend grading checklist

Date: 2026-06-04
Repository: `/code/java/ideophone-arena-api`

## Purpose

This checklist maps the backend implementation to the official Spring Boot project guidelines. It is used for grading readiness, architecture review, and agent steering.

Do not mark an item complete unless there is concrete evidence in the repository or a proof command.

Status markers:

- `[ ]` not done
- `[~]` partly done or needs review
- `[x]` done and proven
- `[!]` blocker or grading risk

## Required backend architecture

### Project structure

- [x] Source code is organized by concern, not by random feature placement.
- [x] Controllers are under a controller package.
- [x] Services are under a service package.
- [x] Repositories are under a repository package.
- [x] Entities/models are under an entity or model package.
- [x] DTOs are under a dto package.
- [x] Mappers are under a mapper package or are clearly separated mapping components.
- [x] Security configuration is under a config/security package.
- [x] Exceptions and global error handling are clearly separated.

Evidence:

```text
src/main/java/io/github/nilsfjp/ideophonearena/{controller,service,repository,model,dto,mapper,config,security,exception}
```

Proof:

```bash
find src/main/java -maxdepth 5 -type d | sort
```

### Controllers

- [x] Every controller method returns `ResponseEntity`.
- [x] Successful GET endpoints return `200 OK`.
- [x] Successful POST endpoints that create resources return `201 Created`.
- [x] Successful DELETE endpoints, if any, return `204 No Content`.
- [x] Not-found cases return `404 Not Found`.
- [x] Invalid request data returns `400 Bad Request`.
- [x] Authentication failures return `401 Unauthorized`.
- [x] Authorization failures return `403 Forbidden`, if roles are used.
- [x] Controllers accept request DTOs, not entities.
- [x] Controllers return response DTOs, not entities.
- [x] Controllers do not call repositories directly.
- [x] Controllers do not contain business logic.
- [x] Controllers delegate work to services.
- [x] Controllers use `@Valid` on request bodies where DTO validation exists.

Files to inspect:

```text
src/main/java/.../controller/*.java
```

Proof:

```bash
rg -n "public ResponseEntity" src/main/java/io/github/nilsfjp/ideophonearena/controller
rg -n "Repository|repository" src/main/java/io/github/nilsfjp/ideophonearena/controller
rg -n "@RequestBody|@Valid" src/main/java/io/github/nilsfjp/ideophonearena/controller
```

2026-06-04 evidence: `rg -n "public ResponseEntity" .../controller` found all controller endpoints returning `ResponseEntity`; repository grep found no controller repository injection; request-body grep found `@Valid` on register, login, start-session, and submit-answer bodies. `./mvnw test` passed with MockMvc coverage for public/protected endpoints and unsupported difficulty returning `400`.

### Services

- [x] Services contain business logic.
- [x] Services do not know about HTTP status codes.
- [x] Services do not return `ResponseEntity`.
- [x] Services return DTOs or domain results intended for mapping, according to the current architecture.
- [x] Service write operations use `@Transactional` where multiple database actions must succeed together.
- [x] Service read operations use `@Transactional(readOnly = true)` where appropriate.
- [x] Services use repositories for persistence.
- [~] Services use mappers for entity/DTO conversion.
- [x] Services throw named custom exceptions for expected failures.
- [x] Services validate business rules, such as duplicate username, unsupported difficulty, already answered round, invalid selected ideophone, or session ownership.

Files to inspect:

```text
src/main/java/.../service/*.java
```

Proof:

```bash
rg -n "ResponseEntity" src/main/java/io/github/nilsfjp/ideophonearena/service
rg -n "@Transactional" src/main/java/io/github/nilsfjp/ideophonearena/service
rg -n "BadRequestException|ConflictException|ForbiddenException|ResourceNotFoundException|AuthenticationFailedException" src/main/java/io/github/nilsfjp/ideophonearena/service
```

2026-06-04 evidence: no `ResponseEntity` references exist under `service`; `AuthService`, `GameService`, and `ScoreService` have transactional boundaries. `GameService` now rejects unsupported difficulty with `BadRequestException`, owns answer correctness evaluation, and rejects duplicate or invalid answers. Mapping is mostly in `GameMapper`, while `AuthService` and answer feedback still construct small response DTOs directly. Open Session in View is disabled in `application.properties`.

### Repositories

- [x] Repositories extend Spring Data JPA repository interfaces.
- [x] Repositories contain no business logic.
- [x] Repositories are never injected into controllers.
- [x] Derived query methods are used where sufficient.
- [x] Custom `@Query` methods use bound parameters.
- [x] No SQL or JPQL string concatenation is used.
- [x] No raw JDBC access exists unless explicitly justified.

Files to inspect:

```text
src/main/java/.../repository/*.java
```

Proof:

```bash
rg -n "@Query|createQuery|JdbcTemplate|EntityManager" src/main/java/io/github/nilsfjp/ideophonearena
rg -n "Repository|repository" src/main/java/io/github/nilsfjp/ideophonearena/controller
```

2026-06-04 evidence: repository interfaces extend `JpaRepository`; custom JPQL in `ArenaRoundRepository` and `PlayerAnswerRepository` uses named parameters; no raw JDBC access or controller repository injection was found.

### Entities and database model

- [x] Entities represent database structure only.
- [x] Entities do not contain API response shaping logic.
- [x] Entities do not contain business workflow logic.
- [x] Entities are not returned directly from controllers.
- [x] Entity table and column constraints match the SQL schema.
- [x] Primary keys use the expected generation strategy.
- [x] Relationships are expressed clearly with JPA annotations.
- [x] Sensitive fields such as password hashes are never exposed in response DTOs.
- [x] The schema supports the MVP: users, ideophones, rounds, sessions, answers, leaderboard/history source data.

Files to inspect:

```text
src/main/java/.../entity/*.java
src/main/resources/*.sql
docs/backend-contract.md
```

Proof:

```bash
rg -n "passwordHash|getPasswordHash|password_hash|password" src/main/java/io/github/nilsfjp/ideophonearena/dto src/main/java/io/github/nilsfjp/ideophonearena/mapper src/main/java/io/github/nilsfjp/ideophonearena/controller
rg -n "@Entity" src/main/java/io/github/nilsfjp/ideophonearena/model
```

2026-06-04 evidence: entities are under `model`, controllers import DTOs/services rather than entities, `RoundResponseSerializationTests` verifies no answer-choice `gloss` leaks, and `./mvnw test` validates JPA mappings against the local MySQL schema.

### DTOs

- [x] Request DTOs are separate from response DTOs where appropriate.
- [x] DTOs contain only fields needed by the API flow.
- [x] DTOs have no JPA annotations.
- [x] DTOs contain no database logic.
- [x] Response DTOs never include password hashes or internal-only fields.
- [x] Request DTOs use Bean Validation for required fields and basic format constraints.
- [x] Validation errors are handled cleanly by the global exception handler or Spring validation handling.

Files to inspect:

```text
src/main/java/.../dto/*.java
```

Proof:

```bash
rg -n "jakarta.validation" src/main/java/io/github/nilsfjp/ideophonearena/dto
rg -n "@Entity" src/main/java/io/github/nilsfjp/ideophonearena/dto
```

2026-06-04 evidence: request DTOs use Bean Validation, including positive answer IDs; DTO package grep found no JPA annotations; password fields appear only on login/register request DTOs, not responses.

### Mappers

- [x] Mapping is handled in dedicated mapper classes or mapper methods.
- [x] Controllers do not perform entity/DTO mapping.
- [x] Repositories do not perform entity/DTO mapping.
- [x] Mappers do not call repositories.
- [x] Mappers do not contain business logic.
- [x] Mapping avoids leaking entity relationships or internal fields into API responses.

Files to inspect:

```text
src/main/java/.../mapper/*.java
```

Proof:

```bash
find src/main/java -type f | grep -i mapper
rg -n "repository|Repository" src/main/java/io/github/nilsfjp/ideophonearena/mapper
```

2026-06-04 evidence: `GameMapper` maps session, round, attempt, and ideophone choice responses; mapper grep found no repository access; DTO serialization tests guard against leaking `correctIdeophoneId` or `gloss` before answer submission.

## Security checklist

### Authentication and password handling

- [x] Spring Security is included and configured.
- [x] Registration hashes passwords with BCrypt before saving.
- [x] Plaintext passwords are never stored.
- [x] Login verifies raw password against BCrypt hash.
- [x] Login returns a JWT response DTO.
- [x] JWT is used for protected API requests.
- [x] JWT filter validates bearer tokens.
- [x] Spring Security loads users from the database.
- [x] User roles are stored with the expected `ROLE_` prefix.
- [x] Role-aware authentication exists even if only `ROLE_USER` is currently used.

Files to inspect:

```text
src/main/java/.../security/*.java
src/main/java/.../config/*.java
src/main/java/.../service/*Auth*.java
src/main/java/.../service/*UserDetails*.java
```

Proof:

```bash
rg -n "BCryptPasswordEncoder|PasswordEncoder|ROLE_|Bearer|Jwt" src/main/java/io/github/nilsfjp/ideophonearena
```

2026-06-04 evidence: `SecurityConfig` defines `BCryptPasswordEncoder`; `AuthService` hashes on register and matches on login; `JwtAuthenticationFilter` reads `Authorization: Bearer`; `ArenaUserDetailsService` loads users from `AppUserRepository`; `Role` values use `ROLE_` prefixes.

### Authorization and endpoint access

- [x] Public endpoints are intentionally public.
- [x] Auth endpoints are public.
- [x] Leaderboard is public if intended by the demo contract.
- [x] Stimuli under `/stimuli/**` are public or otherwise accessible to the frontend.
- [x] Game session and answer endpoints require authentication.
- [x] Personal attempt history requires authentication.
- [x] `anyRequest().authenticated()` or equivalent catch-all exists.
- [x] CSRF is disabled only because JWT/stateless authentication is used.
- [x] Sessions are stateless if JWT is the authentication mechanism.

Expected contract:

```text
POST /api/auth/register          public
POST /api/auth/login             public
POST /api/game/sessions          authenticated
GET  /api/game/sessions/{uuid}/rounds/next  authenticated
POST /api/game/sessions/{uuid}/answers      authenticated
GET  /api/game/me/attempts       authenticated
GET  /api/leaderboard            public
GET  /stimuli/**                 public
```

Proof:

```bash
rg -n "SecurityFilterChain|requestMatchers|csrf|SessionCreationPolicy|anyRequest" src/main/java/io/github/nilsfjp/ideophonearena/config/SecurityConfig.java
```

2026-06-04 evidence: `SecurityConfig` permits `/api/auth/**`, `/api/leaderboard`, static frontend files, and `/stimuli/**`, then uses `anyRequest().authenticated()`. CSRF is disabled with stateless sessions for JWT. Live curl proof returned `401` for unauthenticated `POST /api/game/sessions` and `200 video/mp4` for `GET /stimuli/a0hu-gosogoso.mp4`.

### CORS

- [x] CORS is configured explicitly.
- [x] Local frontend origin `http://localhost:5174` is allowed.
- [x] `http://localhost:5173` may also be allowed for Vite fallback.
- [x] Allowed methods include `GET`, `POST`, and `OPTIONS`.
- [x] CORS is integrated with Spring Security.
- [x] Wildcard origins are avoided unless explicitly justified for local-only development.

Proof:

```bash
rg -n "CorsConfiguration|setAllowedOrigins|setAllowedMethods|cors\\(" src/main/java/io/github/nilsfjp/ideophonearena/config src/main/resources/application.properties src/main/resources/application-local.example.properties
```

2026-06-04 evidence: `SecurityConfig` registers a `CorsConfigurationSource`, calls `.cors(...)`, allows `http://localhost:5174` and `http://localhost:5173`, and avoids wildcard origins.

## Error handling

- [x] Named exceptions exist for expected failure cases.
- [x] There is a global `@ControllerAdvice`.
- [x] Errors return meaningful JSON or text responses.
- [x] Expected client errors do not leak stack traces.
- [x] Generic fallback errors do not expose internal implementation details.
- [x] Invalid credentials return a safe message.
- [x] Invalid session, invalid round, invalid answer, duplicate username/email, and unsupported difficulty are handled intentionally.
- [x] Normal game completion is not treated as an unhandled server error.
- [x] If completion currently uses a 404 response, it is documented as a known compromise or replaced with a clearer `200 OK` or `204 No Content` response.

Files to inspect:

```text
src/main/java/.../exception/*.java
```

Proof:

```bash
rg -n "@RestControllerAdvice|@ExceptionHandler|extends RuntimeException" src/main/java/io/github/nilsfjp/ideophonearena/exception
```

2026-06-05 evidence: `GlobalExceptionHandler` maps validation, unreadable request bodies, bad request, auth failure, forbidden, not found, conflict, Spring authentication, and unexpected errors to JSON. The generic fallback returns `500` with `An unexpected error occurred`, without stack traces or internal exception details. Completion no longer uses the not-found exception path; `GET /api/game/sessions/{sessionUuid}/rounds/next` returns `200 OK` with `completed:true` and message `Game session is complete`.

## Game domain correctness

### Session start

- [x] Session start requires authenticated user.
- [x] Session start accepts a request DTO.
- [x] Session start rejects unsupported difficulty values or normalizes to supported values intentionally.
- [x] Session start rejects unsupported condition names or handles them intentionally.
- [x] For the current demo, `difficultyLevel: 1` is supported.
- [x] For the current demo, `CONDITION_1_SOKUON` is supported.
- [x] Session ownership is stored and enforced.

Proof:

```bash
curl -i -X POST http://localhost:8081/api/game/sessions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1}'
```

2026-06-04 evidence: `GameController.startSession` is authenticated and accepts `StartSessionRequest`; `GameService.startSession` stores the owner and rejects `difficultyLevel != 1`; `GameLoopHttpTests.startSessionRejectsUnsupportedDifficulty` verifies `400 Bad Request`; `GameLoopHttpTests.startSessionRejectsUnknownConditionName` verifies unknown condition names return `400 Bad Request`. Known seeded condition enum values may work, but `CONDITION_1_SOKUON` is documented as the only final-demo setting.

### Round retrieval

- [x] Next-round endpoint requires authentication.
- [x] A user cannot fetch another user's private session.
- [x] Response uses DTOs.
- [x] Response includes enough data for the frontend to show prompt, left/right ideophones, stimuli, and answer options.
- [x] Completion behavior is intentional and documented.
- [x] Round ordering or selection logic is stable enough for the demo.

Proof:

```bash
curl -i http://localhost:8081/api/game/sessions/<SESSION_UUID>/rounds/next \
  -H "Authorization: Bearer <TOKEN>"
```

2026-06-05 evidence: `GameService.getOwnedSession` enforces owner matching; next-round uses `RoundResponse`; `GameServiceTests.getNextRoundReturnsFirstUnansweredRoundForSession` verifies stable first-unanswered ordering and response fields; `GameServiceTests.getNextRoundReturnsCompletionResponseAfterAllRoundsAreAnswered` verifies the service marks the session complete and returns `completed:true`; `GameLoopHttpTests.nextRoundReturnsExplicitCompletionBodyAfterFinalAnswer` verifies the controller returns `200 OK` with the completion body after the final answer.

### Answer submission

- [x] Answer submission requires authentication.
- [x] Request DTO includes `roundId`, `selectedIdeophoneId`, and optionally `responseTimeMs`.
- [x] Service verifies the session exists.
- [x] Service verifies the round exists.
- [x] Service verifies the selected ideophone is one of the valid options.
- [x] Service calculates correctness server-side.
- [x] Service stores the answer.
- [x] Duplicate answers for the same session/round are rejected or handled intentionally.
- [x] Response DTO gives immediate feedback.
- [x] Stored answers feed leaderboard or history.

Proof:

```bash
curl -i -X POST http://localhost:8081/api/game/sessions/<SESSION_UUID>/answers \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"roundId":1,"selectedIdeophoneId":1,"responseTimeMs":1200}'
```

2026-06-04 evidence: `SubmitAnswerRequest` validates positive IDs; `GameService.submitAnswer` checks session, round, session condition/difficulty, duplicate answer, valid selected ideophone, calculates correctness, saves `PlayerAnswer`, and returns `AnswerResultResponse`. `GameServiceTests` and `GameLoopHttpTests` cover this path.

### Leaderboard and history

- [x] Public leaderboard endpoint exists.
- [x] Leaderboard returns DTOs.
- [x] Leaderboard does not expose sensitive user data.
- [x] Personal recent attempts endpoint exists.
- [x] Recent attempts endpoint requires authentication.
- [x] Recent attempts are scoped to the authenticated user.
- [x] Score/progress fields are honestly named as session-scoped or all-time.

Proof:

```bash
curl -i http://localhost:8081/api/leaderboard

curl -i http://localhost:8081/api/game/me/attempts \
  -H "Authorization: Bearer <TOKEN>"
```

2026-06-04 evidence: `ScoreController` exposes `GET /api/leaderboard` and `GET /api/game/me/attempts`; `SecurityConfig` leaves only leaderboard public; `ScoreService.getMyAttempts` scopes attempts by authenticated user id.

## Configuration and secrets

- [x] MySQL connection is configured through local/profile-specific properties.
- [x] Real credentials are not committed.
- [x] `.gitignore` excludes local secret files.
- [x] Application can start from documented local setup.
- [x] Backend port is documented as `8081` if that is the active port.
- [x] Frontend origin is documented as `http://localhost:5174`.

Proof:

```bash
git status --short
git ls-files src/main/resources/application-local.properties src/main/resources/application-local.example.properties src/main/resources/application.properties .gitignore
rg -n "spring\\.datasource|password=|username=|app\\.jwt\\.secret" src/main/resources/application.properties src/main/resources/application-local.example.properties docs/backend-contract.md docs/demo-runbook.md docs/backend-grading-checklist.md .gitignore
```

2026-06-04 evidence: `application-local.properties` contains local credentials but is ignored and not tracked; `git ls-files` lists only `.gitignore`, `application.properties`, and `application-local.example.properties`; the tracked example uses placeholders. `./mvnw test` starts Spring contexts against local MySQL on port `8081`.

## Build and test proof

- [x] Backend compiles.
- [x] Backend tests pass, or failing tests are documented as blockers.
- [x] Application starts locally.
- [x] Database connects.
- [x] Health or basic endpoint responds.
- [x] Auth flow works from curl or Postman.
- [x] Full game flow works from curl or browser.

Proof:

```bash
./mvnw test
./mvnw spring-boot:run
```

2026-06-05 evidence: `./mvnw test` passed with 16 tests. Spring Boot test contexts started, connected to local MySQL, served static frontend resources through MockMvc, registered users, started sessions, fetched rounds, submitted answers, verified unsupported difficulty and unknown condition names return `400`, and verified next-round completion now returns `200 OK` with `completed:true`. Live curl proof against a freshly started `./mvnw spring-boot:run` process returned: health `200`, register `201`, login `200`, authenticated session start `201`, answered 30 rounds through the API, final next-round completion `200` with `{"completed":true,"message":"Game session is complete","sessionUuid":"5605288c-a2eb-4cec-8b1c-82d45a3957b8","conditionName":"CONDITION_1_SOKUON","difficultyLevel":1,"roundId":null}`, recent attempts `200`, and leaderboard `200`.

## Documentation checklist

- [x] `docs/project_guidelines.md` exists.
- [x] `docs/backend-contract.md` exists.
- [x] `docs/backend-grading-checklist.md` exists as the active grading checklist.
- [x] `docs/progress-log.md` exists or the current progress log location is documented.
- [x] README explains how to start backend.
- [x] README explains required database setup.
- [x] README explains required environment/profile configuration.
- [x] README lists the main endpoints.
- [x] README includes a short demo script.
- [x] Stale `8080` references are removed or clearly marked as old examples.
- [x] Stale `5173` references are corrected if the current frontend port is `5174`.

2026-06-04 evidence: required docs exist in `docs/`; the active checklist file is `docs/backend-grading-checklist.md`; current contract/runbook/resources have no stale backend-port or legacy round-endpoint references, while old `8080` entries remain only in dated `docs/progress-log.md` history. `README.md` documents backend startup, MySQL/local profile setup, endpoint paths, and a curl demo script.

## Agent steering rules

Agents working in this repository must follow these rules:

1. Do not mark checklist items complete without file evidence or a proof command.
2. Do not bypass controller-service-repository separation to make a quick feature work.
3. Do not expose entities from controllers.
4. Do not move HTTP logic into services.
5. Do not add features before unresolved `[!]` grading risks are fixed.
6. Prefer small architecture corrections over broad rewrites.
7. Every change must end with one of:
   - passing test command
   - working curl command
   - successful app startup
   - documented blocker
   - commit hash

## Current known grading risks

Update this section after each architecture pass.

- [x] Completion semantics now use a documented `200 OK` DTO with `completed:true` and message `Game session is complete`.
- [x] Verified all controllers use `ResponseEntity`.
- [x] Verified all request DTOs use Bean Validation where appropriate.
- [x] Verified services do not return `ResponseEntity`.
- [x] Verified no entities leak through API responses.
- [x] Verified no local credentials are tracked.
- [x] Verified CORS includes the active frontend origin.
- [ ] Live browser click-through still needs to be run outside MockMvc.
