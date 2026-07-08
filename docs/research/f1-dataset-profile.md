# F1 — Dataset profile & join feasibility (NIL-53 kickoff)

_Fable 5, 2026-07-01. First half of F1: profiling + join design, run against the real files. Second half (selection criteria → ranked candidate list → sign-off sheet) builds on this. Companion to `research/INDEX.md`._

## Dataset profiles (verified by pandas, not provenance notes)

**top600-ninjal-lwp-for-bccwj.xlsx** · sheet `ninjal-lwpp-bccwj-600`: **595 rows × 13**. Per word: 見出し (hiragana), 読み (katakana), ローマ字表記 (Hepburn romaji — the join key), 頻度 (BCCWJ freq), jpTenTen katakana freq + hiragana freq (`Item`/`Frequency` pairs), plus working columns `is G>I?`, `aud-cand`, `is in K?`, `G-I=` (log-ish script-preference score). Script frequency for **all 595 words is already in this one sheet** — the kata-vs-hira workbook is not needed for the pipeline, it's the analysis that produced these columns. Sheet `hira-audio` (34 rows) = a hand-picked auditory candidate list worth revisiting during selection.

**Perceptual_strength_norms_JpnRaw.csv** (Iida & Akita 2023) · **510 rows × 17**, zero duplicate words. `Word` = romaji; six modality ratings (Auditory/Visual/Haptic/Gustatory/Olfactory/Interoceptive) + `Dominant_modality` (22 nulls), `Maximum_perceptual_strength`, `Iconicity` (57 nulls), `Modality_exclusivity`, own `Frequency`. `Category` splits: **112 Ideophones**, 131 Verbs, 123 Adjectives, 106 Nouns, 38 Function words — the non-ideophone rows are a possible future control/foil resource (deferred).

**perceptual-strength-ideophones-kata-vs-hira-in-sketcheng.xlsx** · `per.strg.k>h`: 110 rows = norms ideophones already merged with script freq (kata/hira + `roma`). Modality-split sheets (`k>h-intero` 9, `k>h-audio` 10, `k>h-visual` 13, `k>h-haptic` 6) and an `antonym` scratch sheet — messy working sheets (data-in-header rows); treat `per.strg.k>h` as the only load-bearing sheet.

**jmdict-wordlist…xlsx** · `jm-kata` (358) / `jm-hira` (628): kana Item + jpTenTen freq only — **no glosses**. Sheets 1/3/4/5/6 are scratch. Low pipeline value beyond cross-checking; gloss sourcing needs live JMDict lookup instead.

**japonic-sensory-lexicon/** (CLDF) · `concepts.tsv` 110 concepts (with Japanese exemplar sentences + English translations, `freq_japweb2011`), `dat.csv` 6,119 dialect forms × 49 locations, `languages.csv` with Hirayama geo/population. Not a stimulus source for core 2AFC; it's the **sentence-context + dialect-mode reserve** (licensing check pending — LICENSE/CITATION ship with it).

## Join design

**Key = lowercase Hepburn romaji.** top600 `ローマ字表記` ↔ norms `Word` ↔ sketcheng `roma` all agree. The thesis inventory (68 words incl. 8 practice, extracted from `condition-1-choosing-sokuon.csv` filenames) uses Nihon-shiki-ish romanization (`zi`, `si`, `tu`, `hu`, `N`, `Q`) → convert with the standard mapping.

**Pitfall (must handle in the pipeline):** word-final sokuon. Thesis files write `paQ`, `hoQ`; the norms write `paq`, `hoq`. Naive Hepburn conversion that drops final Q creates false non-matches (and made `paq`/`hoq` masquerade as new candidates in the first pass). Rule: preserve final sokuon as `q` when matching against norms.

## Coverage (romaji join, thesis words excluded)

| Pool | Definition | n | Covariates |
|---|---|---|---|
| **A** | top600 ∩ norms-ideophones, minus thesis | **38** | Everything: BCCWJ freq, kata/hira script share, 6-modality vector, dominant modality, max strength, exclusivity, (partial) iconicity |
| **B** | norms-ideophones only, minus thesis (incl. the `paq`/`hoq` correction → ~49) | **~49** | Modality vector + norms freq + iconicity; **no BCCWJ/script freq** (recoverable: SketchEngine query or jpTenTen lookup) |
| **C** | top600 only (remainder ~480 after thesis/A overlap) | ~480 | Frequency + script share; **no modality** — unusable for modality-gated modes without new norming or imputation |

Sanity anchors: thesis ∩ top600 = 55/68 (the 60-word study drew from this pool, as expected); thesis ∩ norms-ideophones = 23 (+2 with the sokuon fix).

**Pool A dominant-modality split: Visual 16 · Interoceptive 9 · Haptic 8 · Auditory 2 · Gustatory 2 · null 1.**
Pool B adds: Visual 21 · Auditory 12 · Interoceptive 10 · Haptic 6.

## Implications (feed F2 + the full F1 selection pass)

1. **Auditory is the bottleneck, not the surplus.** Pool A has only 2 auditory candidates (batabata, hissori); even A+B gives 14. The thesis ordering (aud easiest) means auditory pairs are the game's "easy floor" fuel — expansion must lean on Pool B + the `hira-audio` hand list, accepting the script-freq lookup cost.
2. **A haptic floor is viable; gustatory/olfactory are not.** Haptic: 14 candidates across A+B (shittori, fuwafuwa, sarasara, nebaneba, dorodoro…). Gustatory: 2 (assari, sappari). The five-modality ladder extension realistically means **four floors (Sound → Sight → Touch → Inner states)**, not six. This constrains the F2 five-modality design.
3. **The full 6-dim vector is richer than dominant modality.** Exclusivity and multimodality (e.g. words strong on two axes) are usable difficulty/flavor variables — a genuinely new axis the thesis didn't use.
4. **Two pipeline gaps are external to these datasets:** English glosses (norms/top600 have none — JMDict lookup + human adjudication per word) and **audio** (every new word needs a recording under invariant 2 — already flagged as a separate decision).
5. **Pairing constraint (invariant 4)** — real contrastive same-modality pairs — will be the binding filter: 38–87 candidates is plenty of words but pairs need meaning contrast within modality, so expect roughly 15–30 new *pairs* after sign-off, not 40+.

## Second half — insights, scoring, pairs (run 2026-07-01, same session)

### Insight mining (Spearman; small n — treat as descriptive, 10 tests run)

1. **The dissociation replicates in Nils's own data.** Across all 30 thesis pairs, accuracy ~ mean rating: **rho = +0.16 (p = .40)** — guessability and felt iconicity are nearly orthogonal. This is McLean, Dunn & Dingemanse's two-measures finding reproduced within the thesis dataset, and it *is* the empirical license for Rating Lab + the divergence endpoint. Landing-page gem: "knowing it when you hear it and feeling it are different skills — our data shows both."
2. **Accuracy ~ median RT: rho = −0.33 (p = .07).** Harder pairs are answered slower; RT is a usable soft difficulty signal once live data accumulates.
3. **Published norms cannot predict thesis difficulty.** Only 9/30 thesis *targets* join the norms (iconicity subset n=3 — unusable). Consequence: difficulty priors for new pairs come from modality ordering + contrast fineness, then get **validated by live play**. This turns a data gap into a design feature: *the game norms its own stimuli* — per-pair live accuracy feeds back into tier placement. (Greenfield hook for F2: the self-norming loop.)
4. **Phoneme shape predicts event structure, not iconicity magnitude** (n=112 norms ideophones). Reduplication: Auditory 69% / Haptic 67% vs Visual 33% / Interoceptive 37%. The -ri suffix inverts it: Visual 50% / Interoceptive 43% vs Auditory 6% (classic continuative-vs-punctual aspect semantics). Voiced onset shows **no** iconicity/strength advantage (medians 3.05 vs 2.84, p = .61). So the F2 phoneme-shape mode should be "read the word's shape" — reduplication/-ri/-q/-N templates as guessable cues to aspect and modality — **not** a bouba-kiki voicing gimmick, which this data does not support.
5. **The Convention Frontier.** hakkiri, yukkuri, shikkari, sukkari, bikkuri: hyper-frequent (up to ~9.9K), norm-rated, iconically opaque. They are literally "where convention has taken over" — a named hard tier that dramatizes the conceptual spine. Kept out of core pairs; sheet `ConventionFrontier` in the workbook.

### Deliverable — `stimulus-expansion-signoff.xlsx`

87 candidates scored (transparent formula in the README sheet), **23 proposed pairs across four floors** (Auditory 6 · Haptic 6 · Interoceptive 5 · Visual 6; gustatory confirmed dead): 13 both-new + 10 mixed pairs reusing an existing thesis word — **36 new recordings** if everything is approved, vs 46 for all-new pairing. All glosses are drafts in the thesis meaning-prompt style and **every row requires Nils's sign-off**; mixed pairs additionally carry a `thesis_modality_check` flag (haptic-floor reuse re-classifies a thesis word per the norms — an explicit adjudication).

### Next

Nils's sign-off pass over the workbook → pairing-pipeline data model (input to F4's architecture review) → SketchEngine/jpTenTen script-frequency lookup for the pool-B shortlist → the audio-recording decision (its own gate, as committed).

## 2026-07-02 — OSF-archive mining (NIL-53/56 extension, Fable 5)

_Five archives extracted and mined (see INDEX.md for provenance/shape); puneslie2024.zip md5-verified as a byte-identical mirror of `triangulating_iconicity/` and skipped. **17 statistical tests run this session** (Spearman / Wilcoxon / Mann-Whitney), exploratory and uncorrected — treat p in [.01, .05] as suggestive, and every n ≤ 30 result as descriptive._

### 1. The divergence framing survives at scale — but the copy needs a correction

mclean2023 (304 items = 101 ideophones + 203 prosaic controls, guessing with constructed max-distance foils + iconicity ratings):

- guess ~ rating: **rho = +.435 all items** (p = 1.9e-15, n = 304); **+.651 within ideophones** (n = 101); +.397 within controls (n = 203). The two measures are correlated, not orthogonal.
- The real dissociation is *what they detect*: guessing does *not* separate ideophones from prosaic words (medians .567 vs .576, p = .94, Cliff's δ = −.01) while ratings separate them massively (.433 vs .298, p = 1.3e-12, **δ = +.50**).

**Consequence:** the thesis-internal rho = +.16 (n = 30, pair-level) fed landing copy like "nearly orthogonal" — retire that phrasing (small-n estimate, contradicted at scale and at a different unit of analysis). The stronger, scale-backed line: **"ratings know an ideophone when they hear one; guessing doesn't — the two measures see different things."** Rating Lab's license is upgraded, not weakened.

### 2. Guessability is a property of the form–meaning *pairing*, not the word

mclean2023 tested two forms under two concepts each: iraira .70 vs .39 guessability; nebaneba .38 vs .20. Same word, different meaning prompt, wildly different guessability. This is direct external validation of our per-pair (`arena_rounds`-level) difficulty model — and an argument that NIL-57's D4 "shared per-item live-stats source" should be keyed on **pairing/round, not word** (escalated to the ledger).

### 3. New external anchors (mclean2023 guess/rating, canonical romaji join, final sokuon = `q`)

9 thesis words: zokuzoku .77/.55 · dokidoki .74/.86 · bonyari .70/.44 · iraira .70/.42 + .39/.49 · shitoshito .57/.32 · hokkori .57/.24 · zuruzuru .57/.29 · nikoniko .50/.40 · sukkiri .40/.31. Approved-batch words: **fuwafuwa .70/.62, nebaneba .38/.33 + .20/.30** (feeds the H-floor priors as *weak* secondary anchors — constructed-foil regime, English online raters; not our task).

### 4. Foil distance does not predict difficulty for real contrastive pairs (the NIL-58 result)

- Applied the SPEC-free-form-entry §5 feature-distance formula between the two words of each thesis pair: **distance ~ accuracy rho = −.155, p = .41, n = 30** — null, trending the *wrong* direction. Mora-Levenshtein is degenerate on invariant-4 content: **28/30 pairs sit at maximum distance** (real same-modality contrastive pairs are nearly always fully segmentally distinct). The most form-similar pair in the set (doshidoshi/gishigishi) was among the *easiest* (83.3%).
- Where form distance demonstrably *does* move accuracy is the constructed/degraded regime: dingemanse2016 (203 items, 82 listeners, within-category meaning foils, 4 audio versions) — original ≈ full resynthesis ≈ **57%** (55% excl. Sound), phones-only **51.8%** / prosody-only **52.2%** (each n.s. vs chance for cross-modal items); Sound-domain items survive degradation (phones-only β = .46, p < .001; prosody-only β = .29, p = .04); category effect Sound ≫ rest (β = .31); no category × version interaction. Segments and prosody are *jointly* necessary — a caution for any purely segmental distance metric.
- mclean2023 ships the enabling artifact: `substitutions.csv`, **105 phoneme-pair featural distances** (incl. palatalized + geminate units) used to build maximal-distance foils — this is IcoTools `foiler`'s logic as data.

**Consequence for NIL-58:** the foil-distance engine's dial governs **constructed-foil content (deferred Foil Arena) and degraded-stimulus designs, not core invariant-4 pairs**. Keep computing and recording pair distance (cheap, from the shared `PhonologyService`) as a *validation covariate*; do not use it as an ordering/difficulty principle for real pairs. The four-floor spec's framing (H-floor as a clean prediction test) is exactly right; the phoneme-shape spec's within-tier ordering claim was not, and has been revised. Note the scope: this validates nothing about the *production scorer* (scoring a player's invention against a target is a different use than predicting 2AFC difficulty between two real words).

### 5. Learnability (lockwood 2016) — real effect, weak per-item measure, no fifth mode

- Condition effect replicated exactly from raw data: real-meaning pairs remembered **86.7%** vs opposite-meaning **71.3%** (Wilcoxon p = 4e-5, **Cohen's dz = 1.07**, n = 29 participants). Iconicity aids learning; clash actively hurts. Post-test 2AFC 73.0%.
- **Provenance find:** Punselie's Collabra guessability *is* the lockwood post-test, per item (max abs diff 0.0000 over 38 items) — same 29 participants. The 38 Collabra anchors and any lockwood-derived learnability numbers are not independent measurements.
- Per-item signal is thin: each item lived in one condition arm (n = 19 per arm); learn-acc ~ guessability rho = +.30 (real arm) / −.25 (opposite arm), both n.s.; learn-acc ~ rating +.43 (p = .067) / +.01. Directionally coherent with iconic bootstrapping, nowhere near load-bearing.
- **Fifth-mode verdict: no.** The clean paradigm requires teaching players *false* meanings (opposite arm) — unacceptable for a public research-honest app — and a real-only variant loses the contrast that makes the measure informative (ceiling at .87). Not a free-form-entry mechanic either (that would measure the generation effect, not iconicity). Filed as a designed-but-deferred mode in `game-mode-roadmap.md`; explicitly **not** a build-one candidate. Salvage: the clash-is-costly finding (incl. its P3/LPC ERP signature) is citable research-flavor copy for feedback/landing ("your brain fights a word whose sound points the wrong way"), and the 37-row opposite-gloss sheet is a vetted antonymic-contrast reference for pairing work.
