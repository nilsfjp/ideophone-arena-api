# Dashboard synthesis brief — NIL-71 starting point

_NIL-77, Opus, 2026-07-04. The decision-oriented synthesis of NIL-70 (PDF-corpus triage) + NIL-75 (web deep-research), both of which independently converged on the same **three panel families**. Fable opens this to start architecting the statistics dashboard **without re-deriving anything**. Fetch list + licenses: `research/dashboard-sourcing-manifest.md`. This brief decides what's settled and flags what Fable must arbitrate._

## The shape

Three panel families, each now carrying both a **build-ready domain precedent** (NIL-70) and a **technique/UX/rigor layer** (NIL-75), plus two candidate panels and one adopted copy correction. The dashboard is the research face of the conceptual spine — "how far does iconicity carry you before convention takes over" — rendered as explorable data, not a leaderboard.

---

## Panel family 1 — Divergence

**What it shows:** guessing accuracy and felt iconicity are *different measurements* of the same words.

- **Chart idiom (adopted):** scatter, one dot per word — x = choosing accuracy, y = mean rating — **colored by lexical stratum** (ideophone vs prosaic), with **marginal densities** on each axis (`seaborn.jointplot` idiom). Optional rigor layer: brms credible-interval ribbons; Wilson score error bars per word.
- **Data source:** `research/data/thesis-per-pair-stats.csv` (30 pairs) **now**; the live `/api/research/divergence` aggregate endpoint as play accumulates; `mclean2023-measures/GuessingRatingScores.csv` (304 items) as the at-scale, stratum-labelled backdrop.
- **Precedent to mirror:** thesis Figs 15/16 (the scatter, in Nils's own data) + McLean 2023 Figs 7/8 (stratum-colored scatter + regression interaction). Both local.
- **Sub-panel (net-new, NIL-75):** SDT d′ / criterion *position-bias* check, powered by the server-seeded left/right + top/bottom shuffle — a legitimacy guard the leaderboard can surface.

## Panel family 2 — Semantic network

**What it shows:** the ideophones/meanings as an explorable graph — "explore the space."

- **Chart idiom (adopted):** force-directed node-link graph with a **domain-wheel entry** that expands into per-domain sub-networks; **hover-preview** on a node; **click / double-click expand-contract**. Radial **hierarchical edge bundling** is the alternative idiom for showing many cross-tier confusions at once.
- **Data source:** McLean's **2-CSV node/edge model** (`nodes.csv` + `edges.csv`) is the proof that this needs almost no data infrastructure. Our graph is populated after NIL-54 ingestion (words + meanings + relations); until then the McLean data itself is the working reference.
- **Precedent to mirror:** McLean's **`JapaneseMimeticNetwork`** (MIT, in-domain, the closest existing thing) and **`SiwuIdeophoneNetwork`** (MIT, domain-wheel + MDS layout). Implement web-native with **`d3-force`** (React/Vite) rather than porting R/Shiny. Legibility reference at scale: SWOW; genre reference: CLICS⁴.

## Panel family 3 — Modality-norm profiles

**What it shows:** each word's sensory fingerprint, and how modality structures difficulty.

- **Chart idiom (adopted):** per-word **6-modality radar** (Auditory/Visual/Haptic/Gustatory/Olfactory/Interoceptive) — a genuinely **new axis** the thesis never visualized — plus per-tier distribution plots (**raincloud** or ridgeline) and a Winter-style density + coefficient plot.
- **Data source:** `Perceptual_strength_norms_JpnRaw.csv` (Iida & Akita, 510×17) feeds the radar directly; `gorilla-tidy-rating.csv` feeds the per-tier 1–7 distributions; Lancaster Sensorimotor Norms (to FETCH) is the English cross-linguistic overlay.
- **Precedent to mirror:** Iida & Akita modality boxplots + the 6-vector; Winter `iconicity_ratings` density/coefficient idioms (already local) + a Rating-Lab **calibration round** (rate a few English anchors, see yourself vs 1,400+ raters).

---

## Adopted decisions — carry forward, do not relitigate

1. **Stratum-framed divergence. Retire "nearly orthogonal."** At scale, guess ~ rating is **ρ = +.44 overall / +.65 within ideophones** — correlated, not orthogonal. The real dissociation is *what each measure detects*: **ratings separate ideophones from prosaic words (Cliff's δ = +.50) while guessing does not (δ ≈ −.01).** All divergence copy says **"the two measures see different things,"** never "they're unrelated." (The thesis-internal ρ=+.16, n=30 that seeded the old line is a small-n pair-level estimate, contradicted at scale.)
2. **Color the divergence scatter by lexical stratum, not modality.** Modality is the radar panel's job; stratum is what the divergence story is about.
3. **The per-word modality radar is a new axis** — not in the thesis. Treat it as a signature panel, not a restatement.
4. **Network data model is trivial** — two CSVs (McLean proves it). Don't architect a heavy graph store for it.
5. **Licensing is friendly** for the core build (McLean apps MIT; triangulating_iconicity + thesis data + CLICS⁴ CC BY 4.0). Watch the three flags in the manifest (Lancaster CC BY-NC-SA; Winter/playful no-license; SWOW/Wordbank study-don't-fork).

---

## Open questions — Fable to arbitrate (trade-offs stated, not decided)

1. **Raincloud vs ridgeline** for per-modality 1–7 distributions. Raincloud (half-violin + box + raw points) shows sample size honestly and is the NIL-75 pick; ridgeline (stacked densities) is more compact and matches the `playful_iconicity` flavor NIL-70 cited. Trade-off: honesty/raw-data vs vertical compactness. Both idioms are already referenced.
2. **The LLM/VLM "AI-player" panel** — "does a model with no ears guess above chance the way you do?" ⚠ **Judgment call:** NIL-75 flags it as high portfolio-story value, low build cost; NIL-70 rejected a corpus LLM paper (Hansen 2024) as off-topic. Trade-off: a memorable novelty hook that sharpens the conceptual spine vs scope creep into a computational sub-project. Treat as optional, not a core panel.
3. **The distractor-confusion-matrix panel** (from Great Language Game). Net-new, the only gamified-2AFC-at-scale precedent; "which wrong word gets picked, for which target." Trade-off: genuinely novel + genre-appropriate vs it needs enough live trials per pair to be non-noisy (thin until play accumulates / NIL-54 widens the pool).
4. **Placement: integrated into the leaderboard vs a separate `/research` dashboard** — the original NIL-71 question. Trade-off: a separate dashboard is a cleaner portfolio artifact and keeps research framing out of the game loop; integration keeps players closer to the data. Not decided here.

---

## App-data → panel mapping (what can populate what, now vs after NIL-54)

| Source | Available | Divergence | Modality radar | Per-tier distributions | Network | Confusion matrix |
|---|---|---|---|---|---|---|
| `thesis-per-pair-stats.csv` (30 pairs) | **now** (local) | ✅ full scatter now | partial (only 9–23 thesis words join norms) | — | — | — |
| `gorilla-tidy-choosing.csv` / `-rating.csv` (trial-level) | **now** (local) | ✅ (recompute per-pair) | — | ✅ 1–7 by tier | — | ⚠ thin (36 subjects) |
| Live `/api/research/divergence` (aggregate) | **now** (running) | ✅ live overlay, grows with play | — | — | — | — |
| Iida & Akita norms (510 words, 6-vector) | **now** (local) | — | ✅ radar for any covered word | — | modality edges | — |
| `mclean2023` (304 items) / `triangulating_iconicity` (81 JP) | **now** (local) | ✅ at-scale stratum backdrop | — | — | — | — |
| **NIL-54 stimulus ingestion** (expanded pool, per-pair live stats) | **after NIL-54** | larger cloud, live-normed pairs | radar across Pool A/B (all carry 6-vectors) | more words per tier | ✅ populates real word/meaning graph | ✅ enough trials per pair to be legible |

**Bottom line for Fable:** the Divergence and Modality panels can be prototyped **today** from local files; the Network and Confusion-matrix panels are real but data-gated on NIL-54 ingestion and accumulated live play — architect them now, populate them later. Start from the three adopted idioms; arbitrate the four open questions; the sourcing manifest has every asset and license you'll need.

_Uncommitted — for Nils's review._
