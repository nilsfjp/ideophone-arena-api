# SPEC — Rating Lab backend (minimal `ratings` vertical slice)

Repo: `ideophone-arena-api`. Place at repo root or `docs/`.

## Goal

A single `ratings` table plus a vertical slice (entity / repository / service / DTO / mapper /
Bean Validation / tests) capturing a 1-7 iconicity rating per word per user, keyed so the
guess-vs-rating divergence can be computed LATER. Mirrors the thesis Rating Task. This is the
minimal standalone table from roadmap step 4 — it does NOT start the Phase-2
GameMode/PresentationMode/RoundTemplate/StimulusAsset/RatingAttempt refactor.

## APPROVAL GATE — schema (confirm before launch)

The table below is PROPOSED and matches `player_answers` conventions. Confirm or edit the columns
before kicking off. Schema changes go through `scripts/generate_seed_sql.py` (+ `--check`), NEVER a
hand-edit to `ideophone_arena.sql`.

```sql
CREATE TABLE ratings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ideophone_id BIGINT NOT NULL,
    session_id BIGINT NULL,
    rating SMALLINT NOT NULL,            -- 1..7
    response_time_ms INT,
    rated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (user_id, ideophone_id),      -- one rating per word per user; re-rate -> 409
    CONSTRAINT fk_ratings_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_ratings_ideophone FOREIGN KEY (ideophone_id) REFERENCES ideophones (id),
    CONSTRAINT fk_ratings_session FOREIGN KEY (session_id) REFERENCES game_sessions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

Decision flagged: user-keyed (above, `UNIQUE(user_id, ideophone_id)`) vs session-keyed
(`UNIQUE(session_id, ideophone_id)`). User-keyed enables joining a user's rating of word W against
their guess accuracy on W via `player_answers.target_ideophone_id`. RECOMMENDED: user-keyed with a
nullable `session_id` for provenance.

Add the table by extending `scripts/generate_seed_sql.py` and regenerating; `--check` must report
up to date (this is also the Stop-hook gate, proving the schema was generated, not hand-written).

## Endpoints (house style)

Thin controller (`ResponseEntity`, DTOs only, `@Valid`, no logic, never touches repositories);
`@Transactional` service that never returns entities; mapping in a mapper; custom exceptions +
`GlobalExceptionHandler`; `DataIntegrityViolationException` -> 409.

- `POST /api/ratings` (ROLE_USER). Body `{ ideophoneId, rating, responseTimeMs?, sessionUuid? }`.
  - Validation: `ideophoneId` `@NotNull`; `rating` `@NotNull @Min(1) @Max(7)`; `responseTimeMs`
    optional, if present `@Min(0) @Max(600000)`; `sessionUuid` optional (resolved to
    `game_sessions.id`; the public identifier is the UUID, never the internal id).
  - 201 Created -> `{ id, ideophoneId, rating, responseTimeMs, ratedAt }` (no entity).
  - Duplicate (same user + ideophone) -> 409 (mirror the answer-race pattern: `saveAndFlush` +
    `DataIntegrityViolationException` translation).
  - Unknown `ideophoneId` -> match the existing not-found/invalid-reference mapping in
    `GlobalExceptionHandler` (404 via a custom NotFound exception, or 400 — be consistent with the
    rest of the API).
- `GET /api/game/me/ratings` (ROLE_USER) -> the caller's own ratings (mirrors
  `GET /api/game/me/attempts`). Provides the test round-trip and a future divergence-UI source.

## Out of scope (flag as scope creep, do not do)

The Phase-2 model (GameMode / PresentationMode / RoundTemplate / StimulusAsset / RatingAttempt);
any rating UI; computing the divergence statistic itself (store the data with the right keys only);
changing the `GET /api/admin/stats` DTO (it is covered by `AdminStatsHttpTests` — leave it); any
change to the guessing flow, conditions, difficulty, or the experiment invariants.

## Verification (the oracle — run each and paste real output)

1. `python3 scripts/generate_seed_sql.py --check` -> up to date (proves schema went through the generator).
2. `./mvnw test` -> green, including a new `RatingHttpTests`:
   register/login -> POST valid rating -> 201 -> `GET /api/game/me/ratings` contains it ->
   POST duplicate (same word) -> 409 -> POST `rating: 8` -> 400 with a `validationErrors` map ->
   POST `rating: 0` -> 400 -> unauthenticated POST -> 401.
3. `./mvnw spring-boot:run` with `ddl-auto=validate` -> clean start (schema matches entities).
4. curl flow against a running instance for the runbook.

## Docs to update

`docs/backend-contract.md` (new "Ratings" section + changelog), `docs/demo-runbook.md` (rating curl
flow), `docs/backend-grading-checklist.md` (Bean Validation / new-endpoint evidence), append
`docs/progress-log.md`, tick the punch list in `CLAUDE.md`.

## Handoff

Clean working tree, NO commit, proposed commit message, next single task.
