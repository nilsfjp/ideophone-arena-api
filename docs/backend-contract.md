# Ideophone Arena demo contract

Date: 2026-06-11
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
4. the order of the two meaning lines in the prompt (currently a reserved draw, see below).

### Derivation spec (compatibility contract — do not change once sessions exist)

1. Base list: the session's scored rounds for its condition and difficulty, ordered by round id ascending.
2. `Random r = new Random(shuffleSeed)`; `Collections.shuffle(baseList, r)`. Both are algorithmically specified
   in the JDK, hence portable and stable across versions and restarts.
<<<<<<< Updated upstream
3. Iterating the _shuffled_ list in order, three draws per round from the same stream, in this order:
=======
<<<<<<< HEAD
<<<<<<< HEAD
3. Iterating the _shuffled_ list in order, three draws per round from the same stream, in this order:
=======
3. Iterating the *shuffled* list in order, three draws per round from the same stream, in this order:
>>>>>>> f10a2dc3545d4185614eadb1dfd8833eea7b741c
=======
3. Iterating the _shuffled_ list in order, three draws per round from the same stream, in this order:
>>>>>>> 40fbefb6bd3668ad61e30ba178f532ff5f5226ac
>>>>>>> Stashed changes
   `targetIsPairSecond = r.nextBoolean()`, `targetOnLeft = r.nextBoolean()`,
   `targetMeaningListedFirst = r.nextBoolean()`. **"Pair second" is the round member with the higher ideophone
   id** (defined on the pair's ideophone ids, never on the stored left/right columns).
4. Practice rounds keep their fixed order (p0 then p1), but take the same three per-round draws from a separate
   stream, `new Random(shuffleSeed + 1)`, so the scored derivation is unaffected by practice on/off.

### Consequences

- **The round DTO shape is unchanged**; only the values vary by seed. `targetTranslation`/`prompt`/
  `translations.target` are the derived target's gloss, `translations.other` the derived distractor's gloss, and
  `left`/`right` are the derived sides.
- **Correctness is judged against the derived target**, never against `arena_rounds.correct_ideophone_id`. That
  column (and `arena_rounds.prompt`, a copy of the same word's gloss) remains in the schema purely as
  documentation of the fixed thesis target and is no longer read in the serving path.
- `player_answers.target_ideophone_id` stores the derived target at answer time, so analytics can aggregate
  per actually-served target — including the 30 complementary targets the thesis never measured. Recent-attempts
  history replays the stored target, not the thesis target.
- `targetMeaningListedFirst` is **reserved**: it is drawn (so the stream consumption above is final) but the
  current Vite frontend always lists the target meaning first, since `translations.target`/`translations.other`
  are semantic fields. A future frontend rider can honor the draw without any backend change.

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

## Changelog

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
