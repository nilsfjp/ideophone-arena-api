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

`conditionName` is required in `POST /api/game/sessions`. `includePractice` is optional (default `false`); when
`true`, a CHOOSING session serves 2 practice rounds before round 1 (see "Practice rounds"). `gameMode` is optional
(default `CHOOSING`); `LADDER` additionally requires a `floor` and forbids `includePractice` (see "Perception Ladder").
`difficultyLevel` is no longer a request or response field (A3): it is the locked experiment invariant, always `1`,
and the column stays server-side only.

The stable Meaning Match demo path uses:

```json
{
  "conditionName": "CONDITION_1_SOKUON"
}
```

The externally supported condition values are:

```text
CONDITION_1_SOKUON
CONDITION_2_SOKUON
CONDITION_3_SOKUON
```

The backend rejects a missing or unsupported `conditionName` with `400 Bad Request` (an unknown enum string is a `400`
on request-body parsing). `TEXT_ONLY` was removed from the enum (A1) and is no longer accepted.

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
  "conditionName": "CONDITION_1_SOKUON"
}
```

with `conditionName` swapped to one of the three supported values.

Round responses for all three supported conditions expose the rendering fields the frontend currently needs:

```text
completed
message
sessionUuid
roundId
targetTranslation
prompt
conditionName
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
right.ideophoneId
right.kana
right.displayForm
right.canonicalForm
right.romaji
right.stimulusFile
right.stimulusUrl
right.modality
timing.fixationMs
timing.preChoiceDelayMs
```

Script display is data, not code (2026-06-10):

- `displayForm` is the exact kana string the player sees before answering. The frontend must render it verbatim and
  must not derive the display script at runtime.
- `canonicalForm` is the word in its canonical script, intended for the feedback reveal.
- `stimulusUrl` now points at one shared per-word audio file, `/stimuli/audio/<modality><pairing><h|k>-<romaji>.m4a`
  (for example `/stimuli/audio/a0h-gosogoso.m4a`). All three condition rows of a word reference the same audio file;
  the script manipulation never alters the audio channel. The per-condition mp4s are legacy assets and are no longer
  referenced by the seed.
- `kana` keeps the dictionary lemma spelling. For the two long-vowel words (`zyaazyaa`, `kyaakyaa`) the stimuli render
  the chouonpu forms, so `displayForm`/`canonicalForm` use the long-vowel mark while `kana` does not. Render
  `displayForm`, not `kana`.
- `canonicalScript` was dropped from the round choice DTO (A7, NIL-41). The `presentations.script_code` column stays,
  but it is no longer exposed to the frontend.

The session carries a `gameMode` (M1): `CHOOSING` (default, Meaning Match) or `LADDER` (Perception Ladder). The session
response echoes `gameMode` and, for a ladder session, `floor`.

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
  "includePractice": false
}
```

The session response echoes `conditionName`, `gameMode`, `floor` (null for CHOOSING), `includePractice`, and
`startedAt`, and adds `totalRounds`.

`totalRounds` is the number of **scored** rounds this session will serve — the denominator of the client's
"Round n / total". Practice rounds are excluded (they are not scored). It is mode-aware, derived through the same
`RoundSource` seam that serves the rounds: a CHOOSING session reports its **sampled session length** (21 since
NIL-85 — see "Session sampling" below), a LADDER session reports only its floor's pair count. Clients must read it
rather than assume a pool size.

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

Practice rounds do not consume round numbers: "Round 1/21" still means the first scored round. Sessions started
without the flag behave exactly as before; existing clients are unaffected.

## Deterministic per-session shuffle (2026-06-12)

Every session stores a server-generated `shuffle_seed` (`SecureRandom`, never exposed in player-facing DTOs).
The seed deterministically derives the session's entire presentation, recomputed from scratch on every request —
nothing but the seed is persisted, so a session replays identically across server restarts:

1. the order of the 47 scored rounds (30 thesis + 17 A/V/I expansion, since NIL-60),
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

The **algorithm** above is frozen. The **base list it shuffles** grew from 30 to 47 scored rounds when NIL-60
brought the 17 A/V/I expansion pairs live (2026-07-08); a given seed therefore derives a different order than it
did pre-NIL-60. This is compatible because no persisted in-progress live session exists (dev/demo DB is reloaded),
and the thesis cohort's answers are keyed by `trial_id` — not by shuffle order — so they are untouched. Any future
change to the base list once real sessions exist would break replay and is forbidden.

### Session sampling (2026-07-10, NIL-85)

A Meaning Match (`CHOOSING`) session serves **21 scored rounds — 7 auditory, 7 visual, 7 interoceptive** — not the
whole 47-pair pool. The sample is a **filter over the derivation above, never a re-derivation**:

1. Derive all 47 scored rounds exactly as specified (shuffle + three draws per round, `+0` stream). Unchanged.
2. Walk that derived list in order and keep the first 7 rounds of each modality, preserving shuffle order.

Because selection happens *after* the draws, each served round carries byte-identical `target` / `targetOnLeft` /
`targetMeaningListedFirst` values to the ones the full derivation gave it. The served list is a **subsequence** of
the full derivation. Two consequences the code depends on:

- **The derivation spec above is untouched.** Shuffling a pre-truncated 21-element list would be a different
  permutation with different draws; that is explicitly not what happens.
- **Replay resolves every answer.** `/api/research/position-bias` re-derives the *full* 47 and looks answers up by
  `trial_id`. A session only persists answers for rounds it served, and every served round is a member of the full
  derivation — so no answer is ever stranded, whatever a session's sample was.

Sampling is **stratified** rather than a plain prefix because modalities differ in difficulty (see
`pairings.thesis_accuracy`): an unstratified prefix would make a player's score partly a function of which
modalities their seed happened to draw. Every session faces the same 7/7/7 mix.

Owned by `ChoosingSample` (the single predicate for "which derived rounds does a session serve", mirroring
`LadderTrials`). It caps rather than throws when a modality is short, so the guarantee that a live session is
really 7+7+7 is enforced against the seed by `IdeophoneSeedIntegrityTests`. Practice rounds are untouched (still
2, `+1` stream); the ladder is untouched (`+4` stream, whole floor).

**Knock-on:** the Rating Lab pool is built from words the player has *answered*, so one completed session now
reveals ~40 ratable words instead of 86. The pool fills across sessions. No endpoint or DTO shape changed.

**Leaderboard:** unchanged. It ranks completed CHOOSING sessions by raw correct count
(`correctCount desc, answeredCount asc, username asc`). Once every completed session is 21 rounds, that ordering
*is* accuracy ordering, and the `answeredCount` tiebreak (built for mixed lengths) goes inert but harmless. No
normalization, epoch column, or reset was added: no real player session exists (deploy target W30), and every
pre-NIL-85 47-answer row in the dev DB belongs to a test-harness account. `scripts/cleanup-test-accounts.sql`
removes them.

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

## Perception Ladder (2026-07-08, NIL-41)

The Perception Ladder is a difficulty-tiered climb (mode `LADDER`) through the modality floors. Its trials are
ordinary Choosing rounds — same round DTO and same answer flow as Meaning Match — served floor-scoped in a fixed
easy→hard order.

Floors overview (authenticated):

```text
GET /api/game/ladder/floors
```

Returns `floors[]` in climb/hierarchy order — Sound → Sight → Touch → Inner states. Array position is the floor
ordinal (the client derives `FLOOR n` from it; ordinals are never persisted). Each floor:

```json
{
  "modality": "HAPTIC",
  "pairCount": 4,
  "finalRungPairCode": "exp-h6",
  "cleared": true,
  "bestCorrect": 3,
  "bestAnswered": 4,
  "pairs": [
    { "pairCode": "exp-h1", "finalRung": false },
    { "pairCode": "exp-h5", "finalRung": false },
    { "pairCode": "exp-h4", "finalRung": false },
    { "pairCode": "exp-h6", "finalRung": true }
  ]
}
```

- `pairs` is the floor's easy→hard order (thesis-facts §8 for A/V/I, four-floor spec §4 for Touch). No per-pair
  difficulty number is exposed pre-answer — the position is the difficulty signal. The last rung is `finalRung: true`
  (`a5` / `v4` / `exp-h6` / `i2`, the at-or-below-chance pairs).
- Floor presence is data-driven: a floor appears only once its pairs have served trials. Today the ladder has four
  floors (Sound 10, Sight 10, Touch 4, Inner states 10).
- `cleared` / `bestCorrect` / `bestAnswered` are the caller's own: `cleared` is true iff the caller has a completed
  LADDER session for that floor, with the best session's score (`bestCorrect`/`bestAnswered` are null when uncleared).
  Sequential unlock is not enforced server-side (the client states `AFTER FLOOR n` in words); replaying a cleared floor
  is allowed (best score stands).

Floor-scoped session start reuses `POST /api/game/sessions` (no new endpoint):

```json
{
  "conditionName": "CONDITION_1_SOKUON",
  "gameMode": "LADDER",
  "floor": "AUDITORY"
}
```

`floor` is a modality (`AUDITORY` / `VISUAL` / `HAPTIC` / `INTEROCEPTIVE`), required for `LADDER` and rejected for
`CHOOSING`. A ladder session forbids `includePractice`. The session then serves only that floor's pairs in map order;
completion is the floor's pair count. LADDER sessions never enter the Meaning Match leaderboard, live divergence, or
position bias; they do appear in admin `byModality` (a `HAPTIC` row surfaces organically).

The Touch floor's Haptic pairs are served **only** through the ladder — Meaning Match (`CHOOSING`) still serves exactly
the 47 A/V/I trials, so its frozen shuffle is unchanged.

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

Response shape (entries are `{ id, ideophoneId, rating, responseTimeMs, ratedAt }`, most recent first). `rated_at`
is a second-resolution `TIMESTAMP`, so the descending id breaks same-second ties by insertion order:

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

## Production — free-form entry (2026-07-09, NIL-62)

The third measure. The player is shown a **meaning only** and invents a Japanese-sounding word in romaji; on
submit the attested word is revealed with a feature-by-feature similarity score. Choosing is recognition, Rating
is reflection, Production is **generation**.

A standalone `productions` table, `ratings`-shaped: keyed `UNIQUE(user_id, word_id)` — one invented word per word
per user, so "your first instinct is the datum" is a database fact — with a nullable `session_id` for provenance.
`raw_input` and `scorer_version` are the durable facts; the feature breakdown recomputes on read, so there is no
JSON column and the scorer stays tunable. `normalized_form` is `VARCHAR(40)`, not 32: the `ja/ju/jo -> zya/zyu/zyo`
folds grow 2 characters to 3, so a legal 24-character entry (`"ja" x 12`) normalizes to 36.

Get the next meaning to produce a word for (authenticated):

```text
GET /api/productions/next
```

- `200 {"completed": false, "ideophoneId": 1, "gloss": "with a rustling sound", "modality": "AUDITORY",
  "totalProducible": 94}`. No romaji, no kana, no audio — the meaning is the whole prompt.
- Once the caller has produced every word: `200 {"completed": true, "ideophoneId": null, "gloss": null,
  "modality": null, "totalProducible": 94}` (the completion-sentinel precedent).
- Selection is deterministic and stateless: the caller's production count is the cycle cursor. The cycle is
  `AUDITORY -> VISUAL -> HAPTIC -> INTEROCEPTIVE`, lowest word id within a modality, skipping exhausted
  modalities. Candidates are the 94 seeded words that belong to at least one non-practice trial (practice is a
  trial fact, ADR-3), so practice words never appear. HAPTIC is in the cycle because the Touch floor is live
  (NIL-41) and its 8 words carry real audio, glosses, and scored trials.
- `totalProducible` is the size of that candidate universe — the `{n}` of Word Mint's frozen `WORD {i} OF {n}`
  status line (`SPEC-view-designs.md` §8.4; V14 forbids the client deriving or hardcoding it). It is
  caller-invariant: minting a word advances `{i}`, never shrinks `{n}`. It is carried on the completed sentinel
  too, so the status line survives the last round. The count is fenced to the four cycle modalities — `Modality`
  also has `TACTILE` and `MOTION`, which the cycle never serves, and counting them would leave `{i}` forever
  one short of `{n}`.

Submit an invented word (authenticated):

```text
POST /api/productions
```

```json
{ "ideophoneId": 60, "input": "pikapika", "responseTimeMs": 5200, "sessionUuid": null }
```

- `input` is trimmed and lowercased, then must match `^[a-z]{2,24}$` **and** segment into Japanese morae. Either
  failure returns `400` with `validationErrors.input`, and the attempt is **not consumed** — parsing happens
  before anything is written.
- `responseTimeMs`, if present, must be between `0` and `600000`. Unknown `ideophoneId` returns `404`.
- `sessionUuid` is optional provenance and is **agnostic to `gameMode`**: any session the caller owns is valid,
  CHOOSING or LADDER. An unknown UUID returns `404`, another user's returns `403`.
- Producing the same word twice as the same user returns `409` (`saveAndFlush` +
  `DataIntegrityViolationException` translation, the answer-race pattern), including under concurrent duplicates.
- `201 Created`:

```json
{
  "id": 40, "ideophoneId": 60, "input": "pikapika", "similarityScore": 78,
  "features": [
    { "feature": "redup",           "yours": true,  "target": true,  "matched": true  },
    { "feature": "sokuon",          "yours": false, "target": false, "matched": true  },
    { "feature": "finalN",          "yours": false, "target": false, "matched": true  },
    { "feature": "riSuffix",        "yours": false, "target": false, "matched": true  },
    { "feature": "voicedOnset",     "yours": false, "target": true,  "matched": false },
    { "feature": "heavyVowelRatio", "yours": 0.00,  "target": 0.50,  "matched": false },
    { "feature": "moraCount",       "yours": 4,     "target": 4,     "matched": true  }
  ],
  "target": { "displayForm": "どきどき", "romaji": "dokidoki", "gloss": "with a rapid heartbeat",
              "stimulusUrl": "/stimuli/audio/i9h-dokidoki.m4a" }
}
```

- `features` always carries all **seven** entries in the frozen chip order of `SPEC-view-designs.md` §8.3. `yours`
  and `target` are heterogeneous by design: five booleans, `moraCount` an integer, `heavyVowelRatio` a 2-dp
  decimal. `matched` means the feature contributed its full weight — a shared absence still matches. The client
  decides which chips to render.
- `target.displayForm` is `words.canonical_form` verbatim (ADR-0: production is not a scripted-condition surface,
  so there is no presentation lookup). **Invariant 1:** the player's `input` is echoed as typed romaji and is never
  converted to kana; the only kana in the response is `displayForm`.
- `similarityScore` is `round_half_even(100 x similarity)` under `scorer_version = 1`. HALF_EVEN because
  `100 x similarity` lands on an exact `.5` tie for roughly a third of word pairs, and it is the rounding the seed
  generator already uses for `pairings.foil_distance` — one rounding convention across the whole scorer. An exact
  form match scores exactly `100`.

The scorer lives in `service/PhonologyService.java` (ADR-8.1): pure functions, no repository access, every method
taking a `PhonologyProfile` (Japanese the only v1 profile). It is the Java half of the ADR-8.2 dual
implementation — `PhonologyServiceTests` asserts byte-for-byte parity against the whole of
`docs/research/phonology-golden.json` (102 words, 21 `foil_distance` values). **Scope fence:** the similarity
formula is a production-scoring instrument, never a difficulty dial between real words.

Read the caller's own productions, paginated (authenticated; the leaderboard/ratings wrapper):

```text
GET /api/game/me/productions?page=0&size=10
```

Query params `page` (default `0`, clamped `>= 0`) and `size` (default `10`, clamped `1..50`). Entries are
`{ id, ideophoneId, input, similarityScore, createdAt }`, most recent first. `created_at` is a second-resolution
`TIMESTAMP`, so the descending id breaks same-second ties by insertion order.

## Triangulation — three measures per word (2026-07-09, NIL-62)

```text
GET /api/research/triangulation
```

Public (no authentication), like `GET /api/research/divergence`, which stays untouched. Returns a JSON array with
one row per word that has **any** data — at least one guess, rating, or production — ordered by `ideophoneId`:

```json
[
  {
    "ideophoneId": 60, "romaji": "dokidoki", "gloss": "with a rapid heartbeat", "modality": "INTEROCEPTIVE",
    "guessAccuracy": 0.625, "guessCount": 32,
    "meanRating": null, "ratingCount": 0,
    "meanProductionScore": 78.0, "productionCount": 4
  }
]
```

- Each measure is **`null`** exactly when its count is zero, independently per measure — "no data" never
  masquerades as "always wrong" or "lowest score". Clients gate on the three counts.
- Three separate `GROUP BY` queries merged per word in the service. A single join across `player_answers`,
  `ratings`, and `productions` would form a cartesian product; the 27B warning applies threefold.
- The guess and rating aggregates are the **same repository methods divergence uses**, so `guessAccuracy`,
  `guessCount`, `meanRating`, and `ratingCount` can never disagree between the two endpoints. The production
  aggregate carries the same Rider A username fences (`browser_loop_%`, `thesis_p%`) for symmetry; it carries no
  `gameMode` fence, because a production has no game mode.
- Triangulation's key set is a **superset** of divergence's: a word with a production but no guess and no rating
  appears here and not there. In particular HAPTIC words show a real `meanProductionScore` (and can show a real
  `meanRating`) with `guessAccuracy: null`, because the guess aggregate is `gameMode = CHOOSING` only and HAPTIC
  serves through the Perception Ladder. That is honest, not a bug.

## Changelog

- 2026-07-10: **Meaning Match session sampling (NIL-85)** — **behavioural, no DTO or endpoint shape change.** A
  `CHOOSING` session now serves 21 scored rounds (7 auditory / 7 visual / 7 interoceptive) sampled from the 47-pair
  pool, instead of the whole pool. `GameSessionResponse.totalRounds` therefore reports `21` where it reported `47`;
  the field already existed (NIL-89) and clients that read it need no change. Sampling is a *filter over the frozen
  derivation*, so the derivation spec is untouched and every served round keeps the draws the full derivation gave
  it — see "Session sampling" above. The new `ChoosingSample` component owns the predicate; `RoundShuffler`,
  practice serving, the ladder, and the leaderboard query are all unchanged. Knock-on: one completed session now
  reveals ~40 ratable words rather than 86 (the Rating Lab pool fills across sessions). Proof: `./mvnw test` ->
  176 tests, 0 failures (+7 `ChoosingSampleTests`, +1 seed-integrity modality-quota guard); live session served 21
  rounds with a 7/7/7 mix, announced `totalRounds` == rounds served, clean completion body; browser loop green at
  1280 and 375 (2 practice + 21 scored, displayed denominator `Round 1 / 21`, 0 console errors).
- 2026-07-09: **`totalProducible` on the production prompt (NIL-62 FE rider)** — additive field on
  `GET /api/productions/next`, both the live and the completed branch. Word Mint's frozen status line is
  `WORD {i} OF {n} · YOUR MEAN {m}` (`SPEC-view-designs.md` §8.4) and V14 forbids the client hardcoding or deriving
  the counts, but nothing served `{n}`: the prompt returns one word, `GET /api/game/me/productions.totalElements`
  counts only the caller's own rows (that is `{i}` and `{m}`), and `/api/research/triangulation` unions only words
  that already carry data. `ProductionRepository.countProducible(Collection<Modality>)` is
  `findNextUnproducedByModality`'s predicate minus the caller's `not exists` clause, widened from one modality to
  the whole cycle. Two things are load-bearing: the **modality fence** (`Modality` has `TACTILE` and `MOTION`,
  which `PROMPT_CYCLE` never serves — an unfenced count would leave `{i}` forever one short of `{n}` the day one
  is trialed; `PROMPT_CYCLE` is passed in as the fence so the two cannot drift), and keeping trial membership an
  **`exists` subquery rather than a join** (a word sits in many non-practice trials; a join would count it once
  per trial). The count is caller-invariant, so it is computed once per request. Both clauses are guarded by
  `CountProducibleTests` against the database — the service test mocks the repository and the seed has no
  TACTILE/MOTION rows, so neither could see them; removing the fence fails two of its cases, and swapping the
  `exists` for a join fails its duplication case. `./mvnw test` -> 168 tests, 0 failures.
- 2026-07-09: **A10 exemption narrowed + ladder predicate unified (NIL-90 / NIL-91)** — no endpoint or DTO change.
  **NIL-90:** the automation exemption now lifts the guard for `browser_loop_` **only**; `thesis_p*` is rejected under
  every profile, including `automation`. That cohort is the one prefix whose rows are *read back as data*
  (`/api/research/thesis/divergence` includes it rather than excluding it), so a stray `thesis_p` row is silent
  corruption of the thesis layer, not just noise. The property is renamed to match what it does:
  `app.automation.allow-browser-loop-registration` (was `app.automation.allow-reserved-registration`), default
  `false`. **Prod is now unreachable four ways:** the `@Value` default; `docker-compose.yml` sets no
  `SPRING_PROFILES_ACTIVE`; `.dockerignore` keeps `application-automation.properties` out of the image, so the key
  has no source there even if the profile were activated; and `thesis_p` is not exempt at all. **NIL-91:** a new
  `LadderTrials` component owns the single predicate "which trials does this floor serve". `validateLadderStart` and
  `LadderRoundSource` both ask it, so a floor can no longer pass the start check and then serve zero rounds — a floor
  whose trials sit under unexpected pair codes is now a `400 Unsupported ladder floor`. `GameService` drops its
  `LadderFloors` dependency. `./mvnw test` -> 163 tests, 0 failures.

- 2026-07-09: **`GameSessionResponse.totalRounds` (NIL-89)** — **additive** field on `POST /api/game/sessions`: the
  count of scored rounds the session will serve, derived through the `RoundSource` seam so it is mode-aware (CHOOSING
  = the scored pool, 47; LADDER = the floor's pair count). Practice rounds are excluded. It exists because the client
  had no way to learn the denominator of "Round n / total" and was hardcoding `30`, which NIL-60 (30 -> 47) turned
  into a frozen counter and a progress bar pegged at 100% for the last 17 rounds. Backend `GameMapper` now takes the
  count from the service (mappers never reach for repositories). No other shape change; no field removed.

- 2026-07-09: **api patch pass (NIL-88)** — three behaviour-internal fixes, no DTO or endpoint shape change.
  (1) **A10 gains a dev-profile exemption.** `ReservedUsernamePrefixValidator` skips the reserved-prefix
  rejection when `app.automation.allow-reserved-registration=true`. The property has exactly one source, the new
  `application-automation.properties`, which loads only under the `automation` profile
  (`./mvnw spring-boot:run -Dspring-boot.run.profiles=local,automation`). **A10 stays pre-deploy-hard**: the
  `@Value` default is `false`, `docker-compose.yml` sets no `SPRING_PROFILES_ACTIVE`, so the container's active
  profile stays `local` and the file is never read. `./mvnw test` likewise runs under `local` alone, which is what
  lets `RegistrationHttpTests` keep proving the guarded default. The exemption exists so the sanctioned browser
  loop can register the throwaway `browser_loop_*` account that fences its playthrough out of the frozen research
  aggregates — the guard was blocking the fence. Note the skip is wholesale: under the profile `thesis_p*` is
  accepted too, so the `automation` profile must never be activated against a research database.
  (2) `GET /api/game/me/ratings` breaks same-second `rated_at` ties by descending id
  (`findByUserIdOrderByRatedAtDescIdDesc`), resolving the defect deferred by NIL-62 below and matching the
  productions vertical. (3) `AuthService` folds the registration email with `Locale.ROOT`, so a Turkish-locale JVM
  cannot dot-fold `I` and let one address register twice; it was the last bare `toLowerCase()` in `src/main/java`.
  `./mvnw test` -> 157 tests, 0 failures. Browser loop green at 1280 and 375 (47 scored + 2 practice rounds).

- 2026-07-09: **Production / free-form entry (NIL-62)** — the third measure. New generator-emitted table
  `productions` (M3, ADR-0 word grain: `word_id` FK + `UNIQUE(user_id, word_id)`), seeded empty. Three
  authenticated endpoints (`GET /api/productions/next`, `POST /api/productions`, `GET /api/game/me/productions`)
  and one public one (`GET /api/research/triangulation`, which needs its own `permitAll` — there is no
  `/api/research/**` wildcard). `service/PhonologyService.java` lands as the shared feature/normalization engine
  with the `PhonologyProfile` seam from day one (ADR-8.1 / NIL-84 X8); it is the Java half of the ADR-8.2 dual
  implementation and asserts full parity against `docs/research/phonology-golden.json`. NIL-58 must import it, not
  fork it. **Two spec corrections, both adjudicated in chat:** (1) `normalized_form` is `VARCHAR(40)`, not the
  spec's 32 — `"ja" x 12` is a legal 24-char entry that folds to 36 chars and would otherwise hit the column
  limit and surface as a spurious `409`; (2) `similarityScore` rounds **HALF_EVEN** (the spec was silent, and
  `100 x similarity` is an exact `.5` tie for 3142 of 10404 word pairs, where HALF_EVEN and HALF_UP disagree on
  1608). **The `/api/productions/next` cycle includes HAPTIC** (`AUDITORY -> VISUAL -> HAPTIC -> INTEROCEPTIVE`,
  94-word pool): both stated activation triggers were met once NIL-86 seeded the 8 HAPTIC words and NIL-41 brought
  the Touch floor live. `/api/research/divergence` is untouched and its aggregates are reused verbatim.
  `ddl-auto=validate` unchanged; all seed via `generate_seed_sql.py --check` (byte-stable, purely additive). Also
  fixed: `GET /api/game/me/productions` breaks same-second `created_at` ties by descending id. **Known defect,
  not fixed here (RESOLVED 2026-07-09 by NIL-88, above):** `GET /api/game/me/ratings` has the same tie ambiguity
  (`rated_at` is second-resolution and same-second ties already exist in live data) — a one-line derived-query
  change, deliberately left to its own patch. `./mvnw test` -> 155 tests, 0 failures.
- 2026-07-08: **Perception Ladder backend + M1 game_mode + essence-review riders (NIL-41)** — new game mode `LADDER`
  and the floors API. `game_sessions` gains `game_mode` (NOT NULL DEFAULT `CHOOSING`) and `ladder_floor` (nullable
  modality). `GET /api/game/ladder/floors` returns the floors in hierarchy order (Sound → Sight → Touch → Inner
  states) with each floor's easy→hard pairs, final-rung marker, and the caller's cleared/best-score progress; a
  floor-scoped session starts via `POST /api/game/sessions` with `gameMode:"LADDER"` + `floor`. Round serving now
  dispatches per mode through a `RoundSource` seam (ADR-2); the ladder derives side/identity from the reserved
  `shuffleSeed + 4` stream over map-ordered floor trials (the `+0`/`+1` streams are byte-identical). **Touch floor is
  live** as a 4-pair floor (`exp-h1/h5/h4/h6`, final rung `exp-h6`): the 4 HAPTIC trials are seeded but served **only**
  through the ladder, so Meaning Match (`CHOOSING`) still serves exactly the frozen 47 A/V/I trials (55 non-practice
  trials total, 47 non-HAPTIC). LADDER answers are kept out of the frozen CHOOSING aggregates (leaderboard, live
  divergence, position bias) via a `gameMode = 'CHOOSING'` guard; admin `byModality` gains a `HAPTIC` row organically.
  **Breaking (rides NIL-42 on the web side):** `difficultyLevel` is removed from `POST /api/game/sessions` request and
  from the session/round responses (A3; the column + locked invariant stay); `canonicalScript` is removed from the
  round choice DTO (A7; the `script_code` column stays); `TEXT_ONLY` is removed from `ConditionName` (A1) and the dead
  `ScriptType` enum is deleted (A2). New guard (A10): registration rejects reserved username prefixes `thesis_p*` /
  `browser_loop_*` with `400` and a contract-style `validationErrors.username` (case-insensitive to match the read-side
  LIKE fences). Suite 98 -> 113 (0 failures); floors/ladder walk/leaderboard-exclusion proven live.
- 2026-07-08: **A/V/I expansion go-live (NIL-60)** — the 17 A/V/I expansion pairs seeded dark by NIL-86 now emit
  trials and serve in the live pool: **scored rounds per session 30 -> 47** (30 thesis + 17 A/V/I expansion),
  total trials 34 -> 51, and the ratable-word pool a completed session yields grows 60 -> 86 (the 26 new A/V/I
  words). The 4 HAPTIC expansion pairs **stay dark** (HAPTIC serves in no mode until the Touch floor, NIL-41/42).
  Expansion trials carry `correct_word_id = NULL` (no thesis fixed target; the served target is shuffle-derived,
  and that column is unread for CHOOSING). **Every public DTO/endpoint shape is unchanged (zero API/frontend
  changes); only counts grow.** The 34 TTS clips (`ja-JP-Wavenet-B`) were audited (ffprobe: aac/mono/24 kHz,
  nonzero duration) and serve via `/stimuli/audio/<file>`. See the shuffle derivation-spec note on the base-list
  growth.
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
