# Ideophone Arena demo contract

Date: 2026-06-05
Deadline: 2026-06-05

## Purpose

This document records the backend/frontend contract for the final demo. It exists to prevent last-minute scope drift.

## Backend base URL

Local backend:

```text
http://localhost:8081
```

## Supported demo settings

The final demo supports:

```json
{
  "conditionName": "CONDITION_1_SOKUON",
  "difficultyLevel": 1
}
```

Other seeded conditions may work:

```text
CONDITION_1_SOKUON
CONDITION_2_SOKUON
CONDITION_3_SOKUON
```

Do not expose arbitrary difficulty selection. Difficulty values above `1` are not seeded and must not be selectable in the frontend.
The backend rejects `difficultyLevel` values other than `1` with `400 Bad Request`.
Unknown `conditionName` values are rejected with `400 Bad Request`. Known seeded condition enum values may work, but
`CONDITION_1_SOKUON` is the only supported final-demo setting.

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
