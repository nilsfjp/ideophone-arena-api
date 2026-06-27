# SPEC — docker-compose local spin-up (backend + MySQL)

Repo: `ideophone-arena-api`. Place at repo root or `docs/`. Packaging only — no app-code change.

## Goal

One command (`docker compose up`) brings up the API + a MySQL with the seeded schema and the
stimulus assets, so a reviewer can hit the running backend. The Vite frontend stays on
`npm run dev` pointed at the container. Full-stack containerization is a SEPARATE later task.

## Files to create

- `Dockerfile` (repo root): multi-stage. Build stage on a Temurin-21 + Maven base runs
  `./mvnw -q -DskipTests package`; runtime stage on `eclipse-temurin:21-jre` runs the jar.
  Run as a non-root user. Expose 8081.
- `docker-compose.yml` (repo root): services `db` and `api` (details below).
- `.dockerignore`: exclude `target/`, `.git/`, `*.iml`, local props.
- `.env.example`: `APP_JWT_SECRET=`, `MYSQL_ROOT_PASSWORD=`, `MYSQL_DATABASE=ideophone_arena`,
  `STIMULI_HOST_DIR=` (resolved host path to the stimulus directory — see Stimuli).

## `db` service

- Image `mysql:8.4` (or 8.0). Env from `.env`.
- **Mount `./src/main/resources/db/init/ideophone_arena.sql` -> `/docker-entrypoint-initdb.d/01-schema.sql:ro`.**
  This is mandatory: `ddl-auto=validate` fails on boot if the schema/seed is not already present.
- Healthcheck via `mysqladmin ping`; named volume for data.

## `api` service

- Build from the `Dockerfile`. `depends_on: db: { condition: service_healthy }`.
- Env: `SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/${MYSQL_DATABASE}` (+ user/pw),
  `SPRING_PROFILES_ACTIVE` as needed, **`APP_JWT_SECRET=${APP_JWT_SECRET}`** (no code default —
  compose MUST supply it), CORS origin for `http://localhost:5174`.
- **Publish `18081:8081`** (NOT `8081`) so the container never collides with a manually-run dev
  backend on 8081. Standardizing the port app-wide is a later decision.
- `ddl-auto` stays `validate`. Never override to `create`/`update`.

### Stimuli (the wrinkle most likely to break the run)

The app serves `/stimuli/**` from `app.stimuli.locations`, which resolves through symlinks into the
frontend repo (`.../ideophone-arena-web/stimuli/final-stimuli/final-sokuon`). That path does not
exist inside the container.

- DO NOT alter the symlink scheme in either repo (AGENTS rule).
- Bind-mount the resolved stimulus directory **read-only** into the `api` container and set
  `app.stimuli.locations` to the mount point (e.g. host `${STIMULI_HOST_DIR}` -> container
  `/srv/stimuli:ro`, property -> `/srv/stimuli`). Parameterize the host path via `.env`
  (`STIMULI_HOST_DIR`) since the two repos' relative location is environment-specific.

## Constraints / invariants

- No application-code, seed-SQL, symlink-scheme, or `ddl-auto` changes. Packaging only.
- No new Maven dependencies.
- The `app.jwt.secret` fail-fast guard must survive containerization (omitting the secret should
  fail fast, not boot insecurely).

## Out of scope (flag as scope creep, do not do)

Containerizing the Vite frontend; any hosting/cloud deploy; changing ports app-wide; touching the
symlink coupling; any application-code change.

## Verification (the oracle — run each and paste real output)

1. `./mvnw test` -> still green (no app code changed).
2. `cp .env.example .env`, fill a test `APP_JWT_SECRET` and `STIMULI_HOST_DIR`; `docker compose up -d --build`.
3. `docker compose ps` -> `db` healthy, `api` up.
4. `curl -i http://localhost:18081/api/health` -> 200.
5. Register + login round-trip (per `docs/demo-runbook.md`, against :18081) -> 200 + JWT.
6. `curl -I http://localhost:18081/stimuli/audio/a0h-gosogoso.m4a` -> 200 `audio/mp4`.
7. Negative: bring `api` up with `APP_JWT_SECRET` unset -> fails fast (proves the secret guard holds in a container).
8. `docker compose down -v` -> clean teardown.

## Docs to update

`README.md` ("Run with Docker" section), `docs/demo-runbook.md` (compose path),
`docs/backend-grading-checklist.md` if relevant; append a `docs/progress-log.md` entry.

## Handoff

Clean working tree, NO commit, proposed commit message, next single task.
