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
