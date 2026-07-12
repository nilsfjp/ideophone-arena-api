# SPEC - Essence review: keep/cut/simplify ledger + XL-readiness annex (NIL-84)

_Fable 5 essence-review session, 2026-07-07. **Status: ADJUDICATED - Nils ruled every row in chat 2026-07-07; all leans adopted as written** (one judgment callout each on D3 and I2, recorded in-row). Method per the NIL-84 launcher: every cut names the artifact, its grep-verified dependents, why it is sediment rather than structure (traced to the project thesis: "explore how far iconicity carries you before convention takes over"), and its removal cost + rider assignment. Every suspicious keep carries one sentence of justification. **Verdicts do not edit repos** - they land as riders on the executing sessions named per row; Linear comments posted 2026-07-07._

_Pre-flight (launcher requirement): api HEAD `1ab2a33` (NIL-44 README committed, gorilla-tidy CSVs untracked), web HEAD `ef2cc0e` (NIL-80 v1.1 committed). `git diff --ignore-cr-at-eol` empty on both - mount-side "M" noise is the known CRLF phantom. Trees read as committed._

_Ground truth read this session: full DDL (`ideophone_arena.sql`, post-M2: languages/words/presentations/pairings/trials/game_sessions/player_answers/ratings), all 15 live endpoints, `tokens.css` (69 tokens) with per-token consumer counts, `package.json`, repository fences, registration validation. Canon read: ARCHITECTURE.md, UI-SYSTEM.md (583 ln), SPEC-view-designs.md, SPEC-stats-dashboard.md, SPEC-four-floor-ladder.md, SPEC-free-form-entry.md, SPEC-hosting.md constraints honored (no verdict orphans the compose→Caddy path)._

## 0. Verdict summary

18 ledger rows + 6 README flags + 11 XL-annex items. **Cuts: 5** (TEXT_ONLY, ScriptType, difficultyLevel, scriptCode-in-DTO, Zen Kaku fallback). **New guard: 1** (reserved username prefixes at registration - the review's sharpest finding). **Keeps with justification: 9. Doc-fixes applied to planning canon this session: 4. Invariant amendments (Nils's, adopted): invariant 1 rewording + the W1/W2 registry.** No verdict threatens W30; the only contract-touching cut (A3) folds into NIL-41/M1, which already touches session start.

## 1. Walk 1 - Architecture ledger

| # | Artifact | Dependents (grep-verified) | Sediment vs structure | Verdict + rider |
|---|---|---|---|---|
| A1 | `ConditionName.TEXT_ONLY` + `GameSession` entity default + DDL default `'TEXT_ONLY'` | 3 test files (fixture trick: sessions with no presentations); session-start validation **rejects** it ("Unsupported conditionName") | Sediment: an unreachable enum value no live row can hold; pre-thesis-alignment residue | **CUT.** Remove enum value + both defaults (column stays NOT NULL, no default or a real condition); refit the 3 test fixtures. Rider: **NIL-41** (api schema session - rides M1's seed regen) |
| A2 | `ScriptType` enum | Zero references outside its own file | Dead code | **CUT.** Delete file. Rider: **NIL-41** |
| A3 | `difficultyLevel` - `game_sessions` column + `StartSessionRequest`/`GameSessionResponse`/`RoundResponse` fields + validation ("Only difficulty level 1 is supported for the current demo") | GameService start path; frontend echoes it | Sediment: carries zero information; trials shed `difficulty_level` at M2 because difficulty is a pairing concern (ADR-3/5) | **CUT** from schema + contract. **Binding sub-ruling: the ladder must NOT overload this field - floor selection gets its own explicit parameter** (e.g. `floor`/modality on session start). Rider: **NIL-41** (folds into M1, which touches session start anyway; contract changelog entry; web sweep rides NIL-42) |
| A4 | `trials.feature_axis` | Entity only; no query/service consumer | Speculative (Word Anatomy, deferred mode) - but nullable, invisible, and its consuming build lands post-W30 where an ALTER costs triple (§13 regime note) | **KEEP** (suspicious-keep). Revisit trigger: template mode formally dropped from the roadmap ⇒ cut then |
| A5 | `words.semantic_category` | Entity only | Same class as A4 (XL/M6 consumer) | **KEEP**, same trigger (XL dropped ⇒ cut) |
| A6 | `languages.family`/`player_note` · `pairings.foil_distance`/`signoff_ref`/`approved_at`/`difficulty_prior` | Entity-only today | Priced, imminent consumers: **NIL-86** computes `foil_distance` (validation-only fence stands, ρ=−.155 n.s.) and fills the sign-off trail; ADR-1's XL copy home | **KEEP** |
| A7 | `presentations.script_code` exposure in the round DTO (`GameMapper:153`) | Web repo has **zero** `scriptCode` consumers | Column = legit seed provenance (keep); the DTO field is a served fact nobody reads | **SIMPLIFY:** drop the DTO field at the next contract-touching session, keep the column. Rider: **NIL-41** (contract changelog) |
| A8 | `GET /api/research/thesis/divergence` | Zero frontend consumers | Not sediment: the live reconciliation instrument (68.6/64.2/59.7) and the thesis cohort's honest public view; documented in the contract | **KEEP** |
| A9 | `words.kana` next to `canonical_form` | Consumed in feedback DTOs (`GameMapper`) | Not duplication: **36/68 rows differ** (hiragana lemma vs canonical-script form incl. chōonpu variants じゃあじゃあ/じゃーじゃー) | **KEEP** |
| A10 | **Reserved username prefixes unenforced at registration** | `RegisterRequest` validates only `@NotBlank @Size(3,50)`; `thesis_p%`/`browser_loop_%` fences exist only in read queries | Integrity hole, not sediment: a live player registering `thesis_p37` **pollutes `GET /api/research/thesis/divergence`** (inverted predicate includes them); `browser_loop_x` silently vanishes from live aggregates | **NEW GUARD (adopted):** registration rejects reserved prefixes (400, contract-style validationError). Rider: **NIL-41** (next api session); hard gate in spirit before public deploy |
| A11 | Confirm-keeps (batch) | - | `trials.correct_word_id` documentation-role for CHOOSING (NIL-54 reconstruction; type-scoped rule) · single `trials` table + `round_type` (ADR-3 structure) · `pairings.modality` denorm (queryable invariant 4) · `include_practice`/`practice_answered` (live feature) · HealthController (compose/Caddy healthcheck, SPEC-hosting) · Rider A fences verified live at HEAD | **KEEP all** |

## 2. Walk 2 - Design-system ledger

| # | Artifact | Finding | Verdict + rider |
|---|---|---|---|
| D1 | `--motion-reveal` token | Zero consumers; the §7 feedback-entrance choreography (opacity + translateY over `--motion-reveal`) was never implemented | **KEEP token; choreography lands with its first consumer** - NIL-62 FE's reveal card (its spec §2.3 expects it). FeedbackPanel backfill optional same session. Rider: **NIL-62** |
| D2 | Zen Kaku Gothic New - 4 subset imports in `main.tsx` (incl. 2 Japanese subsets) + `--font-body` fallback slot | Dead weight: sits behind self-hosted LINE Seed JP in the body stack; only fetched if LINE Seed misses glyphs it doesn't miss | **CUT** imports + stack entry. Proof: screenshot diff + full battery. Rider: **NIL-42** (next web session) |
| D3 | sonner dep + mounted `Toaster` | Zero `toast()` calls anywhere | **KEEP staged (Nils, option a) with a hard deadline: wire auth-expiry/network toasts at W30 deploy polish; still unwired at deploy ⇒ CUT the dep.** Riders: note on **NIL-47** (deploy runbook carries the cut-if-unused check) |
| D4 | shadcn `Dialog` staged, untriggered | V9 consumer imminent (28B floor intro, H7 idiom rider) | **KEEP** |
| D5 | Reserved XL tokens `--text-xl-form` (0 uses), `--font-stimuli-latin` (fallback-only stack) | Ruled reservations (UI-SYSTEM §3.3), zero cost | **KEEP** - listed in the XL annex |
| D6 | UI-SYSTEM staleness ×3 | §10.2 floor-order parenthetical (V1's pending fix) · §10.2 XL name (Polyglot Challenge adopted 2026-07-07) · §16's "three @fontsource packages remain installed" (they are **gone** from package.json) | **DOC-FIX - applied to planning canon this session** (planning-folder jurisdiction). Repo re-copy rides **NIL-42**. SPEC-view-designs §5 D3 (XL name) marked resolved in the same pass |
| D7 | Token inventory overall | 69 tokens: all consumed, reserved-by-ruling, or handled above | **PASS** - the design system earns its place; no further rows |

## 3. Walk 3 - Invariants + registry

| # | Item | Finding | Verdict |
|---|---|---|---|
| I1 | Invariant 1 wording | Stale twice: names `ideophones` (dead table post-M2) and "kana" (Japanese-shaped) | **AMENDED (Nils, 2026-07-07):** "`words.canonical_form` / `presentations.display_form` are the single source of truth for displayed word forms. The frontend renders them verbatim and never converts, detects, or derives forms." Semantics untouched. Nils updates the project-instruction copy (outside Claude's reach); CLAUDE.md decoder-ring row updated this session |
| I2 | Invariant 2 - `<3-char prefix>-<romaji>.m4a` path | Nils asked why keep the prefixes. Answer of record: **nothing at runtime parses them** (server serves `words.stimulus_file` verbatim behind `/stimuli/**`; web builds no paths - grep-verified). The prefix is data-at-rest naming encoding pair identity + script provenance (ADR-6's pairing backfill keyed on it); cutting = renaming ~180 committed audio assets + seed regen for zero behavioral change. XL never inherits it (`/stimuli/xl/{iso}/` reserved, ADR-7) | **KEEP**; invariant text gains the scope clause "(core Japanese inventory only)" at M6 - annex X4 |
| I3 | Invariant 3 - canonical question wording | Language-neutral already; NIL-83's language-neutrality ruling extends the same principle to all player copy | **KEEP verbatim** |
| I4 | Invariant 4 - real contrastive same-modality core pairs | Strengthened to structure at M2 (`pairings.is_core` + generator assertions) | **KEEP**; NIL-86's gate unchanged |
| I5 | Invariant-numbering drift | "Invariant 5" = seed-via-generator in project canon but reserved-slot geometry in every web spec; "invariant 7" (pre-feedback a11y tree position-only) cited in UI-SYSTEM §5/§8 exists in no registry | **REGISTRY (adopted).** Project invariants 1–5 unchanged. Web instrument invariants formally named: **W1** = reserved-slot geometry / timing-validity constants (UI-SYSTEM §2.4) · **W2** = pre-feedback accessibility tree is position-only (no word identity before the verdict). Specs cite W1/W2 going forward; existing "invariant 5/7" references in web docs read as W1/W2 (doc-sweep rides each doc's next touch, no dedicated pass) |

## 4. XL-readiness annex (execute at M6 unless marked otherwise)

Every Japanese-shaped assumption found, with grep evidence. None blocks current work; items marked **M6** execute with the XL ingest migration.

| # | Assumption | Evidence | Disposition |
|---|---|---|---|
| X1 | **`words.kana NOT NULL`** blocks non-Japanese words | DDL: `kana VARCHAR(50) ... NOT NULL`; ARCHITECTURE ADR-0 shape has no nullability note for XL | **M6 (schema): make `kana` nullable** (or rename to a neutral `native_form` - M6's call). The one real defect the annex found in the adopted shape |
| X2 | `lang="ja"` hardcoded | 8 web files (FeedbackPanel, Landing, StimulusDisplay, Observatory panels + tests) | **M6:** `langCode` from API (words.language) drives the attribute; rendering attribute only, zero string manipulation |
| X3 | Invariant-1 "kana" wording | - | Resolved now by the I1 amendment (language-neutral wording) |
| X4 | Audio path prefix convention | No runtime parsing (I2 evidence) | Fine as-is; invariant-2 scope clause at M6 |
| X5 | `romaji` column name + `UNIQUE(language_id, romaji)` | ADR-0 comment already says "ascii join key for XL" | Keep the name; semantics are language-neutral; document at M6, don't rename |
| X6 | `ConditionName` CONDITION_{1,2,3}_SOKUON | Mode-scoped: ADR-2's validation branch requires condition iff mode ∈ {CHOOSING, LADDER} | Fine - XL sessions never see it |
| X7 | Stimulus font stacks are kana-shaped | `--font-stimuli` = LINE Seed JP; `--font-stimuli-latin` reserved for IPA/Latin | **M6 dep gate:** hangul-capable face for Korean (LINE Seed KR exists as the natural sibling); rides the XL build's own font gate per §3.3 |
| X8 | `PhonologyService` (not yet built) must be profile-seamed | ADR-8.1: methods take `PhonologyProfile`, Japanese the only v1 profile | **Binds NIL-62 BE now** - build with the profile parameter from day one (rider posted) |
| X9 | Kana-named tokens/classes (`--text-kana-*`, `.script-display-text`) | Cosmetic naming on jpn-core surfaces | Keep; XL surfaces use their own roles (`--text-xl-form`) |
| X10 | Observatory romaji-equality joins (17/60 norms intersection) | `build-observatory-data.mjs` strict-equality by design | Already routed: M4 `norms_key` alias (SPEC-stats-dashboard §4.5) - cross-ref only |
| X11 | Thesis-facts-coupled copy (researchFlavor, landing stats, ladder benchmark grammar) | jpn-core surfaces only | Fine - XL modes carry their own copy per their specs |

## 5. README flags (NIL-44's six `<!-- NIL-84 -->` markers)

**All six KEEP as written.** F1 instrument/research-surface framing = the project thesis, not embellishment · F2 research-story numbers trace to thesis-facts · F3 mode framings current, XL row already reads Polyglot Challenge · F4 divergence lines are the binding formulation · F5 Observatory framing accurate · F6 deploy correctly conditional (SPEC-hosting is a proposal, not a live instance). Rider: the next api docs touch **strips the six HTML comments** (verdicts recorded here; the markers have served their purpose).

## 6. Privacy gate - web half, sharpened (per launcher addendum: sharpen, not absorb)

`docs/research/data/gorilla-tidy-choosing.csv` (**zero consumers** - pure liability) and `gorilla-tidy-rating.csv` (read only by `build-observatory-data.mjs`, which tolerates absence; committed `thesis-ratings.json` aggregate stays) are both **git-tracked in the web repo** - spec §4.6 violation. Fix at **NIL-42** (first web session): `git rm --cached` both + gitignore; keep on disk locally if convenient. Nothing breaks - verified against the build script's absence branch. **Hard gate before W30 / public repos** (mirrors the completed api half).

## 7. Rider routing (no orphan verdicts)

| Executing session | Carries |
|---|---|
| **NIL-41** (ladder api, next api session) | A1 TEXT_ONLY cut · A2 ScriptType delete · A3 difficultyLevel cut + explicit floor param (no overloading) · A7 scriptCode DTO drop · A10 reserved-prefix registration guard · strip the six README flags (or hand to the next api docs touch) · contract changelog entries for all of the above |
| **NIL-42** (ladder web) | D2 Zen Kaku cut (screenshot + battery proof) · A3 web sweep (drop difficultyLevel echo) · §6 privacy untracking (hard gate) · repo re-copy of doc-fixed UI-SYSTEM.md + this spec |
| **NIL-62** (Word Mint) | D1 motion-reveal choreography with the reveal card · X8 PhonologyService profile seam from day one |
| **NIL-86** (expansion pipeline) | A6 confirmation: fills `signoff_ref`/`approved_at`, computes `foil_distance` (validation-only fence) · invariant-4 gate unchanged |
| **NIL-47** (deploy runbook) | D3 sonner deadline: wire toasts at deploy polish or cut the dep · A10 confirmed pre-deploy |
| **M6** (XL ingest, future) | X1 kana nullable · X2 langCode-driven `lang` · X4 invariant-2 scope clause · X7 font gate |

## 8. W30 statement

No verdict threatens the W30 deploy. A1/A2/A3/A7/A10 fold into NIL-41's existing schema/contract session (M1 already regenerates seed and touches session start); D2/§6 fold into NIL-42's existing battery run; D1/X8 are in-scope guidance for NIL-62; D3 defers to deploy polish. Zero additional sessions created.
