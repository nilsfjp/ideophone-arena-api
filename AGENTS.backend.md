# CLAUDE.md — ideophone-arena-api

## What this is
Spring Boot backend for **Ideophone Arena**, a gamified 2AFC experiment from an MA thesis
on Japanese ideophones and script iconicity (Dingemanse; McLean et al. 2020). Players hear
two real contrastive ideophones and pick which one matches a given English translation,
under one of three script-presentation conditions. Graded course project + portfolio piece.
Correctness of the experimental manipulation outranks features.

**Supersedes** the pre-June-2026 AGENTS.md and its deadline-era scope freezes. The June 5
deadline has passed; this is now a summer-paced quality project. `docs/backend-contract.md`
remains the API authority; `docs/phase-2-api-plan.md` is the Phase-2 model
(GameMode / PresentationMode / RoundTemplate / StimulusAsset / RatingAttempt — do not start unprompted).

## Required reading before code changes
`docs/project_guidelines.md` (teacher-facing architecture authority),
`docs/backend-contract.md`, `docs/backend-grading-checklist.md`, `docs/demo-runbook.md`.
Append to `docs/progress-log.md` after each session (existing format: Session goal /
Changed / Proof / Result / Commit / Blocker / Next single task).

## Hard architecture rules (graded)
- Controllers: thin, `ResponseEntity`, DTOs only, `@Valid` on request bodies, no logic, never touch repositories.
- Services: business logic + `@Transactional`; never return entities; no HTTP knowledge.
- All entity↔DTO mapping lives in mappers — never inline in services or as static factories on DTOs.
- Repositories: derived queries or JPQL with bound params only.
- Custom exceptions + `GlobalExceptionHandler`; translate `DataIntegrityViolationException` to 409; never leak stack traces.
- Bean Validation on request DTOs. NOTE: `@Valid` coverage already exists on auth/session/answer
  endpoints (checklist, 2026-06-04) — verify and complete; move redundant presence checks out of
  `GameService`; add bounds to `responseTimeMs`.
- Constructor injection; no Lombok; no new frameworks or dependencies without explicit approval;
  ASCII-only comments; patch the smallest coherent area.

## Experiment invariants — DO NOT MODIFY without explicit user approval
1. Conditions: `CONDITION_1_SOKUON` (audio only), `CONDITION_2_SOKUON` (congruent script),
   `CONDITION_3_SOKUON` (incongruent script). `difficultyLevel` locked to 1. `TEXT_ONLY` is
   internal/legacy — never externally selectable.
2. **Script display is data, not code.** `ideophones.display_form` is the single source of
   truth for what kana the player sees. Never derive display script at runtime.
3. Stimulus filename prefixes are ground truth, e.g. `a3kh-syakisyaki`:
   pos1 = modality (a/v/i; p = practice), pos2 = pairing index 0–9,
   pos3 = canonical script (h/k), pos4 = displayed script (h/k; u/d = audio-only triangle).
   pos3==pos4 → congruent; pos3≠pos4 → incongruent. Verification must cross-check
   `display_form` against pos4 for every seeded row.
4. Rounds pair contrastive ideophones within the same semantic modality; pairings are
   canonical per the thesis — do not reshuffle.
5. Audio is identical across conditions for a given word; the script manipulation must never
   alter the audio channel.

## Environment & stack
- Spring Boot 4.0.6, Java 21, Maven wrapper. MySQL localhost:3306, server port **8081**.
- Dev in WSL/Arch. Browsers live under `/mnt/c/` when proofs need one. No Windows-pathed config.
- Schema owned by `src/main/resources/db/init/ideophone_arena.sql`.
  `spring.jpa.hibernate.ddl-auto=validate` in ALL profiles — never `update`/`create`.
- Profiles: base + `local` (gitignored; `application-local_example.properties` is the template).
  `spring.jpa.open-in-view=false` stays false.
- `app.stimuli.locations` may point into the frontend repo (`dist/stimuli`) — intentional
  cross-repo coupling; frontend media resolves through symlinks
  (`stimuli/<file> -> stimuli/final-stimuli/final-sokuon/<file>`). Don't "fix" without asking.
- A legacy Spring-served mini-frontend exists at `:8081/` (`/index.html`, `/arena.js`,
  templates/, static assets). DECIDED (2026-06-10): remove it in S3 — the Vite app is the
  only frontend; backend serves API + `/stimuli/**` only. Do not extend it meanwhile.
- Stale tripwires — never reintroduce: port `8080`, `/api/rounds/next`, `/api/rounds/{id}/answer`.

## Security
- JWT is **deliberately hand-rolled** in `JwtService` (HMAC-SHA256, constant-time compare).
  Keep it; cover with unit tests. No jjwt/nimbus without approval.
- `app.jwt.secret` must come from properties with **no code default** — fail fast if absent.
- BCrypt for passwords; never store or return raw passwords or hashes.
- Roles `ROLE_USER`/`ROLE_ADMIN`; admin endpoints use `hasRole`. CSRF disabled only because stateless JWT.
- CORS explicit in `SecurityConfig` for Vite origins (5173/5174).

## Verification & completion rule
`./mvnw test` must pass before any task is done. A change is complete only with:
what changed / why / files touched / **a proof command and result, or the exact blocker** /
next single task. Acceptable proof: passing tests, successful startup, working curl flow
(see `docs/demo-runbook.md`), updated checklist item with evidence, or a written blocker
with exact error output. Never claim done because code looks plausible.
When endpoints, DTOs, ports, auth, CORS, or completion behavior change, update
`docs/backend-contract.md`, `docs/backend-grading-checklist.md`, `docs/demo-runbook.md`.

## Current punch list (June 2026)
- [ ] S1: `display_form` column + seed regeneration (fix gloss typo "feeling fo relief",
      ids 47/107/167); mapper ships display string; switch stimulus assets to per-word audio;
      remove per-condition mp4 dependency; update contract docs.
- [ ] S1: `ddl-auto=validate` in local profile; JWT secret fail-fast.
- [ ] S3: complete Bean Validation coverage; consolidate mapping into `GameMapper`;
      409 on duplicate-answer race; score counts scoped to session (contract already warns
      against user-lifetime totals); session completion moved off the GET into `submitAnswer`;
      remove legacy Spring-served mini-frontend (templates/, static arena assets, their permitAll entries); update runbook.
- [ ] S4: `GET /api/admin/stats` behind `hasRole("ADMIN")`; leaderboard pagination; springdoc;
      practice rounds (p-prefix stimuli).
