# Ideophone Arena demo contract

Date: 2026-06-30
Deadline: 2026-06-05 (passed)

## Purpose

This document records the backend/frontend contract for the final demo. It exists to prevent last-minute scope drift.

## Backend base URL

Local backend:

```text
http://localhost:8081
```

## Supported session-start settings

`conditionName` and `difficultyLevel` are required in `POST /api/game/sessions`. `includePractice` is optional
(default `false`); when `true`, the session serves 2 practice rounds before round 1 (see "Practice rounds").

The stable demo path uses:

```json
{
  "conditionName": "CONDITION_1_SOKUON",
  "difficultyLevel": 1
}
```

The externally supported condition values are:

```text
CONDITION_1_SOKUON
CONDITION_2_SOKUON
CONDITION_3_SOKUON
```

Do not expose arbitrary difficulty selection. Difficulty values above `1` are not seeded and must not be selectable in the frontend.
The backend rejects missing or unsupported `difficultyLevel` values with `400 Bad Request`; only `1` is supported.
The backend rejects missing or unsupported `conditionName` values with `400 Bad Request`. `TEXT_ONLY` is an internal enum
value used by tests and legacy data paths, not an externally supported session-start condition.

## Script Lab frontend assumptions

The frontend may safely build a first Script Lab control around these request values:

```text
CONDITION_1_SOKUON
CONDITION_2_SOKUON
CONDITION_3_SOKUON
```

The user-facing labels should stay frontend-owned. Recommended labels are `Audio-only`, `Script match`, and
`Script mismatch`. The backend contract is the enum string, not the label text.

For Script Lab, keep sending:

```json
{
  "conditionName": "CONDITION_1_SOKUON",
  "difficultyLevel": 1
}
```

with `conditionName` swapped to one of the three supported values. Do not send `TEXT_ONLY`. Do not expose difficulty
selection; `difficultyLevel` remains locked to `1`.

Round responses for all three supported conditions expose the rendering fields the frontend currently needs:

```text
completed
message
sessionUuid
roundId
targetTranslation
prompt
conditionName
difficultyLevel
practice
translations.target
translations.other
left.ideophoneId
left.kana
left.displayForm
left.canonicalForm
left.romaji
left.stimulusFile
left.stimulusUrl
left.modality
left.canonicalScript
right.ideophoneId
right.kana
right.displayForm
right.canonicalForm
right.romaji
right.stimulusFile
right.stimulusUrl
right.modality
right.canonicalScript
timing.fixationMs
timing.preChoiceDelayMs
```

Script display is data, not code (2026-06-10):

- `displayForm` is the exact kana string the player sees before answering. The frontend must render it verbatim and
  must not derive the display script from `canonicalScript` or any other field.
- `canonicalForm` is the word in its canonical script, intended for the feedback reveal.
- `stimulusUrl` now points at one shared per-word audio file, `/stimuli/audio/<modality><pairing><h|k>-<romaji>.m4a`
  (for example `/stimuli/audio/a0h-gosogoso.m4a`). All three condition rows of a word reference the same audio file;
  the script manipulation never alters the audio channel. The per-condition mp4s are legacy assets and are no longer
  referenced by the seed.
- `kana` keeps the dictionary lemma spelling. For the two long-vowel words (`zyaazyaa`, `kyaakyaa`) the stimuli render
  the chouonpu forms, so `displayForm`/`canonicalForm` use the long-vowel mark while `kana` does not. Render
  `displayForm`, not `kana`.
- `canonicalScript` remains the raw two-letter filename code (pos3+pos4, e.g. `HK`) for this session; the frontend
  migrates off it next session.

No new backend `GameMode` or `PresentationMode` field exists yet.

## Authentication

The frontend logs in through:

```text
POST /api/auth/login
```

The response contains a JWT token. Protected requests must send:

```text
Authorization: Bearer <token>
```

## Game flow

Start session:

```text
POST /api/game/sessions
```

Request body (`includePractice` optional, default `false`):

```json
{
  "conditionName": "CONDITION_1_SOKUON",
  "difficultyLevel": 1,
  "includePractice": false
}
```

The session response echoes `includePractice`.

Get next round:

```text
GET /api/game/sessions/{sessionUuid}/rounds/next
```

Submit answer:

```text
POST /api/game/sessions/{sessionUuid}/answers
```

Request body:

```json
{
  "roundId": 1,
  "selectedIdeophoneId": 1,
  "responseTimeMs": 1200
}
```

All three fields are required. `responseTimeMs` must be between `0` and `600000` (10 minutes); out-of-range or
missing values return `400` with a `validationErrors` map. Submitting an answer for a round already answered in
the session returns `409 Conflict`, including under concurrent duplicate submissions.

The answer response includes a `practice` boolean mirroring the round's flag (always `false` for scored rounds).

## Practice rounds (2026-06-11)

When a session is started with `"includePractice": true`, the next-round endpoint serves **2 practice rounds**
(thesis Appendix B pairs p0 auditory and p1 visual, seeded with p-prefix stimuli) before the first scored round.
Practice rounds use the same round DTO with `practice: true`; scored rounds carry `practice: false`.

Practice answers:

- return normal correctness feedback (`practice: true` in the answer response) — a deliberate, documented divergence
  from the thesis, which hid practice feedback;
- are **never persisted**: no `PlayerAnswer` row is created, `totalAnswered`/`totalCorrect` stay at the session's
  scored counts (0 during practice), and practice cannot affect completion or the leaderboard;
- must be submitted in serving order: answering the second practice round first returns `400`, re-answering an
  already-passed practice round returns `409`, and practice answers against a session started without the flag
  return `400`.

Practice rounds do not consume round numbers: "Round 1/30" still means the first scored round. Sessions started
without the flag behave exactly as before; existing clients are unaffected.

## Deterministic per-session shuffle (2026-06-12)

Every session stores a server-generated `shuffle_seed` (`SecureRandom`, never exposed in player-facing DTOs).
The seed deterministically derives the session's entire presentation, recomputed from scratch on every request —
nothing but the seed is persisted, so a session replays identically across server restarts:

1. the order of the 30 scored rounds,
2. which word of each pair is the **target** (identity randomization — a deliberate extension beyond the thesis,
   which fixed targets by pairing parity; decision recorded 2026-06-12),
3. the target's left/right position,
4. the order of the two meaning lines in the prompt (exposed on the round DTO as `targetMeaningListedFirst`
   since 2026-07-03, see below).

### Derivation spec (compatibility contract — do not change once sessions exist)

1. Base list: the session's scored rounds for its condition and difficulty, ordered by round id ascending.
2. `Random r = new Random(shuffleSeed)`; `Collections.shuffle(baseList, r)`. Both are algorithmically specified
   in the JDK, hence portable and stable across versions and restarts.
3. Iterating the _shuffled_ list in order, three draws per round from the same stream, in this order:
   `targetIsPairSecond = r.nextBoolean()`, `targetOnLeft = r.nextBoolean()`,
   `targetMeaningListedFirst = r.nextBoolean()`. **"Pair second" is the round member with the higher ideophone
   id** (defined on the pair's ideophone ids, never on the stored left/right columns).
4. Practice rounds keep their fixed order (p0 then p1), but take the same three per-round draws from a separate
   stream, `new Random(shuffleSeed + 1)`, so the scored derivation is unaffected by practice on/off.

### Consequences

- **The round DTO shape only ever grew additively** (the `targetMeaningListedFirst` boolean, 2026-07-03); the
  values vary by seed. `targetTranslation`/`prompt`/`translations.target` are the derived target's gloss,
  `translations.other` the derived distractor's gloss, and `left`/`right` are the derived sides.
- **Correctness is judged against the derived target**, never against `arena_rounds.correct_ideophone_id`. That
  column (and `arena_rounds.prompt`, a copy of the same word's gloss) remains in the schema purely as
  documentation of the fixed thesis target and is no longer read in the serving path.
- `player_answers.target_ideophone_id` stores the derived target at answer time, so analytics can aggregate
  per actually-served target — including the 30 complementary targets the thesis never measured. Recent-attempts
  history replays the stored target, not the thesis target.
- `targetMeaningListedFirst` is **exposed** (2026-07-03, formerly reserved): the round DTO carries the drawn
  boolean and the Vite frontend orders its two meaning lines by it. `translations.target`/`translations.other`
  stay semantic fields — only the display order varies. The draw itself and the stream consumption above are
  unchanged and final; the field is additive, so older clients that ignore it keep the old target-first display.

## Completion behavior

When there are no more unanswered rounds, the next-round endpoint returns `200 OK` with an explicit completion DTO.
The frontend must treat `completed: true` as normal session completion, not as an error or automatic reset.

The session is marked complete server-side when the final answer is submitted (`POST .../answers`); the next-round
`GET` is read-only and never mutates the session (changed 2026-06-10, response shape unchanged).

Response body:

```json
{
  "completed": true,
  "message": "Game session is complete",
  "sessionUuid": "8e3c93f3-9ea4-4257-abd4-a9fded012ea6",
  "conditionName": "CONDITION_1_SOKUON",
  "difficultyLevel": 1,
  "roundId": null,
  "targetTranslation": null,
  "prompt": null,
  "translations": null,
  "left": null,
  "right": null,
  "timing": null
}
```

Normal round responses from the same endpoint include `completed: false` and the usual round fields. This makes
completion easy to distinguish from a real `404 Not Found`, such as an invalid session UUID.

## Progress display

The `totalAnswered` and `totalCorrect` fields in the answer response are scoped to the current session
(changed 2026-06-10; they were previously cumulative user-wide totals across all sessions). They can be used
directly for per-session progress. The frontend may still keep its own session-local counts; the change is
non-breaking for clients that do.

## Leaderboard

Public leaderboard, paginated (changed 2026-06-11; previously returned a bare array):

```text
GET /api/leaderboard?page=0&size=10
```

Query params: `page` (default `0`, clamped to `>= 0`) and `size` (default `10`, clamped to `1..50`). Out-of-range
values are clamped, not rejected; the response metadata reports the effective values.

The metric is **best completed session** (changed 2026-06-11; previously lifetime account totals): each user is
ranked by the highest number of correct answers achieved within a single _completed_ session. Incomplete sessions
never count. Ordering is deterministic: `bestSessionCorrect` desc, then best-session accuracy desc (equivalently
`bestSessionAnswered` asc), then `username` asc.

Response shape:

```json
{
  "entries": [
    {
      "username": "demo",
      "bestSessionCorrect": 21,
      "bestSessionAnswered": 30,
      "bestSessionAccuracy": 0.7
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 4,
  "totalPages": 1
}
```

**Breaking change for the frontend (2026-06-11):** entry fields renamed/re-scoped from
`totalAnswered`/`totalCorrect`/`accuracy` (lifetime) to `bestSessionCorrect`/`bestSessionAnswered`/
`bestSessionAccuracy` (best completed session). The Vite app's leaderboard rendering needs a follow-up rider.

This should be visible in the final demo.

## Admin stats

Aggregate research statistics, restricted to `ROLE_ADMIN` (`401` unauthenticated, `403` for `ROLE_USER`):

```text
GET /api/admin/stats
```

Registration always assigns `ROLE_USER`; the seed creates the dev admin `arena_admin` (see
`docs/demo-runbook.md`, "Creating an admin").

Response shape:

```json
{
  "totals": {
    "users": 13,
    "sessions": 10,
    "completedSessions": 1,
    "answers": 3
  },
  "byCondition": [
    {
      "conditionName": "CONDITION_1_SOKUON",
      "sessions": 5,
      "answers": 2,
      "correct": 2,
      "accuracy": 1.0
    }
  ],
  "byModality": [
    { "modality": "AUDITORY", "answers": 3, "correct": 3, "accuracy": 1.0 }
  ]
}
```

`byCondition` is grouped by the session's condition; `byModality` joins answers through their round's ideophones.
Conditions with no sessions and no answers are omitted. `accuracy` is `correct / answers` (`0.0` when there are no
answers).

## API docs

springdoc OpenAPI, public by design for the course demo:

```text
GET /v3/api-docs
GET /swagger-ui/index.html
```

## Recent attempts

Authenticated user history:

```text
GET /api/game/me/attempts
```

This is enough for minimal personal progress/history.

## Ratings (2026-06-20)

A standalone `ratings` table captures a 1-7 iconicity rating per word per user (mirrors the thesis Rating Task).
It is keyed `UNIQUE(user_id, ideophone_id)` — one rating per word per user — with a nullable `session_id` for
provenance, so a user's rating of word W can later be joined against their guess accuracy on W
(`player_answers.target_ideophone_id`). This is the minimal standalone table only; it does **not** start the Phase-2
`GameMode`/`PresentationMode`/`RoundTemplate`/`StimulusAsset`/`RatingAttempt` model. The divergence statistic itself is
not computed here — only the data, with the right keys, is stored.

Submit a rating (authenticated):

```text
POST /api/ratings
```

Request body (`responseTimeMs` and `sessionUuid` optional):

```json
{
  "ideophoneId": 1,
  "rating": 6,
  "responseTimeMs": 1500,
  "sessionUuid": "8e3c93f3-9ea4-4257-abd4-a9fded012ea6"
}
```

- `ideophoneId` is required; `rating` is required and must be between `1` and `7`; `responseTimeMs`, if present, must
  be between `0` and `600000`. Out-of-range or missing required values return `400` with a `validationErrors` map.
- `sessionUuid` is optional; the public identifier is the UUID, never the internal id. If present it is resolved to
  `game_sessions.id`; an unknown UUID returns `404`, and a session owned by another user returns `403`.
- An unknown `ideophoneId` returns `404`.
- `201 Created` returns `{ id, ideophoneId, rating, responseTimeMs, ratedAt }` (no entity, no `user_id`/`session_id`).
- Rating the same word twice as the same user returns `409 Conflict` (mirrors the answer-race pattern: `saveAndFlush`
  + `DataIntegrityViolationException` translation), including under concurrent duplicate submissions.

Read the caller's own ratings, paginated (authenticated; mirrors the public leaderboard wrapper):

```text
GET /api/game/me/ratings?page=0&size=10
```

Query params: `page` (default `0`, clamped to `>= 0`) and `size` (default `10`, clamped to `1..50`). Out-of-range
values are clamped, not rejected; the response metadata reports the effective values.

Response shape (entries are `{ id, ideophoneId, rating, responseTimeMs, ratedAt }`, most recent first):

```json
{
  "entries": [
    { "id": 15, "ideophoneId": 1, "rating": 6, "responseTimeMs": 1500, "ratedAt": "2026-06-30T19:40:16Z" }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

**Breaking change for the frontend (2026-06-30):** the response is now a wrapper object instead of a bare array,
mirroring the leaderboard change. The Vite app must read `.entries`.

## Guess-vs-rating divergence (2026-06-30)

A public, read-only population aggregate for the (upcoming) landing page: per ideophone, how guessable its meaning is
versus how iconic players rate it.

```text
GET /api/research/divergence
```

Public (no authentication), like `GET /api/leaderboard`. Returns a JSON array with one row per ideophone that has at
least one guess **or** one rating (words with neither are omitted), ordered by `ideophoneId` (illustrative values):

```json
[
  {
    "ideophoneId": 9,
    "romaji": "dokidoki",
    "displayForm": "どきどき",
    "gloss": "...",
    "modality": "INTEROCEPTIVE",
    "guessAccuracy": 0.62,
    "guessCount": 120,
    "meanRating": 6.1,
    "ratingCount": 18
  }
]
```

- `guessAccuracy` is the fraction of guesses correct for that word, from `player_answers.target_ideophone_id` (the
  round's derived target — the word the player had to identify). It is **`null`** when `guessCount` is `0` (no guesses
  yet), not `0.0`, so "no data" stays distinct from "always wrong". Likewise `meanRating` (mean of the 1-7 `ratings`) is
  **`null`** when `ratingCount` is `0`. Clients gate on `guessCount`/`ratingCount`.
- No schema change: both aggregates read existing tables. They are two separate `GROUP BY` queries merged per word in
  the service (a single join across `player_answers` and `ratings` would form a cartesian product and inflate guess
  accuracy).
- The divergence is not reduced to one number server-side; each row pairs the two measures and the client contrasts them.
- `displayForm` (added 2026-07-06) is the word's kana label, **verbatim** from `ideophones.display_form` (invariant 1:
  rendered as stored, never derived/converted); the Observatory renders kana labels. Additive — clients that ignore it
  are unaffected.
- Rider A (research aggregates only) excludes practice trials, `browser_loop_%` automation accounts, and — since
  NIL-54 (2026-07-06) — the reserved `thesis_p%` ingestion cohort, so this live layer shows only live-player data and
  stays byte-stable across the thesis ingestion. The thesis cohort's own aggregate is served separately (below).

## Thesis divergence — Observatory thesis layer (2026-07-06, NIL-54)

The thesis's own experiment (36 participants) ingested into the live tables and exposed as the inverted-Rider-A
counterpart of `/api/research/divergence`: the same shape, computed over the reserved `thesis_p%` cohort **only**.

```text
GET /api/research/thesis/divergence
```

Public (no authentication). Identical `DivergenceResponse[]` shape as `/api/research/divergence`; one row per thesis
target word (30), ordered by `ideophoneId`. Because each target word was answered and rated by all 36 participants,
every row carries `guessCount == 36` and `ratingCount == 36`, each `guessAccuracy` equals that pairing's
`pairings.thesis_accuracy`, and the guess-count-weighted per-modality rollup reproduces the vendored thesis figures
(AUDITORY 68.6%, VISUAL 64.2%, INTEROCEPTIVE 59.7%; overall 693/1080).

- No schema change and no new DTO: reuses `DivergenceResponse` and the divergence merge, with the Rider A predicate
  inverted (`username like 'thesis!_p%' escape '!'`) in two dedicated repository methods.
- The rows are generator-emitted seed (`generate_seed_sql.py`, verified by `--check`): 36 `app_users`
  (`thesis_p01`..`thesis_p36`), 36 `game_sessions` with **`completed_at = NULL`** (so they feed divergence but never
  the leaderboard), 1080 `player_answers`, 1080 `ratings`. No flag/`data_source` column — provenance is the username
  prefix, fenced from the live aggregates exactly like `browser_loop_%`.

## Rating distributions per modality (2026-07-06)

A public, read-only population aggregate for the Observatory raincloud panels: per modality, how the 1-7 iconicity
ratings are distributed — **per-value counts, not means**.

```text
GET /api/research/rating-distributions
```

Public (no authentication), like `GET /api/research/divergence`. Returns a single object (illustrative values):

```json
{
  "distributions": [
    { "modality": "AUDITORY", "ratingValue": 1, "count": 0 },
    { "modality": "AUDITORY", "ratingValue": 2, "count": 3 },
    { "modality": "AUDITORY", "ratingValue": 3, "count": 5 },
    { "modality": "VISUAL", "ratingValue": 1, "count": 1 }
  ],
  "byModalityN": { "AUDITORY": 42, "VISUAL": 51, "INTEROCEPTIVE": 33 }
}
```

- `distributions` is a **dense** grid: every modality that has at least one rating carries all seven cells
  (`ratingValue` 1..7), zero-filled where a value was never given, ordered by `Modality` enum ordinal then
  `ratingValue`. Modalities with **no** ratings are omitted entirely (no cells, no `byModalityN` key), so the
  frontend never guesses which cells exist.
- `byModalityN[modality]` is that modality's total rating count; its seven per-value cells sum to it. Use it for the
  specimen `n` label so thin tiers stay honest.
- Words with a `null` modality are excluded — they cannot belong to a modality panel.
- No schema change: one JPQL `GROUP BY ideophone.modality, rating.rating` over `ratings` joined to `ideophones`.
  NIL-68 (M2 re-key) later re-points the aggregate to word-grain **without changing this shape**.

## Position bias (2026-07-06)

A public, read-only SDT-flavored fairness check on the Choosing task's forced choice, for the Observatory's
integrity strip. Position is **derived** by replaying the deterministic per-session shuffle (see "Deterministic
per-session shuffle" above) against the stored answers — **no schema change**, no persisted `selected_position`
column.

```text
GET /api/research/position-bias
```

Public (no authentication). Returns a single object (illustrative values):

```json
{
  "n": 612,
  "leftPickCount": 312,
  "rightPickCount": 300,
  "leftPickRate": 0.510,
  "dPrime": 1.42,
  "criterion": -0.03,
  "targetTopN": 305,
  "targetTopCorrect": 192,
  "targetTopAccuracy": 0.630,
  "targetBottomN": 307,
  "targetBottomCorrect": 187,
  "targetBottomAccuracy": 0.609
}
```

Two dissociated axes of the shuffle are checked (they are independent seed draws — see the derivation section):

- **Word-card left/right** (salience fairness): `leftPickCount`/`rightPickCount` sum to `n`; `leftPickRate` is the
  headline vs 0.50. `dPrime`/`criterion` are the signal-detection view of the **same left/right axis** — signal =
  target on the left, response = pick left, so `dPrime` is sensitivity and `criterion` is the left/right response
  bias (0 = unbiased). Computed with the log-linear correction (Hautus 1995: +0.5 per response cell, +1 per stimulus
  total) so the z-scores never diverge.
- **Target-meaning top/bottom** (reading-order fairness): the two candidate meanings are always vertically stacked,
  and which is on top is randomized independently of the card side. `targetTopAccuracy`/`targetBottomAccuracy` are
  accuracy conditioned on where the **target** meaning sat; the choice is fair iff the two are comparable.
  `targetTop{N,Correct}` and `targetBottom{N,Correct}` sum to `n` and back the accuracies.
- Rates and accuracies are **`null`** (not `0.0`) when their denominator is `0`; `dPrime`/`criterion` are `null`
  when a stimulus class is empty. Only scored answers count (practice is never persisted; the query also filters
  `is_practice = false` defensively).
- No schema change: the aggregate is `RoundShuffler` replayed per session in the service and tallied against
  `player_answers`. NIL-68 later re-points it **without changing this shape**.

## Ratable words (2026-07-03)

The Rating Lab's word pool, served by the backend so the pool follows the account instead of the browser. The
thesis contamination rule is enforced server-side: rating shows a word's meaning, so a word becomes ratable only
once an answered (scored) Choosing round has revealed its mapping at feedback. Both members of each answered round
qualify — feedback reveals the full pair mapping — and words the caller has already rated drop out.

```text
GET /api/game/me/ratable-words?page=0&size=10
```

Authenticated. Query params mirror `GET /api/game/me/ratings`: `page` (default `0`, clamped to `>= 0`) and `size`
(default `10`, clamped to `1..50`). Out-of-range values are clamped, not rejected; the response metadata reports
the effective values.

Response shape (`meaning` is the word's own gloss — exactly the mapping the feedback showed):

```json
{
  "entries": [
    {
      "ideophoneId": 9,
      "canonicalForm": "...",
      "romaji": "dokidoki",
      "stimulusFile": "audio/a09-dokidoki.m4a",
      "modality": "INTEROCEPTIVE",
      "meaning": "..."
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

- Entries are ordered by first encounter (earliest `player_answers.answered_at`, ideophone-id tiebreak) and each
  word appears once no matter how many rounds served it, so the pool is identical — content and order — on every
  device the account uses.
- Practice words never appear: practice answers are never persisted, and the query filters
  `arena_rounds.is_practice` defensively on top.
- No schema change: one JPQL `GROUP BY` over `player_answers`/`arena_rounds`/`ideophones` with a `NOT EXISTS`
  subquery against `ratings`.
- This retires the Vite app's `ideophone-arena-rating-pool` localStorage pool (27D), which by construction broke
  for any multi-device hosted user (the W30 deploy blocker). The frontend now sources the pool from this endpoint;
  the localStorage pool is discarded without migration (only pre-deploy test data existed).

## Changelog

- 2026-07-08: **Stimulus-expansion dark inventory (NIL-86)** — 21 signed-off expansion pairs seeded via
  `generate_seed_sql.py` into `words`/`presentations`/`pairings` (68->102 words, 204->306 presentations, 34->55
  pairings) with **no trials**, so they serve in zero live pools (round generation, ratable pool, leaderboard, admin
  stats) until their TTS audio lands — **trials stay 34; every public DTO/endpoint shape is unchanged (zero API/frontend
  changes).** The A6 `pairings` columns are now filled on the 21 expansion rows: `signoff_ref`
  (`stimulus-expansion-signoff.xlsx#<pair>`), `approved_at` (2026-07-02), `foil_distance` (a validation-only covariate,
  never a gameplay/ordering input). `source='EXPANSION'` distinguishes them from `THESIS`; new pair_codes use the
  `exp-<id>` namespace. `HAPTIC` added to the `Modality` enum (additive; the 4 clean Haptic pairs). Provenance:
  every jpn stimulus is one `ja-JP-Wavenet-B` TTS voice — the generator asserts pair-provenance homogeneity, and
  per-stimulus provenance lives in `scripts/tts-manifest.json` until `stimulus_sources` (M5). Audio itself is generated
  out-of-band (`scripts/generate_tts_audio.py`, run by Nils).
- 2026-07-06: **Thesis tidy-data ingestion (NIL-54)** — the thesis Gorilla export (36 participants: 2AFC choosing +
  7-point rating) ingested as generator-emitted seed rows (`generate_seed_sql.py`, `--check` clean): 36 `thesis_p##`
  users, 36 `game_sessions` (`completed_at = NULL`), 1080 `player_answers`, 1080 `ratings`. **No schema change, no
  flag/`data_source` column** — provenance is the reserved `thesis_p%` username prefix. **Rider A extended**: the live
  `divergence` / `rating-distributions` / `position-bias` aggregates now also exclude `thesis_p%` (no shape change;
  live output byte-stable across the ingestion — this **reverses** the earlier ADR-5 "in-band" lean, recorded in
  ARCHITECTURE.md §12). New public read-only endpoint **`GET /api/research/thesis/divergence`** (additive, reuses
  `DivergenceResponse`) is the Observatory thesis layer: the inverted-predicate view whose per-modality rollup
  reproduces the vendored 68.6/64.2/59.7. **Side effect** — `GET /api/admin/stats` (which intentionally counts all
  cohorts) gains +36 users / +36 sessions / +1080 answers, and `byCondition` the thesis cohort (11/13/12 across
  `CONDITION_{1,2,3}_SOKUON`); `completedSessions` stays live-only. `ddl-auto=validate` unchanged. `./mvnw test` -> 97
  tests, 0 failures.

- 2026-07-06: **M2 "The Re-key" (NIL-68)** — internal schema normalized to word grain with **every public response
  shape frozen** (verified by byte-level curl-diff). `ideophones` split into `words` + `presentations`;
  `arena_rounds` renamed/collapsed into `trials` (condition-free); `player_answers`/`ratings` re-keyed to word grain
  with **`UNIQUE(user_id, word_id)`** (closes the cross-condition double-rating: rating a word again under a
  different condition now returns `409`). Public id **semantics** become word ids while the frozen field **names**
  stay (`ideophoneId`, `selectedIdeophoneId`, `correctIdeophoneId`, divergence/ratable `ideophoneId`). Round-card
  `displayForm`/`canonicalScript` now resolve from `presentation(word, session.condition)`; everything else is a
  word-level fact. `GET /api/research/divergence` heals to **one row per word** (the pre-M2 row-split across
  condition rows was the accident) and its `displayForm` is `words.canonical_form`; `rating-distributions` and
  `position-bias` re-point to word grain unchanged. Rider A (research aggregates only): divergence,
  rating-distributions, and position-bias now exclude practice trials (defensive) and `browser_loop_%` automation
  accounts — no shape change. The deterministic per-session shuffle is **byte-for-byte unchanged** (id vocabulary
  re-binds to word ids: "pair second = higher word id" selects the same k-word member as before). `ddl-auto=validate`
  unchanged; all seed via `generate_seed_sql.py --check`. Tests: word-grain seed-replay re-point + grain-heal
  regression; `./mvnw test` -> 90 tests, 0 failures.

- 2026-07-06: Observatory research endpoints (NIL-79). Two new public read-only `ResearchController` siblings:
  `GET /api/research/rating-distributions` (per-modality 1-7 rating-value counts — dense grid + `byModalityN`) and
  `GET /api/research/position-bias` (SDT fairness check reconstructed from the per-session shuffle: left/right pick
  rate + d'/criterion, plus target-meaning top/bottom accuracy). `DivergenceResponse` gained an additive
  `displayForm` (kana, verbatim). No schema change; `ddl-auto=validate` unchanged. `position-bias` carries
  `targetTopCorrect`/`targetBottomCorrect` beyond the originally-drafted shape (additive, so the accuracies are
  reconstructable). Stale `DerivedRound` comment corrected (the meaning-order flag has been frontend-honored since
  2026-07-03). Tests: `RatingDistributionsHttpTests` (2), `PositionBiasHttpTests` (2), `PositionBiasCalculatorTests`
  (4). `./mvnw test` -> 88 tests, 0 failures.

- 2026-07-03: meaning-order draw exposed + server-side ratable pool (NIL-40). The round DTO gained the additive
  boolean `targetMeaningListedFirst` (the third per-round draw, until now reserved); the Vite frontend orders its
  two meaning lines by it — no shuffler or stream change, older clients unaffected. New authenticated
  `GET /api/game/me/ratable-words` returns the caller's encountered-but-unrated words in the `{entries, ...}`
  wrapper (page/size clamped like `/me/ratings`), enforcing the contamination rule server-side and replacing the
  frontend's localStorage pool. No schema change. Tests: flag assertions in `RoundResponseSerializationTests` and
  `ShuffledSessionHttpTests` (served flag matches the seed derivation) and new `RatableWordsHttpTests` (4).
  `./mvnw test` -> 80 tests, 0 failures.

- 2026-06-30: `GET /api/game/me/ratings` is now **paginated** — a wrapper object (`entries` + `page`/`size`/
  `totalElements`/`totalPages`; `page`/`size` query params, size clamped to `1..50`) instead of a bare array.
  **Breaking for the Vite frontend** until it reads `.entries` (mirrors the leaderboard change). `RatingController`
  gained Swagger `@Tag`/`@Operation`. New public read-only `GET /api/research/divergence` returns per-ideophone guess
  accuracy (`player_answers.target_ideophone_id`) vs mean rating (`ratings`), one row per word that has any data,
  `null` for the zero-count side; no schema change (two merged `GROUP BY` projections, no cartesian join). Tests:
  `RatingHttpTests` (+1 pagination/size-clamp test; GET assertions moved to `.entries`) and new `DivergenceHttpTests`
  (3). `./mvnw test` -> 76 tests, 0 failures; `python3 scripts/generate_seed_sql.py --check` clean.

- 2026-06-20: new `ratings` table and vertical slice — `POST /api/ratings` (authenticated) stores a 1-7 rating per
  word per user (`UNIQUE(user_id, ideophone_id)`, nullable `session_id`), returns `201` with the rating DTO, `409` on
  duplicate, `400` on out-of-range `rating`/`responseTimeMs`, `404` on unknown ideophone, `403`/`404` on an
  unowned/unknown `sessionUuid`. `GET /api/game/me/ratings` returns the caller's own ratings. Schema went through
  `scripts/generate_seed_sql.py` (`--check` clean). Standalone table only — not the Phase-2 model. Non-breaking;
  additive.

- 2026-06-12 (Session B): deterministic per-session shuffle — `game_sessions.shuffle_seed` (server-generated,
  never exposed) derives round order, target identity, target side, and meaning order per the spec above;
  correctness is judged against the derived target; `player_answers.target_ideophone_id` stores it. DTO shapes
  unchanged, zero frontend changes; sessions replay identically across restarts.
- 2026-06-11 (Session A): practice rounds — `POST /api/game/sessions` accepts optional `includePractice`
  (default `false`); practice rounds are served before round 1 with `practice: true` in the round DTO; practice
  answers return feedback but are never persisted and cannot affect score, completion, or the leaderboard. The
  answer response gained a `practice` boolean. Non-breaking for existing clients.
- 2026-06-11 (Session A): leaderboard metric reworked to **best completed-session score** — entry fields are now
  `bestSessionCorrect`/`bestSessionAnswered`/`bestSessionAccuracy` (was lifetime `totalCorrect`/`totalAnswered`/
  `accuracy`); only completed sessions count. **Breaking for the Vite frontend** (needs a follow-up rider). The
  pagination wrapper is unchanged.
- 2026-06-11 (Session A): seed extended with the 8 practice words / 12 practice rounds (p0-p3); `arena_rounds`
  gained `is_practice`, `game_sessions` gained `include_practice`/`practice_answered`. Trial ideophone and round
  ids are unchanged.
- 2026-06-11: `GET /api/leaderboard` is paginated and returns a wrapper object (`entries` + `page`/`size`/
  `totalElements`/`totalPages`) instead of a bare array — **breaking for the Vite frontend** until its
  `getLeaderboard()` reads `.entries`. Ordering gained a deterministic `username` tiebreak.
- 2026-06-11: new `GET /api/admin/stats` behind `ROLE_ADMIN` (`/api/admin/**` requires `hasRole("ADMIN")`).
  Seed now creates the dev admin `arena_admin`; registration still assigns `ROLE_USER` only.
- 2026-06-11: springdoc added; `/v3/api-docs` and `/swagger-ui/**` are public (course-demo convenience).
- 2026-06-11: security `403` responses are no longer overwritten to `401` on real Tomcat (the ERROR dispatch to
  `/error` is now permitted in `SecurityConfig`; `sendError` re-entered the filter chain unauthenticated).
- 2026-06-10: `totalAnswered`/`totalCorrect` in the answer response are now session-scoped (previously cumulative
  user-wide totals). Non-breaking for the Vite frontend, which keeps its own session-local counts.
- 2026-06-10: session completion is set by the final `POST .../answers`; `GET .../rounds/next` is read-only.
  The `completed: true` response shape is unchanged.
- 2026-06-10: `responseTimeMs` is required and bounded to `0..600000`; duplicate answer submissions return `409`
  even under concurrent requests.
- 2026-06-10: the legacy Spring-served mini-frontend (`/`, `/index.html`, `/arena.css`, `/arena.js`) was removed;
  the backend serves the API and public `GET`/`HEAD` `/stimuli/**` only. The Vite app is the only frontend.
