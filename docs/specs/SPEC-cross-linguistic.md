# SPEC - Cross-Linguistic Mode (`CROSS_LINGUISTIC`)

_F2 output (NIL-56), Fable 5, 2026-07-02. Specced first because its multi-language data model drives the architecture furthest - this document is a primary **input to NIL-57** (F6 architecture session). Build is sequenced **after** NIL-57. Working title only; player-facing naming is F3 territory._

_**Revised 2026-07-02 (same-day, OSF-mining session; prior version in `archive/`):** licensing gate G1 resolved (CC BY 4.0, NIL-59), G3 recovery attempt exhausted, §13 attribution list added, one D4 nuance escalated to the NIL-57 ledger._

## 1. Concept and vision test

The conceptual spine, universalized: *how far does iconicity carry you in a language you've never heard?* The player hears an ideophone from Siwu, Ewe, Semai, Korean, or Japanese and picks between two English meanings. Japanese is the bridge (the app's home language); the other four are languages virtually no player will know - so every correct answer above chance is iconicity, not convention.

**Vision test - new measurement angle:** cross-linguistic generalization. Choosing = what your ear knows in Japanese; this mode measures whether that skill *transfers*. Uniquely, every item carries a **published guessability score** (Punselie, McLean & Dingemanse 2024, *Open Mind*), so player accuracy can be anchored against the original study population - per item and per language. That yields two stats no other mode has: (a) our-players-vs-published-participants per language, and (b) the language gradient itself (published means: Japanese .666 > Siwu .594 > Ewe/Korean .576 > Semai .518 - does it replicate live?). This is a measurement angle, not just content.

## 2. Source data (verified 2026-07-02, this session)

Root: `research/data/ideophone-dataset/triangulating_iconicity/`

- **Load-bearing file:** `data/ideophones_coded_guessed_rated.csv` - 239 rows × 49 cols. One row per ideophone: `ideophone` (form, verbatim), `meaning` (English gloss, all 239), `meaning_NL` (Dutch), `language`, `category`, `study` (`Language` 201 / `Collabra` 38), `filename`, `score` (guessability 0.10–0.966, mean .60), `rating` (subjective iconicity, observed range 1.33–4.72), full structural coding (`F_*`, `M_*`, `C_*`, `C_cumulative` 0–5).
- **Languages:** Japanese 81 (incl. all 38 Collabra), Korean 46, Ewe 42, Semai 38, Siwu 32.
- **Semantic categories:** Motion 48, Sound 47, Shape 47, Texture 43, ColorVisual 37, Other 17 (Other = Japanese/Collabra only). **These are not our modality taxonomy - never conflate** (see §4).
- **Audio (241 .wav):** join rules verified with 0 misses this session:
  - `study == "Language"` → `stimuli/Language_audiofiles/{filename}_org.wav` (203 files; **2 orphan wavs** not referenced by any row - exclude at ingestion, log them).
  - `study == "Collabra"` → `stimuli/Collabra_audiofiles/{ascii-lowercase(ideophone, spaces stripped)}.wav` (38 files, e.g. `tsuru tsuru` → `tsurutsuru.wav`).
- **Orthography as shipped:** Japanese in romaji (`zaa zaa`), Korean romanized with breves (`ŏngŏng`, `kkŏkkŭlkkŏkkŭl`), Siwu/Ewe/Semai in IPA-flavored Latin (`bɔyibɔyi`, `ɟrweetɟrwɒɒt`, `ŋɛrŋorŋɛrŋorŋɛrŋor`). All renderable as Latin text; **store and render verbatim** (invariant-1 principle extended: no transliteration, no derivation).
- **Foil caveat:** `meaning_NL_opposite` exists only for the 38 Collabra rows, and in Dutch. The original 2AFC foil translations for the 201 Language-study rows are **not in this repo** (see gate G3).
- **Licensing - RESOLVED (NIL-59, 2026-07-02):** dataset and both source studies' stimuli are **CC BY 4.0** - hosted redistribution of the 241 wav is permitted **with attribution** (§13). The OSF export `puneslie2024.zip` was verified a byte-identical mirror of this clone (383/383 files, md5), so provenance is doubly anchored.

## 3. Trial design

**Format: meaning-pick.** One audio stimulus, two English meanings, pick one. This is the published task's format, so the guessability anchors stay maximally valid; it needs one audio per trial (no pairing constraint over a 239-item inventory); and it avoids inventing pair-level structure the dataset doesn't have. The core Choosing Task's word-pick format and its canonical wording (invariant 3) stay untouched - this mode gets its own frozen strings (§8).

Trial flow (mirrors `TrialPlayer` phases):

1. Language chip visible from the start (e.g. "Siwu - spoken in Ghana"). Hearing-before-reading: audio auto-plays once, replay button always available (reuse `StimulusPlayback`).
2. The word form is displayed verbatim (`display_form`) under the play control - secondary visual, the audio is the stimulus.
3. Two gloss cards (positions seed-derived), question line above them.
4. Answer → feedback card: correct/incorrect, both glosses resolved (which word the foil gloss belongs to), published-guessability easter-egg line ("64% of the original study's participants got this one" - F3 microcopy slot).

**Foil source (default, pending gate G3):** the gloss of another ideophone from the **same language and same category** (a real word's real meaning - the invariant-4 spirit carried to gloss space, though note invariant 4 formally governs core-task content only). Foil assignment is precomputed in the seed, not drawn at runtime (reviewable, sign-off-able); pipeline rejects foils sharing a content word with the target gloss (e.g. two "rough surface" Texture glosses) and emits the full list for human review.

**Session shape:** 15 rounds = 3 per language, drawn deterministically from the session's `shuffle_seed` (stream `new Random(shuffleSeed + 2)` - practice already claims `+1`; NIL-57 should codify the stream registry). Gloss top/bottom position from the same stream.

## 4. Data model (forward-compatible; NIL-57 input)

This is the mode that forces **language as a first-class entity**. Proposed target shape (all schema via `scripts/generate_seed_sql.py --check`, `ddl-auto=validate`; **no schema work before NIL-57 adopts this or amends it**):

```sql
CREATE TABLE languages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    iso_code VARCHAR(8) NOT NULL UNIQUE,   -- ISO 639-3: jpn, kor, ewe, akp (Siwu), sea (Semai)
    name VARCHAR(50) NOT NULL,
    family VARCHAR(50),                    -- Japonic, Koreanic, Atlantic-Congo, Austroasiatic
    player_note VARCHAR(255),              -- "spoken in Ghana" etc. (F3 copy)
    PRIMARY KEY (id)
);
```

- `ideophones.language_id BIGINT NOT NULL` FK → `languages`, backfilled to `jpn` for all existing rows. This single column is the forward-compatibility keystone; it costs nothing for existing modes.
- `ideophones.semantic_category VARCHAR(20) NULL` - the dataset's category (Sound/Motion/Shape/Texture/ColorVisual/Other). **Distinct from `modality`**; `modality` stays NULL for cross-linguistic items in v1 (the Iida & Akita modality taxonomy is Japanese-normed; mapping categories onto it would be derivation - don't).
- Provenance/licensing per stimulus (named in NIL-57's brief) - proposed:

```sql
CREATE TABLE stimulus_sources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ideophone_id BIGINT NOT NULL,          -- FK ideophones
    dataset VARCHAR(50) NOT NULL,          -- 'triangulating_iconicity'
    source_study VARCHAR(20),              -- 'Language' | 'Collabra'
    published_guessability DECIMAL(5,4),
    published_rating DECIMAL(5,4),
    license_status VARCHAR(20) NOT NULL,   -- 'PENDING' | 'CLEARED' | 'DENIED'
    citation VARCHAR(255),
    PRIMARY KEY (id), UNIQUE (ideophone_id, dataset)
);
```

- Rounds: new `xl_rounds (id, target_ideophone_id, foil_ideophone_id, difficulty_tier, is_practice)` - the foil row donates its gloss. **NIL-57 decision D2:** whether `arena_rounds` generalizes into a mode-discriminated `trials`/`round` model instead of per-mode tables; this spec works either way, the columns are what matters.
- Column reuse on `ideophones` for XL rows: `kana` and `display_form`/`canonical_form` hold the dataset form verbatim (utf8mb4 handles ɟ/ŋ/ɛ/breves); `romaji` holds the ascii-folded join key; `gloss` holds the cleaned English gloss (gate G2); `canonical_script` = `'LATIN'`; `stimulus_file` = `xl/{iso_code}/{basename}.wav` (see G5); `modality` NULL.
- **Word identity across sources:** 38 Collabra items overlap the Japanese core inventory space (6 thesis words; 8 F1-approved expansion candidates carry these anchors). Keep XL rows as **separate ideophone rows** (different audio, different provenance); cross-source identity (a `lexeme` concept) is **NIL-57 decision D3** - do not merge at ingestion.

## 5. API surface (house conventions; executable once NIL-57 lands mode dispatch)

**NIL-57 decision D1 (mode dispatch)** gates everything here. Lean recommendation to NIL-57: additive `game_sessions.game_mode VARCHAR(30) NOT NULL DEFAULT 'CHOOSING'` + a `gameMode` request field, keeping one session model for all modes. Assuming D1 lands that way:

- `POST /api/game/sessions` - additive optional `gameMode: "CROSS_LINGUISTIC"`; `conditionName` not required for this mode (validation branch per mode in `GameSessionService`); optional `languageScope: ["akp","ewe"]` (default: all five). 400 on unknown codes with `validationErrors`.
- `GET /api/game/sessions/{uuid}/rounds/next` - same endpoint, mode-aware serving; XL round DTO:

```json
{
  "completed": false, "roundId": 301, "gameMode": "CROSS_LINGUISTIC",
  "language": { "isoCode": "akp", "name": "Siwu", "playerNote": "spoken in Ghana" },
  "word": { "ideophoneId": 412, "displayForm": "sɔdzɔlɔɔ", "stimulusUrl": "/stimuli/xl/akp/Siwu_23_Shape_org.wav" },
  "glossTop": { "ideophoneId": 412, "gloss": "..." },
  "glossBottom": { "ideophoneId": 415, "gloss": "..." },
  "timing": { "fixationMs": 500, "preChoiceDelayMs": 1500 }
}
```

  (`glossTop/Bottom` carry the owning `ideophoneId` so the answer API stays id-based like the core task.)
- `POST /api/game/sessions/{uuid}/answers` - unchanged shape: `{roundId, selectedIdeophoneId, responseTimeMs}`; correct iff selected id == target id. Same 400/409 semantics as the contract.
- `GET /api/xl/languages` - public, list with item counts (drives the mode's language picker).
- `GET /api/research/xl/summary` - public, per-language: `{isoCode, name, playerAccuracy, playerGuessCount, publishedMeanGuessability}` - the portfolio chart ("you vs the 2024 study"). `null` accuracy when count 0, per the divergence precedent.

Files (backend, following `Rating*` naming): `controller/XlController.java` (or fold into `GameSessionController` per D1), `service/XlRoundService.java`, `repository/XlRoundRepository.java` + `LanguageRepository.java`, `dto/XlRoundResponse.java`/`LanguageResponse.java`/`XlSummaryResponse.java`, mappers, `XlHttpTests.java`, plus `docs/backend-contract.md` + `docs/backend-grading-checklist.md` updates.

## 6. Frontend (vs the 27C view-state shell + tokens.css)

- `App.tsx`: `AppView` gains `"xl"`; entry card on the 27C home/mode-select surface.
- New `src/components/CrossLinguisticTrial.tsx` (+ test) reusing `StimulusPlayback`; new `GlossCard` (an `IdeophoneCard` sibling: gloss text at `--text-md`–`--text-lg`, not kana sizing).
- Language chip: small-caps label using `--tracking-wide` + `--ink-muted`; **no flags** (languages ≠ countries; Siwu/Semai have none).
- **Font gate (G4):** `--font-stimuli` (LINE Seed JP) will not cover ɟ/ŋ/ɛ/ɔ/ŏ/ŭ. Add a `--font-stimuli-latin` token with an IPA-capable face (candidate: self-hosted Noto Sans, OFL - consistent with the token file's self-hosting policy) and use it for XL word forms. Long Semai forms (`ŋɛrŋorŋɛrŋorŋɛrŋor`) need a clamp below `--text-kana-card` sizing - add `--text-xl-form: clamp(1.6rem, 5vw, 2.4rem)`.
- `src/api/types.ts`: `XlRoundResponse`, `LanguageInfo`, `XlSummary`; `client.ts`: `getXlLanguages`, `getXlSummary` (+ mode param on `startSession`).

## 7. Difficulty, scoring, self-norming

- **Difficulty prior = published guessability** (the only mode with pre-normed difficulty): tiers by observed quartiles - easy ≥ .75 (~60 items), medium .49–.75, hard < .49 (~60 items). Stored as `xl_rounds.difficulty_tier` at seed time.
- **Self-norming loop:** once an item has ≥ 30 live answers, live accuracy supersedes the prior for tier placement. Same mechanism the Ladder needs - **NIL-57 decision D4:** one shared per-item stats source (query/view over `player_answers`) consumed by all modes, not per-mode reimplementations.
- **Foil-distance dial (NIL-58):** not applicable in v1 (foils are glosses, not word forms). Future option: score gloss-pair semantic distance for a difficulty knob - out of scope, noted for NIL-58's backlog.
- **Scoring:** correct = 1 point (session = 15 rounds); per-language sub-scores surface at completion ("iconicity passport": 5 languages × accuracy). Leaderboard: XL sessions do **not** feed the core leaderboard (different task, different chance structure - same reasoning as practice exclusion); mode-local best-session board later if wanted.

## 8. Frozen-copy needs (adjudication gates, thesis-style register)

New strings for `experimentText.ts` (drafts; **Nils adjudicates before build**, F3 owns surrounding chrome):

- Listen line: "Listen to the word. Click to replay."
- Question: "Which meaning do you think it has?"
- Gloss buttons: the two glosses verbatim, no quotes.
- Feedback reveal: "**{displayForm}** means **{gloss}**." + foil resolution line: "The other meaning belongs to **{foilForm}**."
- **Gloss cleanup (G2):** dataset glosses contain artifacts (`fluffy+furry+`, `being oily`, parenthetical examples). Pipeline emits a 239-row `gloss_display` proposal sheet (same pattern as `stimulus-expansion-signoff.xlsx`); cleanup is normalization only - no semantic drift; Nils signs off per row.

## 9. Open gates (blocking order)

- **G1 - Licensing: RESOLVED 2026-07-02 (NIL-59).** CC BY 4.0 with attribution (§13). No longer blocks anything; the W30 deadline pressure is gone.
- **G2 - Gloss cleanup sign-off** (239 rows, workbook pattern).
- **G3 - Foil provenance: recovery attempt exhausted 2026-07-02.** The 2016 OSF supplements contain **no trial-level data and no foil-translation lists** (S1 = item-list PDF, S2 = audio, S3 = resynthesis rules - verified against the extracted archive). The published-supplements route is closed; remaining option is contacting the authors (Nils's call, low priority). Default stands: same-language same-category peer glosses, difficulty anchors treated as priors that self-norming supersedes.
- **G4 - Latin/IPA display font** token + glyph-coverage check.
- **G5 - Audio serving decision:** keep `.wav` (browser-native, ~larger) vs transcode to `.m4a` for consistency with `/stimuli/audio/` and size. Transcoding is asset regeneration → explicit approval gate per workflow rules. Path convention `/stimuli/xl/{iso}/...` either way (the core `<3-char>-<romaji>.m4a` convention stays Japanese-core-only; invariant 2 untouched).
- **G6 - NIL-57 decisions D1–D4** (mode dispatch, trials model, lexeme identity, shared item-stats).

## 10. Build cost and sequencing

| Session | Scope | Est. |
|---|---|---|
| Ingestion/pipeline | CSV→seed script extension (languages, ideophones rows, stimulus_sources, xl_rounds + foil assignment + review sheets), wav copy/transcode | 1 backend session (gated: G2/G3/G5, seed rewrite approval) |
| API | endpoints + tests per §5 | 1 backend session (gated: NIL-57) |
| UI | §6 components + font token | 1 frontend session |

**~3 sessions + external licensing - exceeds the ≤ 1 backend + 1 frontend build-pick criterion.** This spec's near-term job is to drive NIL-57, not to be built first.

## 11. Risks

- ~~Licensing denial after build effort~~ - resolved (G1); only the attribution obligation remains, tracked in §13.
- Gloss quality undermines face validity → G2 sign-off is non-skippable.
- Peer-gloss foils make published anchors approximate → document in research copy; anchors are priors, self-norming is the truth source.
- IPA glyph fallback renders tofu on some platforms → G4 verification in proof steps (browser screenshot across Win/Android).
- Scope creep magnet (per-language leaderboards, more datasets) → v1 is exactly §3–§7.

## 12. Deferred to NIL-57 (explicit ledger)

D1 mode dispatch (`game_mode` column vs per-mode session models) · D2 unified trials table vs per-mode round tables · D3 cross-source word identity (lexeme) · D4 shared per-item live-stats source for self-norming · seed-stream registry for `shuffleSeed + k` · placement of `stimulus_sources` (per-stimulus vs per-ideophone) · whether `semantic_category` and `modality` unify under one typed taxonomy table.

**D4 granularity (escalated 2026-07-02, not resolved here):** mclean2023 tested two forms under two concepts each and got wildly different guessability (iraira .70 vs .39; nebaneba .38 vs .20) - guessability is a property of the form–meaning **pairing**, not the word. D4's "per-item" stats source should therefore key on **round/pairing**, with word-level stats as a derived aggregate, not the primitive. Evidence in `research/f1-dataset-profile.md` §2 (2026-07-02 section).

## 13. Attribution (CC BY 4.0 obligations - ship with the mode AND on the landing page)

All three sources below are Creative Commons Attribution 4.0 International. Hosting, redistribution, and adaptation are permitted with attribution. Frozen credit lines (F3 may restyle, semantics fixed):

- Punselie, S., McLean, B., & Dingemanse, M. (2024). The Structure of Sound Symbolism in Japanese, Korean, Ewe, Semai and Siwu Ideophones. *Open Mind*. Data & audio: `triangulating_iconicity` (CC BY 4.0).
- Dingemanse, M., Schuerman, W., Reinisch, E., Tufvesson, S., & Mitterer, H. (2016). What sound symbolism can and cannot do: testing the iconicity of ideophones from five languages. *Language*, 92(2), e117–e133. Stimuli (CC BY 4.0).
- Lockwood, G., Dingemanse, M., & Hagoort, P. (2016). How iconicity helps people learn new words: neural correlates and individual differences in sound-symbolic bootstrapping. *Collabra*, 2(1). Stimuli & data (CC BY 4.0).

Landing-page note: a compact credits block linking the three papers + the OSF/GitHub repositories; the per-trial feedback card does **not** need per-item attribution (dataset-level credit satisfies CC BY 4.0), keeping trial chrome clean.
