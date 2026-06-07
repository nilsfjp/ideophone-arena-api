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
