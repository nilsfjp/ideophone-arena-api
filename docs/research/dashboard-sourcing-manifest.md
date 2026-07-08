# Dashboard sourcing manifest — execute by hand

_NIL-77, Opus, 2026-07-04. Merges NIL-70 (PDF-corpus triage) + NIL-75 (web deep-research) into one ordered, license-checked fetch list. Companion: `research/dashboard-synthesis-brief.md` (the Fable/NIL-71 starting point). Every external URL below was resolved with web tools this session; licenses are recorded per row. **You** clone/fetch/screenshot by hand — this doc is the map, not the mover._

## How to use this

Work the **Ranked execution order** top-to-bottom — it's sorted by inspirational value to a portfolio dashboard, actionable items only. Each row names its **destination folder** under `research/dashboard-sources/` (scaffold already created, each folder has a README). Full detail for every source is in the per-panel sections below. Two sections need **no fetching**: **Already-local** (things a prior session already cloned — do not re-fetch) and **Method-reference** (techniques, no asset).

**Classification key:** CLONE = git repo · FETCH = raw dataset download · SCREENSHOT = capture a live view · ALREADY-LOCAL = in `research/` now · METHOD-REF = reading only.

**Verification key:** ✅ = URL resolved this session · ⚠ = license or version caveat, read the note.

---

## Ranked execution order (actionable — fetch these, in this order)

| # | Class | Source | Command / URL | License | → folder |
|---|---|---|---|---|---|
| 1 | CLONE ✅ | **McLean — JapaneseMimeticNetwork** (closest precedent to our network panel; same domain; 2-CSV model) | `git clone https://github.com/BonnieMcLean/JapaneseMimeticNetwork.git` | **MIT** | `network/` |
| 2 | SCREENSHOT ✅ | JapaneseMimetics **live demo** (its interaction model) | https://bonnie-mclean.shinyapps.io/JapaneseMimetics/ | — (view) | `screenshots/` |
| 3 | CLONE ✅ | **McLean — SiwuIdeophoneNetwork** (domain-wheel entry + MDS example) | `git clone https://github.com/BonnieMcLean/SiwuIdeophoneNetwork.git` | **MIT** | `network/` |
| 4 | SCREENSHOT ✅ | SiwuIdeophones **live demo** (domain wheel → expand → double-click contract) | https://bonnie-mclean.shinyapps.io/SiwuIdeophones/ | — (view) | `screenshots/` |
| 5 | SCREENSHOT ✅ | **Wordbank** (whole-dashboard UX template) | https://wordbank.stanford.edu/data | data CC BY 4.0 | `screenshots/` |
| 6 | FETCH + SCREENSHOT ✅ | **Great Language Game** confusion data (only gamified-2AFC-at-scale sibling → confusion-matrix panel) | https://f003.backblazeb2.com/file/larsyencken-eu-public/greatlanguagegame/confusion-2014-03-02.tbz2 (viz: https://lars.yencken.org/language-confusion/) | **CC BY 3.0 AU** | `ux-precedent/` |
| 7 | SCREENSHOT ✅ | **Observable — force-directed graph** (web-native impl. of McLean's R/Shiny interaction) | https://observablehq.com/@d3/force-directed-graph/2 | ISC (code) | `screenshots/` |
| 8 | SCREENSHOT ✅ | **SWOW — Explore the lexicon** (large-graph legibility: hover-neighborhood, degree-scaled nodes) | https://smallworldofwords.org/en/project/visualize | ⚠ view only; SWOW norms NC | `screenshots/` |
| 9 | SCREENSHOT ✅ | **Observable — hierarchical edge bundling** (radial idiom for cross-tier confusions) | https://observablehq.com/@d3/hierarchical-edge-bundling | ISC (code) | `screenshots/` |
| 10 | FETCH ✅ | **Lancaster Sensorimotor Norms** (English interoception reference for the radar) | https://osf.io/7emr6/ (landing https://www.lancaster.ac.uk/psychology/lsnorms/) | ⚠ **CC BY-NC-SA** | `modality/` |
| 11 | FETCH + SCREENSHOT ✅ | **CLICS⁴** colexification network (in-domain interactive-network precedent; optional dataset) | https://clics.clld.org/ · download https://clics.clld.org/download | CC BY 4.0 | `network/` + `screenshots/` |

Everything else the two research docs named is either **already local** (§ Already-local — do not re-fetch) or a **method reference** (§ Method-reference — nothing to fetch).

---

## Network panel — detail

**1. McLean — `JapaneseMimeticNetwork`** · CLONE · **MIT** · ✅
`git clone https://github.com/BonnieMcLean/JapaneseMimeticNetwork.git` → `network/`
Zenodo DOI [10.5281/zenodo.14598876](https://doi.org/10.5281/zenodo.14598876). R + `shiny` + `visNetwork`. Files that matter: **`nodes.csv` + `edges.csv`** (the entire data model is two CSVs), `app.R` (the interaction logic), `README.md`, `LICENSE`. Live demo → row 2. **Why #1:** it is literally a Japanese-mimetic semantic network with hover-preview and click-expand — the closest existing thing to our network panel, in-domain, permissively licensed, trivially adaptable. Cite the book chapter when reusing: McLean & Dingemanse, "A multi-methods toolkit for documentary research on ideophones" (in *Capturing Expressivity*, OUP; preprint https://doi.org/10.31219/osf.io/3n85v).

**2. JapaneseMimetics live demo** · SCREENSHOT · ✅ · → `screenshots/`
https://bonnie-mclean.shinyapps.io/JapaneseMimetics/ (Shiny app — JS-rendered, capture by hand). Capture: (a) the full network at rest; (b) **node hover-preview** (lexical entry / image / short video embedded in a node); (c) a **click-expanded** node. These three views define the interaction model to mirror.

**3. McLean — `SiwuIdeophoneNetwork`** · CLONE · **MIT** · ✅
`git clone https://github.com/BonnieMcLean/SiwuIdeophoneNetwork.git` → `network/`
Zenodo DOI [10.5281/zenodo.14598830](https://doi.org/10.5281/zenodo.14598830). Same shiny/visNetwork stack. Files that matter: `app.R`; **`Figure10_nodes.csv`/`Figure10_edges.csv`** and **`Figure11_nodes.csv`/`Figure11_edges.csv`** (domain wheel vs expanded sub-network); **`Figure4_MDS_example.R`** (the MDS→layout method, portable to the JP panel); `siwu-sortingtask-data.csv` + `siwu-sortingtask-analysis.R` (how a pile-sort becomes a graph). **Why:** the **alternative-view** precedent — opens on a domain wheel, click a domain to expand its sub-network, double-click a node to expand/contract clusters. Maps onto the Perception-Ladder hierarchy entry point. Same citation as #1.

**4. SiwuIdeophones live demo** · SCREENSHOT · ✅ · → `screenshots/`
https://bonnie-mclean.shinyapps.io/SiwuIdeophones/ (capture by hand). Capture: (a) the opening **domain wheel**; (b) a **domain clicked → expanded** sub-network; (c) a **double-click expand/contract** in progress.

**7. Observable — force-directed graph** · SCREENSHOT (+ forkable code) · ISC · ✅ · → `screenshots/`
https://observablehq.com/@d3/force-directed-graph/2 (Les Misérables co-occurrence; `d3-force`). Capture the interaction (drag, zoom). **Why:** McLean's apps are R/Shiny; the Arena is React/Vite/TS, so `d3-force` is the likely **web-native implementation** of the same interaction model. Code is ISC (forkable). Module reference: https://d3js.org/d3-force (ISC).

**9. Observable — hierarchical edge bundling** · SCREENSHOT · ISC · ✅ · → `screenshots/`
https://observablehq.com/@d3/hierarchical-edge-bundling (Bostock; Holten 2006 method). **Why:** a radial idiom McLean's node-link apps don't use — bundles many cross-tier confusions along the Perception-Ladder hierarchy into legible arcs; fits the "arena" motif. Method explainer (with R/Python/React/D3 galleries): https://www.data-to-viz.com/graph/edge_bundling.html.

**8. SWOW — Explore the lexicon** · SCREENSHOT · ⚠ view-only · ✅ · → `screenshots/`
https://smallworldofwords.org/en/project/visualize (enter a cue word; JS-rendered, capture by hand). Capture: a cue-word association graph with **cluster colors**, **neighborhood-on-hover**, **degree-scaled nodes**. **Why:** the design reference for making a *large* association graph legible. ⚠ SWOW association norms are released for research use (typically non-commercial) — this is a **legibility/design screenshot only**, not a data fetch; don't redistribute SWOW data.

**11. CLICS⁴** · FETCH (optional) + SCREENSHOT · **CC BY 4.0** · ✅ · → `network/` + `screenshots/`
Live: https://clics.clld.org/ · dataset download: https://clics.clld.org/download · Zenodo [10.5281/zenodo.16900179](https://doi.org/10.5281/zenodo.16900179). ⚠ **Version note:** the seed set said "CLICS³"; the live site is now **CLICS⁴** (Tjuka et al. 2025, adds data + improvements over CLICS³). CLICS³ remains a valid archived Zenodo dataset if you specifically want that snapshot; for a precedent screenshot use the live CLICS⁴. **Why:** the canonical in-domain "linguistic data as an explorable graph" reference; the CLDF colexification data could also seed a meaning-space layer. Screenshot a single concept's colexification graph; only FETCH the CLDF bundle if you actually build on the data.

---

## Divergence panel — detail

The divergence panel's inputs are **almost entirely already local** — the seed set's "FETCH from OSF" for McLean 2023 is superseded (see § Already-local). Fetch nothing here; work from local files + method notes.

**Chart spine:** guess-accuracy (x) × mean-rating (y) scatter, one dot per word, **colored by lexical stratum** (ideophone vs prosaic), marginal densities on each axis. Precedents to mirror: thesis Figs 15/16 (in Nils's own data) and McLean 2023 Figs 7/8 (stratum-colored + regression interaction). Chart-idiom refs and method notes below.

Already-local feeders (see § Already-local for paths/licenses): `thesis-per-pair-stats.csv` (30 pairs, now), `mclean2023-measures/GuessingRatingScores.csv` (304 items, the stratum backbone), `triangulating_iconicity/` (per-item guessability + audio), `playful_iconicity/` (ridgeline idiom), `gorilla-tidy-choosing/rating.csv` (trial-level, live-data shape).

Method notes to drop in `divergence/` as `.md` (see § Method-reference for URLs): scatter-plus-marginal-density (`seaborn.jointplot`), SDT d′/criterion (position-bias sub-panel), Wilson score CIs (honest per-word error bars), brms Bayesian mixed models (credible-interval divergence ribbons).

---

## Modality-norm panel — detail

**10. Lancaster Sensorimotor Norms** · FETCH · ⚠ **CC BY-NC-SA** · ✅ · → `modality/`
Download from OSF **https://osf.io/7emr6/** (landing page https://www.lancaster.ac.uk/psychology/lsnorms/ — JS-rendered, returned no body on fetch; use the OSF project for the files). Lynott, Connell, Brysbaert, Brand & Carney (2020), *Behavior Research Methods* 52(3):1271–1291. 39,707 English words × 11 dims (6 perceptual incl. **interoception** + 5 action effectors). **Why:** the large-scale **English** counterpart to Iida & Akita's Japanese norms — an independent cross-linguistic check on the aud > vis > int axis, and a reference for how interoception is normed at all. ⚠ **License caveat:** unlike the CC BY 4.0 assets, this is **CC BY-NC-SA** — fine for a **non-commercial** portfolio piece **with attribution and share-alike**, but not for anything commercial. Flag before any hosted commercial use.

Already-local feeders (see § Already-local): `Perceptual_strength_norms_JpnRaw.csv` (Iida & Akita, the radar's data), `iconicity_ratings/` (Winter, density + coefficient-plot idioms + calibration round).

Method note for `modality/`: raincloud (`ggdist` `stat_halfeye` + `stat_dots`) vs ridgeline for 1–7 distributions per tier — tutorial URL in § Method-reference. (Fable arbitrates raincloud vs ridgeline; see the brief.)

---

## UX / genre prior art — detail

**5. Wordbank** · SCREENSHOT (study, don't fork) · data CC BY 4.0 / repo GPL-2.0 · ✅ · → `screenshots/`
https://wordbank.stanford.edu/data — two named tools to capture: **Vocabulary Norms** (https://wordbank.stanford.edu/data?name=vocab_norms — aggregate growth curves) and **Item Trajectories** (https://wordbank.stanford.edu/data?name=item_trajectories — per-item drill-down). **Why:** the productionized "research dataset → browsable instrument" template NIL-71 is spec'ing — item-level drill-down *and* aggregate curves in one dashboard, an altitude above McLean's single-dataset apps. ⚠ **Don't fork:** the repo (github.com/langcog/wordbank) is a **GPL-2.0** Django app whose README says to use the web frontend or the `wordbankr` R package, not raw repo data. Study the UX; don't lift the stack.

**6. Great Language Game** · FETCH + SCREENSHOT · **CC BY 3.0 AU** · ✅ · → `ux-precedent/`
Data: https://f003.backblazeb2.com/file/larsyencken-eu-public/greatlanguagegame/confusion-2014-03-02.tbz2 (**145 MB**, ~16M guesses, one JSON record per line: `target, sample, choices, guess, date, country`). Landing: https://lars.yencken.org/datasets/great-language-game. Confusion visualization to screenshot: https://lars.yencken.org/language-confusion/. **Why:** the only **gamified-2AFC-at-scale sibling** — a forced-choice listening game whose aggregate stats are shown as a **confusion matrix**. Directly motivates a distractor-confusion-matrix panel (which wrong word gets picked, for which target) and a "most-confusable pairs" view. **CC BY 3.0 AU** — reusable with attribution.

---

## Already-local — DO NOT re-fetch (correction to the seed set)

A prior session (NIL-53/59, Fable, 2026-07-02) already cloned/extracted most of the divergence + modality substrate. **Four items the seed set listed as CLONE/FETCH are already on disk** — pulling them again would duplicate work and risk Fable "re-inventing" data. Paths verified this session.

| Source | Local path | License | Note |
|---|---|---|---|
| **Thesis per-pair stats** | `research/data/thesis-per-pair-stats.csv` | Nils's own thesis data | 30 pairs: `accuracy_pct, mean_rating, modality, median_rt_ms`. The divergence scatter drives off this now. |
| **McLean 2023 — GuessingRatingScores** | `research/data/ideophone-dataset/mclean2023-measures/GuessingRatingScores.csv` | repo **MIT** (© 2022 B. McLean); paper data CC BY 4.0 | 607 rows = **304 items × {guessing, rating}** (`identifier, ideophone, method, z_score, score`). ⚠ **Seed said "FETCH from OSF" — not needed, it's local** (full repo incl. `substitutions.csv`, `ideophone_list.csv`, analysis `.Rmd`). |
| **Punselie — triangulating_iconicity** | `research/data/ideophone-dataset/triangulating_iconicity/` | **CC BY 4.0** (NIL-59; no in-repo LICENSE file) | 239 ideophones (**81 JP**), per-item guessability, structural coding, `app.R`, **245 `.wav`**. Build asset, confirmed local. |
| **Winter — iconicity_ratings** | `research/data/ideophone-dataset/iconicity_ratings/` | ⚠ **no in-repo LICENSE**; paper open (BRM 2023) | 14,776 words (`ratings/iconicity_ratings_cleaned.csv`) + Bayesian model scripts. ⚠ **Seed said CLONE — already local.** Upstream github.com/bodowinter/iconicity_ratings. Verify license before *hosting* the data. |
| **Dingemanse & Thompson — playful_iconicity** | `research/data/ideophone-dataset/playful_iconicity/` | ⚠ **no in-repo LICENSE**; OSF 7s6xc / paper | 70,462 words × funniness + iconicity; scatter + density-ridgeline idiom. ⚠ **Seed said CLONE — already local.** Upstream github.com/mdingemanse/playful_iconicity. Verify license before hosting data. |
| **Iida & Akita — perceptual strength norms** | `research/data/ideophone-dataset/Perceptual_strength_norms_JpnRaw.csv` | Iida & Akita 2023 (use w/ citation) | 510×17; 6-modality vectors → the per-word radar. Already the project's modality classifier. |
| **Thesis trial-level data** | `research/data/gorilla-tidy-choosing.csv`, `…-rating.csv` | Nils's own | Per-participant × trial; the shape live data will take. Feeds divergence + raincloud panels. |
| **McLean — IcoTools** | `research/data/ideophone-dataset/IcoTools/` | **MIT** (© 2021 B. McLean) | `foiler` = method reference for the deferred Foil Arena; not a panel. |

---

## Method-reference shelf — nothing to fetch (verified reading URLs)

Techniques named by NIL-70/NIL-75; apply per panel, no asset to download. All URLs resolved this session.

| Method | When to apply | Reference (✅ resolves) |
|---|---|---|
| Scatter + marginal density | Divergence scatter (x=accuracy, y=rating, hue=stratum, KDE margins) | https://seaborn.pydata.org/generated/seaborn.jointplot.html |
| Raincloud plot (`ggdist`) | Rating-Lab 1–7 distributions per modality tier; per-word accuracy spread | https://www.cedricscherer.com/2021/06/06/visualizing-distributions-with-raincloud-plots-and-how-to-create-them-with-ggplot2/ |
| Signal Detection Theory (d′ + criterion c) | Position/order-bias sub-panel (uses the server-seeded left/right + top/bottom shuffle) | https://link.springer.com/article/10.3758/s13428-022-01913-5 |
| Wilson score interval | Honest error bars on per-word / per-session accuracy at small n | https://corplingstats.wordpress.com/2024/12/12/bootstrap-intervals/ |
| Bayesian hierarchical models (brms) | Credible-interval divergence ribbons; partial pooling over sparse per-word cells | https://arxiv.org/abs/1611.00083 |
| `d3-force` (network engine) | Web-native force-directed graph in React/Vite | https://d3js.org/d3-force |
| Hierarchical edge bundling | Radial cross-tier-confusion view | https://www.data-to-viz.com/graph/edge_bundling.html |

---

## License summary + flags

**Clean CC BY 4.0 / permissive (reuse with attribution):** JapaneseMimeticNetwork (MIT), SiwuIdeophoneNetwork (MIT), IcoTools (MIT), mclean2023-measures (repo MIT; paper data CC BY 4.0), triangulating_iconicity (CC BY 4.0), CLICS⁴ (CC BY 4.0), Wordbank data (CC BY 4.0), Great Language Game (CC BY 3.0 AU), Observable d3 examples (ISC code).

**Flag before hosting / commercial use:**
- ⚠ **Lancaster Sensorimotor Norms — CC BY-NC-SA.** Non-commercial + share-alike. OK for a non-commercial portfolio piece with attribution; not for commercial deployment.
- ~~⚠ **Winter `iconicity_ratings` & Dingemanse/Thompson `playful_iconicity` — no explicit LICENSE file in the repo.**~~ **RESOLVED 2026-07-05: both confirmed CC BY 4.0 (Nils).** Reusable and hostable with attribution; the flag below is retained only as history.
- ⚠ **SWOW — screenshot/design reference only.** Association norms are research-use (typically non-commercial); don't redistribute the data.
- ⚠ **Wordbank repo — GPL-2.0** Django app; the README says use the frontend / `wordbankr`, not raw repo data. Study the UX, don't fork.

---

## Correction log (what changed vs the seed set)

1. **Winter `iconicity_ratings` and `playful_iconicity`: CLONE → ALREADY-LOCAL.** Both are already cloned under `research/data/ideophone-dataset/`. No fetch.
2. **McLean 2023 `GuessingRatingScores.csv`: FETCH-from-OSF → ALREADY-LOCAL.** Full `mclean2023-measures/` repo is on disk (repo LICENSE = MIT).
3. **`triangulating_iconicity`: confirmed ALREADY-LOCAL** with **245 `.wav`** present (seed said "241 wav" — 245 files on disk).
4. **CLICS³ → CLICS⁴.** The live site upgraded; use CLICS⁴ for the precedent screenshot (CLICS³ still available as an archived Zenodo dataset if a specific snapshot is wanted).
5. **Lancaster norms license surfaced: CC BY-NC-SA** (not flagged in the seed); canonical download is **OSF 7emr6**, not the JS-only Lancaster landing page.
6. **SWOW view pinned** to `/en/project/visualize` ("Explore the lexicon"), the actual network-graph tool, not the participation homepage.
7. **Wordbank reclassified** primarily SCREENSHOT (study, don't fork) — repo is GPL-2.0 and its README discourages raw-repo data use.
8. **No-license flags added** for the two Winter/Dingemanse repos (verify before hosting data).
9. _2026-07-05 (NIL-71 follow-up):_ **Winter `iconicity_ratings` + `playful_iconicity` confirmed CC BY 4.0** (Nils) — entry 8's flags resolved. Deployment additionally confirmed non-commercial, so the Lancaster CC BY-NC-SA caveat is compatible too; attribution remains mandatory everywhere.

_Uncommitted — for Nils's review. Commits are Nils's job._
