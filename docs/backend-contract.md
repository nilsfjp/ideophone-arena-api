# Ideophone Arena demo contract

Date: 2026-06-07
Deadline: 2026-06-05

## Purpose

This document records the backend/frontend contract for the final demo. It exists to prevent last-minute scope drift.

## Backend base URL

Local backend:

```text
http://localhost:8081
```

## Supported session-start settings

`conditionName` and `difficultyLevel` are required in `POST /api/game/sessions`.

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

Request body:

```json
{
  "conditionName": "CONDITION_1_SOKUON",
  "difficultyLevel": 1
}
```

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

## Completion behavior

When there are no more unanswered rounds, the next-round endpoint returns `200 OK` with an explicit completion DTO.
The frontend must treat `completed: true` as normal session completion, not as an error or automatic reset.

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

Do not rely on cumulative user-wide totals for per-session remaining count. The frontend should maintain session-local answered/correct counts, or the backend should provide session-scoped totals.

## Leaderboard

Public leaderboard:

```text
GET /api/leaderboard
```

This should be visible in the final demo.

## Recent attempts

Authenticated user history:

```text
GET /api/game/me/attempts
```

This is enough for minimal personal progress/history.
