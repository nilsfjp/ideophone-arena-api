# Ideophone Arena API

Spring Boot backend for the Ideophone Arena experiment. The backend exposes JWT-authenticated game endpoints, stores users, sessions, rounds, and answers in MySQL, and serves experiment audio stimuli. The Vite app (`/code/js/ideophone-arena-web`) is the only frontend.

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

## Run with Docker

`docker compose up` brings up a seeded MySQL plus the API jar so a reviewer can hit the running
backend without a local Java/MySQL setup. The Vite frontend still runs separately on `npm run dev`
and points at the container; full-stack containerization is a separate later task.

The API is published on host port **18081** (not 8081) so the container never collides with a
manually-run dev backend on 8081.

1. Create your env file from the template and fill in the blanks:

   ```sh
   cp .env.example .env
   ```

   - `APP_JWT_SECRET` — a long random string. There is no code default; compose must supply it or
     the app fails fast.
   - `MYSQL_ROOT_PASSWORD` — any local password for the throwaway MySQL.
   - `STIMULI_HOST_DIR` — host path to the resolved stimulus directory that contains `audio/`
     (e.g. `/code/js/ideophone-arena-web/dist/stimuli`). It is bind-mounted read-only into the
     container at `/srv/stimuli`.

2. Build and start:

   ```sh
   docker compose up -d --build
   docker compose ps
   ```

3. Health check (note the 18081 port):

   ```sh
   curl -i http://localhost:18081/api/health
   ```

4. Tear down (the `-v` removes the MySQL data volume so the next start reseeds):

   ```sh
   docker compose down -v
   ```

The schema and seed are loaded by mounting `src/main/resources/db/init/ideophone_arena.sql` into the
db container's init directory; `ddl-auto` stays `validate`. CORS for `http://localhost:5174` is
already configured in the app, so the Vite frontend works against `http://localhost:18081`.

## Test

```sh
./mvnw test
```

The test suite starts Spring contexts, validates repository/security wiring, exercises the authenticated game flow with MockMvc, covers JWT, seed integrity, admin authorization, and leaderboard pagination.

## Demo Settings

`conditionName` and `difficultyLevel` are required at session start. All three conditions are supported at `difficultyLevel: 1`:

```text
CONDITION_1_SOKUON   (audio only)
CONDITION_2_SOKUON   (congruent script)
CONDITION_3_SOKUON   (incongruent script)
```

Difficulty values other than `1` are rejected with `400 Bad Request`. `TEXT_ONLY` is an internal enum value and is not externally selectable.

## Main Endpoints

Public:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/leaderboard          (paginated: ?page=0&size=10)
GET  /api/health
GET  /stimuli/**
GET  /v3/api-docs
GET  /swagger-ui/index.html
```

Authenticated with `Authorization: Bearer <token>`:

```text
POST /api/game/sessions
GET  /api/game/sessions/{sessionUuid}/rounds/next
POST /api/game/sessions/{sessionUuid}/answers
GET  /api/game/me/attempts
```

`ROLE_ADMIN` only:

```text
GET  /api/admin/stats
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
  -d '{"conditionName":"CONDITION_2_SOKUON","difficultyLevel":1}'
```

Fetch the next round:

```sh
curl -i http://localhost:8081/api/game/sessions/$SESSION_UUID/rounds/next \
  -H "Authorization: Bearer $TOKEN"
```

Submit an answer (`responseTimeMs` is required, range 0–600000):

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

curl -i 'http://localhost:8081/api/leaderboard?page=0&size=10'
```

Leaderboard response wraps entries:

```json
{
  "entries": [
    { "username": "demo", "totalAnswered": 30, "totalCorrect": 21, "accuracy": 0.7 }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

Admin stats (requires `ROLE_ADMIN` token):

```sh
curl -i http://localhost:8081/api/admin/stats \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

The dev seed creates `arena_admin` for local testing; see `docs/demo-runbook.md` for credentials.

## Vite Frontend

Start the frontend from `/code/js/ideophone-arena-web`:

```sh
npm run dev
```

Default frontend URL: `http://localhost:5174`. Backend CORS allows both `http://localhost:5174` and `http://localhost:5173`.

## API Docs (Swagger UI)

With the backend running:

```text
http://localhost:8081/swagger-ui/index.html
http://localhost:8081/v3/api-docs
```

More detailed proof steps live in:

```text
docs/demo-runbook.md
```
