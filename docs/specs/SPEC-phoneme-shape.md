# SPEC — Phoneme-Shape / Template Reading (`TEMPLATE_READING`)

_F2 output (NIL-56), Fable 5, 2026-07-02. Grounded in the Punselie, McLean & Dingemanse structural coding scheme (`triangulating_iconicity/data/codingscheme.md`) and the F1 finding: **templates predict event structure, not iconicity magnitude** — reduplication/-ri/-Q/-N as readable cues to aspect and modality. Explicitly NOT a bouba-kiki voicing mode: F1 found no voiced-onset iconicity advantage (medians 3.05 vs 2.84, p = .61); voicing is excluded as a gameplay cue. Working title only; naming is F3 territory._

_**Revised 2026-07-02 (same-day, OSF-mining session; prior version in `archive/`):** §7 foil-distance ordering demoted to a recorded covariate — the thesis-pair validation found no distance→accuracy signal on real pairs._

## 1. Concept and vision test

The player learns to *read the shape of a word*. Not "what does gorogoro mean?" but "what does the **form** of gorogoro tell you before you know the meaning?" Each trial shows two real inventory words differing on one structural template and asks a property question — "Which word suggests something ongoing, repeating, or spread out?" — answerable purely from form. Feedback names the template and cites the real numbers (the F3 "hidden academic gems" slot: reduplication marks 69% of auditory ideophones in the norms).

**Vision test — new measurement angle: the iconic mapping made visible.** Choosing/Rating/Production all measure iconicity *through* whole words; this mode measures **feature-level structural knowledge** — do naive ears read reduplication as continuation, sokuon as abruptness, -ri as soft completion? Per-axis population accuracy is a live, game-side test of the cumulative-analogies claim (Punselie et al.: C_cumulative correlates with guessability, ~+0.28 in the dataset — verified this session). It also opens a transfer question no other mode can ask: does template training measurably improve subsequent Choosing accuracy? (Analysis-side join on timestamps; keep modes separated by `game_mode` for clean comparison.)

## 2. Ground truth and axis inventory (verified against the live seed, 2026-07-02)

Axes are the Japanese-relevant projections of the coding scheme (`C_iterative`, `C_closure`/`C_punctual`, plus the -ri aspect finding from F1):

| Axis | Form cue | Meaning tendency | Current 68-word inventory |
|---|---|---|---|
| `REDUP` | full reduplication (X·X) | continuous / iterative / distributed | 38 redup vs 30 non-redup |
| `Q` | sokuon (geminate / final -Q) | abrupt cutoff, stopping short | 13 words |
| `RI` | -ri suffix (non-redup) | soft, punctual completion; settled endpoint | 18 words |
| `N` | final moraic nasal | resonance, lingering | **4 words — deferred** (starved until expansion; the approved batch adds potsun, gakun, pokan) |

Feature membership is computed deterministically from `romaji` by the **same feature extractor specced in SPEC-free-form-entry §5 (`PhonologyService`)** — one engine, no hand-coding, no new data. Voiced-onset is extracted and stored for analysis but **never used as a gameplay cue**.

**The honesty constraint (design invariant for this mode):** templates are *tendencies* (69%, not 100%). Two consequences: (1) every candidate pair's correct answer is **human-adjudicated** — the pipeline proposes, Nils signs off per pair (workbook pattern, like `stimulus-expansion-signoff.xlsx`); a pair whose specific meanings flout the template is rejected even if the forms qualify. (2) Feedback copy is hedged and quantified ("usually", "in the norms, X% of…") — the mode teaches a statistical pattern, it never asserts a false universal.

## 3. Trial design

**Format: template-pick 2AFC** (house pair structure, real words — invariant-4 spirit; the core Choosing Task and its canonical wording are untouched, this mode has its own frozen strings):

1. Prompt: the axis question (frozen per axis, §8). No meanings shown.
2. Two word cards — kana (`display_form` verbatim, `--text-kana-card` sizing) + audio for each (shared per-word audio, invariant 2; playback via `StimulusPlayback`, both playable before answering).
3. Constraint on pair construction: the two words differ on the target axis; same modality where inventory allows (soft constraint); **must not differ on a second salient axis** (a REDUP trial must not accidentally also contrast Q — single-feature contrast is the clean measurement and the easy tier, see §7).
4. Answer → feedback card: correct/incorrect; the template segment **highlighted inside the displayed kana** — implemented strictly as literal-substring location within `display_form` as rendered (find the literal `っ`/`ッ` or trailing `り`/`リ` in the verbatim string; if not found, no highlight). **No kana conversion, no derivation — invariant 1.** Then the hedged tendency line + both glosses revealed (the payoff: now you know what they mean, and you got there from shape alone).

Session: 12 rounds (4 per active axis), order + left/right seed-derived (stream `shuffleSeed + 3`; registry to NIL-57).

## 4. Data model

Via `scripts/generate_seed_sql.py --check`, `ddl-auto=validate`, both generated — no hand-edited SQL (invariant 5):

```sql
CREATE TABLE word_features (
    ideophone_id BIGINT NOT NULL,          -- PK + FK ideophones
    redup BOOLEAN NOT NULL,
    sokuon BOOLEAN NOT NULL,
    final_n BOOLEAN NOT NULL,
    ri_suffix BOOLEAN NOT NULL,
    voiced_onset BOOLEAN NOT NULL,         -- analysis only, never a cue
    mora_count SMALLINT NOT NULL,
    PRIMARY KEY (ideophone_id),
    CONSTRAINT fk_features_ideophone FOREIGN KEY (ideophone_id) REFERENCES ideophones (id)
);

CREATE TABLE template_rounds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    feature_axis VARCHAR(10) NOT NULL,     -- 'REDUP' | 'Q' | 'RI'  ('N' reserved)
    word_a_id BIGINT NOT NULL,
    word_b_id BIGINT NOT NULL,
    correct_ideophone_id BIGINT NOT NULL,  -- adjudicated, not derived
    difficulty_tier VARCHAR(10) NOT NULL,  -- 'EASY' | 'HARD' (§7)
    is_practice BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_template_a FOREIGN KEY (word_a_id) REFERENCES ideophones (id),
    CONSTRAINT fk_template_b FOREIGN KEY (word_b_id) REFERENCES ideophones (id),
    CONSTRAINT fk_template_correct FOREIGN KEY (correct_ideophone_id) REFERENCES ideophones (id)
);
```

`word_features` rows are **emitted by the seed script** calling the same normalization/feature logic (port the §5 extractor rules into `generate_seed_sql.py` in Python — the Java `PhonologyService` and the Python generator must agree; add a cross-check proof step). `template_rounds` rows are seeded **only after the sign-off workbook returns**.

**NIL-57 flags:** `word_features` as a table vs columns on `ideophones` (table keeps the lexical core lean and regenerable — my lean); `template_rounds` vs the unified trials model (D2); Python/Java feature-extractor duplication (acceptable v1 duplication with a golden-file test, or generator calls a dumped JSON from the Java side — NIL-57 picks).

## 5. API surface (house conventions; gated on NIL-57 D1 mode dispatch)

Assuming D1 lands as `game_sessions.game_mode` (lean):

- `POST /api/game/sessions` — `gameMode: "TEMPLATE_READING"`; `conditionName` n/a for this mode; optional `axes: ["REDUP","Q","RI"]` filter (default all active). 400 on unknown axis.
- `GET /api/game/sessions/{uuid}/rounds/next` — template round DTO:

```json
{ "completed": false, "roundId": 501, "gameMode": "TEMPLATE_READING",
  "featureAxis": "REDUP", "promptKey": "REDUP",
  "left":  { "ideophoneId": 12, "displayForm": "...", "stimulusUrl": "..." },
  "right": { "ideophoneId": 31, "displayForm": "...", "stimulusUrl": "..." },
  "timing": { "fixationMs": 500, "preChoiceDelayMs": 1500 } }
```

  (No glosses in the round DTO — meanings are the *feedback*, not the stimulus. `promptKey` maps to the frozen axis question client-side.)
- `POST /api/game/sessions/{uuid}/answers` — unchanged `{roundId, selectedIdeophoneId, responseTimeMs}`; answer response gains `templateNote` fields (axis, highlight token, tendency stat key) for the feedback card, plus both glosses.
- `GET /api/research/templates` (public) — per axis: `{axis, answers, correct, accuracy}` — the "which templates do naive ears read?" chart. Population-aggregate, divergence precedent (null-safe counts).

Files: `service/TemplateRoundService.java`, `repository/TemplateRoundRepository.java` + `WordFeaturesRepository.java`, `dto/TemplateRoundResponse.java` / `TemplateSummaryResponse.java`, controller integration per D1, `TemplateHttpTests.java`, contract + checklist updates.

## 6. Frontend (vs 27C view-state shell + tokens.css)

- `App.tsx`: `AppView` gains `"template"`; home-surface card.
- New `src/components/TemplateTrial.tsx` (+ test) — structurally a `TrialPlayer` sibling: two `IdeophoneCard`s, but the prompt block renders the axis question instead of the meaning lines, and the feedback panel renders the highlight + tendency note.
- Kana highlight: wrap the located literal substring in a span using `--accent` text on `--surface-card` (AA-safe per tokens.css notes); zero string transformation.
- Axis chip above the prompt in `--tracking-wide` small caps ("SHAPE: REPETITION").
- `src/api/types.ts` + `client.ts`: template round/answer/summary types and calls; `researchFlavor.ts` gains axis-keyed tendency notes (the quantified F1/norms stats live here as data, sourced from `thesis-facts.md`-style vetted numbers only).

## 7. Difficulty, scoring, self-norming, foil-distance

- **Tiers at seed time:** `EASY` = single-axis contrast + same modality + the axis's prototypical direction (e.g. redup word is auditory — F1: reduplication marks 69% auditory / 67% haptic vs 33% visual); `HARD` = single-axis contrast but cross-modality or atypical direction. Multi-axis contrasts are excluded outright (measurement noise, not difficulty).
- **Foil-distance (NIL-58) — recorded covariate, NOT an ordering principle (revised 2026-07-02):** the assumed dial (high feature distance = easier) failed validation on the thesis pairs — SPEC-free-form-entry §5 feature distance vs live pair accuracy: rho = −.155, p = .41, n = 30 (trend in the *wrong* direction), and mora-Levenshtein is degenerate on real contrastive pairs (28/30 at maximum distance). Evidence: `research/f1-dataset-profile.md` §4 (2026-07-02). The pipeline still computes and stores `foil_distance` per pair (cheap, from the shared `PhonologyService`) so this mode's live data can test whether distance matters *when the features themselves are the stimulus* — a plausibly different regime from meaning-2AFC — but **within-tier ordering falls back to the tier logic above** (prototypical-direction and same-modality pairs first, then alphabetical-by-pair-id for determinism) until live data says otherwise.
- **Self-norming:** live per-round accuracy supersedes tier placement at n ≥ 30 (shared item-stats source, NIL-57 D4).
- **Scoring:** 1 point per correct; per-axis sub-scores at completion ("you read repetition 5/6, abruptness 2/4"). No core-leaderboard feed (different chance structure); mode-local later.

## 8. Frozen-copy needs (all adjudication-gated)

Axis questions (drafts — these are the instrument; Nils adjudicates wording before the workbook round):

- `REDUP`: "Which word suggests something ongoing — repeating or spread out?"
- `Q`: "Which word suggests something that stops short — abrupt and clipped?"
- `RI`: "Which word suggests a motion that settles softly to an end?"

Feedback tendency lines (hedged register, numbers vetted against `thesis-facts.md`/F1 before freezing), e.g.: "Doubling a word's halves usually signals repetition or spread — in the Japanese norms, about two-thirds of sound- and touch-words reduplicate." Plus per-pair gloss reveals (existing gloss data). Chrome/microcopy tone: F3.

## 9. Pipeline and sign-off (the hidden cost — a required pre-build pass)

One scripted pass (Python, alongside `generate_seed_sql.py`): extract features for all 68 inventory words → enumerate candidate pairs per axis under §3 constraints → propose correct answers + tier + foil-distance → emit `template-rounds-signoff.xlsx` (Candidates + Pairs sheets, F1 workbook pattern). **Nils reviews every pair** (reject template-flouting meanings). Signed-off pairs → seeded. Target: ≥ 8 pairs per axis post-rejection; if an axis lands < 6, it ships dark (data model unaffected — `axes` filter simply omits it).

## 10. Proof steps

1. Feature parity: Python extractor output for all 68 romaji == Java `PhonologyServiceTests` golden file (same booleans, same mora counts).
2. `python3 scripts/generate_seed_sql.py --check` clean; `ddl-auto=validate` boots; `./mvnw test` green (round serving, answer judging, axis filter validation, 409/400 semantics).
3. Live curl: session with `gameMode: TEMPLATE_READING` → rounds cycle axes; answers judged against adjudicated `correct_ideophone_id`; `GET /api/research/templates` aggregates.
4. `npm run build` + `vitest`; browser: full 12-round session; highlight renders on a っ word and a -り word; no highlight crash on a word where the literal isn't found.
5. Sign-off workbook archived to `research/` with decisions recorded.

## 11. Build cost

Pipeline + sign-off pass (script + Nils's review — cheap in session time, but a real calendar dependency) + **1 backend session + 1 frontend session**. Code-wise within the ≤ 1+1 criterion **only after** NIL-57 D1 and the sign-off round; both are external to the build slot.

## 12. Risks

- **Ground-truth softness** — a signed-off pair can still be argued; mitigation: hedged copy + reject liberally at sign-off + live accuracy flags pathological pairs (self-norming catches what adjudication missed).
- **Teaching-to-the-test** — template training may raise Choosing accuracy and drift the core measure. Reframed as the *transfer finding* (a feature, measurable 