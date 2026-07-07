# SPEC — Free-Form Entry / Production Measure (`PRODUCTION`)

_F2 output (NIL-56), Fable 5, 2026-07-02. The third measure. Deliberately architected as a **standalone vertical in the `ratings` pattern** (2026-06-20 precedent): no session/mode machinery required, zero NIL-57 dependency, zero external gates. Executable by Opus 4.8 as-is. Working title only; naming is F3 territory._

_**Revised 2026-07-02 (same-day, OSF-mining session; prior version in `archive/`):** one scope note added to §5 fencing the similarity scorer off from difficulty prediction. Nothing else changed — the build plan is untouched._

## 1. Concept and vision test

The player is shown a **meaning only** — no word, no audio — and invents a Japanese-sounding word for it, typed in romaji. Submit → the real word is revealed (kana, audio, gloss) alongside a similarity score: how close did your invention land to the attested form, feature by feature?

**Vision test — new measurement angle: production.** Choosing = recognition (what your ear knows), Rating = reflection (what you believe you hear), Production = **generation** (what your mouth would do). This completes the McLean/Dunn/Dingemanse "two measures are better than one" arc as a *triad* — the app's core research identity. The population statistic is new in kind: per word, do naive players **converge** on the attested form's iconic features (reduplication for the continuous, sokuon for the abrupt, heavy vowels for the heavy)? Convergence-vs-guessability-vs-rating is a three-way dissociation no single dataset in the field ships.

Bonus stat for free: whether a player produced a word **before or after** encountering it in the Choosing Task is derivable by joining `productions` against `player_answers` timestamps — first-contact productions are the clean measure; no schema needed.

## 2. Source data

None new. Uses the existing 60 scored inventory words (practice `p*` words excluded). Expansion words join automatically once seeded (NIL-53 pipeline). No audio, no licensing, no sign-off sheets.

## 3. Trial design

1. Prompt card: meaning line (gloss bold, thesis meaning-prompt style) + a plain text input (romaji), submit button. No audio, no kana anywhere pre-submit.
2. One attempt per user per word — "your first instinct is the datum" (UI copy communicates this; enforced by `UNIQUE(user_id, ideophone_id)`, 409 on repeat — exact `ratings` semantics).
3. Reveal card: the real word — `display_form` verbatim (kana at `--text-kana-feedback`), audio replay (`StimulusPlayback`), gloss — plus the similarity score (0–100) and a feature-chip breakdown (§7): which iconic features your invention shared with the real word.
4. Next prompt.

Input rules (client mirrors server): lowercase Latin letters only, 2–24 chars, must parse as Japanese morae (§5). Unparseable input gets a friendly inline error (F3 microcopy) and does **not** consume the attempt.

## 4. Data model

One new table, `ratings`-shaped (schema via `scripts/generate_seed_sql.py --check`; `ddl-auto=validate`):

```sql
CREATE TABLE productions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ideophone_id BIGINT NOT NULL,
    session_id BIGINT NULL,                -- provenance only, like ratings
    raw_input VARCHAR(24) NOT NULL,        -- as typed (post trim/lowercase)
    normalized_form VARCHAR(32) NOT NULL,  -- canonical phoneme string (§5)
    similarity_score SMALLINT NOT NULL,    -- 0–100
    scorer_version SMALLINT NOT NULL,      -- re-scoring from raw_input stays possible
    response_time_ms INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (user_id, ideophone_id),
    CONSTRAINT fk_productions_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_productions_ideophone FOREIGN KEY (ideophone_id) REFERENCES ideophones (id),
    CONSTRAINT fk_productions_session FOREIGN KEY (session_id) REFERENCES game_sessions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

No JSON column: the feature breakdown is recomputed from `raw_input` + the target's `romaji` on read (deterministic given `scorer_version`), keeping the schema lean and the scorer tunable. `raw_input` + `scorer_version` are the durable facts.

Forward-compat note: production is language-agnostic by construction — when `ideophones.language_id` lands (cross-linguistic spec §4), a Korean production round is the same table, same scorer with a per-language phonology profile (**NIL-57 flag:** `PhonologyService` profile-per-language seam; Japanese profile only in v1).

## 5. PhonologyService (the scoring engine — spec is exact, implement as written)

New `service/PhonologyService.java` — pure functions, no repository access, exhaustively unit-tested. **This is deliberately the shared machinery for NIL-58's foil-distance engine and the phoneme-shape mode's feature table** — one feature extractor, three consumers (flag placement to NIL-57; build it here first).

**Normalization** (applies to player input and to inventory `romaji`, which is Nihon-shiki-ish: `zi`, `si`, `tu`, `hu`, `N`, `Q`):

1. Trim, lowercase. Reject unless `^[a-z]{2,24}$` → 400 with `validationErrors.input`.
2. Hepburn folding, longest-match, exactly this table: `shi→si, sha→sya, shu→syu, sho→syo, chi→ti, cha→tya, chu→tyu, cho→tyo, tsu→tu, ji→zi, ja→zya, ju→zyu, jo→zyo, fu→hu`.
3. Geminates: any doubled consonant `kk|ss|tt|pp|ttyy-fold etc.` (post-folding, any CC with C ∈ consonants) → `Q` + single C. A literal trailing `q` → `Q` (the F1 word-final-sokuon pitfall: `paQ`, `hoQ` must survive).
4. Moraic nasal: `n` at word end, or `n` before a consonant other than `y` → `N`.
5. Mora segmentation, priority order: palatalized `CyV` (C ∈ k,g,s,z,t,n,h,b,p,m,r) > `CV` > bare `V` > `N` > `Q`. Doubled vowels = two morae (`zyaazyaa` = zya-a-zya-a). Any residue → parse failure → 400.

**Features** per normalized form:

| Feature | Definition |
|---|---|
| `moraCount` | number of morae |
| `redup` | form = X·X with X ≥ 2 morae |
| `sokuon` | contains `Q` |
| `finalN` | last mora is `N` |
| `riSuffix` | last mora is `ri` ∧ ¬`redup` |
| `voicedOnset` | first consonant ∈ {g, z, d, b} |
| `heavyVowelRatio` | |{o,u}| / (|{o,u}| + |{i,e}|) over vowel morae; 0.5 if denominator 0 |

**Similarity v1** (player *p* vs target *t*; weights sum to 1.00; **v1-tunable, never retro-edit without bumping `scorer_version`**):

```
score = 100 × ( .20·[redup_p = redup_t] + .15·[sokuon_p = sokuon_t]
              + .10·[finalN_p = finalN_t] + .10·[riSuffix_p = riSuffix_t]
              + .15·[voicedOnset_p = voicedOnset_t]
              + .15·(1 − |heavyVowelRatio_p − heavyVowelRatio_t|)
              + .15·(1 − |moraCount_p − moraCount_t| / max(moraCount_p, moraCount_t)) )
```

Exact-form match trivially scores 100. Voicing is **scored** (form similarity is not the F1 voicing-iconicity null — that ruled out voicing as a *difficulty/magnitude* cue, not as a feature of form distance).

**Scope note (2026-07-02, binding for NIL-58):** this formula is a **production-scoring instrument** — player invention vs attested target. It is *not* a difficulty predictor between real words: applied to the 30 thesis pairs, feature distance did not predict 2AFC accuracy (rho = −.155, p = .41; f1 profile §4, 2026-07-02 section). Do not repurpose the scorer as a difficulty dial for invariant-4 content; its NIL-58 role is supplying `features()` and the distance primitive for *constructed-foil* regimes and for recorded-covariate validation.

## 6. API surface (house conventions — mirrors the ratings vertical 1:1)

- `GET /api/productions/next` (auth) → `200 {ideophoneId, gloss, modality}` — next unproduced word for the caller. Selection rule (deterministic, no seed needed): among scored inventory words without a `productions` row for this user, cycle modality `AUDITORY → VISUAL → INTEROCEPTIVE` (skip empty), lowest `ideophone_id` within modality. All produced → `200 {completed: true}` (completion-DTO precedent).
- `POST /api/productions` (auth) → `201`:

```json
// request
{ "ideophoneId": 9, "input": "dokidoki", "responseTimeMs": 5200, "sessionUuid": null }
// response
{ "id": 1, "ideophoneId": 9, "input": "dokidoki", "similarityScore": 100,
  "features": [ { "feature": "redup", "yours": true, "target": true, "matched": true }, ... ],
  "target": { "displayForm": "どきどき", "romaji": "dokidoki", "gloss": "...", "stimulusUrl": "/stimuli/audio/i9h-dokidoki.m4a" } }
```

  Validation per contract style: `input` regex + parseability → 400 `validationErrors`; unknown `ideophoneId` → 404; duplicate (user, word) → 409 via `saveAndFlush` + `DataIntegrityViolationException` translation (the answer-race pattern); `sessionUuid` resolution: 404 unknown / 403 unowned; `responseTimeMs` 0–600000.
- `GET /api/game/me/productions?page=0&size=10` (auth) → paginated `{entries, page, size, totalElements, totalPages}` wrapper, entries `{id, ideophoneId, input, similarityScore, createdAt}`, most recent first. Clamping per leaderboard precedent.
- `GET /api/research/triangulation` (public) → per word with any data: `{ideophoneId, romaji, gloss, modality, guessAccuracy, guessCount, meanRating, ratingCount, meanProductionScore, productionCount}` — extends the divergence pattern to three measures; `null` for any zero-count measure. Separate `GROUP BY` per measure, merged in service (the cartesian-product warning from 27B applies threefold). Leave `GET /api/research/divergence` untouched (public contract stability).

Files: `controller/ProductionController.java`, `service/ProductionService.java` + `service/PhonologyService.java`, `repository/ProductionRepository.java`, `dto/ProductionRequest.java` / `ProductionResponse.java` / `ProductionPromptResponse.java` / `ProductionPageResponse.java` / `TriangulationResponse.java`, mapper, exceptions (`ProductionAlreadyExistsException`, `UnparseableInputException`), `OpenApiConfig` tag, `ProductionHttpTests.java` + `PhonologyServiceTests.java`, updates to `docs/backend-contract.md` + `docs/backend-grading-checklist.md`.

## 7. Frontend (vs 27C view-state shell + tokens.css)

- `App.tsx`: `AppView` gains `"production"`; card on the 27C home surface.
- New `src/components/ProductionLab.tsx` (+ test): prompt card → input (`--font-body`, input text rendered at `--text-md`) → reveal card reusing `FeedbackPanel` layout conventions and `StimulusPlayback`.
- Feature chips on reveal: matched chips in `--positive`/`--positive-soft`, unmatched in `--ink-muted` on `--surface-card` (not `--negative` — a non-match isn't an error, the copy should feel exploratory). Score numeral at `--text-2xl`.
- `src/api/types.ts`: `ProductionPrompt`, `ProductionRequest/Response`, `FeatureMatch`, `TriangulationRow`; `client.ts`: `getNextProductionPrompt`, `submitProduction`, `getMyProductions`, `getTriangulation`.
- Kana discipline: the only kana shown is `target.displayForm` verbatim post-submit. Player input is displayed as typed romaji, never converted to kana (invariant 1 — **no kana derivation, full stop**).

## 8. Difficulty, scoring, self-norming

- No difficulty tiers in v1 — production has no wrong answers, only distance. The modality round-robin keeps sessions varied.
- **Self-norming contribution:** mean `similarity_score` per word is a new population statistic ("producibility") feeding the triangulation endpoint; over time it becomes a difficulty covariate for the *other* modes (a word players spontaneously reinvent is iconically transparent). **NIL-57 flag:** producibility joins the shared per-item stats source (D4 in the cross-linguistic spec's ledger).
- **Foil-distance synergy (NIL-58):** `PhonologyService.features()` + a distance function over feature vectors *is* the foil-distance engine over real words. NIL-58's implementation should import this service, not fork it.
- Leaderboard: production does **not** feed the core leaderboard (similarity ≠ correctness). Completion screen shows personal mean score + per-modality breakdown.

## 9. Frozen-copy needs

> **SUPERSEDED (2026-07-06, NIL-83):** strings below are drafts of record only. The frozen slate — including the language-neutral rewording of the prompt and parse helper — is **SPEC-view-designs.md §8**. Build from §8, not this list.

New strings in `experimentText.ts` (drafts; **Nils adjudicates before build** — production is a new instrument, its prompt wording is part of the measure):

- Prompt: "Invent a Japanese-sounding word for the meaning below. Type it in roman letters."
- Meaning line: "**{meaning}**" (bold, no quotes — house style).
- One-shot notice: "One try per word — your first instinct is the data." (register: F3 may tune, semantics frozen)
- Reveal: "The real word is **{displayForm}**."
- Parse-error helper: "That doesn't parse as Japanese sounds — stick to romaji like *gorogoro* or *pika*." (F3 tunes tone)

## 10. Proof steps (build session exit criteria)

1. `./mvnw test` green, including `PhonologyServiceTests` golden cases: `gosogoso`↔`gosogoso` = 100; `paq` normalizes to `pa-Q` (final-sokuon pitfall); `shittori`→`sittori` folding; `zyaazyaa` = 4 morae; `ngrk` → parse failure; plus a full-inventory smoke test asserting every seeded `romaji` normalizes without error.
2. `python3 scripts/generate_seed_sql.py --check` clean; `ddl-auto=validate` boots.
3. Live curl: `GET next` → prompt; `POST` valid → 201 with features; repeat → 409; garbage input → 400 `validationErrors`; `GET /api/research/triangulation` → row with `productionCount: 1`.
4. Frontend: `pnpm build` + `pnpm vitest run`; browser: full loop prompt → type → reveal → next; parse error shows helper without consuming the attempt.
5. Contract + checklist docs updated; Swagger lists all four endpoints.

## 11. Build cost

**1 backend session + 1 frontend session, flat.** No gates beyond the standard schema-change approval (one new table through the seed script) and the frozen-copy adjudication (a chat message, not a session). No NIL-57 dependency: nothing here prejudges mode dispatch, trials unification, or language modeling — worst case, a later `game_mode` column tags production sessions retroactively via the nullable `session_id`.

## 12. Risks

- **Score face-validity** — a player types something reasonable and gets a low score → the feature chips exist precisely to make the score legible; copy frames it as distance, not grade. Weights are v1; `scorer_version` makes tuning safe.
- **Anglophone orthography** (`shiny`, `splash`) → parser rejects politely; helper copy trains romaji fast. Watch early live data for rejection-rate spikes (log parse failures server-side at INFO with the failing input — it's [a-z]-constrained, safe to log).
- **Gloss leakage** — some glosses hint at the form ("munching, crunching" → anything crunchy scores well). Fine: that *is* iconicity working. No mitigation needed, just note it in research copy.
- **Prompt-order determinism vs boredom** — fixed selection rule means all users produce the same early words; acceptable for data density (fast per-word convergence stats), revisit post-launch.

## 13. Deferred to NIL-57

`PhonologyService` placement as shared engine (production / NIL-58 foil-distance / phoneme-shape features) · per-language phonology profiles · producibility joining the shared item-stats source · whether `/api/research/triangulation` eventually supersedes `/api/research/divergence` (keep both until the landing page picks).
