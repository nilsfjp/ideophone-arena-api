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

2026-06-07 evidence: no `ResponseEntity` references exist under `service`; `AuthService`, `GameService`, and `ScoreService` have transactional boundaries. `GameService` rejects unsupported session-start difficulty, rejects unsupported session-start conditions such as `TEXT_ONLY`, owns answer correctness evaluation, and rejects duplicate or invalid answers. Mapping is mostly in `GameMapper`, while `AuthService` and answer feedback still construct small response DTOs directly. Open Session in View is disabled in `application.properties`.

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

2026-06-07 evidence: request DTOs use Bean Validation, including required session-start `conditionName`, required positive `difficultyLevel`, and positive answer IDs; DTO package grep found no JPA annotations; password fields appear only on login/register request DTOs, not responses.

2026-06-10 evidence: Bean Validation completed — `SubmitAnswerRequest.responseTimeMs` is now `@NotNull @Min(0) @Max(600000)`; redundant null presence checks were removed from `GameService.validateSupportedStartRequest` (business rules — supported condition set and difficulty value — remain in the service). `RoundResponse.completed(...)` static factory removed; completion and answer-result mapping live in `GameMapper`. `GameLoopHttpTests.submitAnswerRequiresResponseTimeWithinBounds` proves missing and out-of-range `responseTimeMs` return `400` with a `validationErrors.responseTimeMs` entry.

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
- [x] Role-aware authorization is enforced (admin endpoints require `ROLE_ADMIN`).

2026-06-11 evidence (role-aware authorization): `SecurityConfig` requires `hasRole("ADMIN")` for `/api/admin/**`;
registration still assigns `ROLE_USER` only; the seed creates the dev admin `arena_admin` (generator-owned).
`AdminStatsHttpTests` proves `GET /api/admin/stats` returns `401` unauthenticated, `403` for `ROLE_USER`, and `200`
with the aggregate shape for `ROLE_ADMIN`; live curl proof reproduced `401`/`403`/`200` against the running backend.

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
- [x] Admin endpoints require `ROLE_ADMIN`.

Expected contract:

```text
POST /api/auth/register          public
POST /api/auth/login             public
POST /api/game/sessions          authenticated
GET  /api/game/sessions/{uuid}/rounds/next  authenticated
POST /api/game/sessions/{uuid}/answers      authenticated
GET  /api/game/me/attempts       authenticated
GET  /api/leaderboard            public (paginated)
GET  /api/admin/stats            ROLE_ADMIN
GET  /stimuli/**                 public
GET  /v3/api-docs, /swagger-ui/** public (demo convenience)
```

Proof:

```bash
rg -n "SecurityFilterChain|requestMatchers|csrf|SessionCreationPolicy|anyRequest" src/main/java/io/github/nilsfjp/ideophonearena/config/SecurityConfig.java
```

2026-06-04 evidence (superseded by 2026-06-10 evidence below): `SecurityConfig` permits `/api/auth/**`, `/api/leaderboard`, static frontend files, and `/stimuli/**`, then uses `anyRequest().authenticated()`. CSRF is disabled with stateless sessions for JWT. Live curl proof returned `401` for unauthenticated `POST /api/game/sessions` and `200 video/mp4` for `GET /stimuli/a0hu-gosogoso.mp4`. The static frontend files and mp4 stimulus references are legacy — see 2026-06-10 evidence for current public surface and audio/mp4 stimuli.

2026-06-10 evidence: stimulus references switched to per-word audio. `SecurityConfig` now permits both `GET` and `HEAD` on `/stimuli/**`. Live curl proof returned `200` with `Content-Type: audio/mp4` for both `GET` and `HEAD` on `http://localhost:8081/stimuli/audio/a0h-gosogoso.m4a`.

2026-06-10 evidence (legacy frontend removal): the Spring-served mini-frontend (`/`, `/index.html`, `/arena.css`, `/arena.js`, `templates/`) was deleted and its permitAll entries removed from `SecurityConfig`; public surface is now OPTIONS preflight, `GET`/`HEAD` `/stimuli/**`, `/api/health`, `/api/auth/**`, and `GET /api/leaderboard`, with `anyRequest().authenticated()`. `StaticResourceHttpTests.legacyMiniFrontendIsGone` proves `/`, `/index.html`, `/arena.js`, `/arena.css` return `401`. `JwtService` no longer has a code default for `app.jwt.secret` (startup fails fast when absent or blank) and is covered by `JwtServiceTests` (round-trip, expiry, tampered payload/signature, wrong secret, blank-secret fail-fast).

2026-06-11 evidence (admin authorization + API docs): `/api/admin/**` requires `hasRole("ADMIN")`; `/v3/api-docs/**`
and `/swagger-ui/**` are deliberately public for the course demo. The ERROR dispatch to `/error` is permitted so
`sendError` 403 responses are not overwritten to `401` on real Tomcat (live curl previously reproduced the
overwrite; after the fix the live proof returned `401` unauthenticated, `403` for `ROLE_USER`, `200` for the seeded
`arena_admin`). Full suite: `./mvnw test` -> 47 tests, 0 failures.

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

2026-06-10 evidence: a concurrent duplicate answer no longer surfaces as `500` — `GameService.submitAnswer` uses `saveAndFlush` and translates `DataIntegrityViolationException` (from `UNIQUE(session_id, round_id)`) into the existing `ConflictException` (`409`), with a `DataIntegrityViolationException -> 409` handler in `GlobalExceptionHandler` as backstop. `GameServiceTests.submitAnswerTranslatesConcurrentDuplicateInsertToConflict` and the duplicate `POST` in `GameLoopHttpTests.nextRoundReturnsExplicitCompletionBodyAfterFinalAnswer` (expects `409`) prove the path. Session completion is now set by the final `submitAnswer`; `getNextRound` is `@Transactional(readOnly = true)` and the completion DTO shape is unchanged. `totalAnswered`/`totalCorrect` are session-scoped (`countBySessionId`/`countBySessionIdAndCorrectTrue`).

## Game domain correctness

### Session start

- [x] Session start requires authenticated user.
- [x] Session start accepts a request DTO.
- [x] Session start requires `conditionName`.
- [x] Session start requires `difficultyLevel`.
- [x] Session start rejects unsupported difficulty values.
- [x] Session start rejects unsupported condition names.
- [x] Session start rejects `TEXT_ONLY` as an externally requested condition.
- [x] For the current demo, `difficultyLevel: 1` is supported.
- [x] For the current demo and Phase 2 Script Lab preparation, `CONDITION_1_SOKUON`, `CONDITION_2_SOKUON`, and `CONDITION_3_SOKUON` are supported at difficulty `1`.
- [x] Seed data contains difficulty-1 rounds for all three externally supported sokuon conditions.
- [x] Session ownership is stored and enforced.

Proof:

```bash
curl -i -X POST http://localhost:8081/api/game/sessions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1}'
```

2026-06-07 evidence: `GameController.startSession` is authenticated and accepts `@Valid StartSessionRequest`; `StartSessionRequest` requires `conditionName` and positive `difficultyLevel`; `GameService.startSession` stores the owner, requires difficulty `1`, and allowlists only `CONDITION_1_SOKUON`, `CONDITION_2_SOKUON`, and `CONDITION_3_SOKUON` for external session start. `GameLoopHttpTests` verifies missing `conditionName` returns `400`, missing `difficultyLevel` returns `400`, unsupported difficulty returns `400`, `TEXT_ONLY` returns `400`, all three supported sokuon conditions create sessions at difficulty `1`, all three can fetch a renderable first round, and seeded difficulty-1 rounds exist for all three supported sokuon conditions.

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

2026-06-10 evidence: round responses now ship `displayForm` (exact kana shown pre-answer) and `canonicalForm` (canonical-script form for feedback reveal) per choice, mapped in `GameMapper` from the new NOT NULL `ideophones.display_form`/`canonical_form` columns; the seed derives both from the stimulus filename prefix ground truth and `IdeophoneSeedIntegrityTests` (7 tests) permanently asserts script-family agreement with pos3/pos4 for all 180 rows. Live curl of `rounds/next` for `CONDITION_3_SOKUON` showed `displayForm` ゴソゴソ vs `canonicalForm` ごそごそ (left, HK) and `displayForm` かたかた vs `canonicalForm` カタカタ (right, KH) with shared per-word `stimulusUrl` values `/stimuli/audio/a0h-gosogoso.m4a` and `/stimuli/audio/a0k-katakata.m4a`.

2026-06-11 evidence (Session A, practice rounds): `POST /api/game/sessions` accepts optional `includePractice` (default `false`, echoed in the session response); when set, `rounds/next` serves 2 practice rounds (thesis pairs p0/p1, `arena_rounds.is_practice` flag, p-prefix audio) before the first scored round, each with `practice: true` in the same `RoundResponse` DTO. Practice answers return feedback (`practice: true`) but create no `PlayerAnswer` rows and never affect `totalAnswered`/`totalCorrect`, completion, or the leaderboard; out-of-order practice answers return `400`, repeats `409`. Seed extended to 204 ideophones / 102 rounds (trial ids unchanged); `IdeophoneSeedIntegrityTests` (8 tests) now also cross-checks the practice rows and the round practice flags; display forms verified against the practice stimulus PNGs (e.g. p0hk renders ソット). Live proof: practice session served `/stimuli/audio/p0h-sotto.m4a` (200, `audio/mp4`), 2 practice + 30 scored rounds, totals stayed 0 during practice, session completed after round 30.

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
- [x] Leaderboard is paginated (wrapper with `entries`, `page`, `size`, `totalElements`, `totalPages`; size capped at 50).
- [x] Leaderboard ranks by best completed-session score (`bestSessionCorrect`/`bestSessionAnswered`/`bestSessionAccuracy`); incomplete sessions excluded.
- [x] Personal recent attempts endpoint exists.
- [x] Recent attempts endpoint requires authentication.
- [x] Recent attempts are scoped to the authenticated user.
- [x] Score/progress fields are honestly named as session-scoped or all-time.

Proof:

```bash
curl -i 'http://localhost:8081/api/leaderboard?page=0&size=10'

curl -i http://localhost:8081/api/game/me/attempts \
  -H "Authorization: Bearer <TOKEN>"
```

2026-06-04 evidence: `ScoreController` exposes `GET /api/leaderboard` and `GET /api/game/me/attempts`; `SecurityConfig` leaves only leaderboard public; `ScoreService.getMyAttempts` scopes attempts by authenticated user id.

2026-06-11 evidence (pagination): `GET /api/leaderboard` is now paginated; response is a wrapper object (`entries` list + `page`/`size`/`totalElements`/`totalPages`) instead of a bare array. `page` and `size` query params are accepted (defaults 0/10, size clamped to 1–50). Ordering: `totalCorrect` desc, `totalAnswered` desc, avg response time asc, `username` asc as tiebreak (superseded by the Session A best-session metric below). `LeaderboardPaginationHttpTests` covers defaults, size cap, explicit params, and clamping. Live proof: `?page=0&size=5` returned wrapper with 3 entries; `?size=500` clamped to `size:50`.

2026-06-11 evidence (Session A, best-session metric): the leaderboard ranks each user by their best *completed* session — most correct answers in a single session, ties broken by best-session accuracy (fewer answers) then `username`; incomplete sessions never count. Entry fields are `username`/`bestSessionCorrect`/`bestSessionAnswered`/`bestSessionAccuracy` (**breaking** for the Vite frontend; wrapper shape unchanged). Implemented as a paged JPQL query with explicit `countQuery` in `PlayerAnswerRepository.findLeaderboard` (derived-table aggregate + not-exists argmax, no native SQL); mapping in `GameMapper`. `LeaderboardPaginationHttpTests` covers best-of-several-sessions, incomplete-session exclusion, accuracy and username tiebreaks, plus the original pagination behavior. Live proof: a played-through 30-round session appeared as `bestSessionCorrect: 15, bestSessionAnswered: 30, bestSessionAccuracy: 0.5`. Companion cleanup: `scripts/cleanup-test-accounts.sql` idempotently deletes `browser_loop_%` users with their sessions/answers (proof: registered `browser_loop_proof`, ran script, row count 1 -> 0; second run clean).

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

2026-06-10 evidence: `app.jwt.secret` has no code default — `JwtService` uses `@Value("${app.jwt.secret}")` plus a blank-value guard, so startup fails fast with a clear error when the property is absent or blank (`JwtServiceTests.blankSecretFailsFastAtConstruction`). The local profile uses `spring.jpa.hibernate.ddl-auto=validate` (verified directly); the tracked example template now also ships `validate` per the all-profiles rule.

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

2026-06-07 evidence (superseded; count and static-frontend reference are stale): `./mvnw test` passed with 24 tests. Spring Boot test contexts started, connected to local MySQL, served static frontend resources through MockMvc (legacy mini-frontend, since removed), registered users, started sessions, fetched rounds, submitted answers, verified missing session-start fields return validation `400`s, verified unsupported difficulty and `TEXT_ONLY` return clear `400`s, verified all three supported sokuon conditions can create difficulty-1 sessions and fetch renderable first rounds, verified seeded difficulty-1 rounds exist for those conditions, and verified next-round completion returns `200 OK` with `completed:true`. Live curl proof against the running `http://localhost:8081` backend returned `201 Created` for `CONDITION_1_SOKUON`, `CONDITION_2_SOKUON`, and `CONDITION_3_SOKUON` with `difficultyLevel: 1`, and `400 Bad Request` for `TEXT_ONLY`.

2026-06-11 evidence (superseded by Session A count below): `./mvnw test` -> 47 tests, 0 failures. Suite covers JWT (7 tests), seed integrity (7 tests), game loop HTTP tests (with `responseTimeMs` validation and duplicate-answer 409), static resource access (mini-frontend 401, stimuli 200), admin stats HTTP tests (401/403/200), leaderboard pagination tests, service unit tests, and serialization tests. Legacy static-frontend tests removed with the feature (2026-06-10).

2026-06-11 evidence (Session A, current): `./mvnw test` -> 59 tests, 0 failures. New coverage: `PracticeRoundHttpTests` (5 tests: practice-first flow with no persisted practice answers, no-flag sessions unchanged, serving-order enforcement, `includePractice` request/response handling, seeded p-prefix data), best-session leaderboard tests in `LeaderboardPaginationHttpTests` (+3), practice unit tests in `GameServiceTests` (+3), seed-integrity practice cross-checks (8 total). Clean `spring-boot:run` startup against the reseeded schema with `ddl-auto=validate` (started in 3.5 s).

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
- [x] Verified session start externally supports only the three sokuon conditions at difficulty `1`.
- [ ] Live browser click-through still needs to be run outside MockMvc.
