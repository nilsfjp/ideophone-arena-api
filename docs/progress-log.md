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
S4: make role-aware authorization real - `GET /api/admin/stats` behind `ROLE_ADMIN`, seeded dev admin, paginated leaderboard, springdoc.

Changed:

- `scripts/generate_seed_sql.py` now emits a dev-only `arena_admin` row (`ROLE_ADMIN`, frozen BCrypt hash; throwaway password documented in the runbook); seed regenerated and re-applied.
- `SecurityConfig`: `/api/admin/**` requires `hasRole("ADMIN")`; `/v3/api-docs/**` + `/swagger-ui/**` public (demo convenience); ERROR dispatch to `/error` permitted so `sendError` 403s are not overwritten to 401 on real Tomcat (bug found during live proof - MockMvc does not error-dispatch, so only curl exposed it).
- New admin stats slice: `ConditionSessionCountProjection`/`ConditionAnswerStatsProjection`/`ModalityAnswerStatsProjection`, JPQL aggregates in `GameSessionRepository`/`PlayerAnswerRepository`, `Admin*Response` DTOs, `AdminStatsMapper`, `AdminStatsService`, `AdminController` (`@Tag`/`@Operation`).
- Leaderboard pagination: `findLeaderboard` returns `Page` with explicit `countQuery` and a `username` tiebreak; `LeaderboardPageResponse` wrapper (`entries` + `page`/`size`/`totalElements`/`totalPages`); `page`/`size` params (defaults 0/10, size capped at 50, clamped); projection-to-DTO mapping moved from `ScoreService` into `GameMapper`.
- `pom.xml`: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3` (pre-approved; the 3.0.x line targets Spring Boot 4) + `OpenApiConfig` (bearer scheme for the Swagger Authorize button).
- Tests: `AdminStatsHttpTests` (401/403/200 + seeded-admin login guard), `LeaderboardPaginationHttpTests` (defaults, size cap, explicit params, clamping).
- Docs: contract (admin endpoint, paginated leaderboard, API docs, changelog), runbook ("Creating an admin", Swagger URLs, leaderboard curl), grading checklist (role-aware authorization rows + 2026-06-11 evidence), CLAUDE.md punch list.

Proof:
`python3 scripts/generate_seed_sql.py --check` -> "SQL is up to date"; `./mvnw test` -> 47 tests, 0 failures.
Live flow against `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`: login as `arena_admin` -> 200 with token; `GET /api/admin/stats` -> 200 `{"totals":{"users":16,"sessions":10,"completedSessions":1,"answers":3},...}`; same call as fresh `ROLE_USER` -> 403; unauthenticated -> 401. `GET /api/leaderboard?page=0&size=5` -> wrapper with 3 entries, `page:0,size:5,totalElements:3,totalPages:1`; `?size=500` -> `size:50`. `/swagger-ui/index.html` -> 200; `/v3/api-docs` lists all 9 paths including `/api/admin/stats`.

Result:
Role-aware authorization is enforced and proven (closes the last open course requirement); leaderboard response shape is now a wrapper - breaking for the Vite frontend until `getLeaderboard()` reads `.entries`.

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
Grep sweep: `grep -rn "mini-frontend|static frontend|index\.html|arena\.js|arena\.css|\.mp4|video/mp4|account total" docs/backend-contract.md docs/backend-grading-checklist.md docs/demo-runbook.md README.md CLAUDE.md` - zero unresolved hits in active doc files after edits; all remaining hits are either correct current URLs (swagger-ui/index.html), correctly labelled legacy/removal notes, or research data CSVs (not active docs).
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
- `scripts/extract-audio.sh`: expected count 60 -> 68 (glob already covered the p-prefix); ran it - 8 new practice m4a files (ffmpeg stream copy from the u/d mp4s), copied into `ideophone-arena-web/dist/stimuli/audio/`.
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
All three Session A items done. Practice answers are feedback-only by design (documented divergence from the thesis, which hid practice feedback). Leaderboard entry fields changed - BREAKING for the Vite frontend (needs a follow-up rider to read `bestSession*` fields).

Commit:
Not committed (proposed message below).

Blocker:
None.

Next single task:
Frontend rider: update the Vite leaderboard to the `bestSessionCorrect`/`bestSessionAnswered`/`bestSessionAccuracy` fields (and optionally adopt `includePractice`).

## 2026-06-12 ("Session B")

Session goal:
Deterministic per-session shuffle: a server-generated seed derives round order, target identity (deliberate extension beyond the thesis's parity-fixed targets), target side, and meaning order; sessions replay identically across restarts; answers are judged against, and store, the derived target.

Changed:

- `scripts/generate_seed_sql.py` + regenerated `ideophone_arena.sql`: `game_sessions.shuffle_seed BIGINT NOT NULL`; `player_answers.target_ideophone_id BIGINT NOT NULL` + FK to `ideophones` (approval gate cleared 2026-06-12; seed data rows unchanged).
- `GameSession.shuffleSeed` (5-arg constructor; `GameService.startSession` fills it from a `SecureRandom`); `PlayerAnswer.targetIdeophone` (constructor param).
- New `model/DerivedRound` (round + derived target/other/side/meaning-order, never persisted) and `service/RoundShuffler` (@Component): scored rounds ordered by id asc -> `Collections.shuffle(list, new Random(seed))` -> per shuffled round, in order, `targetIsPairSecond`/`targetOnLeft`/`targetMeaningListedFirst` from the same stream; "pair second" = higher ideophone id; practice rounds keep fixed order with draws from `new Random(seed + 1)`. Spec documented verbatim in the contract as a compatibility contract.
- `GameService`: `getNextRound` serves the first unanswered round of the derived order (recomputed per request, nothing persisted but the seed); `submitAnswer` judges against the derived target and stores it; practice answers judged against the practice-stream derivation. `arena_rounds.correct_ideophone_id`/`prompt` are no longer read in the serving path (kept as thesis-target documentation).
- `GameMapper`: round/answer/practice mapping from `DerivedRound` (prompt/translations = derived target/other glosses, left/right = derived sides); `toAttemptResponse` replays the stored `targetIdeophone`. `PlayerAnswerRepository`: EntityGraphs and the modality-stats join switched from `round.correctIdeophone` to `targetIdeophone`.
- `targetMeaningListedFirst` is reserved: drawn (stream consumption final) but the current frontend always lists the target meaning first (semantic `translations.target`/`other` fields, `TrialPlayer.tsx` renders target line first) - future frontend rider, no backend change needed.
- Tests: new `RoundShufflerTests` (7: determinism across instances, seed divergence, permutation integrity, pair-second stability under swapped left/right columns, practice-stream independence, 200-seed both-identities/both-sides sweep) and `ShuffledSessionHttpTests` (full practice-on loop: served order/sides/meanings match the derivation, stored targets match, duplicate 409, completion unchanged); `GameServiceTests` reworked derivation-aware (+restart-continuity, +derived-distractor-incorrect); `PracticeRoundHttpTests`/`LeaderboardPaginationHttpTests` adapted.
- Docs: contract (new "Deterministic per-session shuffle" section with the verbatim derivation spec + changelog), runbook (shuffle proofs section), grading checklist (Session B evidence), punch list.

Proof:

- `python scripts/generate_seed_sql.py --check` -> "SQL is up to date: 204 ideophones, 102 rounds".
- Reseed via mysql.exe; `./mvnw spring-boot:run` with `ddl-auto=validate` -> clean start.
- `./mvnw test` -> 69 tests, 0 failures (was 59).
- Live: two same-user CONDITION_1 sessions -> different first-5 sequences, same round 18 served with different targets ("crisp appearance, stiffly" vs "dim, faint, indistinct") and round 20 with different sides (left ideophone 40 vs 39). Killed and restarted the app mid-session -> `rounds/next` byte-identical before/after (round 54, same target/left/right), answered rounds stayed answered.
- Browser (Vite 5174 + Edge CDP): `verify-browser-loop.mjs` played 2 practice + 30 scored rounds to completion with zero frontend changes, 0 console errors, 0 muted stimuli, 192/192 stimulus fetches OK; leaderboard and recent attempts rendered. Cleanup script removed the `browser_loop_%` account.

Result:
Session B complete. Identity randomization doubles the effective item pool; `player_answers.target_ideophone_id` accumulates per-target difficulty data including the 30 complementary targets the thesis never measured. DTO shapes unchanged; no frontend changes.

Commit:
Not committed (proposed message below).

Blocker:
None.

Next single task:
Frontend rider: honor `targetMeaningListedFirst` (needs a small DTO addition decided at that point) or proceed with the Phase-2 plan; also still pending from Session A: Vite leaderboard switch to `bestSession*` fields.

## 2026-06-20 ("Rating Lab slice")

Session goal:
Add a standalone `ratings` vertical slice (entity / repository / DTO / mapper / Bean Validation / service / controller / tests) capturing a 1-7 iconicity rating per word per user, keyed `UNIQUE(user_id, ideophone_id)` with a nullable `session_id`, so the guess-vs-rating divergence can be computed later (data only, not the statistic). Minimal standalone table from roadmap step 4 - NOT the Phase-2 model.

Changed:

- `scripts/generate_seed_sql.py` + regenerated `ideophone_arena.sql`: new `ratings` table (`UNIQUE(user_id, ideophone_id)`, nullable `session_id`, `rating SMALLINT NOT NULL`, `response_time_ms INT`, `rated_at TIMESTAMP`, FKs to `app_users`/`ideophones`/`game_sessions`); added the matching `DROP TABLE IF EXISTS ratings;`. Seed data rows unchanged (204 ideophones / 102 rounds).
- New `model/Rating` (`@UniqueConstraint(user_id, ideophone_id)`, nullable `session` ManyToOne, `short rating`, `@CreationTimestamp ratedAt`).
- New `repository/RatingRepository` (`existsByUserIdAndIdeophoneId`, `findByUserIdOrderByRatedAtDesc` with `@EntityGraph(ideophone)`).
- New DTOs `RatingRequest` (`ideophoneId` `@NotNull @Positive`; `rating` `@NotNull @Min(1) @Max(7)`; `responseTimeMs` optional `@Min(0) @Max(600000)`; `sessionUuid` optional) and `RatingResponse` (`id`/`ideophoneId`/`rating`/`responseTimeMs`/`ratedAt` only - no `user_id`/`session_id`, no entity).
- New `mapper/RatingMapper`, `service/RatingService` (`@Transactional` create: resolve user, 404 unknown ideophone, resolve optional session with 404 unknown / 403 unowned, pre-check duplicate -> 409, `saveAndFlush` + `DataIntegrityViolationException` -> 409 backstop; `@Transactional(readOnly = true)` getMyRatings), `controller/RatingController` (`POST /api/ratings` -> 201, `GET /api/game/me/ratings`; both `@Valid`/thin, authenticated via the existing `anyRequest().authenticated()` catch-all - no SecurityConfig change).
- New `RatingHttpTests` (3 tests). No changes to the guessing flow, conditions, difficulty, admin stats DTO, or any experiment invariant.
- Docs: contract (new "Ratings" section + changelog), runbook (rating curl flow), grading checklist (Bean Validation + build/test evidence), punch list ticked.

Proof:

- `python3 scripts/generate_seed_sql.py --check` -> "SQL is up to date: 204 ideophones, 102 rounds".
- Reseed via mysql.exe; `./mvnw spring-boot:run` with `ddl-auto=validate` -> clean start ("Started IdeophoneArenaApiApplication ... Tomcat started on port 8081").
- `./mvnw test` -> 72 tests, 0 failures (was 69).
- Live curl against the running backend: register -> login -> `POST /api/ratings` (rating 6) -> `201 {"id":...,"ideophoneId":1,"rating":6,"responseTimeMs":1500,"ratedAt":...}`; `GET /api/game/me/ratings` returned both ratings, most recent first; duplicate `POST` ideophone 1 -> `409`; `rating:8` -> `400 {"validationErrors":{"rating":"must be less than or equal to 7"}}`; `rating:0` -> `400`; unauthenticated `POST` -> `401`; unknown `ideophoneId` -> `404`.

Result:
Rating Lab slice complete. The `ratings` table is user-keyed (`UNIQUE(user_id, ideophone_id)`) with nullable `session_id` provenance, so a user's rating of word W can later be joined against their guess accuracy on W via `player_answers.target_ideophone_id`. Additive and non-breaking; standalone table only, Phase-2 model not started.

Commit:
Not committed (proposed message below).

Blocker:
None.

Next single task:
If/when the guess-vs-rating divergence UI is wanted, add a read-only endpoint or admin aggregate that joins `ratings` against `player_answers.target_ideophone_id` per user/word (still data, not the Phase-2 refactor).

Proposed commit message:

    Add standalone ratings vertical slice (1-7 iconicity rating per word per user)

    User-keyed ratings table (UNIQUE(user_id, ideophone_id), nullable session_id
    for provenance) added through scripts/generate_seed_sql.py so the guess-vs-
    rating divergence can be computed later. POST /api/ratings (authenticated,
    @Valid 1-7 rating, saveAndFlush + DataIntegrityViolationException -> 409 on
    duplicate, 404 unknown ideophone, 403/404 unowned/unknown sessionUuid) returns
    201 with the rating DTO; GET /api/game/me/ratings returns the caller's own
    ratings. Entity/repository/DTOs/mapper/service/controller follow house style;
    RatingHttpTests covers the round trip (suite: 72). Standalone table only - not
    the Phase-2 model; divergence statistic not computed. Contract, runbook, and
    grading checklist updated.

## 2026-06-20 ("Docker compose spin-up")

Session goal:
Per `docs/archive/SPEC-docker-compose.md` (archived 2026-06-28): one `docker compose up` brings up the API jar plus a seeded MySQL with the stimulus assets so a reviewer can hit the running backend on host port 18081. Packaging only -- no application-code, seed-SQL, symlink-scheme, `ddl-auto`, or Maven-dependency changes.

Changed:

- `Dockerfile` (repo root): multi-stage. Build stage on `maven:3.9-eclipse-temurin-21` copies `.mvn`/`mvnw`/`pom.xml`/`src` and runs `./mvnw -q -DskipTests package`; runtime stage on `eclipse-temurin:21-jre` copies the jar, creates a non-root `arena` user, exposes 8081, and runs `java -jar /app/app.jar`.
- `docker-compose.yml` (repo root): `db` (mysql:8.4) mounts `src/main/resources/db/init/ideophone_arena.sql` -> `/docker-entrypoint-initdb.d/01-schema.sql:ro` (mandatory: `ddl-auto=validate` needs the schema/seed present at boot), named `db_data` volume, `mysqladmin ping` healthcheck. `api` builds from the Dockerfile, `depends_on: db { condition: service_healthy }`, env supplies `SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/${MYSQL_DATABASE}` + root user/pw, `APP_JWT_SECRET=${APP_JWT_SECRET}` (no code default), `APP_STIMULI_LOCATIONS=file:/srv/stimuli/`; publishes `18081:8081`; bind-mounts `${STIMULI_HOST_DIR}` read-only at `/srv/stimuli`. `ddl-auto` left at `validate`.
- `.dockerignore`: excludes `target/`, `.git/`, IDE files, `application-local.properties`, `.env*` (keeps `.env.example`), docs.
- `.env.example`: `APP_JWT_SECRET=`, `MYSQL_ROOT_PASSWORD=`, `MYSQL_DATABASE=ideophone_arena`, `STIMULI_HOST_DIR=`.
- Docs: README "Run with Docker" section; demo-runbook "Start Backend with Docker Compose" section; grading-checklist 2026-06-20 evidence note. No app code, seed SQL, symlinks, `ddl-auto`, or `pom.xml` dependencies touched. CORS for `http://localhost:5174` was already configured in `SecurityConfig`, so no override was needed.

Proof (all 8 spec verification steps executed and green):

- Step 1 -- `./mvnw test` -> 69 tests, 0 failures, BUILD SUCCESS (no app code changed). (This worktree was missing the gitignored `application-local.properties`; copied it from the main checkout `/code/java/ideophone-arena-api` for the local-profile MySQL/JWT config -- gitignored, working tree stays clean.)
- Steps 2-8 were run against a user-space **rootless Docker** (Docker Engine 29.6.0 static binaries + slirp4netns 1.3.4 + compose v5.1.4, all under `~/dockerbin`/`~/bin`/`~/.docker`; no root, no sudo). Daemon ran on overlay2 with the expected rootless cgroup warnings (no cpuset/io delegation, harmless).
- Step 2 -- `docker compose up -d --build`: multi-stage image built (Maven build inside the container), `mysql:8.4` pulled, db came up healthy, then api started. `.env` was the pre-filled gitignored file (cp .env.example .env equivalent).
- Step 3 -- `docker compose ps`: `arena-api-docker-db-1  mysql:8.4 ... Up (healthy)`; `arena-api-docker-api-1 ... Up  0.0.0.0:18081->8081/tcp`.
- Step 4 -- `curl -i http://localhost:18081/api/health` -> `HTTP/1.1 200`, body `{"status":"ok"}`, `Content-Type: application/json`.
- Step 5 -- register `POST /api/auth/register` -> `HTTP/1.1 201`; login `POST /api/auth/login` -> `200` with a 3-segment JWT (179 chars).
- Step 6 -- `curl -I http://localhost:18081/stimuli/audio/a0h-gosogoso.m4a` -> `HTTP/1.1 200`, `Content-Type: audio/mp4`, `Content-Length: 28902` (served from the read-only `/srv/stimuli` bind mount).
- Step 7 -- `docker compose run --rm -e APP_JWT_SECRET= api` -> container exits 1 with `IllegalStateException: app.jwt.secret must be set to a non-blank value` (JwtService.java:36); the fail-fast guard survives containerization.
- Step 8 -- `docker compose down -v` -> exit 0; both containers, the `db_data` volume, and the network removed; `docker compose ps -a` empty, no arena volumes remain.

Result:
Done and fully proven. The compose stack builds the API jar and a seeded MySQL, comes up healthy on host port 18081, serves the API and `/stimuli/**`, and tears down cleanly; the JWT fail-fast guard holds in the container. No application code, seed SQL, symlink scheme, `ddl-auto`, or Maven dependencies were changed.

Commit:
Not committed (proposed message below).

Blocker:
None. (This machine had no container runtime; rather than punt, a rootless Docker was set up in the user's home -- no sudo/password needed -- to run the full oracle. It writes only to `~/dockerbin`, `~/bin/slirp4netns`, `~/.docker/cli-plugins/docker-compose`, and the image/build cache at `~/.local/share/docker` (~1.9G). The daemon was stopped after verification (no unattended process left). To reuse: `source ~/dockerbin/env.sh` then `dockerd-rootless.sh &`. To remove entirely: `rm -rf ~/dockerbin ~/bin/slirp4netns ~/.docker/cli-plugins/docker-compose ~/.local/share/docker ~/.config/docker`.)

Next single task:
Commit the packaging files (Dockerfile, docker-compose.yml, .dockerignore, .env.example) and the doc updates with the proposed message below.

## 2026-06-28 ("Docs cleanup: archive implemented specs")

Session goal:
Docs-only housekeeping on `dev`: archive the two now-implemented design specs and confirm the 2026-06-20 Docker + Rating Lab worktrees are integrated. No code, schema, or seed changes.

Changed:

- Moved `docs/SPEC-docker-compose.md` and `docs/SPEC-rating-lab.md` into `docs/archive/` via `git mv` (history preserved). Both features shipped on `dev` (Docker compose spin-up + standalone Rating Lab slice), so the specs are now reference history, not pending work.
- Updated the one inbound reference (progress-log.md Docker session goal) to the new `docs/archive/SPEC-docker-compose.md` path.
- Refreshed the `Date:` line in `docs/backend-contract.md` to 2026-06-28 (content already current through 06-20).

Proof:

- `grep -rn '^<<<<<<<\|^=======\|^>>>>>>>' .` -> no output (no conflict markers).
- `./mvnw test` -> 72 tests, 0 failures, BUILD SUCCESS (no code touched).
- `python3 scripts/generate_seed_sql.py --check` -> "SQL is up to date".

Result:
Done. The 2026-06-20 Docker and Rating Lab worktrees are integrated on `dev`; their specs are archived under `docs/archive/`. Working tree changes are docs-only.

Commit:
Not committed (proposed message below).

Blocker:
None.

Next single task:
Commit the docs cleanup with the proposed message below.

## 2026-06-30 ("Rating Lab backend: pagination + divergence (NIL-32/33/34)")

Session goal:
Finish the Rating Lab backend vertical on `dev`: paginate `GET /api/game/me/ratings` to the leaderboard convention and give `RatingController` Swagger metadata (NIL-32); add a public read-only guess-vs-rating divergence endpoint (NIL-33); cover both with MockMvc tests and update the contract/checklist (NIL-34). No schema change, no new dependencies.

Changed:

- NIL-32: `RatingRepository.findByUserIdOrderByRatedAtDesc` now returns `Page<Rating>` (kept the to-one `@EntityGraph`); new `RatingPageResponse` mirrors `LeaderboardPageResponse`; `RatingMapper.toPageResponse`; `RatingService.getMyRatings(userDetails, page, size)` clamps size to 50 (mirrors `ScoreService`); `RatingController` gained `page`/`size` params, returns the wrapper, and carries `@Tag`/`@Operation`. Response shape changed bare-array -> wrapper (**breaking** for the Vite frontend; needs a `.entries` rider).
- NIL-33: new `GET /api/research/divergence` (public, mirrors the leaderboard permitAll). Two `GROUP BY` interface projections (`IdeophoneGuessStatsProjection` via `PlayerAnswerRepository.aggregateGuessStatsByIdeophone`, `IdeophoneRatingStatsProjection` via `RatingRepository.aggregateRatingStatsByIdeophone`) merged per ideophone in `ResearchService` the way `AdminStatsService` merges condition stats -- deliberately two queries, not one join, so a cartesian product cannot inflate guess accuracy. New `DivergenceResponse` DTO, `ResearchMapper`, `ResearchController` (`@Tag` "Research"). One row per word with >=1 guess or rating; `guessAccuracy`/`meanRating` are `null` for the zero-count side (clearer than `0.0`). Count fields are `guessCount`/`ratingCount`, not `nGuesses`/`nRatings`: a `getNGuesses` getter serializes as `NGuesses` under the Java Beans two-leading-capitals rule.
- NIL-34: `RatingHttpTests` GET assertions moved to `.entries` + a pagination/size-clamp test; new `DivergenceHttpTests` (public access + per-row invariants, deterministic rated-word row, guessed-word row through the session/answer flow). Updated `docs/backend-contract.md` (ratings section, new divergence section, changelog) and `docs/backend-grading-checklist.md` (authorization + build/test evidence).

Proof:

- `./mvnw test` -> 76 tests, 0 failures, BUILD SUCCESS (was 72).
- `python3 scripts/generate_seed_sql.py --check` -> "SQL is up to date: 204 ideophones, 102 rounds" (no schema change).
- Live curl against `http://localhost:8081`: register -> `201`; `POST /api/ratings` -> `201`; `GET /api/game/me/ratings?page=0&size=5` -> `{entries,page,size,totalElements,totalPages}`; `GET /api/research/divergence` (no auth) -> `200`, 87 rows (a `guessCount=1`/`ratingCount=1` row showed `guessAccuracy:0.0` paired with `meanRating:6.0`); `/v3/api-docs` tags `[Admin, Ratings, Research]`, both new paths present.

Result:
Done and proven. NIL-32 and NIL-33 are implemented to course conventions; NIL-34 tests and docs are in. Working tree is a clean, reviewable set of additions/edits. The thesis showcase words (dokidoki/sakutto/shobon) have no guesses or ratings in the dev DB yet, so endpoint directionality awaits real participant data; the pairing mechanism is proven.

Commit:
Not committed (commits are the user's).

Blocker:
None.

Next single task:
Frontend rider: update the Vite app's `getMyRatings()` to read `.entries` (breaking pagination change), and optionally surface `/api/research/divergence` on the W29 landing page.

## 2026-07-03 ("27E: meaning-order draw exposed + server-side ratable pool (NIL-40)")

Session goal:
Close W27 on `dev`: (a) expose the already-drawn `targetMeaningListedFirst` on the round DTO so the frontend can
honor the meaning-line draw - shuffler untouched, the stream consumption stays final; (b) move the Rating Lab's
word pool server-side as read-only `GET /api/game/me/ratable-words`, enforcing the thesis contamination rule in the
backend and clearing the W30 multi-device blocker. No schema change, no seed change, no new dependencies.

Changed:

- `RoundResponse` gained the additive boolean `targetMeaningListedFirst` (both constructors; `false` on the
  completed-session sentinel, which carries no translations); `GameMapper.toRoundResponse` passes
  `DerivedRound.isTargetMeaningListedFirst()` through. `RoundShuffler` and the draw order are untouched.
- New authenticated `GET /api/game/me/ratable-words`, paginated `{entries, ...}` wrapper mirroring `/me/ratings`
  (identical clamps): `PlayerAnswerRepository.findRatableWordsByUserId` is one JPQL `GROUP BY` over both members of
  the caller's answered rounds with a `NOT EXISTS` against `ratings`, ordered by first encounter (ideophone-id
  tiebreak) so the pool is byte-identical across devices; `RatableWordProjection` (interface projection per the
  AdminStats precedent), `RatableWordResponse`/`RatableWordPageResponse` DTOs, `RatingMapper.toRatableWord*`,
  `RatingService.getMyRatableWords` (`@Transactional(readOnly = true)`), thin `RatingController` endpoint with
  `@Operation`. The `round.practice = false` predicate is defensive only - practice answers are never persisted.
  `meaning` is the word's own gloss: exactly the mapping the round feedback revealed.
- Tests: `RoundResponseSerializationTests` covers the new property; `ShuffledSessionHttpTests.assertServedAsDerived`
  now asserts the served flag equals the seed derivation on every round; new `RatableWordsHttpTests` (4 tests -
  pool contents/meanings + practice exclusion + duplicate-encounter dedup + rated-word removal through the real
  HTTP answer flow; per-user scoping including a foreign rating being a no-op; unauthenticated 401; pagination
  clamps).
- `scripts/cleanup-test-accounts.sql`: browser-loop accounts rate a word since 27D, so the `app_users` delete
  now hits the `ratings` FKs - added a leading `ratings` delete (by user; also unblocks `game_sessions` for
  pre-27E session-linked ratings). Not executed this session; the two `browser_loop_*` proof accounts from the
  27E runs are still in the dev DB.
- Docs: `backend-contract.md` (derivation item 4 and both Consequences bullets updated - the flag is now exposed,
  not reserved; new "Ratable words (2026-07-03)" section; changelog bullet), `backend-grading-checklist.md`
  (expected-contract line, authorization evidence, build/test evidence), `demo-runbook.md` (ratable-words curl).

Proof:

- `./mvnw test` -> 80 tests, 0 failures, BUILD SUCCESS (was 76).
- Live curl against a rebuilt `http://localhost:8081`: register -> `201`; round payloads carry
  `targetMeaningListedFirst` booleans; after 2 practice + 1 scored answer, `GET /api/game/me/ratable-words` ->
  `200` wrapper with exactly the scored round's 2 words (practice words absent, `meaning` = each word's gloss);
  `POST /api/ratings` for one -> `201`; re-fetch -> `totalElements` 1; unauthenticated -> `401`; `/v3/api-docs`
  lists the path.
- Frontend end-to-end proof (browser loop, desktop and 375px) recorded in the web repo's progress log: per-round
  DOM meaning-line order matched each round's own flag, refetch determinism held, and a fresh client saw the
  identical pool.

Result:
Done and proven. Both 27E jobs are additive and to house conventions; the contamination rule now lives server-side
and the round DTO exposes the full derivation. Clean, reviewable tree.

Commit:
Not committed (commits are the user's). Proposed message:
"expose the meaning-order draw on the round DTO and serve the ratable-words pool (NIL-40)" - body: RoundResponse
gains seed-drawn targetMeaningListedFirst (GameMapper pass-through, shuffler untouched, false on the completion
sentinel); new authed GET /api/game/me/ratable-words serves encountered-but-unrated words in the {entries,...}
wrapper via one JPQL GROUP BY + NOT EXISTS, first-encounter order; RatableWordsHttpTests + per-round flag
assertion in ShuffledSessionHttpTests; contract/checklist/runbook updated. No schema change.

Blocker:
None.

Next single task:
NIL-62 free-form-entry build, from the kickoff prompt the NIL-57 architecture session emits (NIL-57 itself is a
chat session, not Claude Code).

## 2026-07-06

Session goal:
Execute M2 "The Re-key" (NIL-68, ADR-0/-1/-3/-6): normalize `ideophones` into `words` + `presentations`, add
`languages` + `pairings` (thesis backfill), rename/collapse `arena_rounds` -> `trials` (102 -> 34), and re-key
`player_answers` + `ratings` to word grain -- with every public response shape frozen. Fixes the event-plane grain
defect (row-grain ratings/pool accept the same word twice across conditions).

Changed:
- `scripts/generate_seed_sql.py`: restructured to emit `languages`(1) + `words`(68) + `presentations`(204) +
  `pairings`(34) + `trials`(34) instead of `ideophones`(204) + `arena_rounds`(102); added `THESIS_PAIR_ACCURACY`
  (correct/36 from thesis-facts §4); `validate_unique_constraints` re-targeted (words UNIQUE(language,romaji),
  presentations UNIQUE(word,condition), pair_code, is_core invariant-4, and the `word_a id < word_b id`
  shuffle-order invariant). `--check` clean; regenerated `db/init/ideophone_arena.sql`.
- Schema (via the generator only): 4 new tables (`languages`,`words`,`presentations`,`pairings`), `arena_rounds`
  -> `trials` (drop prompt/condition_name/difficulty_level/left/right/correct_ideophone_id; add round_type/
  pairing_id/correct_word_id/feature_axis), `player_answers` -> trial_id/selected_word_id/target_word_id,
  `ratings` -> word_id + **UNIQUE(user_id, word_id)**; `ideophones` dropped.
- Entities: new `Language`/`Word`/`Presentation`/`Pairing`/`Trial`(+`RoundType`); re-keyed `PlayerAnswer`/`Rating`/
  `DerivedRound`; deleted `Ideophone`/`ArenaRound`.
- Repositories: `IdeophoneRepository`->`WordRepository`, `ArenaRoundRepository`->`TrialRepository`, new
  `PresentationRepository`; every JPQL re-pointed to word grain; the ratable-pool query grain-heals (GROUP BY
  word.id) so a word met under any condition appears once.
- Services/mappers: `RoundShuffler.pairMember` -> higher **word** id (algorithm byte-for-byte identical -- word
  ids are CSV-ordered so word_b is the k-word, exactly as before); `GameService` serves condition-free trials and
  resolves `presentation(word, session.condition)` for rendering; `GameMapper` takes displayForm/canonicalScript
  from the presentation, the rest from the word; `RatingService` guards UNIQUE(user,word); `ResearchService`
  word-grains divergence (one row per word) and replays position-bias over the condition-free trial list (shuffle
  untouched); `ResearchMapper` maps divergence `displayForm` -> `words.canonical_form`.
- Rider A: the three research aggregates (divergence guess+rating sides, rating-distributions, position-bias) now
  exclude practice trials (defensive) and `browser_loop_%` automation accounts -- no response-shape change.
- Tests re-pointed to word-grain seed-replay (no Trial fixtures; every session uses CONDITION_1_SOKUON seeded
  trials); `IdeophoneSeedIntegrityTests` rewritten to parse the new tables (+1 M2 assertion: every trial maps to a
  pairing whose members include its correct word, word_a<word_b); new grain-heal cross-condition test in
  `RatableWordsHttpTests`.

Proof:
- `python3 scripts/generate_seed_sql.py --check` clean -> regen -> fresh dev re-init via mysql.exe -> app boots
  under `ddl-auto=validate` (Hibernate 7.2.12 EntityManagerFactory built, zero schema-validation errors).
- `./mvnw test` -> **90 tests, 0 failures, 0 errors, 0 skips** (was 88; +1 seed assertion, +1 grain-heal test).
- Live curl-diff against a rebuilt backend (:8082): round + answer + divergence + rating-distributions +
  position-bias + leaderboard shapes byte-match `docs/backend-contract.md` (field names, casing, nesting;
  `canonicalScript` = 2-letter `HU`, `displayForm` verbatim kana, `dPrime` casing, `targetTop/BottomCorrect`).
- Grain defect closed: rate word 47 in CONDITION_1, encounter it again under CONDITION_2, re-rate -> **409**;
  divergence returns exactly **1 row** for word 47 (not 3).
- Rider A: a `browser_loop_%` account rated word 40 (POST 201) yet divergence `ratingCount` stayed 0; it played a
  full 30-answer session yet only the 2 non-loop guesses appear in divergence. Excluded on both sides.
- Rider B: position-bias seed-replay survives (shuffle bytes untouched, only joins moved) -- green in
  `PositionBiasHttpTests` + `ShuffledSessionHttpTests` (byte-identical derivation over the wire).
- **Frontend battery green against the migrated backend on :8081, web repo unedited** (`ideophone-arena-web`):
  `pnpm vitest run` -> 24 files / 153 tests pass; `verify-browser-loop.mjs` desktop **and** 375px both exit 0 with
  `relevantConsoleErrorCount: 0`. The loop played a full 2-practice + 30-scored `CONDITION_1_SOKUON` session,
  asserted DOM meaning-line order against each round's `targetMeaningListedFirst` (32 rounds, stable on refetch),
  rendered choice cards from the migrated word/presentation shapes (0 muted stimuli, 193 successful audio fetches),
  and proved the grain-heal live: ratable pool = **60 distinct words**, deterministic order, cross-device parity,
  drops to 59 after one rating, legacy localStorage pool absent. No horizontal overflow at 375px
  (scrollWidth == innerWidth). The only failures are the documented-benign StrictMode `net::ERR_ABORTED` duplicate
  blob aborts.

Result:
Backend re-key done and proven end-to-end (backend + real frontend). All public shapes frozen; the event plane
enforces one-rating-per-word at the DB layer. Clean, reviewable tree (commits are the user's).

Commit:
Not committed. Proposed message:
"M2 re-key: normalize ideophones into words+presentations, collapse arena_rounds into trials, word-key the event
plane (NIL-68)" - body: languages/words/presentations/pairings + trials (ADR-0/-1/-3/-6) via generate_seed_sql.py;
player_answers/ratings re-keyed to word grain with UNIQUE(user_id, word_id); RoundShuffler id-vocabulary re-bind
(algorithm byte-identical); serving path resolves presentation(word, condition); research aggregates word-grained
+ Rider A browser_loop/practice exclusion; ddl-auto=validate; 90 tests green; public shapes frozen.

Blocker:
None. Process note for the user: the stale pre-M2 backend (a `java -jar target/...jar` from before this session)
was stopped, and the migrated backend now runs on :8081 via `./mvnw spring-boot:run` (current sources). For the
normal jar-based workflow, rebuild with `./mvnw -q package -DskipTests` and run that jar. A dedicated Edge CDP
instance (profile `C:\Temp\edge-cdp-ideophone`) was launched for the browser proof and left running; the web
repo's own Vite dev server on :5174 was reused (not restarted) and never edited.

Next single task:
NIL-54 thesis ingestion (`trials.correct_word_id` is its reconstruction key; ingest with `completed_at = NULL`,
no flag column -- mechanism decided in chat first).

## 2026-07-06 ("NIL-54: thesis tidy-data ingestion + Observatory thesis layer")

Session goal:
Ingest the thesis Gorilla tidy data (2AFC choosing + 7-point rating, 36 participants) into the M2 schema as
generator-emitted seed rows, reconstructed against `trials.correct_word_id`, so divergence/rating stats run on real
data. Decided mechanism (Nils, in chat): `completed_at = NULL`, no flag/`data_source` column, provenance = reserved
`thesis_p%` username prefix excluded from the live Rider A aggregates. Two calls confirmed this session: **exclude
from live + ship a new `/api/research/thesis/divergence`** (inverted predicate) as the Observatory thesis layer, and
**store the one out-of-bound RT (1121099 ms) as-is** (faithful; the 0-600000 bound is live-submission validation only).

Changed:
- `scripts/generate_seed_sql.py`: reads `docs/research/data/gorilla-tidy-{choosing,rating}.csv`; deterministic
  (`uuid.uuid5` for session uuids, sorted participant ordering, no random/datetime). New dataclasses/readers,
  `participant_order`, `resolve_word_id` (a 4-entry sokuon alias map `sakutto->sakuQ`/`kiritto->kiriQ`/`hotto->hoQ`/
  `katitto->katiQ` -- the tidy export's doubled-consonant form vs the seed's Q-truncated romaji), a self-validating
  `validate_thesis` (selection/target are pairing members, target == trial `correct_word_id`, derived is_correct ==
  CSV Correct, rated word is a member), and row builders. Emits 36 `thesis_p01..thesis_p36` app_users (frozen shared
  BCrypt digest, never authenticate), 36 `game_sessions` (`completed_at`/`started_at` omitted -> DDL defaults;
  `shuffle_seed` inert), 1080 `player_answers`, 1080 `ratings`. `ideophone_arena.sql` regenerated (+2249 lines);
  `--check` clean.
- Rider A: added `and <user>.username not like 'thesis!_p%' escape '!'` to the 4 live aggregate queries
  (`aggregateGuessStatsByWord`, `findScoredForPositionBias`, `aggregateRatingStatsByWord`, `aggregateRatingDistribution`);
  added two inverted-predicate methods (`aggregateThesisGuessStatsByWord`, `aggregateThesisRatingStatsByWord`).
  Leaderboard (structural `completed_at is not null`) and admin-stats (intentionally all-cohort) untouched.
- New endpoint `GET /api/research/thesis/divergence`: `ResearchService.getThesisDivergence()` (getDivergence refactored
  to a shared `mergeDivergence`), `ResearchController` mapping, `SecurityConfig` permit. Reuses `DivergenceResponse` --
  no new DTO, no frozen shape changed.
- Tests: `IdeophoneSeedIntegrityTests` (+4, now 13) parses the emitted SQL and reconstructs per-modality accuracy +
  cohort shape; new `ThesisDivergenceHttpTests` (endpoint rollup) and `ThesisCohortExclusionTests` (repo-level
  `raw == rider + thesis + browser_loop` identity).
- Docs: `backend-contract.md` (new endpoint section + changelog), `ARCHITECTURE.md` §7/§12 (recorded the reversal of
  the ADR-5 "in-band" lean), `backend-grading-checklist.md`, this log.

Proof:
- `python3 scripts/generate_seed_sql.py --check` -> clean ("68 words, 204 presentations, 34 pairings/trials, 36 thesis
  users, 1080 answers, 1080 ratings"); emitted-SQL parse confirms 693/1080 correct, 247/231/215 per modality,
  UNIQUE(session_id,trial_id) and UNIQUE(user_id,word_id) hold, RT outlier 1121099 preserved.
- Fresh dev re-init from the regenerated SQL; app boots under `ddl-auto=validate` (no drift). `./mvnw test` -> **97
  tests, 0 failures**.
- Live curl on a pristine reseed: `GET /api/research/divergence` = `[]` and `/api/research/rating-distributions` empty
  (thesis excluded from the live layer despite 1080 seeded thesis ratings); `GET /api/research/thesis/divergence` =
  200 with 30 rows (guessCount/ratingCount 36 each) rolling up to 68.6/64.2/59.7; row 1 `gosogoso` guessAccuracy
  0.6944 == `pairings.thesis_accuracy` for `a0`. DB cross-check confirmed thesis 360 answers/modality at
  68.6/64.2/59.7, `live_ans` = 0.

Result:
Thesis data is in the DB and reconciles to the vendored figures. The live Observatory stays live-player-only and
byte-stable; the thesis baseline is a separable inverted-predicate layer. No schema change (`ddl-auto=validate`),
no flag column, response shapes frozen (the new endpoint is additive). Clean, reviewable tree (commits are Nils's).

Commit:
Not committed. Proposed message:
"NIL-54: ingest thesis tidy data as generator-emitted seed + Observatory thesis layer" -- body: 36 thesis_p## users /
36 sessions (completed_at NULL) / 1080 answers / 1080 ratings via generate_seed_sql.py (--check clean; 4-entry sokuon
alias; self-validating reconstruction against trials.correct_word_id); Rider A extended to exclude thesis_p% from the
live divergence/rating-distributions/position-bias aggregates; new public GET /api/research/thesis/divergence
(inverted predicate, reuses DivergenceResponse) reconciles to 68.6/64.2/59.7; reverses ADR-5 "in-band" (ARCHITECTURE
§7/§12 amended); no schema change; 97 tests green.

Blocker:
None. Process note: the local dev DB was reseeded from the regenerated `ideophone_arena.sql` (drops all tables incl.
any prior test accounts); the validate-boot jar (`java -jar target/...jar`) launched for the live curl proof was
stopped. `scripts/__pycache__/` is untracked build noise (not staged).

Next single task:
View-design adjudication artifact.

## 2026-07-07 ("NIL-44: portfolio README narrative + architecture story + privacy rider")

Session goal:
Rewrite README.md as a portfolio-grade artifact (research story, iconicity hook, architecture story,
honest current state, one-command run) with the three Observatory portfolio figures placed one per
narrative beat; clear the api half of the gorilla-tidy participant-data privacy gate. Docs-only;
commits are Nils's.

Changed:
- README.md rewritten (was a how-to-run doc). Narrative arc (MA thesis -> gamified 2AFC instrument ->
  live research surface); the dumbbell/scatter/radar figures at "the claim" / "the two measures" /
  "the fingerprint"; adopted mode slate (Meaning Match, Rating Lab, Perception Ladder, Word Mint,
  Word Anatomy, Polyglot Challenge -- Script Lab framed as a feature within Meaning Match); the
  word-grain schema story (M2), deterministic shuffle, three planes, hard layering rules, hand-rolled
  JWT; current-state LIVE vs ROADMAP; deploy = designed/costed, not live; full endpoint surface incl.
  the four /api/research Observatory endpoints; Paulsson (2025) full-title citation + McLean 2023 /
  Iida & Akita 2023 (CC BY). Six interpretive sections carry NIL-84 essence-review HTML-comment flags.
- Figures copied web -> api docs/images/ (observatory-{dumbbell,scatter,radar}.png).
- Privacy rider: git rm --cached docs/research/data/gorilla-tidy-{choosing,rating}.csv (both carry a
  "Participant Private ID" column); .gitignore entries added; files kept on disk for the seed
  generator; aggregate thesis-per-pair-stats.csv stays tracked.

Proof:
- Docs-only tree (git diff --cached --name-only): README.md, .gitignore, docs/images/*.png (added),
  gorilla-tidy-*.csv (deleted). No .java/src/main/src/test/pom.xml changes, so the 97-test suite is
  untouched (last green: 97 tests, NIL-54).
- Rider: `git ls-files | grep gorilla-tidy` -> empty; both CSVs still on disk; grep confirmed no other
  tracked file carries participant IDs (generate_seed_sql.py matches "Private ID" only as a CSV
  column-name reference; signoff.xlsx sharedStrings clean; design-archive/gorilla-*.png are UI-design
  screenshots, two spot-checked, out of the rider's scope).
- README verified: no "orthogonal"/"unrelated" in prose; <em> tags balanced (no nesting); no repo-
  escaping links; all three image paths resolve; anchors match headings. Two adversarial review
  workflows (gather + 5-critic review): 0 blockers, honest-state clean, framing compliant.

Result:
README reads as a portfolio piece and traces every number to thesis-facts.md or a committed endpoint;
the api half of the privacy gate is closed. Reviewable, docs-only tree; commits are Nils's.

Commit:
Not committed. Proposed (two commits, or squashed to one):
1. "NIL-44: portfolio-grade README (research story + architecture + Observatory figures)"
2. "NIL-44 rider: untrack gorilla-tidy participant CSVs before public (privacy gate, api half)"
Squashed: "NIL-44: portfolio README + untrack participant-data CSVs (privacy gate, api half)".

Blocker:
None for the api half. The WEB half of the privacy gate is still open (NIL-80's raincloud reads a
gorilla-tidy CSV in place -> vendored-aggregate replacement) and rides the first post-NIL-80-commit
web session. The demo GIF (NIL-45) is a placeholder comment, not recorded. Planning docs
(summer-2026-plan.md / execution-plan / SPEC-hosting) read from the /mnt/c planning folder, not the
api repo.

Next single task:
NIL-84 (essence review, Fable, morning).

## 2026-07-08 ("NIL-86: stimulus-expansion pipeline - dark inventory + TTS route + foil_distance")

Session goal:
Build the expansion pipeline in the api repo: ingest the signed-off expansion pairs from the sign-off
workbook into words/presentations/pairings as dark inventory (zero live pools until audio exists), fill
the A6 sign-off trail (signoff_ref/approved_at) and compute+store foil_distance (validation-only), report
the mixed-pair count, and emit the GCP TTS script + manifest for Nils to run. Seed changes via
generate_seed_sql.py --check only; contract shapes frozen.

Changed:
- scripts/generate_seed_sql.py: reads docs/research/data/stimulus-expansion-{pairs,candidates}.csv (committed
  derived exports of the .xlsx; generator stays stdlib-only), mints 34 new words + 3 presentations each,
  appends 21 pairings (source='EXPANSION', pair_code exp-<id>) with NO trials (dark), resolves the 8 mixed
  pairs' reused word_2 to existing rows (word_a = lower id, preserving the shuffle-order invariant). New:
  hepburn_to_kunrei validator, mora_segments/feature_vector/foil_distance (SPEC-free-form-entry §5, Decimal-
  deterministic), pair-provenance-homogeneity assertion. Count asserts 68/204/34 -> 102/306/55; foil_distance
  DECIMAL(6,4) branch in sql_literal; signoff_ref/approved_at wired; trials gated to THESIS only.
- src/main/java/.../model/enums/Modality.java: + HAPTIC (additive; 4 clean Haptic pairs).
- src/test/java/.../seed/IdeophoneSeedIntegrityTests.java: counts 102/306/55; AUDIO_PATTERN \d+ and 'h'->HAPTIC;
  pairings split THESIS(34)/EXPANSION(21); new expansionPairingsAreDarkWithNoTrials.
- New: docs/research/data/stimulus-expansion-pairs.csv (21) + stimulus-expansion-candidates.csv (34);
  scripts/generate_tts_audio.py + scripts/tts-manifest.json (34 pending + 68 present backfill; Nils runs the batch,
  this session does NOT call GCP); docs/research/phonology-golden.json + scripts/generate_phonology_golden.py
  (ADR-8.2 Python side; Java PhonologyService parity owed to NIL-62). Docs: backend-contract changelog, AGENTS.md
  punch list.
- Scope decision (Nils, in chat): defer H2/H3 (fuwafuwa/gotsugotsu, shittori/bosabosa - reuse VISUAL thesis words
  34/40 on the Haptic floor; flipping would break the same-modality invariant, crash ResearchService, and re-bucket
  72 thesis answers). 21 pairs this session; H2/H3 + full Haptic go-live ride NIL-57.

Proof:
- python3 scripts/generate_seed_sql.py --check -> exit 0 (102 words, 306 presentations, 55 pairings [21 expansion
  dark, 8 mixed thesis x expansion], 34 trials, 36 thesis users, 1080 answers, 1080 ratings). Byte-diff vs prior
  seed: only 3 changed lines (former-last row of words/presentations/pairings, `;`->`,` append terminator; data
  identical); trials/player_answers/ratings INSERT blocks byte-identical.
- Both new build-failing assertions demonstrated firing (monkeypatch, tree untouched): cross-modality ->
  "is_core pairing exp-i1 members disagree on modality (VISUAL vs INTEROCEPTIVE)"; provenance-mixed ->
  "Pairing exp-h1 members disagree on audio provenance (tts:gcp:ja-JP-Wavenet-B vs human)".
- ./mvnw test -> 20 suites, 98 tests, 0 failures/errors/skipped.
- Live darkness (booted app on 8081 against reloaded DB): full session serves 30 scored rounds / 60 distinct
  thesis words, 0 expansion words; ratable pool totalElements=60, 0 expansion; admin byModality =
  [AUDITORY, INTEROCEPTIVE, VISUAL] (no HAPTIC). DB: 102/306/55/34; max trial correct_word_id = 67.
- foil_distance hand-check (3 pairs) matches the emitted seed + phonology-golden.json: exp-h1 sarasara/nebaneba
  0.0750, exp-h4 dorodoro/tsururi 0.5375, exp-a3 zyaazyaa/potopoto 0.2250.

Result:
The 21 expansion pairs + 34 new words are seeded as dark inventory, invisible to every live pool; the A6 columns
are filled and foil_distance is computed and stored; the TTS batch tooling + provenance manifest + phonology golden
are emitted for Nils's GCP run. Zero API/frontend surface changed. Clean tree; commits are Nils's.

Commit:
Not committed (Nils's). Proposed single commit:
"NIL-86: seed 21 expansion pairs as dark inventory + fill A6 sign-off/foil_distance + TTS batch tooling".

Blocker:
None. Open riders (handoff): (a) Nils runs scripts/generate_tts_audio.py --run to synthesize the 34 pending clips,
then a later session audits the audio and flips them live (add trials); (b) H2/H3 + full Haptic-floor go-live =
NIL-57 (needs the gotsugotsu/bosabosa modality reclassification, a deliberate thesis-data change); (c) stimulus_sources
(M5) is the eventual provenance home; (d) Java PhonologyService parity vs phonology-golden.json = NIL-62; (e) the
launcher said "~180 existing audio files" but the repo has 68 per-word .m4a - manifest covers the 68.

Next single task:
Nils runs the TTS batch (scripts/generate_tts_audio.py --run) with his GCP credentials.

## 2026-07-08 ("NIL-60: A/V/I expansion go-live - audit 34 TTS clips + seed trials for 17 pairs")

Session goal:
Verify the 34 expansion TTS clips synthesized 2026-07-08 (ja-JP-Wavenet-B) sound and are correctly named, then
bring the A(auditory)/V(visual)/I(interoceptive) expansion pairs live (trials in the seed, served in Meaning Match)
while the HAPTIC pairs stay dark (no served mode until the Touch floor, NIL-41/42).

Changed:
- scripts/generate_seed_sql.py: flip the trial-emission filter so the 17 A/V/I expansion pairs emit CHOOSING trials
  (`source != EXPANSION or modality != HAPTIC`); expansion trials carry `correct_word_id = NULL` (no thesis fixed
  target; the served target is shuffle-derived and that column is unread for CHOOSING). Added `HAPTIC_MODALITY`
  constant; updated the render/validate/summary comments and the `main()` summary (now reports A/V/I live vs HAPTIC
  dark). `--check` byte-stable (thesis/practice rows 1-34 untouched; only 17 new trial rows + a header comment).
- src/main/resources/db/init/ideophone_arena.sql: regenerated. trials 34 -> 51 (30 thesis + 17 A/V/I expansion + 4
  practice). New expansion trial ids 35-40 (A1-A6), 45-49 (I1-I5), 50-55 (V1-V6); ids 41-44 (H1/H4/H5/H6) skipped.
- src/test/.../seed/IdeophoneSeedIntegrityTests.java: `expansionPairingsAreDarkWithNoTrials` reworked into
  `hapticExpansionStaysDarkWhileAviExpansionServes` (asserts the served pool contains all 17 A/V/I expansion
  pairings and zero HAPTIC, 8 HAPTIC words stay dark); `trialsCollapseToThirtyFourWithFourPractice` ->
  `trialsAreFortySevenScoredPlusFourPractice` (51/47/4); `everyTrialMapsToAPairing...` branches on source
  (expansion trials assert `correct_word_id` NULL, thesis assert a member); class doc updated.
- src/test/.../controller/: GameLoopHttpTests (30L->47L scored assertion, completion loop cap 40->60);
  PracticeRoundHttpTests (hardcoded 30 -> scored.size()); RatableWordsHttpTests (dropped the SCORED_WORDS=60 literal
  for the dynamic `expectedScoredWordIds().size()`, loop cap 40->60); stale "30 scored" comments in PositionBias /
  Leaderboard updated.
- docs/backend-contract.md (shuffle spec 30->47 + base-list-growth compatibility note + changelog entry),
  docs/demo-runbook.md (scored 30 -> 47).
- ideophone-arena-web/dist/stimuli/audio/: copied the 34 new .m4a from source stimuli/audio/ into the backend-served
  dist (the documented manual media step until the next vite build); 68 -> 102 clips served.

Proof:
- Audit: ffprobe on all 34 clips -> exist, named `<3char>-<romaji>.m4a` (invariant 2), aac / mono / 24 kHz,
  duration 0.57-0.94 s (shortest = the punctual h6h-gyuQ). No silent/failed synth.
- `python3 scripts/generate_seed_sql.py --check` -> exit 0 ("102 words, 306 presentations, 55 pairings [17 A/V/I
  expansion live, 4 HAPTIC expansion dark, 8 mixed], 51 trials, 36 thesis users, 1080 answers, 1080 ratings").
- DB reloaded; `SELECT`: trials_scored=47, trials_total=51, expansion_trials=17, haptic_trials=0,
  expansion_null_correct=17.
- `./mvnw test` -> 98 tests, 0 failures, 0 errors.
- Live (booted on 8081): register 201 -> start session 201 -> walked 47 scored rounds to `completed:true`;
  expansion word `urouro` (id 98, VISUAL, round 53) served live; `GET /stimuli/audio/v13h-urouro.m4a` -> 200
  audio/mp4 9984 bytes, ffprobe aac/mono/24 kHz/0.808 s (auditory a10k-batabata + interoceptive i13h-guruguru also
  200 audio/mp4); `GET /api/game/me/ratable-words` totalElements=86 with expansion words present; `GET
  /api/leaderboard` 200, thesis_p excluded, our session bestSessionAnswered=47.

Result:
The 17 A/V/I expansion pairs serve live in Meaning Match (30 -> 47 scored rounds; ratable pool 60 -> 86); the 4
HAPTIC pairs remain dark with a test asserting exactly that; audio plays via the API path. Zero DTO/endpoint shape
changes. Clean api tree except the generated seed + tests + docs; commits are Nils's.

Commit:
Not committed (Nils's). Proposed single commit (api):
"NIL-60: bring A/V/I expansion pairs live (17 trials, 30->47 scored rounds); keep HAPTIC dark".
(web dist/stimuli/audio is gitignored; the 34-clip copy is a runtime media step, not a commit.)

Blocker:
None. Riders: (a) HAPTIC-floor go-live (H1/H4/H5/H6 + the reused-VISUAL H2/H3) rides NIL-41/42's Touch floor;
(b) NIL-85 sampling design consumes the enlarged pool (next session); (c) the dist copy is interim until the next
vite build regenerates dist/stimuli from source.

Next single task:
NIL-85: sampling design over the enlarged 47-round pool.

## 2026-07-08 ("NIL-41: Perception Ladder backend - M1 game_mode + floors API + Touch go-live + essence riders")

Session goal:
Build the Perception Ladder floor model and serving (mode LADDER): a floors endpoint in hierarchy order (Sound ->
Sight -> Touch -> Inner states) with each floor's easy->hard pairs, and floor-scoped session start. Land M1
(game_sessions.game_mode) which this build rides. Ship the Touch floor live as a 4-pair floor (user decision at
plan time). Fold in essence-review riders A1/A2/A3/A7/A10.

Changed:
- scripts/generate_seed_sql.py: dropped the dark-filter so all 21 expansion pairs emit trials (4 HAPTIC now live,
  ids 41-44, `correct_word_id = NULL`); added `game_mode VARCHAR(30) NOT NULL DEFAULT 'CHOOSING'` + `ladder_floor
  VARCHAR(30) NULL` to the game_sessions DDL and dropped the `condition_name DEFAULT 'TEXT_ONLY'` (A1). Regenerated
  the seed; `--check` byte-stable except the DDL + 4 trial rows + a comment.
- Enums: added `GameMode {CHOOSING,LADDER,TEMPLATE_READING,CROSS_LINGUISTIC}` (M1); deleted dead `ScriptType` (A2);
  removed `ConditionName.TEXT_ONLY` (A1).
- `GameSession`: `+gameMode` (default CHOOSING) `+ladderFloor` (Modality); dropped the TEXT_ONLY field default; ctors
  no longer take `difficultyLevel` (field stays =1, the locked invariant).
- DTOs: `StartSessionRequest` `+gameMode +floor -difficultyLevel` (A3); `GameSessionResponse`/`RoundResponse`
  `-difficultyLevel` (A3), session response `+gameMode +floor`; `IdeophoneChoiceResponse` `-canonicalScript` (A7);
  new `LadderFloorsResponse`/`LadderFloorResponse`/`LadderPairResponse`.
- Serving seam (ADR-2): `RoundSource` + `ChoosingRoundSource` + `LadderRoundSource` dispatched by
  `Map<GameMode,RoundSource>`; `RoundShuffler.deriveLadderRounds` on the reserved `+4` stream (no shuffle);
  `LadderFloors` holds the code map (thesis-facts §8 for A/V/I, four-floor §4 for Touch). `GameService` routes
  getNextRound/submitAnswer/completion through the seam; per-mode validation (LADDER needs a served floor, forbids
  practice). `LadderService` + `GET /api/game/ladder/floors` (per-caller cleared/best-score).
- Shuffle-contract guard: `TrialRepository.findScoredChoosingTrials()` excludes HAPTIC, so Meaning Match stays the
  frozen 47-pool (both `GameService` and `ResearchService` position-bias consume it). New
  `findScoredTrialsByPairCodes` for floor serving; `PlayerAnswerRepository.findCompletedLadderSessionScores` for
  progress; `gameMode = 'CHOOSING'` guards added to `findLeaderboard` (x3), `findScoredForPositionBias`, and live
  `aggregateGuessStatsByWord` so ladder answers stay out of the frozen aggregates (byModality left open -> HAPTIC
  organic).
- A10: `@NotReservedUsername` validator on `RegisterRequest.username` (trim + lowercase, rejects `thesis_p` /
  `browser_loop_` -> 400 `validationErrors.username`).
- Tests: refit the seed-integrity (invert HAPTIC-dark -> all-expansion-serve; 55 trials/51 scored), Game loop,
  GameService (RoundSource seam ctor), RoundResponse serialization tests; added LadderFloors/LadderService/
  RoundShuffler(+4)/LadderHttp/RegistrationHttp tests. Docs: backend-contract (new Perception Ladder section +
  changelog; session-start/round-field lists trimmed).

Proof:
- `python3 scripts/generate_seed_sql.py --check` -> exit 0 ("... 55 pairings [17 A/V/I + 4 HAPTIC expansion, all
  live] ... 55 trials ..."). DB re-init; `SELECT` trials_total=55, scored_choosing(non-HAPTIC)=47, haptic=4;
  game_mode + ladder_floor columns present.
- `./mvnw test` -> 113 tests, 0 failures, 0 errors.
- Live (booted on 8081, pristine seed): `GET /api/game/ladder/floors` -> 4 floors in hierarchy order, within-floor
  order = §8 (a9->a5 / v2->v4 / i3->i2), Touch pairCount=4 (exp-h1,exp-h5,exp-h4,exp-h6; finalRung exp-h6), no
  difficulty numbers. Start LADDER+HAPTIC -> served the 4 Touch pairs in map order; `/stimuli/audio/h6h-gyuQ.m4a`
  -> 200 audio/mp4; after completion HAPTIC cleared=true bestCorrect=3 bestAnswered=4. Meaning Match (CHOOSING)
  still walked exactly 47 scored rounds to completion. Registration of `thesis_p37` -> 400.

Result:
Perception Ladder ships as a 4-floor climb (Sound 10 / Sight 10 / Touch 4 / Inner states 10) with a data-driven
floors API and floor-scoped serving; M1 game_mode landed. Touch is live but served only through the ladder, so
Meaning Match and its frozen shuffle are untouched. Five essence riders (A1/A2/A3/A7/A10) folded in. Clean api tree
except the generated seed + code + tests + docs; commits are Nils's.

Commit:
Not committed (Nils's). Proposed single commit (api):
"NIL-41: Perception Ladder backend (M1 game_mode + floors API + Touch floor live) + essence riders A1/A2/A3/A7/A10".
(web dist/stimuli/audio already carries the 8 HAPTIC clips from NIL-86; no media step this session.)

Blocker:
None. Handoff to 28B/NIL-42 (frontend): floors-from-data verified; DTO shapes are floor {modality, pairs[],
finalRungPairCode, pairCount, cleared, bestCorrect, bestAnswered} and session-start {gameMode:"LADDER", floor:MODALITY};
replay allowed; sequential unlock is advisory (render AFTER FLOOR n, no server block); FINAL RUNG grammar must handle
a 4-pair Touch floor; and the web sweep of the removed `difficultyLevel`/`canonicalScript` fields + the six README
flags rides NIL-42.

Next single task:
28B/NIL-42: Perception Ladder frontend (floor chrome from the floors API) + the web `difficultyLevel`/`canonicalScript`
sweep.

## 2026-07-09

Session goal:
NIL-62 backend half: build the production vertical (free-form entry / Word Mint) -- the third measure -- on the
adopted architecture (M3 productions table, ADR-8.1 PhonologyService with the profile seam, ADR-5 triangulation).

Changed:
- `scripts/generate_seed_sql.py`: `productions` DDL (DROP + CREATE, no INSERT -- the measure is player-generated).
  `--check` clean; the seed diff is purely additive (37 insertions, 0 deletions), so thesis rows stay byte-stable.
  `generate_phonology_golden.py --check` still up to date (the golden is untouched).
- `service/PhonologyProfile` + `PhonologyFeatures` + `PhonologyService`: the SPEC section 5 engine. Pure functions,
  no repository access, `PhonologyProfile` on every method (Japanese the only v1 profile). Java half of the ADR-8.2
  dual implementation. Two entry points, deliberately: `normalizeInput()` is the player path (gate + Hepburn folds
  + n->N + trailing q->Q), while `morae()`/`features()` take canonical romaji, which already carries N/Q markers.
- Production vertical, mirroring ratings 1:1: `model/Production`, `repository/ProductionRepository` (+
  `IdeophoneProductionStatsProjection`), `service/ProductionService`, `mapper/ProductionMapper`,
  `controller/ProductionController`, seven DTOs, `exception/UnparseableInputException` + its handler.
- `GET /api/research/triangulation` (public): reuses the two divergence aggregates verbatim and adds one production
  aggregate; three separate GROUP BYs merged on word id. `/api/research/divergence` untouched. `SecurityConfig`
  gains the one `permitAll` line it needs.

Two spec defects found by running the Python oracle rather than reading it, both adjudicated in chat:
- `normalized_form VARCHAR(32)` overflows on legal input: `"ja" x 12` is 24 chars, passes the gate, parses to 12
  morae, and folds to `"zya" x 12` = 36 chars. It would have reached `saveAndFlush` and surfaced as a spurious 409.
  Column is now VARCHAR(40).
- The scorer's rounding mode was unspecified and is highly observable: `100 x similarity` is an exact .5 tie for
  3142 of 10404 word pairs, and HALF_EVEN vs HALF_UP disagree on 1608. Pinned to HALF_EVEN (the generator's own
  convention for `foil_distance`), recorded by `scorer_version = 1`.

Decisions:
- HAPTIC joins the prompt cycle now (`AUDITORY -> VISUAL -> HAPTIC -> INTEROCEPTIVE`, 94-word pool). Both stated
  activation triggers are met: NIL-86 seeded the 8 HAPTIC words, and NIL-41 brought the Touch floor live. Deferring
  would have left HAPTIC with a permanently-null `meanProductionScore` beside a real `meanRating`, and adding it
  later would reorder every existing player's prompts.
- `features[]` carries all seven chips with polymorphic `yours`/`target` (5 booleans, an int, a decimal).
- No `ProductionAlreadyExistsException` (reuse `ConflictException`) and no `OpenApiConfig` change (`@Tag` lives on
  the controller). Both deviate from the SPEC section 6 file list, deliberately.

Proof:
- `python3 scripts/generate_seed_sql.py --check` -> "SQL is up to date: 102 words, 306 presentations, 55 pairings,
  55 trials, 36 thesis users, 1080 answers, 1080 ratings". `generate_phonology_golden.py --check` -> up to date.
- `./mvnw test` -> 155 tests, 0 failures, 0 errors (was 113). `PhonologyServiceTests` (20) asserts byte-for-byte
  parity against the ENTIRE `docs/research/phonology-golden.json`: 102 words (morae, all 7 features, heavy/light
  counts) and 21 `foil_distance` strings, plus the SPEC section 10.1 goldens, the adjudicated
  `pikapika -> dokidoki = 78` mockup example, and the seven silent-divergence traps.
- Booted on 8081 with `ddl-auto=validate` unchanged. Live: `GET /api/productions/next` -> word 1 gosogoso AUDITORY
  (no romaji/kana/audio); `POST` exact form -> 201 score 100 with 7 features + `displayForm` ごそごそ;
  `pikapika` vs word 60 -> 201 score 78, mismatched chips `voicedOnset` + `heavyVowelRatio`; repeat -> 409;
  `"ngrk"` -> 400 `validationErrors.input` and word 21 still on offer; unknown ideophone -> 404; unknown session ->
  404, another user's -> 403, own -> 201; `me/productions?page=-3&size=999` -> page 0 size 50, most recent first;
  unauthenticated `GET /api/research/triangulation` -> 200, 87 rows vs divergence's 86 (the extra is HAPTIC word 79
  `sarasara`: real `meanProductionScore`, `guessAccuracy: null`), 0 shared-measure mismatches with divergence,
  0 null-for-zero-count violations. Swagger lists all four endpoints.

Result:
The measurement triad is complete on the backend. Production scoring is a proven port of the seed generator's
metric, not a re-derivation from prose -- the golden file is the contract, and NIL-58 must import
`PhonologyService.features()`/`distance()` rather than fork it. Clean api tree except the generated seed + code +
tests + docs; commits are Nils's.

Commit:
Not committed (Nils's). Proposed single commit (api):
"NIL-62: production vertical (free-form entry) + PhonologyService + triangulation endpoint".

Blocker:
None. Two notes for the record:
- Found but NOT fixed (own patch, own contract line): `GET /api/game/me/ratings` has the same same-second ordering
  ambiguity this session fixed for productions. `rated_at` is a second-resolution TIMESTAMP and ties already exist
  in live data (3 ratings sharing one `rated_at`), so "most recent first" is currently insertion-order-dependent.
  The fix is `findByUserIdOrderByRatedAtDescIdDesc`; no response-shape change.
- The launcher asks for a spec-staleness diff against planning canon. The planning folder is not mounted in this
  session, so the repo copies were only checked for internal currency (`SPEC-free-form-entry.md` has section 13 and
  the section 9 supersession banner; `SPEC-view-designs.md` section 8 has the frozen NIL-83 strings).

Next single task:
NIL-62 frontend session: `ProductionLab` in the mode shell against the post-NIL-65/NIL-69 stack -- prompt card ->
romaji input -> reveal card reusing the feedback-panel layout + `StimulusPlayback`, motion-reveal choreography
(essence-review D1), only the frozen SPEC-view-designs section 8 strings, kana discipline per invariant 1.

## 2026-07-09 ("NIL-88: A10 automation exemption + ratings tie-break + AuthService Locale.ROOT")

Session goal:
Land three adjudicated api fixes and, as their proof-of-life, take NIL-42's sanctioned browser loop to green end to
end -- closing the proof gate the A10 guard had been blocking.

Changed:
- `validation/ReservedUsernamePrefixValidator`: gains a constructor
  `@Value("${app.automation.allow-reserved-registration:false}") boolean` and returns `true` early when set. Spring's
  autoconfigured `LocalValidatorFactoryBean` installs `SpringConstraintValidatorFactory`, which instantiates
  validators through `beanFactory.createBean(...)`, so the lone constructor is autowired without `@Autowired` (Boot
  4.0.6 / Framework 7.0.7, read from source before relying on it). Prefix list, message, and the NIL-41 `Locale.ROOT`
  fold are untouched.
- `src/main/resources/application-automation.properties` (new, tracked): the property's only source in the repo.
  Loads solely under the `automation` profile. **A10 stays pre-deploy-hard**: `docker-compose.yml` sets no
  `SPRING_PROFILES_ACTIVE`, so the container's active profile stays `local` and this file is never read; the `@Value`
  default is `false`. `./mvnw test` also runs `local`-only, which is precisely what keeps `RegistrationHttpTests`
  honest as the guarded-default proof. Placement was adjudicated in-session: putting the key in
  `application-local.properties` would have been loaded by the test JVM, failing the three rejection tests and making
  "absent => guarded" untestable.
- `repository/RatingRepository` + `service/RatingService`: `findByUserIdOrderByRatedAtDesc` ->
  `findByUserIdOrderByRatedAtDescIdDesc` (single call site), with the rationale comment mirrored from
  `ProductionRepository`. No response-shape change; resolves the defect NIL-62 deferred.
- `service/AuthService`: the registration email fold takes `Locale.ROOT`. Swept `src/main/java`: this was the last
  bare `toLowerCase()`; `PhonologyService`, `ProductionService`, and the validator already passed `Locale.ROOT`, and
  there is no `toUpperCase(` anywhere. No register/login mismatch exists -- login authenticates by username, and the
  username is never case-folded.
- Tests: new `RegistrationAutomationHttpTests`
  (`@SpringBootTest(properties = "app.automation.allow-reserved-registration=true")`, the `StaticResourceHttpTests`
  precedent) asserts `browser_loop_*` -> 201. It deliberately never registers a `thesis_p*` row. `RatingHttpTests`
  gains `sameSecondRatedAtTiesBreakByDescendingId`, which fabricates an exact tie with a raw-JDBC
  `update ratings set rated_at = ?` (JPA maps the column `updatable = false`) and asserts id-descending order.
- Docs: contract changelog entry + the ratings tie sentence + the NIL-62 "known defect" note marked RESOLVED;
  demo-runbook gains a "Booting for the browser loop (automation profile)" section; grading checklist retires its last
  open box ("live browser click-through outside MockMvc") with evidence and records the A10 default/exemption proof.
- `docs/specs/SPEC-free-form-entry.md`: re-copied from planning canon by Nils mid-session; now carries both NIL-62
  as-built corrections (`normalized_form VARCHAR(40)`, `similarity_score` rounding pinned HALF_EVEN).
- web `scripts/verify-browser-loop.mjs` (3 harness-correctness edits, authorised in-session; no product/copy change):
  removed the `"difficultyLevel":1` session-request assertion (A3 deleted the field in NIL-41); raised the 40-round
  runaway cap to 60 and de-hardcoded its message; relaxed `Round 1 / 30` to `Round 1 / ` since the scored total moved
  30 -> 47 in NIL-60 and the backend seed tests own that number. All three sat *past* the A10 register wall, so none
  had executed since NIL-41/NIL-60 landed.

Proof:
- `./mvnw test` -> **Tests run: 157, Failures: 0, Errors: 0** (was 155). `RegistrationHttpTests` 4/4 still reject
  under `local`; `RegistrationAutomationHttpTests` 1/1 allows under the flag; `RatingHttpTests` 4 -> 5.
- The tie test genuinely bites: reverted to `findByUserIdOrderByRatedAtDesc` and re-ran it -> `AssertionFailedError:
  expected: <1161> but was: <1159>` (MySQL surfaced ascending PK order for the tied rows). Restored, green. Recorded
  honestly: post-fix the assertion is deterministic; pre-fix red is empirical, since SQL leaves tied-row order
  unspecified.
- Boot `./mvnw spring-boot:run` (profiles: `local`): `POST /api/auth/register` `browser_loop_smoke_<ts>` -> **400**
  `{"validationErrors":{"username":"Username uses a reserved prefix"}}`; `thesis_p99` -> **400**; normal -> **201**.
- Boot `-Dspring-boot.run.profiles=local,automation` (log: `The following 2 profiles are active: "local",
  "automation"`): the same `browser_loop_*` register -> **201** + token; normal -> **201**.
- **Sanctioned loop green** (backend `:8081` on `local,automation`, Vite `:5174`, headless Chromium CDP `:9224`):
  `node scripts/verify-browser-loop.mjs` exit 0 at **1280** (`scrollWidth 1265 <= 1280`) and at **375**
  (`391 == 391`), plus the default-desktop window. Each run: 49 answered rounds (47 scored + 2 practice), completion
  + leaderboard + recent attempts visible, ladder played to `finalRungSeen` over 10 pairs, 7/7 rating scale enabled
  and a rating confirmed, ratable pool 86 with fresh-client parity (85 = 86 - 1 rated), meaning-order both-ways
  across 59 asserted rounds, `staleControlCount: 0`, `mutedStimulusCount: 0`, **0 console errors**.
- `pnpm lint` clean after the harness edits.

Result:
All three patches landed behind a green suite, and NIL-42's sanctioned-loop proof gate is closed. A10 is unchanged in
every boot a grader or the deploy will use; the exemption exists only where the fence needs it.

Commit:
Not committed (Nils's). Proposed single commit (api):
"NIL-88: dev-profile automation exemption for reserved prefixes + ratings tie-break + AuthService Locale.ROOT".
The web harness edits are a separate, uncommitted web-tree change.

Blocker:
None. Four notes for the record:
- **Found here, fixed in the NIL-89 entry below:** `src/App.tsx:39` `const DEMO_TOTAL_ROUNDS = 30` fed
  `TrialPlayer.totalRounds` on the CHOOSING path while a session serves 47 scored rounds (NIL-60). Every loop run
  above shows the head of it (`firstScoredRound.progressText: "Round 1 / 30"`).
- **The A10 exemption is wholesale**, per the decision of record: under `automation`, `thesis_p*` registers too. It is
  dev-only and the allow-path test never creates such a row, but the profile must never be activated against a
  research database. Narrowing the skip to `browser_loop_` alone would remove the foot-gun and still serve the loop.
- The loop's 375px run flaked once ("Timed out waiting for feedback panel") and passed on retry -- the same
  Web-Audio-throttling flake this log already records for headless. 1280 and default-desktop passed first try.
- The harness reports `relevantFailedRequestCount: 355` (`ERR_ABORTED` on `<audio>` fetches, exactly half of 710
  stimulus requests, identical on both viewports) but never asserts on it -- it gates on console errors only. Audio
  did play: `mutedStimulusCount: 0` and 49 rounds cleared the audio gate.

Next single task:
NIL-62 frontend session: `ProductionLab` in the mode shell against the post-NIL-65/NIL-69 stack -- prompt card ->
romaji input -> reveal card reusing the feedback-panel layout + `StimulusPlayback`, motion-reveal choreography
(essence-review D1), only the frozen SPEC-view-designs section 8 strings, kana discipline per invariant 1.

## 2026-07-09 ("NIL-89: GameSessionResponse.totalRounds -- the client stops guessing the round count")

Session goal:
Fix the CHOOSING progress bug NIL-88's green loop exposed, and make the harness structurally able to catch its class.

Changed:
- `dto/GameSessionResponse`: new `totalRounds` (int). `mapper/GameMapper.toSessionResponse(session, totalRounds)`
  takes the count as an argument -- it is derived from the `RoundSource` seam, not from the entity, and mappers never
  reach for repositories. `service/GameService.startSession` supplies `scoredRoundsForSession(saved).size()`, so the
  number is mode-aware for free: CHOOSING reports the scored pool (47), LADDER its floor's pair count. Practice rounds
  are excluded -- they are not scored and never enter the denominator. Additive; no field removed.
- web `src/api/types.ts`: `totalRounds: number` required on `GameSessionResponse`. `src/App.tsx`: `DEMO_TOTAL_ROUNDS`
  deleted, `totalRounds={session.totalRounds}`.
- web `scripts/verify-browser-loop.mjs`: the first-scored-round check now asserts a **relationship** -- the displayed
  denominator must equal the number of scored rounds the session actually served -- instead of a literal. It fails
  closed (an unparseable progress text yields `NaN`, which never equals the count).
- Tests: `GameLoopHttpTests.sessionStartReportsTheScoredRoundTotalSoTheClientNeedNotGuessIt` asserts `totalRounds`
  equals `trialRepository.findScoredChoosingTrials().size()` and is unchanged by `includePractice`.
  `LadderHttpTests` asserts the announced `totalRounds` equals the rounds the Touch floor actually serves.
  `GameServiceTests.startSessionCreatesLadderSessionForServedFloor` now stubs a trial carrying a **real** floor pair
  code (the old fixture used `"code"`, which `LadderRoundSource` filters out, so the stub had been inert) and asserts
  the mapper receives the seam's count. `TrialPlayer.test.tsx` gains a case that counts past the old hardcoded 30.

Proof:
- `./mvnw test` -> **Tests run: 158, Failures: 0, Errors: 0** (was 157).
- web `pnpm lint` clean · `pnpm exec tsc -b` clean · `pnpm vitest run` -> **205 passed** (was 204).
- Sanctioned loop re-run against the fix (backend `local,automation`, Vite :5174, headless Chromium CDP :9224):
  exit 0 at **1280** (`scrollWidth 1265 <= 1280`) and **375** (`385 == 385`), 49 answered rounds each (47 scored + 2
  practice), ladder to `finalRungSeen`, 0 console errors. `firstScoredRound.progressText` now reads
  **"Round 1 / 47"** (was "Round 1 / 30"), and the new denominator assertion passed on both.

Result:
The player is no longer told a 47-round session ended at round 30. The wrong number is now unrepresentable: the
backend states the total, the type system requires the client to read it, and three tests assert it against the rounds
actually served rather than against a literal.

Commit:
Not committed (Nils's). Proposed commits:
- api: "NIL-89: GameSessionResponse.totalRounds so the client stops guessing the scored-round count"
- web: "NIL-89: read totalRounds from the session; assert the progress denominator against rounds served"

Blocker:
None. One note, now closed by NIL-91 below: `validateLadderStart` checked only that `findScoredTrialsByPairCodes` is
non-empty, while `LadderRoundSource.scoredRounds` additionally keys trials by the floor's pair codes.

Next single task:
NIL-62 frontend session: `ProductionLab` in the mode shell against the post-NIL-65/NIL-69 stack -- prompt card ->
romaji input -> reveal card reusing the feedback-panel layout + `StimulusPlayback`, motion-reveal choreography
(essence-review D1), only the frozen SPEC-view-designs section 8 strings, kana discipline per invariant 1.

## 2026-07-09 ("NIL-90/91/92: narrow the exemption, unify the ladder predicate, stop the harness at the last wall")

Session goal:
Apply the three follow-ups the NIL-88/89 sessions filed against themselves.

Changed:
- **NIL-90** `validation/ReservedUsernamePrefixValidator`: the exemption now lifts the guard for `browser_loop_` only.
  `thesis_p*` is rejected under every profile -- it is the one reserved prefix whose rows are READ BACK as data
  (`/api/research/thesis/divergence` includes the cohort rather than excluding it), so a stray thesis_p row is silent
  corruption, not noise. Property renamed `app.automation.allow-reserved-registration` ->
  `app.automation.allow-browser-loop-registration`, because a name that promises more than it delivers is the same
  class of trap the finding was about. `.dockerignore` now excludes `application-automation.properties`: the key has
  no source inside the image even if someone activates the profile there, so the `@Value` default wins. Prod is
  unreachable four independent ways.
- **NIL-91** new `service/LadderTrials`: the single owner of "which trials does this floor serve", keyed by the floor's
  pair codes. `LadderRoundSource.scoredRounds` and `GameService.validateLadderStart` both ask it, so validation and
  serving cannot diverge; a floor whose trials sit under unexpected codes is now a `400 Unsupported ladder floor`
  instead of a saved session that serves zero rounds. `GameService` drops its `LadderFloors` dependency;
  `LadderRoundSource` drops `TrialRepository` and `LadderFloors`.
- **NIL-92** web `scripts/verify-browser-loop.mjs`: verdict assertions are collected (`check()`, 33 sites) and reported
  together at the end; the process exit code is decided there. Independent proof stages run under `runStage()`, so a
  broken Perception Ladder walk no longer hides console errors or geometry findings. Navigation, timeout and
  prerequisite failures still throw -- nothing downstream of them means anything. A stage that drives the UI sets
  `domStateTrusted = false` when it throws, and the live-DOM checks (muted media, stale controls, overflow) are then
  skipped rather than measured against whatever view happened to be mounted; passive evidence (captured requests,
  console errors, sampled geometry) is still reported. `assertionFailureCount` / `assertionFailures` / `domStateTrusted`
  join the JSON summary.
- Tests: `RegistrationAutomationHttpTests` 1 -> 5 (browser_loop 201, BROWSER_LOOP 201 under the same fold, thesis_p99
  400, THESIS_P37 400, normal 201). `GameServiceTests.startSessionRejectsAFloorWhoseTrialsCarryUnexpectedPairCodes` is
  new. Two Mockito fixtures that stubbed a trial under pairCode `"code"` were **inert** -- `LadderRoundSource` silently
  discarded it -- and now carry a real floor pair code, which is what gives the assertions force.

Proof:
- `./mvnw test` -> **Tests run: 163, Failures: 0, Errors: 0** (was 158).
- Both new guards proven red before green, by temporarily restoring the old code:
  - wholesale skip -> `thesis_p99` and `THESIS_P37` both returned **201** where the tests demand 400;
  - old ladder predicate -> `startSessionRejectsAFloorWhoseTrialsCarryUnexpectedPairCodes` did not throw
    `BadRequestException` (it reached `gameSessionRepository.save`, which the test asserts is never called).
- Live, backend on `local,automation` (log: `The following 2 profiles are active: "local", "automation"`):
  `browser_loop_n90_*` -> **201**; `thesis_p99` -> **400** `validationErrors.username`; `THESIS_P37` -> **400**;
  normal -> **201**.
- **NIL-92 demonstrated, not asserted:** two deliberate failures injected at opposite ends of the run (a landing-card
  verdict and a session-request verdict). One run reported **both**, exited 1, and still completed all 49 rounds, the
  ladder walk, and the full JSON summary. The pre-NIL-92 harness would have aborted on the landing card, before it
  even registered.
- Sanctioned loop green after restore: exit 0 at **1280** (`scrollWidth 1265 <= 1280`) and **375** (`386 == 386`),
  49 answered rounds each, ladder to `finalRungSeen`, `assertionFailureCount: 0`, `domStateTrusted: true`,
  0 console errors, `Round 1 / 47`.
- web `pnpm lint` clean · `tsc -b` clean · `pnpm vitest run` 205 passed · `verify-presentation-logic.mjs` OK.

Result:
The exemption can no longer touch the thesis cohort, and its name no longer suggests it can. Validation and serving
ask the ladder one question instead of two that could disagree. The harness reports what a run found rather than what
it hit first.

Commit:
Not committed (Nils's). Proposed commits:
- api: "NIL-90/91: narrow the automation exemption to browser_loop_; unify the ladder served-floor predicate"
- web: "NIL-92: collect browser-loop verdict failures instead of aborting at the first"

Blocker:
None. Two notes:
- The property rename is a breaking change for any local `application-automation.properties` copy that predates this
  session; the tracked file is updated, and the old key is now inert (its absence means "guarded").
- 88 `throw`s remain in the harness. They are navigation, timeout and prerequisite failures -- an element that is not
  there, a session that never completed -- where nothing downstream is meaningful. Converting them would report
  cascades of consequences, not findings. The line drawn is: verdicts on data already in hand are collected;
  everything the rest of the run depends on still stops it.

Next single task:
NIL-62 frontend session: `ProductionLab` in the mode shell against the post-NIL-65/NIL-69 stack -- prompt card ->
romaji input -> reveal card reusing the feedback-panel layout + `StimulusPlayback`, motion-reveal choreography
(essence-review D1), only the frozen SPEC-view-designs section 8 strings, kana discipline per invariant 1.

---

Session goal:
NIL-85 -- shorten live Meaning Match sessions by sampling deterministically from the expanded pool, without breaking
the frozen shuffle derivation, leaderboard comparability, or documented contract shapes. Plus two small web riders
(score-band captions, linguistic neutrality).

Changed:
- api: new `ChoosingSample` -- the single owner of "which of a session's derived rounds are served". It filters the
  full 47-round derivation down to the first 7 rounds of each modality in shuffle order (21 = 7 auditory / 7 visual /
  7 interoceptive). `ChoosingRoundSource` composes it after `RoundShuffler`. Nothing else moved: `RoundShuffler`,
  practice serving (+1 stream), the ladder (+4 stream), every DTO, every endpoint, and the leaderboard query are
  untouched. `GameSessionResponse.totalRounds` (NIL-89) already flowed off the `RoundSource` seam, so it reports 21
  with no mapper or DTO change.
- api: `ResearchService.getPositionBias` keeps deriving the FULL 47 and looking answers up by trial id. That was
  incidental before and is load-bearing now -- comment says so, so nobody "fixes" it by routing through the sample.
- api tests: `ChoosingSampleTests` (7) pins determinism, stratification, and the load-bearing property -- the served
  list is a *subsequence of the full derivation with unchanged draws*. `IdeophoneSeedIntegrityTests` gains a
  modality-quota guard (the sample caps rather than throws, so the "a live session is really 7/7/7" guarantee lives
  against the seed). Four HTTP tests that assumed a full-pool session were reworked to ask what the session actually
  served: `ShuffledSessionHttpTests`, `PracticeRoundHttpTests`, `GameLoopHttpTests`, `RatableWordsHttpTests`.
- api: `scripts/cleanup-test-accounts.sql` now removes every harness account, not just `browser_loop_%`, matching on
  the `nanoTime`/`Date.now()` username suffix so it needs no prefix list. Keeps `arena_admin`, `thesis_p01..36`, and
  real dev logins.
- web (rider 1): the four `MINT_BAND_*` score-band captions drop the leading numeral (the `.score-figure` beside
  them already carried it) and every em-dash, including the one inside `MINT_BAND_DIFFERENT`. `scoreBandTail` ->
  `scoreBandCaption`. `SPEC-view-designs.md` §8.2 amended.
- web (rider 2): linguistic-neutrality sweep, INCLUDING the frozen instrument text, as Nils ruled. Amended
  `LISTEN_INSTRUCTION`, `RATING_INTRO_AFTER_COUNT`, `RATING_LISTEN_LINE_1` (both frozen blocks now carry an
  INSTRUMENT AMENDMENT note); neutralized chrome/mode copy in `modes.ts`, `ModeSelect`, `Instructions`, `AuthForm`,
  `Landing` (hero, body, stat card, steps, scatter `alt`), `StyleGuide`. A new `experimentText.test.ts` guard walks
  *every* export and fails if any names a language. Left alone: publication citations, the "Ideophones beyond
  Japanese" cross-linguistic card (the contrast is the point), `lang="ja"`, font imports.
- web: `Landing.tsx` said "the same 30 pairs" -- false since NIL-60 (pool is 47) and doubly so now. Fixed.
- docs: `backend-contract.md` gains a "Session sampling" section + changelog entry; `demo-runbook.md` round counts
  and cleanup section; `SPEC-view-designs.md` §8.2.

Proof:
- `./mvnw test` -> 176 tests, 0 failures (baseline 168; +7 `ChoosingSampleTests`, +1 seed guard). Verified the
  baseline by stashing.
- Live session (`local,automation`, :8081): announced `totalRounds` = 21, served 21, modality mix
  `{VISUAL: 7, INTEROCEPTIVE: 7, AUDITORY: 7}`, 21 distinct trial ids, server `answered=21 correct=13` matching a
  locally counted 13, then an explicit completion body (`roundId: null`). Ratable pool after one session: 40 (was
  86) -- 21 pairs x 2 words minus 2 words shared across pairs.
- Determinism: `ChoosingSampleTests` proves same seed -> same subset AND order, different seeds -> different trial
  membership (not merely reordering), and that each served round carries the exact draws the full derivation gave
  it. `RatableWordsHttpTests.crossConditionReplayHealsToOneRowPerWord` now pins both sessions to one seed and
  asserts the replay serves the same trials.
- Both web guards proven red before green: reinstating "Japanese" in `LISTEN_INSTRUCTION` and the old em-dash form
  of `MINT_BAND_SOME` failed exactly the two new assertions, then passed on restore.
- Web battery: `tsc --noEmit` clean, `vitest run` 256/256 across 32 files, `verify-presentation-logic.mjs` verified.
- `verify-browser-loop.mjs` green at 1280 and 375: 23 answered = 2 practice + 21 scored, displayed denominator
  `Round 1 / 21` (asserted as a relationship against rounds served, not a literal), 0 assertion failures, 0 console
  errors.

Result:
A Meaning Match session is now 21 rounds instead of 47, drawn deterministically from the seed with a guaranteed
7/7/7 modality mix. The frozen derivation contract did not have to change: because `deriveScoredRounds` shuffles the
whole pool and *then* draws presentation per trial, serving a subsequence of that derived list preserves every
round's target/side/meaning-order draw byte-for-byte. Sampling is a serving-layer filter, not a re-derivation --
which is also what lets the position-bias replay resolve every persisted answer against the full 47.

Commit:
Not committed (Nils's). Proposed commits:
- api: "NIL-85: sample Meaning Match sessions to a stratified 21 rounds from the seeded derivation"
- web: "NIL-85: neutralize player copy and drop the score numeral from the mint captions"

Blocker:
None. Four notes:
- The rating-task strings are no longer Gorilla-verbatim. Live ratings are now elicited under wording the thesis
  cohort never saw, and the Observatory plots live vs thesis mean rating per word -- two arms that now differ
  slightly in instrument (a dropped language name, not a changed task or scale). Recorded in the `experimentText.ts`
  header; any write-up comparing the arms should say so rather than imply identical elicitation. Nils ruled this
  deliberately, on the option flagged as requiring an invariant-1 amendment.
- A session that already holds >= 21 answers can never satisfy `totalAnswered == totalRounds` and so never completes.
  This is unreachable for real players (none exist) and reachable only for pre-change dev residue. `==` was left
  alone rather than loosened to `>=`: S3 moved completion off the GET on purpose, and `>=` would not rescue the
  "exactly 21 answered, client already told it is complete" case anyway. A reseed clears the residue.
- The dev leaderboard is topped by 168 completed 47-answer sessions. Every one is harness-owned (`shuffle_http_`,
  `ratable_*`, `complete_http_`, `practice_http_`), verified read-only -- zero real players, which is exactly what
  Nils's "accept mixed lengths, no code change" ruling rests on. Run `scripts/cleanup-test-accounts.sql` before any
  leaderboard demo. I did not run it (it deletes rows; Nils's call).
- The em-dash ban was applied only to the score-band captions, as the rider scoped it. 78 other player-facing
  strings still contain em-dashes (`ladderText`, `researchFlavor`, `modes`, plus Observatory's "-" null-marker,
  which is a legitimate typographic use, not prose). A genuinely project-wide sweep is its own issue.

Next single task:
Decide whether the project-wide em-dash ban is real; if so, sweep the remaining 78 player-facing strings (exempting
the Observatory null-marker) and record the rule in `UI-SYSTEM.md`, which does not currently state it.
