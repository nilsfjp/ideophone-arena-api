2026-05-25
Did: Added SecurityConfig and public /api/health.
Proof: curl -i http://localhost:8080/api/health -> 200 {"status":"ok"}
Commit: 000abfc
Next: Add first real Ideophone read endpoint.

## 2026-06-03

Session goal:
Align the first round DTO with the uploaded trial-flow screenshots.

Changed:
Renamed the public round response meaning field from `prompt` to `targetTranslation`.

Proof:
`./mvnw test`

Result:
Passing. The backend now exposes one target translation, two visible translations, and left/right ideophone stimuli for the frontend-controlled fixation and playback sequence.

Commit:
Not committed.

Blocker:
None.

Next single task:
Add registration/login with JWT so `/api/rounds/next` and the answer endpoint can be protected for real users.

## 2026-06-03

Session goal:
Implement the authenticated backend game loop for the screenshot trial flow.

Changed:
Added JWT register/login, explicit CORS for `http://localhost:5173`, protected game sessions, deterministic next-round selection, answer submission with duplicate rejection, recent attempts, public leaderboard, DTO mapping, and global API errors.

Proof:
`./mvnw test`
`curl -i http://127.0.0.1:8080/api/health`
`curl -i -X POST http://127.0.0.1:8080/api/auth/register ...`
`curl -i -X POST http://127.0.0.1:8080/api/game/sessions ...`
`curl -i http://127.0.0.1:8080/api/game/sessions/{sessionUuid}/rounds/next ...`
`curl -i -X POST http://127.0.0.1:8080/api/game/sessions/{sessionUuid}/answers ...`
`curl -i http://127.0.0.1:8080/api/game/me/attempts ...`
`curl -i http://127.0.0.1:8080/api/leaderboard`
`curl -i -X OPTIONS http://127.0.0.1:8080/api/game/sessions -H 'Origin: http://localhost:5173' ...`

Result:
Passing. Round responses include `sessionUuid`, `roundId`, `prompt`, translations, left/right stimuli, and timing, without exposing `correctIdeophoneId` before answer submission. Answer submission stores `player_answers`, returns immediate correctness feedback, and rejects duplicate answers with 409.

Commit:
Not committed.

Blocker:
Frontend was not edited in this backend workspace.

Next single task:
Implement the React `TrialPlayer` state machine against the new `/api/game/sessions/{sessionUuid}/rounds/next` and `/api/game/sessions/{sessionUuid}/answers` endpoints.

## 2026-06-03

Session goal:
Make the frontend trial loop and stimulus media paths demo-reliable.

Changed:
Allowed backend CORS for both `http://localhost:5173` and the active fallback `http://localhost:5174`. In the frontend repo, exposed seeded mp4 files at `/stimuli/<filename>` via root-level symlinks, kept kana visible during media playback, removed forced video muting, and corrected the frontend README API base URL.

Proof:
`./mvnw test`
`npm run build` in `/code/js/ideophone-arena-web`
`curl -i http://127.0.0.1:5174/api/health` -> 200 via the Vite `/api` proxy.
`curl -i http://127.0.0.1:5174/api/leaderboard` -> 200 via the Vite `/api` proxy.
Compared all 180 seeded backend `stimulus_file` values against frontend `/stimuli/<filename>` paths with no missing files.
`curl -I http://127.0.0.1:5174/stimuli/a0hu-gosogoso.mp4` -> 200 `video/mp4`
`curl -i -X OPTIONS http://127.0.0.1:8080/api/game/sessions -H 'Origin: http://localhost:5174' ...` -> 200 with `Access-Control-Allow-Origin: http://localhost:5174`

Result:
Passing. The backend contract, frontend API client, trial state machine, media URLs, and CORS are aligned for a local demo.

Commit:
Not committed.

Blocker:
No browser click-through was performed in this turn.

Next single task:
Open the Vite app, register/login, start a session, complete one full trial in the browser, and note any browser-specific media autoplay issue.

## 2026-06-03

Session goal:
Add a reproducible demo runbook and re-audit the current implementation against the game-loop goal.

Changed:
Added `docs/demo-runbook.md` with backend startup, frontend startup, media URL proof, browser demo steps, and curl API flow.

Proof:
`./mvnw test`
`npm run build` in `/code/js/ideophone-arena-web`

Result:
Passing. Backend and frontend build gates are green, and the runbook captures the remaining manual browser click-through needed to prove media autoplay and the full visual sequence.

Commit:
Not committed.

Blocker:
No local browser or browser automation binary is available in this environment for a rendered click-through.

Next single task:
Perform the browser demo flow from `docs/demo-runbook.md` and adjust only if a browser-specific media/autoplay issue appears.

## 2026-06-03

Session goal:
Harden frontend media playback against browser autoplay blocking.

Changed:
Updated the frontend stimulus player so blocked unmuted playback shows a `Play` button on the active stimulus card instead of silently advancing. Updated the demo runbook with this fallback behavior.

Proof:
`npm run build` in `/code/js/ideophone-arena-web`
`./mvnw test`

Result:
Passing. The automatic left/right playback path remains, and browser autoplay blocking now has a user-recoverable path that keeps the trial on the current stimulus.

Commit:
Not committed.

Blocker:
No local browser automation or browser binary is available in this environment for a rendered click-through.

Next single task:
Run the browser demo at `http://localhost:5174` and confirm whether automatic playback works or the `Play` fallback appears.

## 2026-06-04

Session goal:
Harden the game-loop DTO contract against hidden answer leakage and align docs with the current local ports.

Changed:
Removed `gloss` from `IdeophoneChoiceResponse` so left/right answer options do not expose their translations. Added `targetTranslation` to round, answer, and attempt responses while keeping `prompt` as a compatibility alias for the existing frontend. Added a DTO contract test for the round response and corrected the demo runbook backend port from `8080` to `8081`.

Proof:
`./mvnw test`

Result:
Passing. The backend still supports the current Vite frontend's `prompt` fallback, but the clearer `targetTranslation` field is now available and choice DTOs no longer carry the hidden gloss that the frontend could display.

Commit:
Not committed.

Blocker:
Live curl/browser verification was not possible in this sandbox because localhost socket creation failed with `Operation not permitted`. The temporary Spring server was started on `18081` and shut down cleanly, but curl could not connect from this environment.

Next single task:
Run the browser demo at `http://localhost:5174` against backend `http://localhost:8081` and confirm the full fixation, left playback, right playback, choice, feedback, next-round sequence.

## 2026-06-04

Session goal:
Add maintainable frontend state for the screenshot game loop inside the backend workspace.

Changed:
Added a Spring-served static frontend at `/` with auth, session start, fixation, left stimulus playback, right stimulus playback, choice buttons, answer submission, feedback, and next-round progression. Permitted the static files and `/stimuli/**` in `SecurityConfig` while keeping game APIs protected. Added `StaticFrontendContractTests` to guard the endpoint paths, DTO fields, and phase ordering used by the frontend state machine. Updated the demo runbook with the Spring-served frontend option.

Proof:
`./mvnw test`

Result:
Passing: 7 tests. Spring detects `static/index.html` as the welcome page during context startup, and the checked-in frontend state machine now consumes the same game-loop endpoints as the Vite app.

Commit:
Not committed.

Blocker:
Rendered browser verification is still unavailable in this sandbox.

Next single task:
Run `./mvnw spring-boot:run`, open `http://localhost:8081/`, register or log in, and complete one browser trial to verify real media/autoplay behavior.

## 2026-06-04

Session goal:
Make the Spring-served frontend's `/stimuli/<filename>` media path real and verify static HTTP access without a live socket.

Changed:
Added `StimulusResourceConfig` with configurable `app.stimuli.locations`, defaulting to `classpath:/static/stimuli/` and `/code/js/ideophone-arena-web/dist/stimuli/`. Updated the local example properties and demo runbook. Added `StaticResourceHttpTests` covering unauthenticated access to `/`, `/index.html`, `/arena.js`, a sample `/stimuli/a0hu-gosogoso.mp4`, and confirming `/api/game/sessions` remains protected.

Proof:
`node --check src/main/resources/static/arena.js`
`./mvnw test`

Result:
Passing: 10 tests. MockMvc proves the static frontend loads, the configured media route serves an mp4 with `video/mp4`, and the game API remains JWT-protected.

Commit:
Not committed.

Blocker:
Rendered browser click-through is still unavailable in this sandbox.

Next single task:
Run `./mvnw spring-boot:run`, open `http://localhost:8081/`, register or log in, and complete one browser trial to verify actual visual timing and browser media/autoplay behavior.

## 2026-06-04

Session goal:
Harden the Spring-served frontend state machine against stale async playback work.

Changed:
Added sequence tokens and playback cleanup to `arena.js` so logout, session changes, or a new round cancel any pending fixation/playback timers and media listeners before stale work can mutate the visible phase. Updated static frontend contract tests to guard the cancellation functions.

Proof:
`node --check src/main/resources/static/arena.js`
`./mvnw test`

Result:
Passing: 11 tests. The frontend trial loop now has a cancellation boundary around fixation, left playback, right playback, and choice entry.

Commit:
Not committed.

Blocker:
Rendered browser click-through is still unavailable in this sandbox.

Next single task:
Run `./mvnw spring-boot:run`, open `http://localhost:8081/`, register or log in, and complete one browser trial to verify actual visual timing and browser media/autoplay behavior.

## 2026-06-04

Session goal:
Verify the backend game loop through the real HTTP controller/security/JWT stack without requiring a live socket.

Changed:
Added `GameLoopHttpTests`, which seeds a unique round, registers a user through `/api/auth/register`, starts a session through `/api/game/sessions`, fetches `/rounds/next`, verifies no `gloss` or `correctIdeophoneId` leaks in the round response, submits `/answers`, verifies correctness and score feedback, then confirms the one-round session completes.

Proof:
`node --check src/main/resources/static/arena.js`
`./mvnw test`

Result:
Passing: 11 tests. The backend game-loop endpoints, JWT authentication, DTO serialization, answer persistence, feedback response, and completion path are covered through MockMvc.

Commit:
Not committed.

Blocker:
Rendered browser click-through is still unavailable in this sandbox.

Next single task:
Run `./mvnw spring-boot:run`, open `http://localhost:8081/`, register or log in, and complete one browser trial to verify actual visual timing and browser media/autoplay behavior.

## 2026-06-04

Session goal:
Add backend test coverage for the game-loop service states that the frontend trial player depends on.

Changed:
Added `GameServiceTests` covering first unanswered round selection, target/translation/timing mapping, answer persistence with correctness feedback, score totals, and rejection of an ideophone id that is not one of the two choices for the round.

Proof:
`./mvnw test`

Result:
Passing: 5 tests. The backend loop behavior is now guarded beyond context startup and DTO shape checks.

Commit:
Not committed.

Blocker:
The frontend repo remains outside this workspace's writable root, and localhost/browser verification remains unavailable here.

Next single task:
Run the browser demo at `http://localhost:5174` against backend `http://localhost:8081` and confirm the full fixation, left playback, right playback, choice, feedback, next-round sequence.

## 2026-06-04

Session goal:
Perform a backend grading-readiness pass without feature expansion.

Changed:
Rejected unsupported `difficultyLevel` values at session start with `400 Bad Request`; moved answer correctness evaluation out of entities and into `GameService`; tightened answer request ID validation; disabled Open Session in View; updated MockMvc/service tests, backend contract, and grading checklist evidence.

Proof:
`./mvnw test`

Result:
Passing: 15 tests. The test suite starts Spring contexts, connects to local MySQL, verifies static/frontend access boundaries, registers users, starts authenticated sessions with difficulty 1, fetches rounds, submits answers, rejects unsupported difficulty and unknown condition names, and covers documented completion handling.

Commit:
Not committed.

Blocker:
Live `./mvnw spring-boot:run` plus curl/browser proof was not run in this pass; verification is through Maven tests and MockMvc.

Next single task:
Run `./mvnw spring-boot:run` and perform a live curl flow against `http://localhost:8081` for register, login, session start, next round, answer submission, attempts, and leaderboard.

## 2026-06-04

Session goal:
Add live HTTP proof for the backend grading-readiness pass.

Changed:
Updated the grading checklist with live curl evidence for health, public leaderboard, registration, login, protected endpoint rejection without a bearer token, unsupported difficulty rejection, authenticated session start, next round, answer submission, recent attempts, public leaderboard after answer storage, and public stimulus media GET.

Proof:
`curl` against `http://localhost:8081` using unique user `grade_1780605732072310051`

Result:
Passing live proof: health `200`, leaderboard `200`, register `201`, protected game session without token `401`, unsupported difficulty `400`, login `200`, session start `201`, next round `200`, answer submission `200`, attempts `200`, leaderboard after answer `200`, and `GET /stimuli/a0hu-gosogoso.mp4` returned `200 video/mp4`.

Commit:
Not committed.

Blocker:
A fresh `./mvnw spring-boot:run` could not bind because an existing `spring-boot:run` for this repository already occupied port `8081`; the live curl proof used that running process. Rendered browser click-through was not run.

Next single task:
Run the rendered browser demo at `http://localhost:8081/` or the Vite frontend at `http://localhost:5174` and confirm actual media/autoplay behavior.

## 2026-06-04

Session goal:
Close small remaining backend grading-readiness documentation and error-handling gaps.

Changed:
Added a safe generic exception fallback that returns JSON `500` without stack traces or internal exception details. Added `README.md` with backend startup, MySQL/local profile setup, current endpoints, demo settings, and a curl demo script. Updated the grading checklist evidence.

Proof:
`./mvnw test`

Result:
Passing: 15 tests. The generic fallback and README documentation changes did not regress the Spring context, MockMvc API flow, static frontend/media checks, or service tests.

Commit:
Not committed.

Blocker:
Rendered browser click-through remains unverified.

Next single task:
Perform the rendered browser demo at `http://localhost:8081/` or `http://localhost:5174`.

## 2026-06-05

Session goal:
Replace next-round session completion as `404 Not Found` with an explicit frontend-friendly completion response.

Changed:
Changed `GET /api/game/sessions/{sessionUuid}/rounds/next` so completed sessions return `200 OK` with `completed:true` and message `Game session is complete`. Updated the static frontend to branch on `state.round.completed` instead of relying only on completion-related error text. Added service and MockMvc coverage for completion after the final answer. Updated the backend contract, demo runbook, and grading checklist evidence.

Proof:
`./mvnw test`
`./mvnw spring-boot:run`
Live curl flow against `http://localhost:8081` using user `completion_live_1780623465987150066`

Result:
Passing: 16 tests. The suite verifies the normal round response still exposes no hidden answer fields, and the final next-round call returns an explicit completion body through the real controller/security/JWT stack.
Live proof passed: register `201`, login `200`, session start `201`, answered 30 rounds, final next-round completion `200` with `{"completed":true,"message":"Game session is complete","sessionUuid":"5605288c-a2eb-4cec-8b1c-82d45a3957b8","conditionName":"CONDITION_1_SOKUON","difficultyLevel":1,"roundId":null}`, attempts `200`, and leaderboard `200`.

Commit:
Not committed.

Blocker:
Rendered browser click-through remains unverified.

Next single task:
Run the rendered browser demo at `http://localhost:8081/` or `http://localhost:5174` and confirm the completion screen/status after the final round.

## 2026-06-07

Session goal: Add first Phase 2 research-flavor vertical slice.

Changed: Added frontend research notes after answer feedback using current round, selected/correct answer, modality, correctness, and condition.

Proof: npm run lint passed. npm run build passed. Browser verification passed with 30 answered rounds, completion visible, leaderboard/recent attempts visible, 360 successful /stimuli/ requests, 0 muted stimulus elements, 0 failed requests, and 0 relevant console errors.

Result: Phase 2 frontend-only research flavor slice completed without backend/schema/auth/scoring changes.

Commit: TODO

Blocker: Technical review pass not yet done.

Next single task: Run backend and frontend technical review before adding Script Lab mode.

## 2026-06-07

Session goal:
Harden backend session-start contract before frontend Script Lab mode.

Changed:
Required `conditionName` and `difficultyLevel` in `StartSessionRequest`; removed session-start defaults from `GameService`; added service-layer validation for `CONDITION_1_SOKUON`, `CONDITION_2_SOKUON`, and `CONDITION_3_SOKUON`; rejected `TEXT_ONLY` and unsupported difficulty values with clear `400` errors; updated MockMvc/service tests plus contract, runbook, and checklist docs.

Proof:
`./mvnw test`
`./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=18081`

Result:
Passing: 23 tests. MockMvc covers missing condition, missing difficulty, unsupported difficulty, `TEXT_ONLY` rejection, supported condition session creation for all three sokuon conditions, and seeded difficulty-1 rounds for all three supported conditions. A temporary server on `18081` started and shut down cleanly, but live mutation curl proof for register/session creation and `TEXT_ONLY` rejection failed from the separate sandbox curl process with `curl exit 7` / status `000`.

Commit:
Not committed.

Blocker:
Live mutation curl proof was blocked by local socket/runtime inconsistency; tests and MockMvc proof passed.

Next single task:
Add ownership, duplicate-answer, CORS preflight, and recent-attempt authentication tests before implementing Script Lab UI.

## 2026-06-07

Session goal:
Prepare the backend Phase 2 game-mode contract without schema or endpoint redesign.

Changed:
Inspected the current backend layers and the Phase 2 roadmap. Added MockMvc coverage proving all three supported sokuon conditions can start sessions and fetch renderable first rounds at difficulty 1. Added `docs/phase-2-api-plan.md` for future `GameMode`, `PresentationMode`, `RoundTemplate`, `StimulusAsset`, and `RatingAttempt` extension points. Updated `docs/backend-contract.md` with Script Lab frontend assumptions and updated checklist evidence.

Proof:
`./mvnw test`
Live curl against `http://localhost:8081/api/game/sessions`

Result:
Passing: 24 tests. Live curl returned `201 Created` for `CONDITION_1_SOKUON`, `CONDITION_2_SOKUON`, and `CONDITION_3_SOKUON` with `difficultyLevel: 1`, and `400 Bad Request` for `TEXT_ONLY`.

Commit:
Not committed.

Blocker:
None.

Next single task:
Add ownership, duplicate-answer, CORS preflight, and recent-attempt authentication tests before implementing Script Lab UI.

## 2026-06-10

Session goal:
Make script display data, not code: add `display_form`/`canonical_form` to `ideophones` as the single source of truth for displayed kana, and switch stimulus references from per-condition mp4 video to one shared per-word audio file.

Changed:
Added NOT NULL `display_form` and `canonical_form` columns to `ideophones` and dropped the `stimulus_file` UNIQUE constraint (three condition rows per word now share one audio file). Regenerated all 180 seed rows through `scripts/generate_seed_sql.py`, which now derives both forms from the stimulus filename prefix ground truth (pos3 = canonical script, pos4 = displayed script; u/d rows reuse the canonical form) and fixes the gloss typo "feeling fo relief" (ids 47/107/167). The two long-vowel words use the chouonpu forms the stimulus PNGs actually rendered (zyaazyaa: じゃーじゃー/ジャージャー, kyaakyaa: きゃーきゃー/キャーキャー), which differ from the `kana` lemma column; documented in the contract. Extracted 60 per-word audio files with `ffmpeg -vn -c:a copy` from the audio-only `hu`/`kd` mp4 variants (bit-identical AAC, all 1.216 s) into `../ideophone-arena-web/stimuli/audio/` plus a `dist/stimuli/audio/` copy; seed `stimulus_file` now points at `audio/<p1><p2><p3>-<romaji>.m4a` for all three rows of each word; mp4s kept as legacy assets. Pre-existing per-word mp3s were rejected as audio source because their duration (0.984 s) does not match the mp4 audio track. Extended `Ideophone`, `IdeophoneChoiceResponse`, and `GameMapper` to ship `displayForm`/`canonicalForm`. Added `IdeophoneSeedIntegrityTests` (7 tests, parses the seed SQL, no DB) asserting per row: well-formed `canonical_script`, audio filename agreement with romaji/modality/canonical script, `canonical_form` script family matches pos3, `display_form` script family matches pos4 (or equals `canonical_form` for u/d), one shared audio file per word across exactly three rows, and gloss typo absence. Allowed `HEAD` next to `GET` on `/stimuli/**` in `SecurityConfig` so media HEAD proofs return 200. Updated `docs/backend-contract.md`, `docs/demo-runbook.md`, `docs/backend-grading-checklist.md`, and the punch list.

Proof:
`./mvnw test`
`mysql < src/main/resources/db/init/ideophone_arena.sql` followed by `./mvnw spring-boot:run` with `ddl-auto=validate`
Live curl: register/login, `POST /api/game/sessions` (`CONDITION_3_SOKUON`), `GET /api/game/sessions/<uuid>/rounds/next`
`curl -I http://localhost:8081/stimuli/audio/a0h-gosogoso.m4a`

Result:
Passing: 31 tests including the 7 new seed-integrity tests. App started cleanly against the reseeded schema with `ddl-auto=validate`. The condition-3 round returned left (HK) `displayForm` ゴソゴソ / `canonicalForm` ごそごそ and right (KH) `displayForm` かたかた / `canonicalForm` カタカタ with `stimulusUrl` `/stimuli/audio/a0h-gosogoso.m4a` and `/stimuli/audio/a0k-katakata.m4a`. HEAD and GET on the audio URL both returned `200` with `Content-Type: audio/mp4` (28902 bytes).

Commit:
Not committed.

Blocker:
None.

Next single task:
Migrate the frontend to render `displayForm`/`canonicalForm` and play the per-word audio, removing its runtime script derivation from `canonicalScript`.

## 2026-06-10 (session 2)

Session goal:
Backend hygiene batch: close the June-audit architecture violations (validation, mapping consolidation), fix two correctness bugs (lifetime-scoped score totals, completion mutation on GET), translate the duplicate-answer race to 409, remove the legacy Spring-served mini-frontend, and land the dropped S1 riders (JWT secret fail-fast, ddl-auto confirmation).

Changed:
`SubmitAnswerRequest.responseTimeMs` is now `@NotNull @Min(0) @Max(600000)`; the redundant null presence checks were removed from `GameService.validateSupportedStartRequest` (business rules for supported condition set and difficulty stay in the service). Answer-result construction and the completion DTO moved into `GameMapper` (`toAnswerResultResponse`, `toCompletedRoundResponse`); the `RoundResponse.completed(...)` static factory was deleted and the DTO is now logic-free. `submitAnswer` uses `saveAndFlush` and translates `DataIntegrityViolationException` from `UNIQUE(session_id, round_id)` into the existing `ConflictException` (409); `GlobalExceptionHandler` also maps `DataIntegrityViolationException` to 409 as backstop. Score totals switched from `countBySessionUserId*` (user lifetime) to new derived `countBySessionId`/`countBySessionIdAndCorrectTrue` (session-scoped); the old JPQL queries were removed. Session completion moved off the GET: `submitAnswer` marks the session complete when the stored answer is the last round for the session's condition/difficulty; `getNextRound` is `@Transactional(readOnly = true)` and returns the unchanged completion DTO. Deleted the legacy mini-frontend (`static/index.html`, `arena.css`, `arena.js`, empty `templates/`, plus an untracked stray browser-save HTML) and its permitAll entries in `SecurityConfig`; public surface is now OPTIONS, GET/HEAD `/stimuli/**`, `/api/health`, `/api/auth/**`, GET `/api/leaderboard`. `JwtService` lost the hard-coded secret default (`@Value("${app.jwt.secret}")` plus blank guard) and `isTokenValid` now parses/verifies the token once instead of twice. Tests: new `JwtServiceTests` (7: round-trip, wrong user, expired, tampered payload, tampered signature, wrong secret, blank-secret fail-fast); `GameServiceTests` gained duplicate-race-to-Conflict and completion-on-last-answer tests; `GameLoopHttpTests` gained responseTimeMs validation bounds and asserts completion is set by the final POST and a duplicate POST returns 409; `StaticResourceHttpTests` now proves the mini-frontend is gone (401) while stimuli stay public; `StaticFrontendContractTests` deleted with the feature. Riders: CLAUDE.md invariant 3 documents the 3-character per-word audio prefix; new `scripts/extract-audio.sh` reproduces the 60 .m4a files (ffmpeg -vn -c:a copy from the u/d mp4s); `application-local.example.properties` now ships `ddl-auto=validate`. Docs updated: contract (session-scoped totals, required bounded responseTimeMs, completion-on-POST, changelog), runbook (mini-frontend section removed, Vite is the only frontend), grading checklist (dated evidence for validation, security surface, 409 race, secrets).

Proof:
`./mvnw test`: 39 tests, 0 failures (was 41 with 3 mini-frontend failures mid-removal; the two obsolete frontend-guard tests were removed with the feature).
Startup without `app.jwt.secret` (temporarily commented in the local profile, then restored): `PlaceholderResolutionException: Could not resolve placeholder 'app.jwt.secret' in value "${app.jwt.secret}"`.
Live flow against `./mvnw spring-boot:run` (`ddl-auto=validate`): register 201, session 201, missing `responseTimeMs` -> 400 `{'responseTimeMs': 'must not be null'}`, `600001` -> 400 `must be less than or equal to 600000`, duplicate answer -> 409 `This round has already been answered in this session`, 30 rounds answered with `totalAnswered` ending at 30, completion GET returned the unchanged `completed:true` DTO, and the first answer of a second session by the same user returned `totalAnswered: 1` (session-scoped; previously 31).
`curl -i http://localhost:8081/` -> 401 (also `/index.html`, `/arena.js`); `curl -I http://localhost:8081/stimuli/audio/a0h-gosogoso.m4a` -> 200; `GET /api/leaderboard` -> 200.

Result:
All S3 punch-list items and both S1 riders landed; experiment invariants untouched (no stimulus, seed, or condition changes).

Commit:
Not committed.

Blocker:
None.

Next single task:
S4: `GET /api/admin/stats` behind `hasRole("ADMIN")`.

## 2026-06-11

Session goal:
S4: make role-aware authorization real — `GET /api/admin/stats` behind `ROLE_ADMIN`, seeded dev admin, paginated leaderboard, springdoc.

Changed:
- `scripts/generate_seed_sql.py` now emits a dev-only `arena_admin` row (`ROLE_ADMIN`, frozen BCrypt hash; throwaway password documented in the runbook); seed regenerated and re-applied.
- `SecurityConfig`: `/api/admin/**` requires `hasRole("ADMIN")`; `/v3/api-docs/**` + `/swagger-ui/**` public (demo convenience); ERROR dispatch to `/error` permitted so `sendError` 403s are not overwritten to 401 on real Tomcat (bug found during live proof — MockMvc does not error-dispatch, so only curl exposed it).
- New admin stats slice: `ConditionSessionCountProjection`/`ConditionAnswerStatsProjection`/`ModalityAnswerStatsProjection`, JPQL aggregates in `GameSessionRepository`/`PlayerAnswerRepository`, `Admin*Response` DTOs, `AdminStatsMapper`, `AdminStatsService`, `AdminController` (`@Tag`/`@Operation`).
- Leaderboard pagination: `findLeaderboard` returns `Page` with explicit `countQuery` and a `username` tiebreak; `LeaderboardPageResponse` wrapper (`entries` + `page`/`size`/`totalElements`/`totalPages`); `page`/`size` params (defaults 0/10, size capped at 50, clamped); projection-to-DTO mapping moved from `ScoreService` into `GameMapper`.
- `pom.xml`: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3` (pre-approved; the 3.0.x line targets Spring Boot 4) + `OpenApiConfig` (bearer scheme for the Swagger Authorize button).
- Tests: `AdminStatsHttpTests` (401/403/200 + seeded-admin login guard), `LeaderboardPaginationHttpTests` (defaults, size cap, explicit params, clamping).
- Docs: contract (admin endpoint, paginated leaderboard, API docs, changelog), runbook ("Creating an admin", Swagger URLs, leaderboard curl), grading checklist (role-aware authorization rows + 2026-06-11 evidence), CLAUDE.md punch list.

Proof:
`python3 scripts/generate_seed_sql.py --check` -> "SQL is up to date"; `./mvnw test` -> 47 tests, 0 failures.
Live flow against `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`: login as `arena_admin` -> 200 with token; `GET /api/admin/stats` -> 200 `{"totals":{"users":16,"sessions":10,"completedSessions":1,"answers":3},...}`; same call as fresh `ROLE_USER` -> 403; unauthenticated -> 401. `GET /api/leaderboard?page=0&size=5` -> wrapper with 3 entries, `page:0,size:5,totalElements:3,totalPages:1`; `?size=500` -> `size:50`. `/swagger-ui/index.html` -> 200; `/v3/api-docs` lists all 9 paths including `/api/admin/stats`.

Result:
Role-aware authorization is enforced and proven (closes the last open course requirement); leaderboard response shape is now a wrapper — breaking for the Vite frontend until `getLeaderboard()` reads `.entries`.

Commit:
Not committed.

Blocker:
None.

Next single task:
S4 remainder: practice rounds (p-prefix stimuli).

## 2026-06-11 (session 2)

Session goal:
Docs-only housekeeping: bring all documentation in line with reality after S1a migration, hygiene batch (S3), and S4. No Java, SQL, or script changes.

Changed:
- `README.md`: rewrote to remove all mini-frontend references ("serves the minimal static demo frontend", "Browser Demo" pointing at `:8081/`); expanded Demo Settings to all three conditions; added admin stats and Swagger endpoints to Main Endpoints; updated leaderboard curl to paginated form with response shape; added Vite Frontend and API Docs sections; fixed test description (removed "static frontend/media route").
- `CLAUDE.md` (`.AGENTS.backend.md`): replaced the Bean Validation imperative NOTE (verify and complete; add bounds) with a done statement reflecting S3 completion.
- `docs/backend-contract.md`: updated Date from 2026-06-07 to 2026-06-11.
- `docs/backend-grading-checklist.md`: added `[x]` leaderboard pagination checklist item; updated leaderboard proof curl to paginated form; added 2026-06-11 leaderboard evidence; added 2026-06-11 build proof evidence (47 tests); annotated two stale historical blocks (2026-06-04 authorization evidence referencing "static frontend files" and "200 video/mp4 for a0hu-gosogoso.mp4"; 2026-06-07 build evidence referencing "served static frontend resources through MockMvc" and 24 tests) as "(superseded)" with current state noted.

Proof:
Grep sweep: `grep -rn "mini-frontend|static frontend|index\.html|arena\.js|arena\.css|\.mp4|video/mp4|account total" docs/backend-contract.md docs/backend-grading-checklist.md docs/demo-runbook.md README.md CLAUDE.md` — zero unresolved hits in active doc files after edits; all remaining hits are either correct current URLs (swagger-ui/index.html), correctly labelled legacy/removal notes, or research data CSVs (not active docs).
`./mvnw test` -> 47 tests, 0 failures (no code changed).

Result:
All active documentation is internally consistent and matches current codebase state. Historical evidence blocks are annotated "(superseded)" rather than deleted.

Commit:
Not committed.

Blocker:
None.

Next single task:
S4 remainder: practice rounds (p-prefix stimuli).

## 2026-06-11 (session 3, "Session A")

Session goal:
Backend small batch: (1) practice rounds from the p-prefix stimuli, (2) cleanup script for `browser_loop_*` test accounts, (3) leaderboard reworked from lifetime totals to best completed-session score.

Changed:
- `scripts/generate_seed_sql.py`: also reads the `display == "practice"` CSV rows (4 per condition, pairs p0-p3 per thesis Appendix B); practice ideophones/rounds are appended after all trial rows so trial ids 1-180 / round ids 1-90 are unchanged (practice: ideophones 181-204, rounds 91-102, `is_practice = 1`); schema gains `arena_rounds.is_practice`, `game_sessions.include_practice`, `game_sessions.practice_answered`.
- `scripts/extract-audio.sh`: expected count 60 -> 68 (glob already covered the p-prefix); ran it — 8 new practice m4a files (ffmpeg stream copy from the u/d mp4s), copied into `ideophone-arena-web/dist/stimuli/audio/`.
- `ArenaRound.practice`, `GameSession.includePractice`/`practiceAnswered` (+ constructor overloads, `recordPracticeAnswer()`).
- `StartSessionRequest.includePractice` (optional, default false); `GameSessionResponse` echoes it; `RoundResponse` and `AnswerResultResponse` gained `practice`.
- `GameService`: serves the first 2 practice rounds of the session's condition before scored rounds when the flag is set; practice answers are validated (in-order, 400/409 otherwise), evaluated for feedback, never persisted; main round list/completion count switched to practice-excluding repository methods.
- `ArenaRoundRepository`: practice-aware derived queries (replacing the unpaged ordered query and an unused Pageable variant).
- `PlayerAnswerRepository.findLeaderboard`: rewritten as paged JPQL (derived-table per-session aggregate + not-exists argmax, explicit countQuery) ranking by best completed-session correct, tiebreak accuracy (fewer answers) then username; `LeaderboardEntryProjection`/`LeaderboardEntryResponse` now `bestSessionCorrect`/`bestSessionAnswered`(+`bestSessionAccuracy` in the DTO); mapping in `GameMapper` (which also gained `toPracticeAnswerResultResponse`).
- `scripts/cleanup-test-accounts.sql`: idempotent FK-ordered deletion of `browser_loop_%` users + sessions + answers.
- Tests: new `PracticeRoundHttpTests` (5); `LeaderboardPaginationHttpTests` +3 best-session tests with isolated fixtures; `GameServiceTests` +3 practice unit tests; `IdeophoneSeedIntegrityTests` extended to 204 rows / 68 audio files / practice-round flag cross-check (8 tests); `RoundResponseSerializationTests` asserts the `practice` property.
- Docs: `backend-contract.md` (practice section, leaderboard rework, changelog), `demo-runbook.md` (practice curl flow, audio proof, cleanup-script section), `backend-grading-checklist.md` (new items + dated evidence), punch list in `.AGENTS.backend.md`.

Proof:
- `python3 scripts/generate_seed_sql.py --check` -> "SQL is up to date: 204 ideophones, 102 rounds".
- Reseed via mysql.exe -> 204 ideophones, 102 rounds, 12 practice rounds, `arena_admin` restored.
- `scripts/extract-audio.sh` -> "Extracted 68 audio files (expected 68)"; practice display forms cross-checked against the stimulus PNGs (p0hk renders ソット, p2kh がんがん, p1kh ぱっ, p3hk ソックリ).
- `./mvnw test` -> 59 tests, 0 failures (was 47).
- `./mvnw spring-boot:run` with `ddl-auto=validate` -> clean start in 3.5 s.
- Live flow (fresh server): start session with `includePractice: true` -> first round `practice: true` with `/stimuli/audio/p0h-sotto.m4a`; practice answers returned feedback with `totalAnswered`/`totalCorrect` stuck at 0; 2 practice + 30 scored rounds -> `completed`; `GET /api/leaderboard` -> `{bestSessionCorrect: 15, bestSessionAnswered: 30, bestSessionAccuracy: 0.5}`.
- `curl -I /stimuli/audio/p0h-sotto.m4a` -> 200, `Content-Type: audio/mp4`.
- Cleanup script: registered `browser_loop_proof`, ran script -> `browser_loop_%` count 1 -> 0; second run clean (idempotent).

Result:
All three Session A items done. Practice answers are feedback-only by design (documented divergence from the thesis, which hid practice feedback). Leaderboard entry fields changed — BREAKING for the Vite frontend (needs a follow-up rider to read `bestSession*` fields).

Commit:
Not committed (proposed message below).

Blocker:
None.

Next single task:
Frontend rider: update the Vite leaderboard to the `bestSessionCorrect`/`bestSessionAnswered`/`bestSessionAccuracy` fields (and optionally adopt `includePractice`).
