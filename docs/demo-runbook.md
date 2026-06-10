# Ideophone Arena MVP Demo Runbook

## Start Backend

From `/code/java/ideophone-arena-api`:

```sh
./mvnw spring-boot:run
```

Expected backend origin:

```text
http://localhost:8081
```

Quick proof:

```sh
curl -i http://localhost:8081/api/health
```

## Start Frontend

The backend workspace now includes a small Spring-served frontend for the trial loop:

```text
http://localhost:8081/
```

It uses the same `/api/auth`, `/api/game/sessions`, `/rounds/next`, and `/answers` endpoints as the Vite app. If the
browser blocks unmuted media playback, the active stimulus card shows a `Play` button.

Stimulus media is served from `/stimuli/<filename>`. The default local configuration checks
`classpath:/static/stimuli/` and `/code/js/ideophone-arena-web/dist/stimuli/`; override `app.stimuli.locations` if your
media files live elsewhere.

The separate Vite frontend can still be run from `/code/js/ideophone-arena-web`:

From `/code/js/ideophone-arena-web`:

```sh
npm run dev
```

Expected frontend origin with the current Vite config:

```text
http://localhost:5174
```

The backend CORS config allows both `http://localhost:5173` and `http://localhost:5174`. The frontend also proxies
`/api` to `http://localhost:8081`, so local Vite development can keep `VITE_API_BASE_URL` empty.

## Media Path Proof

The backend returns one shared per-word audio stimulus for all three condition rows of a word:

```text
/stimuli/audio/a0h-gosogoso.m4a
```

The audio files live in the frontend repo at `stimuli/audio/` (extracted with `ffmpeg -vn -c:a copy` from the
audio-only `hu`/`kd` mp4 variants, so they are bit-identical to the experiment audio) and are copied into
`dist/stimuli/audio/`, which the backend serves. The legacy per-condition mp4s remain available through
`public/stimuli -> ../stimuli` and the root-level symlinks into `stimuli/final-stimuli/final-sokuon/`, but the seed no
longer references them.

Quick proof against the running backend (GET and HEAD both return `200` with `Content-Type: audio/mp4`):

```sh
curl -I http://localhost:8081/stimuli/audio/a0h-gosogoso.m4a
```

Quick proof while Vite is running:

```sh
curl -I http://localhost:5174/stimuli/audio/a0h-gosogoso.m4a
```

## Browser Demo Flow

1. Open the frontend URL.
2. Register a new user or log in.
3. Read the instruction screen.
4. Click `Start Game`.
5. Confirm the trial flow:
   - fixation cross appears
   - translations appear
   - left ideophone appears and plays
   - left disappears
   - right ideophone appears and plays
   - both ideophones appear
   - answer buttons appear below
6. Click one ideophone answer.
7. Confirm immediate feedback appears.
8. Confirm the next trial starts automatically from the fixation cross.

If the browser blocks unmuted media playback, the current stimulus card shows a `Play` button. Click it once; the trial
continues from the same left or right stimulus instead of skipping the trial.

## Backend API Curl Flow

Register:

```sh
curl -i -X POST http://localhost:8081/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","email":"demo@example.com","password":"password123"}'
```

Login:

```sh
curl -i -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"password123"}'
```

Start session:

```sh
curl -i -X POST http://localhost:8081/api/game/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"difficultyLevel":1,"conditionName":"CONDITION_1_SOKUON"}'
```

Fetch next round:

```sh
curl -i http://localhost:8081/api/game/sessions/$SESSION_UUID/rounds/next \
  -H "Authorization: Bearer $TOKEN"
```

While unanswered rounds remain, this returns `200 OK` with `completed:false` and the round fields used by the frontend.

Submit answer:

```sh
curl -i -X POST http://localhost:8081/api/game/sessions/$SESSION_UUID/answers \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"roundId":1,"selectedIdeophoneId":1,"responseTimeMs":1200}'
```

After the final round has been answered, fetch next round again:

```sh
curl -i http://localhost:8081/api/game/sessions/$SESSION_UUID/rounds/next \
  -H "Authorization: Bearer $TOKEN"
```

Expected completion behavior:

```text
HTTP/1.1 200
```

```json
{
  "completed": true,
  "message": "Game session is complete"
}
```
