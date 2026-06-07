# Ideophone Arena API

Spring Boot backend for the Ideophone Arena MVP demo. The backend exposes JWT-authenticated game endpoints, stores users, sessions, rounds, and answers in MySQL, and serves the minimal static demo frontend from the same application.

## Requirements

- Java 21
- MySQL with an `ideophone_arena` database
- Maven wrapper from this repository

## Local Configuration

The active backend port is:

```text
http://localhost:8081
```

Local credentials and JWT secrets belong in the ignored file:

```text
src/main/resources/application-local.properties
```

Use the tracked placeholder file as the template:

```text
src/main/resources/application-local.example.properties
```

The default local media lookup is configured by `app.stimuli.locations`. The checked-in default supports classpath stimuli and the local Vite build media directory.

## Run

From the repository root:

```sh
./mvnw spring-boot:run
```

Quick health check:

```sh
curl -i http://localhost:8081/api/health
```

## Test

```sh
./mvnw test
```

The test suite starts Spring contexts, validates repository/security wiring, exercises the authenticated game flow with MockMvc, and checks the static frontend/media route.

## Demo Settings

Use the supported demo request:

```json
{
  "conditionName": "CONDITION_1_SOKUON",
  "difficultyLevel": 1
}
```

Difficulty values other than `1` are rejected with `400 Bad Request`.

## Main Endpoints

Public:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/leaderboard
GET  /api/health
GET  /stimuli/**
```

Authenticated with `Authorization: Bearer <token>`:

```text
POST /api/game/sessions
GET  /api/game/sessions/{sessionUuid}/rounds/next
POST /api/game/sessions/{sessionUuid}/answers
GET  /api/game/me/attempts
```

## Curl Demo Script

Register:

```sh
curl -i -X POST http://localhost:8081/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","email":"demo@example.com","password":"password123"}'
```

Login and copy the `token` value:

```sh
curl -i -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"password123"}'
```

Start a session:

```sh
curl -i -X POST http://localhost:8081/api/game/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1}'
```

Fetch the next round:

```sh
curl -i http://localhost:8081/api/game/sessions/$SESSION_UUID/rounds/next \
  -H "Authorization: Bearer $TOKEN"
```

Submit an answer:

```sh
curl -i -X POST http://localhost:8081/api/game/sessions/$SESSION_UUID/answers \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"roundId":1,"selectedIdeophoneId":1,"responseTimeMs":1200}'
```

Recent attempts and leaderboard:

```sh
curl -i http://localhost:8081/api/game/me/attempts \
  -H "Authorization: Bearer $TOKEN"

curl -i http://localhost:8081/api/leaderboard
```

## Browser Demo

With the backend running, open:

```text
http://localhost:8081/
```

The separate Vite frontend can also run from `/code/js/ideophone-arena-web` on `http://localhost:5174`. Backend CORS allows both `http://localhost:5174` and `http://localhost:5173`.

More detailed proof steps live in:

```text
docs/demo-runbook.md
```
