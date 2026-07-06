# SPEC — The Observatory (site-wide statistics dashboard)

_NIL-71 output, Fable 5, 2026-07-05. Consumes the NIL-77 groundwork (`research/dashboard-synthesis-brief.md`, `research/dashboard-sourcing-manifest.md`, `research/dashboard-sources/`), which supersedes the raw NIL-70 pointer in the issue. Divergence framing throughout follows the corrected scale numbers (guess~rating ρ ≈ +.44 overall / +.65 within ideophones); the thesis-internal ρ=+.16 n=30 line is retired._

_**Revised 2026-07-05 (same-day, Nils's review):** name adopted — **The Observatory**; d3-scale/d3-shape dep gate **approved**; Winter/playful licenses confirmed CC BY 4.0 (upstream repo confirmation via Nils); deployment confirmed non-commercial, attribution mandatory; **implementation pulled forward from W31+ into the Fable window** — issue slate NIL-78/79/80/81 (+ NIL-82 gated), schedule in §8; §4.5 added (dataset → DB architecture carries, routed to M4/NIL-54 planning)._

## 0. Decision table (the issue's five questions + the brief's four)

| # | Question | Verdict | § |
|---|---|---|---|
| I-1 | Leaderboard absorbs it, or separate surface? | **Separate surface.** Leaderboard unchanged. New `AppView` `"observatory"`, no router dep. | §1 |
| I-2 | v1 content | Modality dumbbell + divergence scatter + per-word radar, honest coming-soon slots for the rest. | §3 |
| I-3 | Data source | Live `/api/research/divergence` (exists, public) + vendored static layers (thesis, McLean 2023, Iida & Akita). Both, from day one; NIL-54 not a blocker for v1. | §4 |
| I-4 | canvas-design / algorithmic-art stages | canvas-design → stage E1 (figure-export pass); algorithmic-art → stage N2 (network build) + optional S1 header motif. Named per session in §7. | §7 |
| I-5 | Chart stack / deps | No recharts (the issue's "already available" is stale — it is **not** in package.json). Bespoke SVG-in-React + d3 micro-packages. **`d3-scale` + `d3-shape` — gate APPROVED 2026-07-05** (ISC); `d3-force`/`d3-drag`/`d3-zoom` deferred to the network session's own gate. | §5 |
| B-1 | Raincloud vs ridgeline | **Raincloud**, n printed in the specimen label. | §3.4 |
| B-2 | AI-player panel | **Parked** (post-deploy novelty; not a core panel; one line in §9). | §9 |
| B-3 | Confusion-matrix panel | **Architect now, build at v2** — data-gated on NIL-54 + accumulated play. | §9 |
| B-4 | Placement (same as I-1) | Separate; bridges from completion panel + landing instead of merging. | §1 |

Player-facing name — **The Observatory** (**ADOPTED, Nils 2026-07-05**; watching the whole arena — fits scatter/network imagery and the lab register). Internal/route name `observatory`; measure-chip-style label in the header: `RECORD · ALL PLAYERS`.

## 1. Placement and information architecture

**The leaderboard does not absorb this.** They do different jobs for different audiences: the leaderboard is a *competition* surface inside the completion flow (session-scoped, momentum-preserving); the dashboard is the *research face of the conceptual spine* — "how far does iconicity carry you before convention takes over" rendered as explorable data. Merging them would bloat the game loop with scatterplots and bury the portfolio artifact inside an auth'd completion tab. The §10.3 gems principle already gives the loop its data moments (research notes, completion benchmark); the Observatory is where those moments *lead*.

**Concretely:**

- New `AppView` value `"observatory"` in `App.tsx` — the app is a view-state machine, not a router; this costs no dependency and no IA upheaval. A real router remains a non-goal until some future need (public deep-links, §1 last bullet) forces it.
- **Not a seventh mode card.** The §2.3 six-mode shell is for measures; the Observatory records nothing. Entry points instead: (a) a header nav link (ghost button, same tier as logout), (b) a completion-panel link — "See where this session lands" — deep-linking into the divergence panel with the player's session overlaid (§3.3), (c) the landing page's "Read the research" CTA (NIL-43) gains the Observatory as its destination beyond the stats strip.
- **Leaderboard v1 changes: none.** The brief's idea of surfacing the position-bias legitimacy guard on the leaderboard is deferred to v1.1+ (§3.5) — keep v1 surfaces clean.
- **Auth boundary:** v1 ships inside the existing app shell (auth'd), like everything else. `GET /api/research/divergence` is already `permitAll`, so a public read-only Observatory is API-ready — but *exposing* it publicly restructures the public/auth boundary, which is exactly NIL-43's territory. Decision explicitly delegated to the NIL-43 landing session; this spec only requires that nothing in the Observatory assume an authenticated user beyond the shell it sits in.

**Within-page IA — a narrative arc, not a grid of widgets** (mirrors the §10.5 landing arc; editorial strips per §2.5 grammar: alternating page/washi tints, prose on `--container-read`, charts may use `--container-app`):

1. **Header strip** — name, one-line register ("Every guess and rating in the arena, aggregated live."), live counts as specimen-labeled figures (players · guesses · ratings), wave-rule motif (§10.6).
2. **The claim** — accuracy by modality: thesis 6.86/6.42/5.97 vs live arena, as a dumbbell/paired-dot panel (§3.2). The spine's headline, replicating in public.
3. **The two measures** — divergence scatter (§3.3). The deep finding.
4. **The fingerprint** — per-word 6-modality radar + (v1.1) per-tier rating rainclouds (§3.4).
5. **The integrity strip** (v1.1) — position-bias d′/criterion check (§3.5). "We check ourselves" as a design statement.
6. **Coming-soon slots** — network + confusion panels in the §2.3 honest pattern: full-opacity copy stating exactly what will be measured and what gates it ("Populates as the word pool grows past 60"), no lock icons, no mystery.

## 2. Surface classification and design language

Boundary test (§5): renders no frozen string, no stimulus in trial position, no reserved slot, records no measure → **chrome**. Shell, tabs, tables, buttons = shadcn on tokens. The charts themselves are **bespoke SVG primitives on the same tokens** — same split as the rest of the app: themed shadcn for chrome, bespoke for the surfaces that carry meaning.

- **Axis/legend text is the specimen label** (§3.4 primitive): small-caps tracked `ACCURACY · % CORRECT`, `RATING · MEAN 1–7`, `N = 214`. This is the move that makes the charts look like *this* app and not a charting library.
- **Ink-and-paper chart style:** hairline `--border-soft` gridlines, `--ink-primary` marks, `--vermillion` reserved for the arena's own live data (elevation grammar analogue: the accent is the experiment speaking), soft modality fills for reference layers. No drop shadows, no chart-junk.
- **Kana in charts:** any displayed kana comes verbatim from API/vendored seed data with `lang="ja"` and the §3.3 kana ladder (invariant 1 — never derived). Note: `DivergenceResponse` currently carries `romaji` + `gloss` but no `display_form`, so **v1 scatter labels/tooltips are romaji + gloss**; adding `displayForm` to the DTO is an additive v1.1 server change if kana labels are wanted.
- **CVD rule graduates with the data:** modality color is never the sole carrier (§4.3) — dumbbell rows and radar axes are direct-labeled; scatter strata differ by marker shape *and* fill; the stratum legend names the shapes.
- **Accessibility:** every chart ships a data-table twin (shadcn `Table`, collapsed behind a "View as table" toggle) — satisfies §9.2 and gives screen readers the real numbers. Tooltips keyboard-reachable (focusable marks, `aria-describedby`). Motion per §7 ladder, `prefers-reduced-motion` honored (force layout settles instantly; no idle animation).
- **Responsive:** panels stack single-column ≤860; scatter keeps aspect and gains tap-tooltips; radar renders fine at 320px; no horizontal scroll anywhere.

## 3. Panels

### 3.1 Header strip (v1.0)

Live counts derived client-side from the divergence response (Σ guessCount, Σ ratingCount) — player count joins later via an existing/summary endpoint if trivially available, else omit the figure rather than fake it. Copy register: lab-calm, no exclamation marks (§10.4 bans apply).

### 3.2 The claim — accuracy by modality (v1.0)

- **Idiom:** dumbbell per modality row: thesis mean (ink dot, from vendored `thesis-per-pair-stats.csv` aggregated per modality) vs live arena mean (vermillion dot, client-side weighted aggregate over divergence rows, which carry `modality`). Chance line at 50% as a hairline with a specimen label `CHANCE · 50%`.
- **Copy hook:** "Sound carries furthest" — the aud > vis > int ordering stated plainly, with live numbers testing it out-of-sample as play accumulates.
- **Low-n honesty:** live dots render hollow until a per-modality n threshold (n ≥ 30 guesses), with `N = …` in the row label. No error bars in v1.0; Wilson CIs arrive with the v1.1 polish if wanted.

### 3.3 The two measures — divergence scatter (v1.0)

- **Idiom (adopted, brief):** x = guess accuracy, y = mean rating, one mark per word; marginal densities on both axes (hand-rolled Epanechnikov KDE, ~15 lines, no dep).
- **Three layers:**
  1. **McLean 2023 backdrop** (304 items, vendored): the stratum story — ideophone vs prosaic in soft fills + distinct marker shapes. This is where "ratings separate ideophones from prosaic words (δ = +.50); guessing doesn't (δ ≈ −.01)" is *visible*.
  2. **Thesis layer** (30 pairs, vendored): ink marks — the study this app replicates.
  3. **Arena layer** (live divergence endpoint): vermillion marks, growing with play. Words with `ratingCount = 0` or `guessCount = 0` are excluded from the cloud (no fake coordinates), counted in an honest margin note ("14 words await their first rating").
- **Stratum, not modality, colors this panel** (adopted decision 2). Live/thesis marks are all ideophones and say so in the legend; the stratum contrast lives in the backdrop. Modality is the radar's job.
- **Hover/tap:** romaji, gloss, n per measure, accuracy with Wilson interval (computed client-side), rating mean. **Session overlay:** arriving via the completion-panel link draws a labeled crosshair at (session accuracy, player's mean rating when Rating Lab data exists — `labRecord` supplies it client-side; omit the y-line otherwise) — the §10.3.2 benchmark grammar, one level up.
- **Copy (binding, brief):** "the two measures see different things" — never "unrelated", never "orthogonal". Footnote line carries ρ ≈ +.44 / +.65.

### 3.4 The fingerprint — modality profiles (radar v1.0; rainclouds v1.1)

- **Radar (v1.0):** per-word 6-axis profile (Auditory/Visual/Haptic/Gustatory/Olfactory/Interoceptive) from vendored Iida & Akita norms (510×17), for every pool word that joins the norms. Signature panel — a genuinely new axis the thesis never visualized. Word picker = searchable list (romaji + gloss); two-word compare overlay (e.g. kirakira vs dokidoki) as the default demo state, seeded to a strong contrast pair.
- **Rainclouds (v1.1, arbitrated):** per-modality 1–7 rating distributions — half-violin (KDE) + quartile box + seeded-jitter raw dots. **Raincloud over ridgeline** because the register of this app is honesty about thin data: raw dots *show* sample size while ratings are sparse, and the density earns its place as data accrues; a ridgeline would smooth 12 ratings into fake confidence. Compactness (ridgeline's one win) doesn't matter on an editorial page. n in the specimen label per tier. Thesis `gorilla-tidy-rating.csv` distributions render as a reference layer behind the live one.
- **Data gate:** rainclouds need per-value counts, not means → new endpoint (§4.2). Radar needs nothing from the server.

### 3.5 The integrity strip — position bias (v1.1)

- **What:** SDT-flavored check that the arena's forced choice is fair: left/right and target-top/bottom pick rates vs 50%, d′/criterion per the methods note. Powered by the deterministic per-session shuffle — position is *derivable* from `shuffle_seed` (GameSession) + round order; `player_answers` stores the selected ideophone, so chosen-side reconstructs server-side without any schema change.
- **Endpoint:** `GET /api/research/position-bias` (public, read-only aggregate). Implementation choice — seed-replay aggregate vs persisting `selected_position` additively at answer time — belongs to the backend session that builds it; note the additive column would be a schema change → **explicit gate** at that session.
- **Copy:** "A fair coin, checked" — the panel that says the 64% means something.

## 4. Data plan

### 4.1 Live (exists today — v1.0 needs zero backend changes)

`GET /api/research/divergence` → per-ideophone `{ideophoneId, romaji, gloss, modality, guessAccuracy, guessCount, meanRating, ratingCount}`, public, read-only, merged independent aggregates. Feeds: header counts, modality dumbbell (client-side weighted aggregation), scatter arena layer. This is the whole v1.0 API surface.

### 4.2 Live (new, v1.1 — one backend session, all additive, read-only, ResearchController siblings)

- `GET /api/research/rating-distributions` — modality × rating-value (1–7) counts → rainclouds.
- `GET /api/research/position-bias` — §3.5.
- Optional: `displayForm` added to `DivergenceResponse` (kana labels, invariant-1-verbatim).

No schema changes required for any of these (position-bias has a flagged optional-column variant, gated separately). All stay within the three-layer + DTO + mapper conventions; grading checklist untouched areas stay untouched.

### 4.3 Vendored static layers (baked at build, no runtime fetch of research files)

`src/data/observatory/` in the web repo + a small `scripts/build-observatory-data.mjs` that regenerates the JSON from the research CSVs; vendored JSON is committed (build stays network-free and the planning folder stays out of the build graph). README in the folder carries source + license per file:

| Layer | Source (already local in `research/`) | License | Vendor? |
|---|---|---|---|
| Thesis per-pair (30) | `data/thesis-per-pair-stats.csv` | Nils's own | ✅ |
| McLean 2023 backdrop (304) | `ideophone-dataset/mclean2023-measures/GuessingRatingScores.csv` | repo MIT; paper data CC BY 4.0 — attribute | ✅ |
| Iida & Akita norms (510×17) | `ideophone-dataset/Perceptual_strength_norms_JpnRaw.csv` | cite Iida & Akita 2023 | ✅ (radar) |
| Thesis rating trials | `data/gorilla-tidy-rating.csv` | Nils's own | v1.1 — **pre-aggregated only** (per-tier distribution JSON; raw participant-level trials never enter a public repo) |
| Lancaster norms | `dashboard-sources/modality/` | CC BY-NC-SA (fine — deployment confirmed non-commercial, Nils 2026-07-05) | ❌ v1 — excluded because unneeded, not because blocked; optional later overlay |
| Winter / playful_iconicity | local | **CC BY 4.0 — confirmed Nils 2026-07-05** (supersedes the manifest's no-LICENSE flag) | ❌ v1 — not needed by any v1 panel; idiom reference only; vendorable later with attribution |

Attribution footer on the Observatory (mirrors §10.3.4 landing footnotes): thesis line + McLean, Dunn & Dingemanse 2023 + Iida & Akita 2023 + McLean & Dingemanse toolkit chapter when the network panel ships.

### 4.4 NIL-54 relationship

Not a v1 blocker. Post-NIL-54: scatter cloud widens (more words, live-normed pairs), radar covers Pool A/B fully, and the two gated panels (§9) become buildable. The Observatory is architected so NIL-54 changes *data volume, not structure* — no panel schema assumes 60 words.

### 4.5 Dataset → DB-architecture carries (routed to M4 / NIL-54 planning, not built here)

Three concrete things the vendored datasets teach the ARCHITECTURE.md M-plan; recorded so the inspiration lands in the right sessions:

1. **Iida & Akita's shape is the `word_features` (M4) design.** Their 17 columns reduce to *per-word × per-dimension scalars plus derived values* — exactly what a long-format `word_features(word_id, feature, value, source_id)` handles uniformly for the 6-vector, `Modality_exclusivity`, their bonus `Iconicity` column, corpus frequency, `kata_share`, and difficulty priors (the top-600 and sign-off sheets carry the same shape). Post-M4 the radar swaps vendored JSON → API with zero panel changes (§4.4's volume-not-structure promise made concrete). Provenance stamps via M5 `stimulus_sources`.
2. **External anchors attach to form–meaning pairings, not words.** McLean 2023's long format (`identifier, ideophone/stratum, method, z_score, score`) — with nebaneba scoring differently under two concepts — is the standing reminder (already noted in SPEC-four-floor-ladder §2) that guessability anchors for NIL-54 priors may need to key on *pairing/meaning*, not word alone. Carry into the M4 keying decision.
3. **The graph needs no graph store, and the arena already generates its own edges.** McLean's 2-CSV nodes/edges model maps to one relational `word_relations(word_a, word_b, relation_type, weight, source_id)` table in the ADR-3 world — and the richest edge types are *ours for free*: pairing-history edges from `pairings`, confusion edges from aggregating unified `trials` (which wrong word got picked). The Great Language Game's record shape (`target, sample, choices, guess`) independently confirms ADR-3's unified `trials` is already the right long format for confusion aggregation. Nothing to add server-side now; this is validation, plus one future table.

### 4.6 File placement map (what goes in which repository)

| Artifact | Destination | Note |
|---|---|---|
| `SPEC-stats-dashboard.md` | Planning folder = canon; **copy into both repos' `docs/specs/`** before their sessions (NIL-78 web, NIL-79 api) per the re-copy-canon rule | api sessions consume §4.2; web sessions the rest |
| Source CSVs: `thesis-per-pair-stats.csv`, `GuessingRatingScores.csv`, `Perceptual_strength_norms_JpnRaw.csv` | **Web repo** `data/observatory-sources/` + license/citation README | Tiny (30/608/510 rows), licensed, makes `build-observatory-data.mjs` self-contained — the build graph never reaches into the planning folder |
| Derived JSON | **Web repo** `src/data/observatory/` (committed) | Regenerated by the script; runtime imports only this |
| `gorilla-tidy-*.csv` (trial-level) | **Stays in planning folder.** Only *pre-aggregated* per-tier distribution JSON enters the web repo (v1.1) | Participant-level thesis data doesn't belong in a public repo, even anonymized |
| McLean cloned apps, CLICS⁴, screenshots, Lancaster, manifest, brief | **Stays in planning folder** (`research/`) | Reference material, never repo content; McLean repos are separate MIT projects — study, don't absorb |
| E1 exports | Planning `docs/design/observatory-exports/`; **og-image only** → web repo `public/` | Per NIL-81 |
| API repo data files | **None.** v1 endpoints aggregate live tables; thesis-data-into-DB is NIL-54's ingestion pipeline (mechanism decision pending there) | No raw research CSVs into the api repo |

## 5. Chart stack and the dependency gate

**Finding: the issue's "recharts already available" is stale — no chart library exists in `package.json`.** Any charting choice is a new-dep decision; per the workflow rule, nothing installs without approval. The ask:

- **Adopt: bespoke SVG-in-React + d3 micro-packages.** v1.0: **`d3-scale`, `d3-shape`** (+ `@types/d3-scale`, `@types/d3-shape` devDeps) — **gate APPROVED with the spec sign-off (Nils, 2026-07-05)**; install happens at NIL-78 session start. ISC, tiny (~30 kB each pre-gzip), zero components — d3 does math, React owns the DOM. Network session later adds `d3-force`, `d3-drag`, `d3-zoom` (separate gate at NIL-82, per the manifest's web-native-d3 verdict on the McLean precedent).
- **Rejected: recharts** — heavier, weak at exactly our signature idioms (marginal densities, rainclouds, dumbbells), and it looks like recharts; this dashboard must look like the arena's lab bench. **Rejected: full `d3`** — imperative DOM ownership fights React and drags in 30 modules for the 5 we need. **Rejected: zero-dep hand-rolled scales** — re-implementing `scaleLinear`/ticks/`line`/`area` badly to avoid two ISC micro-deps is false economy; KDE and Wilson intervals, by contrast, are ~15 lines each and *are* hand-rolled.
- Everything renders as inline SVG on tokens — no canvas in interactive panels (canvas appears only in stage E1 exports, §7).

## 6. Component plan (web repo, all new files except the two touch-points)

```
src/observatory/
  Observatory.tsx            — view shell, strips, coming-soon slots
  panels/ModalityDumbbell.tsx
  panels/DivergenceScatter.tsx
  panels/WordRadar.tsx
  panels/RatingRainclouds.tsx   (v1.1)
  panels/IntegrityStrip.tsx     (v1.1)
  chart/{scales,axis,SpecimenLabel,kde,wilson,useChartSize}.ts(x)
src/data/observatory/         — vendored JSON + README (licenses)
scripts/build-observatory-data.mjs
```

Touch-points: `App.tsx` (add `"observatory"` to `AppView`, header nav link, completion-panel link with session payload) and `api/client.ts` (typed fetch). Semantic hooks per the §5 stable-hook contract: `.observatory`, `.observatory-panel`, `.chart-figure`, first in `className`. New tests: chart-math units (KDE, Wilson, weighted aggregation — vitest), a render smoke per panel with fixture JSON, and the browser-loop idiom for any new Radix control the shell adopts (§16 H7 — expected: none beyond existing Tabs/Button).

## 7. canvas-design / algorithmic-art — exact invocation stages

| Stage | Session | Skill | Scope |
|---|---|---|---|
| **S1** (optional rider on the shell session) | Observatory shell build | **algorithmic-art** | The header's wave-rule variant: a seeded, data-driven waveform (live aggregate numbers modulate the L5 zigzag), static SVG render — generative art constrained to chrome, `prefers-reduced-motion`-safe because it doesn't move. If it reads as gimmick in review, the plain §10.6 wave rule stays. |
| **E1** (own small session, after v1.0 renders real data) | Figure-export pass | **canvas-design** | Poster-grade *static* exports of the three signature figures (dumbbell, scatter, radar) with titles, citation footers, thesis palette — outputs: og-image/social card, landing "Read the research" strip art (NIL-43 consumer), portfolio/README figures. Canvas/PNG/PDF territory, deliberately outside the interactive SVG pipeline. |
| **N2** (network panel session, post-NIL-54) | Network panel build | **algorithmic-art** | Seeded d3-force layout aesthetics: deterministic seed → reproducible layout (the project's seeded-shuffle ethos applied to art), domain-wheel entry transitions (Siwu precedent), hover-preview with **per-word audio playback** (JapaneseMimetics uses video; our twist is the shared per-word audio — invariant 2, playback separated from presentation). |

Not invoked during ordinary panel construction — SVG-in-React chart work is not these skills' lane, and pinning them to named stages is what keeps them from leaking into everything (the issue's Q4 asked for exactly this).

## 8. Build sequencing — **pulled into the Fable window (Nils, 2026-07-05)**; issues live on the board

_Original plan said buffer-tier W31+; superseded same-day — Nils wants v1 fully finished within Fable week (ends Wed 2026-07-08). "Fully finished" = NIL-78 + 79 + 80 + 81; NIL-82 stays data-gated on NIL-54 by design and ships as its honest coming-soon slot._

| Order | Issue | Scope | Due | Blocked by | Backend? |
|---|---|---|---|---|---|
| 1 | **NIL-78** | Observatory v1.0: deps (gate settled) + vendored data + shell + dumbbell + scatter + radar | 07-06 | — | **None** |
| 2 | **NIL-79** | rating-distributions + position-bias + `displayForm` | 07-07 | — (parallel-delegable with NIL-78) | Yes — additive, read-only |
| 3 | **NIL-80** | Observatory v1.1: rainclouds + integrity strip + kana labels | 07-08 | NIL-78, NIL-79 | None |
| 4 | **NIL-81** | E1 figure-export pass (canvas-design) | 07-08 | NIL-78 | None |
| 5 | **NIL-82** | N2 network panel (algorithmic-art) + confusion sibling | W32 buffer | NIL-54 | Graph endpoint TBD in its own plan |

All four Fable-week issues sit in the W28 "Second mode + build-out" milestone (whose charter is exactly this kind of pulled-forward work). **Known compression:** W28 also carries 28A/28B ladder + NIL-62 + NIL-76 + NIL-43 + NIL-58, plus NIL-63 finishing and NIL-73 due 07-08 — the Observatory slate consumes roughly four of the remaining Fable-window sessions; deadlines govern sequencing, Nils arbitrates what slips past Wednesday. Landing (NIL-43) consumes the Observatory as CTA destination whenever it lands relative to these.

## 9. Later / parked (documented so they don't relitigate)

- **Semantic network** (v2, N2): force-directed, domain-wheel entry, audio hover. Data model = two CSV-shaped tables (nodes/edges — McLean proves the model; do not architect a graph store). Gated: NIL-54 words+meanings.
- **Confusion matrix** (v2): "which wrong word gets picked, for which target" — Great Language Game precedent; gated on trials-per-pair volume post-NIL-54; architect the panel slot now (coming-soon strip), build when n makes it legible.
- **AI-player panel:** **parked**, post-deploy novelty at most. High story value, but it drags a computational sub-project into a read-only aggregation surface during a deploy-focused window. Revisit only after W30 go-live.
- **Leaderboard cross-surface** (position-bias badge on the leaderboard): revisit with v1.1.
- **Lancaster overlay** (English cross-linguistic radar comparison): optional, NC-SA license note travels with it.

## 10. Invariants and risks

- **Invariants 1–5: untouched.** Read-only aggregation; kana (when added) rendered verbatim from DB-sourced fields; audio reuse in the future network panel follows invariant 2 with playback separated from presentation; no trial surfaces, no wording, no pairing logic, no seed edits.
- **Risks:** (a) thin live data early — mitigated by reference layers + hollow-dot/low-n honesty rules, which are design features, not apologies; (b) scatter overplotting post-NIL-54 — mitigation planned (quadtree hover, optional brush) but not built in v1; (c) Fable-window compression (§8) — the slate is four sessions in ~3 days alongside the rest of W28; Nils arbitrates slippage; (d) public exposure ambiguity — explicitly delegated to NIL-43, nothing here blocks either answer.

_Planning-folder canon — maintained by Claude (Nils 2026-07-05: planning-folder file state is Claude's call; his commits concern the api/web repos). Repo copies of this spec ride NIL-78/NIL-79._
