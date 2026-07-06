# ARCHITECTURE — Target data model for all modes, languages, and the expansion pipeline

_NIL-57 (F6) output, Fable 5, 2026-07-03. **Status: ADOPTED 2026-07-03** (adjudication in session: ADR-1/-2/-4/-5/-7/-8 adopted as drafted; ADR-0 produced on reopen and adopted; ADR-3 re-decided with the rename; 28A gate and NIL-54 Gate A confirmed; migration plan approved final as a plan — execution stays gated per session). Scope is data-model only per the pre-session adjudications (UI system is NIL-64/65; sequencing NIL-57 → NIL-64 → NIL-65 → builds). Audited against the post-27E backend (80 tests green, contract changelog 2026-07-03) and the four F2 specs' D-ledgers. Home: planning folder now; copied to the backend repo's `docs/` by a later session. Supersedes F4's architecture-review remit._

_Constraints honored throughout: invariants 1–5 unconditionally; schema SQL-owned via `scripts/generate_seed_sql.py --check`; `ddl-auto=validate`; no new dependencies. **Constraint revision (adjudicated mid-session, binding):** the pre-session additive-over-rewrite preference is lifted; the mandate is the most coherent target model, with backward compatibility a design input only for the frozen public contract (`/api/research/divergence` response shape) and invariants 1–5. Pre-W30 rework is cheap by the §13 regime note (full regen, dev-only data, thesis rows generator-emitted); decisions where the additive preference was load-bearing were re-examined under this revision (ADR-0, ADR-3)._

## 0. Audit verdict (the F4 question)

**Does the post-27E schema hold 600 words, ladder floors, phoneme features, and a third measure?** Volume, yes — 600 words ≈ 600 `words` + 1,800 `presentations` rows plus 239 cross-linguistic words is nothing to MySQL. Structurally, no, on four counts:

1. **No word identity above the condition row.** `ideophones` is one row per word × script condition (68 words → 204 rows). Every aggregate that says "per word" today actually keys on a condition-row id, and — verified against source this session — the defect is live in the **event plane**, not just analytics: the ratings uniqueness guard and the ratable-pool query both operate at row grain, so cross-condition Script Lab play can offer and accept the same word twice (ADR-0, which resolves this structurally).
2. **No pairing identity.** The pair is the thesis's unit of difficulty and mclean2023 proves guessability attaches to the form–meaning pairing, not the word (iraira .70 vs .39 under two concepts). Pairing identity exists only as the stimulus-filename prefix (`a0`, `i9`) — data the generator parses but the schema never materializes. The expansion pipeline (23 signed-off pairs), per-pair provenance, floors, priors, and foil-distance covariates have no home (ADR-6).
3. **No language dimension.** Nothing marks the 68 Japanese words as Japanese, and the lexical unique key has no language column (ADR-1).
4. **No feature or provenance side-tables.** Phoneme features (template mode) and per-stimulus source/licensing (241 CC BY 4.0 wavs) need homes (ADR-8.3, ADR-7).

**What is sound and stays** (the adjudicated scope guard): sessions, answers, auth, the deterministic-shuffle contract (2026-06-12, algorithm byte-for-byte), and the measure-vertical separation. **What restructures:** two tables — `ideophones` splits into `words` + `presentations` (ADR-0), and `arena_rounds` becomes `trials`, collapsed and cleaned (ADR-3) — plus two new reference tables (`languages`, `pairings`), two side-tables (`word_features`, `stimulus_sources`), one specced measure vertical (`productions`), and one session column (`game_mode`). The event plane keeps its one-answers-table shape; it re-keys to word grain.

## 1. Target entity model

```
languages 1──< words 1──< presentations      (core scripted words only: 68 × 3 conditions;
                │  │                          XL words carry no presentations)
                │  ├──1 word_features        (jpn words, v1; one row per word)
                │  └──< stimulus_sources     (external datasets/anchors, per word × dataset)
                │
languages 1──< pairings 1──< trials 1──< player_answers >──1 game_sessions >──1 app_users
                (word_a/word_b     (round_type, correct_word_id,   (selected_word_id,      (game_mode,
                 FKs to words)      feature_axis, is_practice)      target_word_id)         shuffle_seed,
                                                                                            condition_name)

app_users 1──< ratings     >──1 words   (UNIQUE(user_id, word_id); session_id nullable)  } measure verticals,
app_users 1──< productions >──1 words   (UNIQUE(user_id, word_id); session_id nullable)  } session-free by design
```

Three planes, kept deliberately distinct:

- **Content plane** (seed-owned, regenerable): `languages`, `words`, `presentations`, `pairings`, `trials`, `word_features`, `stimulus_sources`. Everything here is emitted by `generate_seed_sql.py`; `--check` is the integrity layer.
- **Event plane** (player-owned, append-only): `player_answers` (all round-based modes, one table, word-keyed), `ratings`, `productions`. Measure verticals stay separate tables because their event shape is different (no trial, one-per-user-per-word — now enforceable at exactly that grain).
- **Account plane**: `app_users`, `game_sessions`. Sessions gain the mode discriminator and keep `condition_name` (a session-level fact; it left the content plane with the trials collapse).

What stays language-agnostic: sessions, answers, ratings, productions, the shuffle contract, the stats engine. Language attaches at exactly two points — `words.language_id` (ADR-1) and its denormalized copy on `pairings` (ADR-6) — plus a per-language profile seam inside `PhonologyService` (ADR-8).

## 2. ADR-0 — Word identity as an entity

**Status:** Adopted (produced on adjudicated reopen; the additive preference that had suppressed this ADR was lifted). **Deciders:** Nils. **Context:** Audit defect #1 named the missing word grain, but the first draft compensated by convention in three places — the lowest-id-row rule for pairing member FKs and anchor attachment, `word_features` triple-emission with triple-consistency assertions, and romaji-grouping for the word stat tier — and left the event plane row-keyed.

**The verified finding (against source, 2026-07-03).** `PlayerAnswerRepository.findRatableWordsByUserId` groups by `ideophone.id` and its `NOT EXISTS` checks `rating.ideophone = ideophone` — both at condition-row grain. `RatingService.createRating` guards duplicates with `existsByUserIdAndIdeophoneId` plus the `UNIQUE(user_id, ideophone_id)` race constraint — same grain. Consequence: a player who answers in `CONDITION_1`, rates word W (condition-1 row id), then plays `CONDITION_2` — exactly what Script Lab invites — sees W re-enter the ratable pool under its condition-2 row id, and a second rating of the same word passes both the pre-check and the DB constraint. The specced `productions` table (`UNIQUE(user_id, ideophone_id)`) inherits the identical hole in "your first instinct is the datum." The decisive point is structural, not cosmetic: **the race-proof 409 pattern — the verticals' core integrity mechanism — cannot express "one per word per user" at the DB layer, because no word key exists to put a UNIQUE constraint on.** Query discipline can mask this; it cannot enforce it.

**Fidelity arguments, weighed as instructed.** (1) The thesis Rating Task is word-level by design — audio presentation, one rating per word, rated word = Choosing target (thesis-facts §5); the ratable-pool endpoint already returns word-level facts (`canonicalForm`, `stimulusFile`). The row key is a schema accident of the script manipulation, not a modeling choice. (2) Invariant 2 — one shared audio file per word — is today a convention asserted over three duplicated `stimulus_file` values; with a word entity it becomes structure: one column on one row.

### Options

| | 0a — status quo + conventions | 0b — full normalization | 0c — minimal `words` + FK |
|---|---|---|---|
| Event-plane fix | service checks only; **DB cannot enforce word grain** | UNIQUE(user, word) — race-proof at instrument grain | same as 0b (re-key to word_id) |
| Word-fact duplication (gloss, modality, stimulus_file ×3) | stays, assertion-guarded | eliminated | stays, assertion-guarded |
| Invariant 2 | convention | **structure** | convention |
| Compensation machinery (lowest-row rule, triple-emission, romaji tier) | all three retained | all three deleted | partially retained (columns stay ×3) |
| Trials | ×3 per pair (condition rows) | collapse to 1 per pair (ADR-3) | ×3 (no column moves → rounds still reference condition rows) |
| Churn | none | one heavy gated session (M2) | ~80% of 0b's churn (all verticals re-key) for ~40% of the payoff |

**Decision: 0b — full normalization.** 0c is the dominated middle: it pays the coordination cost of two entities and the full vertical re-key while keeping the duplication, the mixed-grain entity, and convention-enforced invariant 2. 0a's only remaining defense after the constraint revision was cheapness, and the verified event-plane finding removes "harmless" from its description. Under "most coherent target model," 0b is not close.

### The shape

```sql
CREATE TABLE words (
    id BIGINT NOT NULL AUTO_INCREMENT,
    language_id BIGINT NOT NULL,             -- FK languages
    romaji VARCHAR(100) NOT NULL,            -- canonical (Nihon-shiki-ish, final sokuon Q); ascii join key for XL
    kana VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,        -- dictionary lemma
    canonical_form VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL, -- verbatim, presentation-invariant
    canonical_script VARCHAR(20) NOT NULL,   -- 'H' | 'K' | 'LATIN' (the word-level fact; kata_share rule recorded in generator)
    gloss VARCHAR(255) NOT NULL,
    modality VARCHAR(50) NULL,
    semantic_category VARCHAR(20) NULL,      -- dataset taxonomy; never unified with modality (ADR-1)
    stimulus_file VARCHAR(100) NOT NULL,     -- invariant 2 as structure: one audio per word, by schema
    PRIMARY KEY (id),
    UNIQUE (language_id, romaji),
    CONSTRAINT fk_words_language FOREIGN KEY (language_id) REFERENCES languages (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE presentations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    word_id BIGINT NOT NULL,                 -- FK words
    condition_name VARCHAR(50) NOT NULL,     -- which scripted condition this appearance belongs to
    display_form VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL, -- verbatim (invariant 1)
    script_code VARCHAR(2) NOT NULL,         -- legacy 2-letter provenance code (HU/KD/…), informational
    PRIMARY KEY (id),
    UNIQUE (word_id, condition_name),
    CONSTRAINT fk_presentations_word FOREIGN KEY (word_id) REFERENCES words (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

- **`presentations` = the script manipulation, and nothing else.** Rows exist only for words that participate in scripted conditions (core 68 × 3 = 204 — the same count as today's `ideophones`, which is the tell that this was always two entities). XL words carry **zero** presentation rows; every non-CHOOSING surface (XL trials, template trials, rating, production reveal) renders `words.canonical_form` verbatim. CHOOSING renders `presentation(word, session.condition_name).display_form` verbatim. Invariant 1 is untouched: every rendered string is a stored string; the split moves *where* forms live, never *how* they are produced.
- **Answers re-key to words, uniformly: `selected_word_id`, `target_word_id`.** The adjudication's "served-presentation reference where the presentation is the meaningful fact" resolves as a derivation rule rather than a column: CHOOSING is the only mode where a presentation is served, and there `(session.condition_name, word_id)` → presentation is a total function over immutable seed content — the reference is preserved losslessly without a column that would be NULL for three of four modes. Recorded revisit trigger: if a future mode ever mixes presentations *within* a session, that mode adds `served_presentation_id` then.
- **Ratings and productions re-key to `word_id` with `UNIQUE(user_id, word_id)`** — the 409 race guard now says what the instrument means. The ratable pool groups by word and the cross-condition double-offer dies structurally.
- **Shuffle contract: algorithm byte-for-byte, id vocabulary re-binds.** The derivation ("pair second = higher ideophone id") becomes "pair second = higher **word** id" — same algorithm, same stream consumption, same draws. Member relative order is preserved by seed construction (words are collected in the same order the condition rows were), and no live sessions exist pre-deploy to replay differently. The contract text gets a vocabulary rider at M2; the "session's scored rounds for its condition" clause simplifies to "the session's scored trials" (trials are condition-free post-collapse; each condition served the same 30 pairs, so the base list is unchanged in content and order).
- **`/api/research/divergence` heals to its own documentation.** The frozen contract is the response shape and the documented semantics — "one row per ideophone." Under 0b `ideophoneId` carries the word id and the endpoint finally returns what its docs always claimed; the row-id splitting was the implementation accident. Shape unchanged, clients unaffected.
- **Public DTO vocabulary:** card ids in round DTOs become word ids (the card *means* the word; the frontend echoes ids back). Whether field names stay `ideophoneId`/`selectedIdeophoneId` (vocabulary: "ideophone" = word) or rename is decided in the M2 contract rider alongside NIL-65's frontend migration — the id *semantics* are fixed here, the *names* are a rider.

### Amendments to already-adopted ADRs (all consequential, none reopened)

- **ADR-4:** within-core identity is now materialized — `UNIQUE(language_id, romaji)` on `words` *is* the lexeme key the normalization rule described. The cross-source verdict stands unchanged: XL rows stay separate words; no cross-source pooling; no lexeme table.
- **ADR-5:** the word tier keys on `words.id` instead of romaji-grouping. The directed-pairing primitive is untouched.
- **ADR-6:** pairing member FKs point at `words`; the lowest-id-row convention is deleted.
- **ADR-7:** `stimulus_sources` attaches to `word_id`, `UNIQUE(word_id, dataset)`; the lowest-row attachment note is deleted.
- **ADR-8.3:** `word_features` keys on `word_id`, one row per word; triple-emission and its triple-consistency assertions are deleted. The Python/Java golden-file parity test stays (it was never about the triplication).

**Consequences.** Easier: instrument-grain uniqueness with the existing 409 pattern; invariant 2 as schema; XL ingestion is one `words` row per item; word-level stats lose a GROUP-BY-romaji hop; the generator stops emitting the same gloss three times; three conventions and their assertions disappear. Harder: M2 becomes a heavy churn session — two entities replace one, every repository/query/mapper/fixture that touched `Ideophone` re-points, and the round-serving path gains a presentation lookup (one indexed query per round; fetch-join for members via pairing). Priced at one dedicated gated session with the 80-test net, per the adjudication.

## 3. ADR-1 — Language as a first-class entity

**Status:** Adopted. **Context:** The cross-linguistic mode ships 239 items in five languages; SPEC-cross-linguistic §4 proposes `languages` + a language FK on the lexical entity and defers the attach-point question here. Siwu/Ewe/Korean/Semai must absorb without touching Japanese-core behavior.

**Options.** (A) `languages` table + NOT NULL FK on the lexical entity, backfilled `jpn`. (B) Bare `language_code` VARCHAR, no table. (C) Attach language to presentations/trials instead of the word.

**Decision: A — `languages` as specced in SPEC-cross-linguistic §4, FK on `words` (post-ADR-0).** The table earns its rows immediately: `player_note`, `name`, `family` are player-facing copy the XL language chip needs (`GET /api/xl/languages` is a straight projection), and ISO codes as FK'd reference data beat stringly-typed codes scattered across tables. (B) saves one join and loses the copy home; (C) is wrong because language is a property of the word — presentations and trials inherit it. `semantic_category VARCHAR(20) NULL` lands on `words` in the same delta; it is the dataset's own taxonomy and **never unifies with `modality`** (Iida & Akita is Japanese-normed; mapping categories onto it would be derivation — the XL ledger's taxonomy question is answered: two nullable columns, no shared table). `Language` is an entity + repository, not an enum — new languages must never require code changes.

**Consequences.** Existing modes see nothing (all current words backfill `jpn`). XL ingestion becomes a pure content addition. The lexical natural key is `UNIQUE(language_id, romaji)` on `words` (ADR-0); `presentations` carries `UNIQUE(word_id, condition_name)` — the old `UNIQUE(kana, canonical_script)` compound dissolves into these two honest keys, and the generator's `validate_unique_constraints` re-targets them in the same commit.

## 4. ADR-2 — D1: mode dispatch

**Status:** Adopted. **Context:** Four session-based modes (Choosing/Script Lab, Ladder, Template Reading, Cross-Linguistic) must share or split the session machinery. Both the XL and template specs assume the lean option and defer confirmation here.

**Options.** (A) `game_sessions.game_mode VARCHAR(30) NOT NULL DEFAULT 'CHOOSING'` — one session model, one endpoint family, per-mode serving strategy. (B) Per-mode session tables/endpoints. (C) No discriminator; infer mode from content served.

**Decision: A — confirm the specs' lean.** One session model keeps: one auth story, one UUID contract, one shuffle-seed home, one answers pipeline, one completion semantics — all already built and tested. (B) multiplies five verticals' worth of boilerplate for zero query benefit at this scale; (C) makes the phoneme-shape spec's transfer analysis (template training → Choosing accuracy, separated by `game_mode`) impossible and leaves W28 ladder sessions permanently indistinguishable from core sessions.

Mechanics: `GameMode` Java enum, `EnumType.STRING` (house pattern: `Role`, `Modality`, `ConditionName`). Values: `CHOOSING` (default; Script Lab is `CHOOSING` + condition), `LADDER`, `TEMPLATE_READING`, `CROSS_LINGUISTIC`. Rating and Production are **not** session modes — they are measure verticals (nullable `session_id` for provenance only), which is why they carry no enum value. Session-start validation branches per mode: `conditionName`/`difficultyLevel` required exactly as today iff mode ∈ {CHOOSING, LADDER}; `TEMPLATE_READING` accepts optional `axes`; `CROSS_LINGUISTIC` accepts optional `languageScope`; unknown fields for the mode → 400 `validationErrors` per contract style. Round serving dispatches through a `Map<GameMode, RoundSource>` of Spring beans — a strategy seam, not a framework; `GameService` stays thin per `project_guidelines.md`.

**Consequences.** `POST /api/game/sessions` grows one optional field (`gameMode`, default `CHOOSING`) — additive, older clients unaffected. The round DTO already grows per-mode nullable blocks additively (27E precedent: `targetMeaningListedFirst`). Timing: the column lands with the **first consumer = 28A** (see §11).

## 5. ADR-3 — D2: one trials table, renamed and collapsed

**Status:** Adopted (re-decided under the constraint revision: substance confirmed, rename and cleanup added). **Context:** SPEC-phoneme-shape proposes `template_rounds`, SPEC-cross-linguistic proposes `xl_rounds`, both deferring to D2. The first draft extended `arena_rounds` in place and rejected renaming as big-bang churn; the adjudication re-opened the rename as a fairly-priced mechanical session.

**Decision, part 1 — one table, one answer pipeline (substance, unchanged):** the decisive constraint is in the schema — `player_answers` has exactly one FK to exactly one trials table, with `UNIQUE(session_id, trial_id)` carrying the answer-race 409. Per-mode round tables fork the event plane (three copies of the race logic, histories, stats paths) or break FK integrity. Every proposed mode's trial is "a pair of word references and a correct answer" — one table fits all three.

**Decision, part 2 — rename `arena_rounds` → `trials`, collapse the condition dimension, drop the legacy columns (new under the lifted constraint):** with ADR-0's presentations split, the three per-condition copies of each round lose their reason to exist — each condition served the same 30 pairs; only the *displayed form* differed, and that now resolves at serve time via `presentation(word, session.condition)`. The table collapses 102 → 34 rows and sheds its accidents:

```sql
CREATE TABLE trials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_type VARCHAR(20) NOT NULL DEFAULT 'CHOOSING',  -- 'CHOOSING' | 'TEMPLATE' | 'XL'
    pairing_id BIGINT NOT NULL,              -- FK pairings (every trial has one; ADR-6)
    correct_word_id BIGINT NULL,             -- CHOOSING: thesis-fixed target, documentation + NIL-54 reconstruction;
                                             -- TEMPLATE: load-bearing adjudicated answer; XL: load-bearing = target
    feature_axis VARCHAR(10) NULL,           -- TEMPLATE only: 'REDUP' | 'Q' | 'RI' ('N' reserved)
    is_practice BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_trials_pairing FOREIGN KEY (pairing_id) REFERENCES pairings (id),
    CONSTRAINT fk_trials_correct_word FOREIGN KEY (correct_word_id) REFERENCES words (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

Dropped, with their reasons: `prompt` (was a copy of the target gloss — glosses live on `words`); `condition_name` (a session fact, never a content fact — the NOT-NULL-default hack dies); `difficulty_level` (difficulty is a pairing concern: priors + live stats, ADR-5/6); `left/right_ideophone_id` (membership lives on the pairing; left/right was always presentation-order, which the shuffle derives). `trials` vs `pairings` is inventory vs servable content: a pairing can exist unserved (the 23 signed-off pairs before their recordings land), and one pairing may later serve multiple trial types — 1:1 today, 1:n by design.

Judging and rendering rules per `round_type` (contract material when each mode builds):

| round_type | Members | correct_word_id | Judging rule | Rendered form | Position/order source |
|---|---|---|---|---|---|
| `CHOOSING` | pairing word_a/word_b | **documentation** (thesis fixed target; NIL-54 needs it) | derived target from `shuffle_seed` — 2026-06-12 algorithm untouched, id vocabulary = word ids (ADR-0) | `presentation(word, session.condition).display_form` | base stream (+1 practice) |
| `TEMPLATE` | pairing word_a/word_b | **load-bearing: adjudicated** (never derived — honesty constraint) | selected == correct | `words.canonical_form` | stream +3 |
| `XL` | pairing target/foil-donor | **load-bearing: = target** (the audio played) | selected == correct | `words.canonical_form` (audio is the stimulus) | stream +2 |

This resolves the contract's "correct answer column is no longer read in the serving path" note into a type-scoped rule: *unread for CHOOSING, authoritative for everything else.* Identity randomization is and stays CHOOSING-only.

JPA: single `Trial` entity, nullable fields, no inheritance — a `SINGLE_TABLE` hierarchy buys type-safety at the cost of discriminator config, per-subtype repositories, and mapper churn; the boring option wins. `RoundType` enum, `EnumType.STRING`.

**Consequences.** One answers endpoint, race guard, history, and stats join for every current and future 2AFC-shaped mode; the table is thinner than what it replaces. Cost: the rename + collapse + re-key is the core of the M2 churn session — every query, mapper, and fixture that said `ArenaRound` re-points, priced with the 80-test net per the adjudication. Revisit trigger unchanged: a mode whose trial is *not* two word references and a correct answer (sentence-context would be it; it is deferred).

## 6. ADR-4 — D3: lexeme identity across sources

**Status:** Adopted (cross-source verdict; within-core identity superseded-by-strengthening via ADR-0). **Context:** 38 Collabra XL items overlap the Japanese core space (6 thesis words, 8 approved expansion words). SPEC-cross-linguistic §4 keeps XL rows as separate word rows (different audio, different provenance) and escalates cross-source identity here.

**Decision: no cross-source lexeme structure.** Within-core identity is now the `words` table itself — `UNIQUE(language_id, romaji)` with romaji canonicalized by the shared normalizer (Hepburn/Nihon-shiki variance folds to one string: `fuwafuwa` → `huwahuwa`; dataset `zaa zaa` → `zaazaa`; final sokuon survives as `Q` — the `paQ`/`paq` false-non-match pitfall stays codified). Cross-source identity (our `huwahuwa` vs Collabra's) remains a *derivation*, not a schema fact: XL words stay separate rows in their own right (different audio, different task, different foil regime — mclean2023's own lesson is that pooling across regimes is wrong), published anchors attach per word × dataset via `stimulus_sources` (ADR-7), and any future cross-source join is `(language_id, normalize(romaji))` using the one shared normalizer. A `lexeme` super-entity gets built the day a feature must *display* core and XL data as one word, and not before.

## 7. ADR-5 — D4: item-stats granularity (escalated: pairing, not word)

**Status:** Adopted. **Context:** Every mode's self-norming loop ("live accuracy supersedes prior at n ≥ 30") needs one shared stats source. The 2026-07-02 escalation, backed by mclean2023 (iraira .70 vs .39, nebaneba .38 vs .20 under two concepts) and by the app's own identity randomization (both pair members serve as targets — 30 complementary targets the thesis never measured), fixes the grain: guessability is a property of the form–meaning pairing.

**Decision: the stats primitive is the *directed pairing* — key `(pairing_id, target_word_id)` — implemented as one repository projection, not a schema object.**

- **Key.** `player_answers` carries everything: `trial_id → trials.pairing_id` supplies the pairing; `target_word_id` supplies the direction. One JPQL interface projection: join answers → trial → pairing, `GROUP BY pairing.id, answer.targetWord.id`. No new event columns, no DB view (un-generated schema surface; the projection is the house pattern, per `AdminStats`).
- **Aggregation ladder.** Directed pairing (primitive) → pairing (both directions pooled) → word (across pairings, within language and round_type) → modality/floor/language. Word-level is *derived, never primitive*, and keys on `words.id` (ADR-0). Slicing by `condition_name` (via session) and `round_type` stays possible; **cross-`round_type` pooling is forbidden** (meaning-pick, template-pick and word-pick have different chance structures). Measure verticals join at the word grain: producibility (mean `similarity_score`) and mean rating are word-keyed by nature, so they enter as word-level covariates beside the derived word-level guess stats — which is exactly what `/api/research/triangulation` reads (production spec §8's flag, resolved).
- **One consumer surface.** `ItemStatsService` (name final at build time) owns the n ≥ 30 supersede rule: `effectiveDifficulty(pairing) = live directed/pooled accuracy if n ≥ 30 else prior`, where prior = `pairings.thesis_accuracy` (exact, thesis pairs) else `pairings.difficulty_prior` (sign-off vocabulary). Ladder boss re-evaluation, template tier re-ranking, and XL tier placement all call this; nobody reimplements it.
- **Thesis-ingested answers (NIL-54): fenced from the live aggregates; self-norming via the prior (amended 2026-07-06, reversing the in-band lean).** The pre-ingestion lean was to pool them in-band; at execution Nils reversed it. They are ordinary `player_answers`/`ratings` but carry the reserved `thesis_p%` username prefix, and the live research aggregates (Rider A) exclude that prefix, so the public Observatory shows only live-player data and stays byte-stable across the ingestion. The self-norming intent is preserved through the *prior* — `pairings.thesis_accuracy`, the same 36 humans — not through pooling ingested rows; the cohort's own view is `GET /api/research/thesis/divergence` (inverted predicate). When `ItemStatsService` is built it applies the same prefix fence (or deliberately includes the cohort via the inverted predicate). Still no flag/`data_source` column (see §12).
- **The divergence endpoint heals rather than forks.** Its response shape and documented semantics ("one row per ideophone") are the frozen contract; under ADR-0 the implementation finally matches them — `ideophoneId` carries the word id, one row per word (the row-id splitting was the accident, not the contract). New consumers (triangulation, templates summary, XL summary, self-norming) key on the ADR-5 primitive from day one.

**Consequences.** Self-norming becomes a one-service feature instead of four reimplementations. N+1 exposure is structurally avoided: aggregates are single GROUP-BY projections; the 27B cartesian-product warning generalizes — every multi-measure merge (triangulation, threefold) stays separate-GROUP-BY-merged-in-service. Cost: the M2 pairing backfill must be right (proof steps assert every trial maps to a pairing whose members match the thesis CSVs).

## 8. ADR-6 — Pairings: the expansion pipeline's data model

**Status:** Adopted (member FKs re-keyed to `words` per ADR-0; lowest-id-row convention deleted). **Context:** 23 signed-off pairs (`stimulus-expansion-signoff.xlsx`, approved 2026-07-02 per `fable-week-plan.md`) need seeding with per-pair provenance, priors, floor membership, sign-off trail, and the NIL-58 foil-distance covariate. ADR-5 needs a pairing key on trials. Invariant 4 needs structural teeth.

**Decision: one `pairings` table — the durable pair-level inventory entity; `trials.pairing_id` NOT NULL.**

```sql
CREATE TABLE pairings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pair_code VARCHAR(20) NOT NULL UNIQUE,   -- 'a0'…'p3' (thesis/practice, from stimulus prefixes),
                                             -- 'A1'…'V6' (expansion workbook), 'xl-<iso>-<nnn>' (XL items)
    language_id BIGINT NOT NULL,             -- FK languages; both members must match (generator-asserted)
    word_a_id BIGINT NOT NULL,               -- FK words
    word_b_id BIGINT NOT NULL,               -- FK words
    modality VARCHAR(50) NULL,               -- the shared modality where is_core; NULL otherwise
    is_core BOOLEAN NOT NULL,                -- TRUE = invariant-4 domain: real contrastive same-modality words
    source VARCHAR(30) NOT NULL,             -- 'THESIS' | 'EXPANSION_2026' | 'TEMPLATE_PIPELINE' | 'XL_TRIANGULATING'
    difficulty_prior VARCHAR(10) NULL,       -- 'easy' | 'medium' | 'hard' (sign-off vocabulary)
    thesis_accuracy DECIMAL(5,4) NULL,       -- exact per-pair prior where measured (thesis-facts §4)
    foil_distance DECIMAL(6,4) NULL,         -- recorded covariate (NIL-58): computed and stored, NEVER an
                                             --   ordering/difficulty input for is_core content (f1 profile §4:
                                             --   rho = -.155 n.s. on real pairs; validation-only)
    signoff_ref VARCHAR(100) NULL,           -- 'stimulus-expansion-signoff.xlsx#H1', 'thesis', workbook refs
    approved_at DATE NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pairings_language FOREIGN KEY (language_id) REFERENCES languages (id),
    CONSTRAINT fk_pairings_word_a FOREIGN KEY (word_a_id) REFERENCES words (id),
    CONSTRAINT fk_pairings_word_b FOREIGN KEY (word_b_id) REFERENCES words (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
```

Design calls, each with its why:

- **Members are FKs to `words`** — real referential integrity, direct joins for gloss/modality, no row-picking convention (ADR-0 deleted that problem at the root).
- **Invariant 4 becomes structural at the right layer.** MySQL cannot CHECK across tables; the schema's owner is the generator, so the generator enforces at `--check` time: `is_core=TRUE` ⇒ same non-null `modality` on both members, same `language_id`, both members real seeded words, contrastive glosses present. A service-layer guard (`RoundSource` refuses to serve an `is_core=FALSE` pairing in a core-task context) backs it at runtime. Template pairs (real words, but their own honesty-constraint adjudication) and XL pairs (gloss-space foils) are `is_core=FALSE` — invariant 4 formally governs core-task content only, and the flag makes that boundary queryable.
- **XL items get pairings rows too** (target + foil-donor, 1:1 with their trial). This buys grain uniformity: *every* trial has `pairing_id NOT NULL`, and ADR-5's one projection serves all modes with no special-casing. 239 generated rows is a non-cost.
- **Floors are not schema.** Floor = modality (Sound/Sight/Touch/Inner states) + within-floor order = `effectiveDifficulty()` over pairings. A `floors` table waits until floors stop being modalities (ladder spec §13's trigger: >4 floors) — noted, not designed.
- **External anchors:** mclean2023/Collabra per-word guessability lands as `stimulus_sources` rows (ADR-7), not pairings columns — anchors attach to words-under-a-dataset; `thesis_accuracy` is the only pairing-level prior because the thesis is the only source measured on *our* pairings.
- **Expansion mechanics:** the pipeline (extends `generate_seed_sql.py` inputs) canonicalizes workbook romaji through the shared normalizer (Hepburn `jaajaa`/`shittori`/`gyuq` → seed `zyaazyaa`/`sittori`/`gyuQ`; the A3/H3 rows *reference existing thesis words* and must resolve to their `words` rows, not mint duplicates), emits per new word one `words` row + three `presentations` rows, one `pairings` row + one `trials` row per pair, and assigns `canonical_script` by the recorded `kata_share > 0.5` rule. Hygiene flag: the workbook's `sign_off` column is blank though approval is recorded in `fable-week-plan.md` (2026-07-02) — the ingestion session backfills the column or cites the plan doc in `signoff_ref`; don't leave the trail split.

**Consequences.** Per-pair provenance, priors, covariates, and sign-off trail become queryable; ADR-5 gets its key; the 28A static map gets a future seed-driven home without an API change; H-floor re-classification (gotsugotsu, bosabosa → HAPTIC) stays what the ladder spec decided — a word-level modality flip at seed regen, now literally one value on one `words` row. Backfill correctness is asserted, not assumed: pairing identity already exists deterministically in every stimulus filename prefix.

## 9. ADR-7 — Per-stimulus provenance and licensing

**Status:** Adopted (attachment re-keyed to `word_id` per ADR-0). **Context:** NIL-59 resolved licensing (CC BY 4.0, attribution required) for the 241 wav files and both source studies; the XL spec drafts `stimulus_sources` and defers placement here. mclean2023 offers per-word anchors for 9 thesis + 2 expansion words.

**Decision: `stimulus_sources` per the XL spec §4 shape, keyed `word_id`, `UNIQUE(word_id, dataset)`, plus two columns the spec draft lacked: `license VARCHAR(30) NOT NULL` (the actual identifier, e.g. `CC-BY-4.0`, `OWN`) and `source_url VARCHAR(255) NULL`.** `license_status` stays (`PENDING|CLEARED|DENIED`) for future datasets — status and identifier are different facts.

Placement rationale: the word is the provenance-bearing unit (its one audio file, its published anchors — `dataset='mclean2023'`, `published_guessability=0.70` for fuwafuwa), and post-ADR-0 the word is a real row — the placement question dissolves. Attribution obligations (XL spec §13's three frozen credit lines) are **content, not schema** — landing page + mode credits; `citation` per row satisfies audit needs, dataset-level credit satisfies CC BY 4.0. Audio serving (G5 wav-vs-m4a) is explicitly not decided here — asset-regeneration gate, Nils's call at XL build; `words.stimulus_file` holds either; path convention `/stimuli/xl/{iso}/…` stands; the core `<3-char>-<romaji>.m4a` convention stays Japanese-core-only.

## 10. ADR-8 — Shared engines and registries (the cross-spec flags)

**Status:** Adopted (8.3 amended by ADR-0). Four smaller decisions the spec ledgers routed here, settled together because they share one principle: *one implementation, many consumers, drift made loud.*

1. **`PhonologyService` placement.** `service/PhonologyService.java`, pure functions, no repository access — exactly as SPEC-free-form-entry §5 specs it. It is the single feature/normalization engine for three consumers: production scoring (its birthplace — built in NIL-62), `word_features` emission, and NIL-58 distance computation. Per-language profile seam from day one: methods take a `PhonologyProfile` parameter (Japanese the only v1 profile); Korean production later = new profile, same table, same scorer. Scope fence (binding, from the 2026-07-02 revision): the similarity formula is a production-scoring instrument, not a difficulty dial — `features()` and the distance primitive are what NIL-58 borrows.
2. **Python/Java extractor single-sourcing.** The generator needs the same feature logic in Python (`word_features` is seed-emitted). Decision: **dual implementation + one committed golden file** (`docs/research/phonology-golden.json`: every inventory romaji → normalized form, morae, features). Java asserts against it in `PhonologyServiceTests`; the generator asserts against it during `--check`. Both sides break loudly on drift; no build-step coupling. (Template spec §13's alternative — generator consumes a Java-side dump — couples seed regeneration to a Maven build; rejected.)
3. **`word_features` = side-table keyed `word_id`, one row per word** (ADR-0 amendment). Confirms the template spec's lean against columns-on-the-lexical-core: the table is wholly generator-emitted and regenerable; rows exist for `jpn` words only in v1 (absence = not-yet-profiled language, no nullable-boolean smell). Triple-emission and its assertions died with ADR-0; the golden-file parity test remains.
4. **Seed-stream registry** (codifying `shuffleSeed + k`): **0** base (scored trial order + the three per-round draws — the frozen 2026-06-12 algorithm, id vocabulary word-bound per ADR-0) · **+1** practice · **+2** cross-linguistic · **+3** template · **+4** ladder (reserved: per-round side/order draws over map-ordered trials) · future modes claim the next integer here before build. A `GameStreams` constants class materializes the registry when its second consumer builds. The base contract is never re-derived or re-consumed by new modes — new streams only.

Also settled here: `GET /api/research/triangulation` and `/divergence` **coexist** until the landing page picks (production spec §13's question — answered: keep both; divergence heals to word grain under ADR-0 with its shape frozen; triangulation is the ADR-5-grained three-measure surface).

## 11. Gate adjudication — 28A ladder data model

**Pre-adjudicated lean:** static server-side ordering map from thesis per-pair accuracy, no schema. **Verdict: CONFIRMED (adjudicated), with one co-location amendment (adjudicated).**

The map: floor order Sound → Sight → Inner states (Touch inserts third when its recording gate resolves), within-floor order = thesis-facts §8 (exact §4 values: a9→a5, v2→v4, i3→i2), boss pair last (a5, v4, i2 — honest at-or-below-chance copy). It lives in code behind the ladder service's ordering seam; when M2 lands, the same seam reads `effectiveDifficulty()` over pairings with **zero API change** — the map is an implementation detail of a stable contract, which is what makes the lean safe rather than merely cheap. No schema for ordering, confirmed.

**Amendment (firm):** `game_sessions.game_mode` (ADR-2's column, migration **M1**) **lands with 28A, not later.** Reasons: (a) without it, W28 ladder sessions are permanently indistinguishable from core Choosing sessions — un-backfillable ambiguity in exactly the data the phoneme-shape transfer analysis and mode-scoped stats need clean; (b) it is one defaulted column + enum + validation branch; (c) it is ADR-2's migration arriving with its first consumer, not ladder scope creep. Note: 28A builds against the pre-M2 schema (`arena_rounds`/`ideophones`); the M2 churn re-points the ladder serving code mechanically along with everything else — priced into M2, and the ladder's public contract is unaffected.

## 12. Gate adjudication — NIL-54 Gate A (leaderboard contamination)

**Pre-adjudicated lean:** ingest with `completed_at = NULL`, pending the one query check. **Verdict: CONFIRMED (adjudicated) — the query check was performed this session, against source.**

Verified in `PlayerAnswerRepository`/`RatingRepository` (read 2026-07-03): `aggregateGuessStatsByIdeophone()` joins the answer's target only — no session join, no completion predicate; `aggregateRatingStatsByIdeophone()` joins the rating's word only — sessions never enter; `findLeaderboard` requires `session.completedAt is not null` in both main and count queries. Therefore lever (a) holds exactly as hoped: 36 thesis sessions with `completed_at = NULL` can never surface on the leaderboard (it requires `completedAt is not null`). No schema change, no flag column, deliberately **no** `data_source` column.

**Amendment (2026-07-06, at execution — reverses the "in-band" clause above).** The verdict kept `completed_at = NULL` and the no-schema/no-flag posture, but Nils reversed the "belong in-band, no username-prefix filter" part: the cohort is provenance-tagged by a reserved **`thesis_p%`** username prefix and the live research aggregates (Rider A: `divergence`, `rating-distributions`, `position-bias`) now **exclude** it, keeping the public Observatory live-player-only and byte-stable. The thesis numbers surface via a new inverted-predicate endpoint `GET /api/research/thesis/divergence` (reconciles to 68.6/64.2/59.7). Rationale for the reversal: a clean live-crowd layer separable from the thesis baseline reads better scientifically than blending 36 thesis participants into the live crowd stats. The self-norming intent survives through the *prior* (`pairings.thesis_accuracy`), unchanged.

Sequencing note (new under ADR-0): NIL-54 is cleanest **after M2** — ratings then key on `word_id`, which *is* the thesis's own semantics (rated word = target word), and answers reference trials + target words directly. If it runs before M2 instead, its generator-emitted rows are carried through the churn mechanically (they regenerate; nothing is lost) — Gate A holds either way. Two side-effects for the contract changelog when it executes: `GET /api/admin/stats` totals gain +36 users/+36 sessions/+1,080 answers and `byCondition` gains the thesis cohort (11/13/12 across `CONDITION_{1,2,3}_SOKUON`); `completedSessions` stays honest at live-only. Features wearing the costume of surprises — flag them so the dashboard doesn't look haunted.

## 13. Migration plan (APPROVED as a plan — execution gated per session; renumbered post-ADR-0)

**Regime note first:** until the W30 deploy cutover, "migration" = a `generate_seed_sql.py` delta + full regen + fresh `db/init` re-init on dev (data loss acceptable: dev holds test accounts only — and NIL-54's thesis rows are *generator-emitted*, so they regenerate). Entities change in the same commit; `ddl-auto=validate` boot is the proof each time. **After W30, this regime ends:** the hosted DB gets hand-written, individually gated expand-contract ALTERs (no Flyway — no new deps without approval; a `docs/migrations/` log keeps the applied statements), and the init file remains the from-scratch truth that `--check` verifies. Every M below lands **before** the W30 cutover — post-W30 the same change costs triple, and M2 in particular exists *because* the pre-W30 window makes it cheap.

| M | Contents | Rides with | Blocks |
|---|---|---|---|
| **M1** | `game_sessions.game_mode` NOT NULL DEFAULT 'CHOOSING' + `GameMode` enum + per-mode validation branch | **28A** (W28; §11 amendment, firm) | mode-scoped stats, every non-core mode |
| **M2** | **The Re-key** (one dedicated gated churn session, W29 head, post-NIL-65): `languages` (ADR-1) · `words` + `presentations` split (ADR-0) · `pairings` with thesis backfill — 34 pairs a0–a9/v0–v9/i0–i9/p0–p3, `source='THESIS'`, `is_core=TRUE`, `thesis_accuracy` from thesis-facts §4, practice rows `difficulty_prior=NULL` (ADR-6) · `trials` rename + condition collapse (102→34) + `round_type`/`feature_axis`/`correct_word_id`/`pairing_id`, drop `prompt`/`condition_name`/`difficulty_level` (ADR-3) · `player_answers` re-key to `trial_id`/`selected_word_id`/`target_word_id` · `ratings` re-key to `word_id` + `UNIQUE(user_id, word_id)` · generator restructure + `validate_unique_constraints` re-target · contract riders (round-DTO id semantics + field-name vocabulary with NIL-65; shuffle-derivation vocabulary; divergence grain-heal note) · full 80-test re-point | dedicated session | everything below; expansion; template; XL |
| **M3** | `productions` — SPEC-free-form-entry §4 with the ADR-0 key amendment: `word_id` FK + `UNIQUE(user_id, word_id)` | NIL-62 build (post-M2) | the third measure |
| **M4** | `word_features` keyed `word_id` (1 row/word) + Python extractor port + committed golden file + `--check` parity assertions | template pipeline session | template mode, NIL-58 covariate computation |
| **M5** | `stimulus_sources` keyed `word_id` (+ `license`/`source_url`) | XL ingestion session | cross-linguistic mode, anchor ingestion |
| **M6** | XL content ingest: 5 `languages` rows, ~239 `words` (no presentations), 239 `pairings` + `trials`, foil assignments, gloss-cleanup G2 output; gates G2/G5 + seed-regen approval | XL ingestion session (with M5) | cross-linguistic mode |
| — | Expansion seeding: 23 pairs → new `words` + `presentations` + `pairings` + `trials`; H-floor re-classification | post-M2, gated on NIL-60 (recordings) | four-floor ladder content |

Ordering rationale: M1 rides the nearest build and is orthogonal to the churn (it touches only `game_sessions`). M2 is the load-bearing session and goes first in W29 so NIL-62 (M3) builds against the final key space; if W28 finishes early, M2 may pull forward ahead of 28A at Nils's discretion (28A then builds against the new schema and its re-point line item in M2 disappears — sequencing within the approved plan, not a new decision). M4/M5/M6 ride their consuming builds so no migration lands speculatively.

Per-M proof pattern (unchanged): `python3 scripts/generate_seed_sql.py --check` clean → regen → `ddl-auto=validate` boots → `./mvnw test` green → one new seed-integrity assertion per M (extend `IdeophoneSeedIntegrityTests`). M2's additional proofs, named now because they are the point: ratable pool dedupes across conditions (the ADR-0 finding as a regression test); second rating of the same word under a different condition → 409; shuffle-derivation golden test passes with word-id vocabulary (same draws, same order); divergence returns one row per word; answer race 409 intact under the renamed FK.

## 14. What each upcoming session consumes

| Session | Consumes from this doc | Must not touch |
|---|---|---|
| **28A/28B** (W28, ladder) | §11 map + M1; `LADDER` mode value; stream +4 reservation; 28B builds floors-from-data (ladder spec §6 flag) | pairings (not yet, unless M2 pulled forward), shuffle base algorithm |
| **M2 churn session** (W29 head) | ADR-0 shape; ADR-3 trials table; ADR-6 pairings + thesis backfill; contract riders list; §13 M2 proofs | shuffle algorithm/draw order; divergence response shape; frozen strings |
| **NIL-62** (production build, post-M2) | M3 (word-keyed productions); PhonologyService §5-exact with profile seam (ADR-8.1); ratings-vertical conventions at word grain | any session/mode machinery |
| **NIL-54** (thesis ingestion, cleanest post-M2) | §12 verdict: `completed_at = NULL`, no flag column; changelog side-effect notes; `trials.correct_word_id` = the thesis fixed target it reconstructs against | leaderboard query, ratings uniqueness |
| **Expansion seeding** (post-M2 + NIL-60 gate) | ADR-6 mechanics (normalizer canonicalization, existing-word resolution, `kata_share` rule, sign-off hygiene flag) | invariant-4 flag semantics; the recording decision itself |
| **Template build** (post sign-off round) | M4; ADR-3 role table (canonical_form rendering); ADR-8.2 golden file; pairings `source='TEMPLATE_PIPELINE'`; stream +3 | canonical Choosing wording, derived-target judging |
| **XL build** (~3 sessions) | M5+M6; ADR-1/-7; XL pairings+trials roles; stream +2; G2/G4/G5 remain Nils-gated | divergence endpoint shape, core audio convention |
| **NIL-58** (foil-distance, re-scoped) | ADR-8.1 scope fence; `pairings.foil_distance` as recorded covariate only | any ordering/difficulty use on `is_core` content |
| **W29 landing** | triangulation-vs-divergence coexistence (ADR-8); attribution block (XL spec §13); thesis-facts copy | frozen strings |

## 15. Invariants check

1. **Display forms verbatim** — upheld and clarified by ADR-0: every rendered string is a stored string (`presentations.display_form` for scripted conditions, `words.canonical_form` everywhere else); no conversion, detection, or derivation anywhere in the serving path. The generator's `romaji_to_hiragana` remains a *seed-construction* tool, never a runtime path.
2. **One shared per-word audio file** — *strengthened from convention to structure*: `stimulus_file` lives once, on `words` (ADR-0). The per-condition duplication that invariant 2 policed no longer exists to police.
3. **Canonical question wording** — untouched; new modes carry their own frozen strings per their specs.
4. **Real contrastive same-modality pairs for core content** — *strengthened*: `pairings.is_core` + generator assertions + serving-layer guard make the invariant queryable and enforced (ADR-6).
5. **Seed changes via `generate_seed_sql.py --check`** — every M is a generator delta; `--check` gains assertions per M; no hand-edited SQL anywhere in the plan.

## 16. Explicitly not designed here

Deferred modes stay deferred: Foil Arena (accommodated by `stimulus_sources` + `is_core=FALSE` pairings if it ever unfreezes — one sentence, no design), sentence-context (the one candidate that would break ADR-3's two-word-references-and-an-answer shape — its arrival is the revisit trigger), campaign/codex, Corpus Route. UI system: NIL-64's problem, on purpose. `floors` table: trigger noted in ADR-6. `served_presentation_id` on answers: trigger noted in ADR-0 (a mode mixing presentations within a session). Post-W30 migration tooling (Flyway vs manual log): manual log under the no-new-deps rule; revisit only if migration volume post-deploy embarrasses it.

