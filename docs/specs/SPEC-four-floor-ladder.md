# SPEC — Four-Floor Ladder Extension (`LADDER` + Touch floor)

_F2 output (NIL-56), Fable 5, 2026-07-02. An extension of the committed W28 Modality Ladder (NIL-41/42, sessions 28A/28B), not a separate mode: adds a **Touch (haptic) floor** built from the six approved haptic pairs in `research/stimulus-expansion-signoff.xlsx` (H1–H6, all signed off 2026-07-02). Hard-gated on the **audio-recording decision** — the one candidate that needs new recordings._

_**Revised 2026-07-02 (same-day, OSF-mining session; prior version in `archive/`):** mclean2023 secondary anchors noted in §2; §7 foil-distance clause pinned to validation-only after the thesis-pair null._

## 1. Concept and vision test

The Ladder's climb — Sound → Sight → Inner states — gains a fourth rung: **Sound → Sight → Touch → Inner states**. Placement follows McLean's (2021) implicational hierarchy (SOUND < MOVEMENT < VISUAL < OTHER SENSORY < INNER STATES): haptic sits between visual and interoceptive. The floor is fueled by the F1 finding that haptic is the *only* viable new floor (14 A+B candidates; gustatory/olfactory confirmed dead at 2 candidates).

**Vision test — measurement angle, not just content:** (a) **finer resolution on the modality gradient** — the thesis measured aud > vis > int (6.86/6.42/5.97); a haptic floor adds the missing intermediate cell and tests the hierarchy's *prediction* out-of-sample (does live haptic accuracy land between Sight and Inner states?). (b) **First native consumer of the self-norming loop** — these six pairs have no thesis accuracy data at all; their difficulty starts as a prior (`difficulty_prior` from the sign-off sheet) and is *replaced* by live play. The game norms its own stimuli here first, by necessity. The player-facing framing writes itself and is honest: "no lab has measured these pairs — you're the norming study" (F3 register).

## 2. Source data (all approved)

`research/stimulus-expansion-signoff.xlsx`, `Pairs` sheet, floor = Haptic — six pairs, signed off, with draft glosses in the thesis meaning-prompt style and per-pair difficulty priors:

| Pair | Words | Contrast | Prior | New recordings |
|---|---|---|---|---|
| H1 | sarasara / nebaneba | dry-smooth vs sticky (textbook) | easy | 2 |
| H2 | fuwafuwa / **gotsugotsu** (thesis word) | fluffy-soft vs rugged-hard | easy | 1 |
| H3 | shittori / **bosabosa** (thesis word) | silky-moist vs dry-tousled | medium | 1 |
| H4 | dorodoro / tsururi | viscous sludge vs frictionless slip | medium | 2 |
| H5 | togetoge / fusafusa | prickly vs fluffy | easy | 2 |
| H6 | gyuq / funyafunya | tight grip vs limp squish | medium | 2 |

**10 new recordings**; 2 words reuse existing thesis audio. The two reused thesis words carried `thesis_modality_check` flags — their re-classification to HAPTIC (per the Iida & Akita norms) was part of the sign-off and is **approved**; see §4 migration note. External difficulty anchors exist for several new words via the Collabra guessability set (dorodoro, fuwafuwa, nebaneba, sarasara have published per-word guessability) — usable as secondary priors, noted per pair at seed time. _Added 2026-07-02:_ mclean2023 contributes a second, **weak** anchor for fuwafuwa (guess .70 / rating .62) and nebaneba (.38/.33 and .20/.30 under two different concepts) — constructed-foil design, English online raters, so treat as tie-breakers only; the two-concepts spread on nebaneba is itself a reminder that anchors attach to form–meaning pairings, not words._

Candidate-level covariates (hira/kata forms, `kata_share` for canonical-script assignment, perceptual-strength values) are already in the `Candidates` sheet — no new data collection needed beyond audio.

## 3. Trial design

**None new.** Floor 4 trials are ordinary Choosing Task rounds (canonical wording invariant 3 verbatim, same round DTO, same answer flow) served in the Ladder's floor sequence by whatever 28A ships. This spec adds content + ordering, not mechanics — by design, so the extension stays cheap and the measurement stays comparable across floors.

## 4. Data model

Rides the 28A gate decision (lean recommendation there: **static server-side ordering map**, no schema). Floor 4 deltas, all through `scripts/generate_seed_sql.py --check`:

- **Seed additions:** 10 new words → following the established pattern, one `ideophones` row per condition (3 rows per word, shared per-word audio file across all three — invariant 2), `display_form`/`canonical_form` from the sign-off sheet's hira/kata columns **verbatim** (invariant 1), `canonical_script` assigned from `kata_share` (threshold rule: kata-dominant if `kata_share` > 0.5 — record the rule in the seed script), `modality = 'HAPTIC'`, `gloss` from approved drafts. 6 new `arena_rounds` per condition set, `difficulty_level = 1`, `is_practice = FALSE`.
- **Audio paths:** `/stimuli/audio/h{pair}{h|k}-{romaji}.m4a` — extends the existing `<modality-letter><pair-digit><script-letter>` 3-char prefix convention (`h0`–`h5`; `h` is unclaimed: a/v/i/p in use). Invariant 2 intact.
- **Modality value:** `'HAPTIC'` is additive (`modality` is `VARCHAR(50)`) — no enum migration server-side; frontend `Modality` union type must gain `"HAPTIC"` (it already tolerates `| string`, but make it explicit).
- **Re-classification migration (approved, but handle deliberately):** gotsugotsu and bosabosa flip modality on their existing rows (seed regeneration — an approval-gated seed rewrite). Consequences: `GET /api/admin/stats` `byModality` re-buckets their historical answers; the thesis floors lose two words' worth of stats to the Touch bucket. **Decision recorded here:** accept the re-bucket (modality is word-level truth, stats recompute); do not dual-classify. Their existing `arena_rounds` in thesis pairings stay untouched (a word may serve on multiple floors; floor membership is the ladder map's business, not the word's).
- **Ladder map:** floor 4 entry with within-floor order = priors easy→hard (H1, H2, H5, H3, H4, H6), boss pair = H6 (hardest prior; provisional until self-norming, see §7). If 28A instead lands schema columns, floor 4 is 6 more rows in that structure — this spec is agnostic.

## 5. API surface

**No new endpoints.** Whatever 28A ships (e.g. `GET /api/game/ladder/floors`, floor-scoped session start) simply returns four floors instead of three, ordered Sound → Sight → Touch → Inner states. Contract deltas: the floors response documents the new element; `byModality` admin stats gain a `HAPTIC` row organically. `docs/backend-contract.md` changelog entry required.

## 6. Frontend (vs 28B ModalityLadder + tokens.css)

- `ModalityLadder.tsx` (28B) renders floors from the API — a fourth floor should be a data change, not a code change; verify 28B builds it that way (flag to 28B: **do not hardcode three floors**).
- `src/api/types.ts`: `Modality` gains `"HAPTIC"`; floor metadata type unchanged.
- **Token gate:** tokens.css has three modality accents (auditory/visual/interoceptive) derived from the thesis figure palette. Touch needs `--accent-haptic` + `--accent-haptic-soft` — there is no thesis figure color to derive from, so this is an **F3/art-direction adjudication** (constraint: AA as text on both paper surfaces, distinct from the existing three and from `--accent`; a warm clay/terracotta family suggests itself next to the existing set, but the pick is F3's).
- Floor intro copy slot (framing per §1), `researchFlavor.ts` haptic notes.

## 7. Difficulty, scoring, self-norming, foil-distance

- **Priors:** sign-off sheet `difficulty_prior` (3 easy / 3 medium) orders the floor; Collabra published guessability refines within-tier order where available (noted per pair at seed time).
- **Self-norming (the point):** live per-pair accuracy replaces priors at n ≥ 30 (shared item-stats source — NIL-57 D4); boss-pair designation re-evaluates then. This floor is the mechanism's proving ground before it back-propagates to the thesis floors (where thesis per-pair stats already exist as strong priors).
- **Foil-distance dial (NIL-58):** compute feature-vector distance (shared `PhonologyService`) for all six pairs at pipeline time and record alongside priors — H-floor pairs then participate in NIL-58's validation set (does foil distance predict live accuracy on brand-new pairs? — a clean test, since no thesis data contaminates it). _Note 2026-07-02: on the 30 thesis pairs this prediction already failed (rho = −.155, n.s.; see f1 profile §4) — so distance is recorded for validation only and must not drive H-floor ordering, which stays priors-based per §4._
- **Scoring:** identical to the base Ladder (28B): per-floor progression + score; completing Touch feeds the same summary.

## 8. Frozen-copy needs

- **Zero new trial strings** — canonical Choosing wording throughout (the extension's cheapest property).
- New floor framing copy (intro card, completion line) — F3 register, hierarchy-grounded, honest about unnormed pairs.
- Glosses are approved drafts from the sign-off sheet; they freeze verbatim into the seed (any later tweak = seed regeneration + adjudication).

## 9. Open gates (blocking order)

- **G1 — Audio recording decision (the committed separate gate; blocks everything):** 10 recordings. Constraints from the TTS ruling (2026-07-02): TTS is banned for core content; recordings must be human, and **voice-consistency with the existing 60-word set matters** — same speaker and comparable recording conditions as the original stimuli, or the floor introduces a speaker confound the thesis floors don't have. Nils owns: speaker sourcing (original speaker if reachable), recording protocol (match original: quiet room, same format pipeline → m4a), and the go/no-go.
- **G2 — Seed regeneration approval** (30 ideophone rows + 18 round rows + 2 re-classifications; destructive-adjacent per workflow rules).
- **G3 — 28A/28B landed** (floor model + UI exist to extend). Also verify 28B's floors-from-data property (§6).
- **G4 — `--accent-haptic` adjudication** (F3).

## 10. Proof steps

1. `python3 scripts/generate_seed_sql.py --check` clean; `ddl-auto=validate` boots; `./mvnw test` green.
2. Audio QA: all 10 files play; loudness within tolerance of the existing set (spot-check against 2–3 thesis files); every haptic word's three condition rows reference one shared file (invariant-2 query check).
3. Live curl: ladder floors endpoint returns 4 floors in hierarchy order; Touch floor serves H1→H6 in prior order, boss last; answers judge correctly.
4. Browser: climb all four floors; kana renders verbatim from `display_form` (spot-check gyuq's small-tsu form as authored in the sheet — **rendered, never derived**); `--accent-haptic` passes AA (run `design:accessibility-review` on the floor view).
5. Admin stats show `HAPTIC` bucket; re-classified words' historical answers re-bucketed as decided in §4.

## 11. Build cost

Smallest code delta of the four candidates: **≤ 1 backend session (seed + map + contract docs) + ≤ 1 frontend session (types, token, copy slots — possibly folded into 28B follow-up)**. But the wall-clock cost is dominated by G1 (external recording logistics) — and G1 is exactly the criterion the build pick excludes: **needs new audio recording → fails build-pick criterion 1 by definition.** Sequence: after W28 lands and whenever the recording gate resolves; W31+ realistically.

## 12. Risks

- Recording gate stalls indefinitely → the floor is cleanly deferrable (nothing else depends on it; the other three floors ship in W28 regardless).
- Speaker mismatch introduces a confound → G1 constraint is non-negotiable; if the original speaker is unavailable, record **all** floor-4 words with one new speaker and document the break (never mix speakers within a floor).
- Re-classification surprises in historical stats → §4 decision is recorded; mention in the contract changelog so the dashboard/landing numbers don't look haunted.
- gyuq's word-final sokuon: keep the F1 romaji pitfall in mind end-to-end (`gyuQ` in thesis-style romaji; audio filename `h5h-gyuq.m4a` lowercase; norms-join preserves final `q`).

## 13. Deferred to NIL-57

Shared item-stats source for self-norming (D4) · whether floor membership becomes schema (`floors` table) once floors multiply beyond four · pairing-pipeline data model for the remaining 17 approved pairs (A/I/V floors — same batch, separate concern from this floor extension).
