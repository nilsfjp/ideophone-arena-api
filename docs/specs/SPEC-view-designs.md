# SPEC — View designs: Perception Ladder floors · Word Mint · Landing composition

_Fable 5 view-design adjudication session, 2026-07-06 (evening, post-NIL-54). NIL-64 pattern: options → verdicts → binding ledger, at mockup + ruling level, so the consuming builds run on Opus without Fable judgment. Mockups: `docs/design/view-adjudication-mockups/` (one responsive file per surface, desktop + 375px via media queries — the NIL-64 mockup convention). Consumed by: **28A/28B** (ladder build, Opus post-window), **NIL-62 frontend session** (Word Mint UI), **NIL-43** (landing build, Wed if budget holds)._

_**Spec-version verification (this session, file-tool fingerprinting; shell still dead on the WSL UNC mount):** UI-SYSTEM.md (582 lines), ARCHITECTURE.md (incl. the 2026-07-06 §7/§12 amendment), and thesis-facts.md are **in sync** across planning folder + both Windows checkouts + both WSL trees. Three repo-side copies are stale by one amendment sweep: SPEC-stats-dashboard.md (repos lack the NIL-78 as-built sweep: §3.2 figcaption caveat, §3.3 z-within-study, §3.4 17-word radar), SPEC-four-floor-ladder.md and SPEC-free-form-entry.md (repos lack their §13 "Deferred to NIL-57" appendix). Direction is uniform — planning ≥ repos everywhere — so canon is unambiguous; **re-copy chore assigned to the next session in each repo** (28A/28B for web+api ladder specs, NIL-62 backend for free-form). This spec designs from the planning copies._

_Binding context: UI-SYSTEM.md §16 H1–H8 is wiring canon (standard Tailwind/shadcn utility surface; token names per H2 — `--vermillion`, `--modality-*`); mode names = adopted slate (Meaning Match / Rating Lab / Perception Ladder / Word Mint); frozen `experimentText.ts` strings untouched; invariants 1–5 out of reach (design layer only); Script Lab framing rule; **no new dependencies proposed anywhere in this spec**; XL mode name stays an open pair (not this session's call). All chrome copy in this spec is **draft register** (Nils's veto stands) except lines already adopted verbatim in UI-SYSTEM §10.5._

## 0. Method and inputs

Inputs: UI-SYSTEM.md (§1–§7 surface/type/color/state/motion systems, §2.3 shell, §2.5 landing layout, §3.4 specimen label, §4.4 KOKE adoption, §10 art direction incl. §10.2 name slate / §10.5 landing arc / §10.6 wave rule, §13 ledger L1–L5, §16 wiring canon); SPEC-four-floor-ladder.md (Touch floor, gates G1–G4, hierarchy placement); SPEC-free-form-entry.md + NIL-62 kickoff prompt (post-M2 word-keyed deltas); SPEC-stats-dashboard.md §1 (NIL-43 delegations: CTA destination, public-boundary decision) + §7 (E1 exports feed the landing); thesis-facts.md §3/§4/§5/§8/§9 (all numbers below trace there); `docs/design/NIL-64-mockups/` (pattern + the landing-hero record).

Each surface section ends with **build notes** naming what its Opus session lifts verbatim. §4 is the binding ledger; §5 the deferred table. Anything not ruled here and not in UI-SYSTEM.md defaults to UI-SYSTEM.md's systems (surface grammar, type ladder, states, motion) — consuming sessions do not invent.

---

## 1. Perception Ladder (28A/28B) — floor chrome

### 1.1 The composition: a specimen drawer, not a tower graphic

Three options considered:

- **(a) Literal tower/ladder illustration** — floors drawn as a climbing diagram with a player marker. Rejected: quiz-show energy; fights the lab-notebook register; needs bespoke art that earns nothing (the mode's content is ordinary Choosing rounds).
- **(b) Horizontal stepper** — floors as a left-to-right progress bar. Rejected: cramped at 375px with four floors; no room for per-floor stats and honesty copy; reads as checkout flow.
- **(c) Vertical floor stack** — floors as full-width washi cards in a single `--container-read` column, in climb order, top → bottom. **Adopted.** It reuses the mode-card grammar players already know, scales 3 → 4 → n floors as pure data (the four-floor spec's §6 mandate), gives every floor room for its label, description, stats slot, and status pill, and stacks losslessly at 375px.

Reading order = climb order = hierarchy order (§1.2). The view is the `ladder` `AppView` (28B): heading block (page title "Perception Ladder", wave rule per §10.6 section-heading use, one-line intro), then the floor stack, then a footnote line. One `--container-read` wrapper, rhythm per §2.2 — nothing new.

**The intro line is the mode's hidden gem (§10.3 register), drafted:** "The deeper you go, the more iconic the words *feel* — and the worse everyone guesses. Interoceptive words were rated most word-like (4.45/7) and guessed worst (60%)." Numbers: thesis-facts §3. This is the rating-reversal finding doing the difficulty narration for free.

### 1.2 Floor order — adjudicated (doc conflict resolved)

**Conflict found this session:** UI-SYSTEM §10.2's name-slate row says "floors: Sound → Sight → Inner States → Touch"; SPEC-four-floor-ladder §1/§5 orders **Sound → Sight → Touch → Inner states** per McLean's (2021) implicational hierarchy, and thesis-facts §8 slots future floors "between Sound and Inner states."

**Verdict: hierarchy order — Sound → Sight → Touch → Inner states.** The four-floor spec is the dedicated authority and the ordering is the *point* (its vision test asks whether live haptic accuracy lands between Sight and Inner states — the design should visibly stage that prediction). §10.2's parenthetical is a naming-table aside, not an adjudicated ordering; **doc fix:** correct §10.2's parenthetical to hierarchy order when UI-SYSTEM.md is next touched (noted in §4 ledger; no repo edit this session).

Consequence the design absorbs: floor ordinals are **render-time data** (`FLOOR {i}` from API array position). Today's three floors read 1/2/3; when Touch goes live it becomes floor 3 and Inner states re-numbers to 4. That is correct behavior, not drift — identity lives in the modality, the ordinal is just position. 28B must not persist or hardcode ordinals anywhere.

### 1.3 Floor card anatomy (mockup: `ladder-floors.html`, `.ladder-floor`)

Washi card (`--surface-card`, `1px --border-soft`, `--radius-lg` panel radius), **no shadow ever** (chrome; §1 surface grammar), with one modality accent: a **3px inline-start rule** in the floor's `--modality-*` color. Top → bottom:

1. **Floor label** — §3.4 specimen label, tinted variant: `FLOOR 1 · SOUND` in `--modality-auditory` (the §3.4 example, verbatim idiom). Tinted text sits on the washi card ⇒ AA per §4.5's card-surface rule. Touch uses `--modality-haptic` (KOKE) — the §4.4 mandatory-label rule is satisfied by construction (the label *is* the text pairing).
2. **Floor name** — display face, `--text-md` (mode-card scale): "Sound" / "Sight" / "Touch" / "Inner states".
3. **One-line description** — `--text-sm`, `--ink-muted`, honesty register. Drafts: Sound "Noises mapped to noises — the most transparent rung (thesis floor mean 6.9/10)." · Sight "Motion and light in vowels and stops (6.4/10)." · Touch (pre-gate copy in §1.6) · Inner states "Feelings from the body's inside — rated most iconic, guessed worst (6.0/10)."
4. **Stats slot** — reserved even when empty (§2.3 equal-height discipline): plain specimen label, e.g. `BEST · 8/10 · THESIS MEAN 6.9` after a clear; `10 PAIRS` before first attempt.
5. **Status pill** — §3.4 pill variant, one of §1.4's states.

The whole card is the floor's button (`<button class="ladder-floor">`, mirroring `.mode-card` semantics) when playable; a non-interactive `<article>` when not. Hover/active/focus per §6 cards-as-buttons grammar. **No progress bars on floor cards** — the numbers are copy (register rule); the in-run progress track already exists on the trial chrome.

### 1.4 Floor states — pills, never padlocks

§2.3's honest-pattern extends from coming-soon modes to sequential floors: full-opacity copy, status carried by a pill + interactivity, **no lock icons, no grayed-out mystery, no overlays**.

| State | Pill (specimen pill variant) | Card behavior |
|---|---|---|
| Cleared | `CLEARED · 8/10` in `--positive` | interactive (replayable; best score stands) |
| Current | `UP NEXT` in `--vermillion` (card surface ⇒ AA ok) | interactive, `--border-mid` affordance border |
| Ahead (sequentially locked) | `AFTER FLOOR {n}` in `--ink-muted` | non-interactive, `aria-disabled`, description stays fully readable |
| In preparation (Touch, pre-G1) | `IN PREPARATION` in `--ink-muted` | non-interactive; copy per §1.6 |

Sequential unlock itself is 28A's ordering-map behavior; the design's job is stating it in words (`AFTER FLOOR 2`), not iconography. Replay of cleared floors is assumed permitted (28A confirms; if not, the pill row simply drops the affordance — no layout change).

### 1.5 Difficulty and score presentation

- **Pre-answer, difficulty is structural only.** Within-floor order = thesis-facts §8 easy→hard sequences (a9→a5 / v2→v4 / i3→i2); the player sees position (`PAIR 4 OF 10`), never a per-pair number. Showing thesis accuracy before an answer would prime the guess (a 94%-easy signal is information about the pair); the existing post-answer research note is where the number lands ("In the thesis study this pair was the easiest of all 30 — 94% got it.").
- **The final pair of each floor gets a specimen label, not a boss fight.** Player-facing marker: `FINAL RUNG` (plain specimen label above the trial board's chrome header, tinted in the floor color). "Boss" stays internal vocabulary (F2 spec term). The honesty hook is the copy, post-answer or on the floor-completion panel: all three thesis final pairs (a5, v4, i2) sat at or below chance — draft line: "Most players do no better than a coin flip here. The thesis cohort didn't either."
- **In-run chrome** (above the untouched trial board): one header strip = tinted floor label (`FLOOR 2 · SIGHT`) + `PAIR 4 OF 10` + the existing progress track. The trial board below is byte-identical Choosing chrome — invariant 5 geometry, frozen strings, reserved slots, all out of reach. The ladder adds a *frame*, never a board variant.
- **Floor completion = benchmark grammar** (§10.3.2 pattern, per floor): "Floor mean in the thesis: 6.9/10. You: 8/10." — player number vs thesis floor mean (6.86/6.42/5.97; Touch says "no thesis baseline — you're the norming study" until self-norming matures). Then: next-floor CTA (primary) / back to the stack (ghost).
- **Summit (all floors cleared):** one summary panel on the stack view replacing the intro line — per-floor pairs of numbers in the same benchmark grammar, no chart. A "See where this lands" link into the Observatory (completion deep-link idiom the Observatory spec already defines) is sanctioned.

### 1.6 The 4th floor now — the Touch teaser ruling

Options: (a) strictly render API floors (3 today, teaser nowhere) · (b) client-side coming-soon Touch card at the **end** of the stack · (c) client-side coming-soon Touch card at its **hierarchy position** (3rd, between Sight and Inner states).

**Verdict: (c).** The teaser is a static client-side entry (marked in code as presentation-only, removed the day the API serves four floors) rendered at the position the hierarchy predicts. Rationale: the insert position *is* the scientific claim (haptic should land between Sight and Inner states — F2 vision test (a)), and the design should stage the prediction where the data will test it; it also gets KOKE on-screen pre-gate with its mandatory text label. Anatomy per §1.3 with: KOKE label `TOUCH` (no ordinal — it has no API position yet; the only floor card without one, which is honest), description draft: "Six touch pairs are signed off — prickly vs fluffy, sticky vs dry-smooth. They need studio recordings before they can be measured; when they arrive, you're the norming study — no lab has baseline numbers for these." Pill: `IN PREPARATION`. Non-interactive.

CVD note (§4.4 discharged by construction): KOKE's accepted deuteranopia weakness vs the auditory brown never presents as color-only — the two floors are separated by position, ordinal text, name, and label text.

### 1.7 Floor intro dialog

§5's component table already assigns "ladder floor intro" to shadcn `Dialog` (staged, themed, untriggered — noted in §16). Content: tinted floor label, floor name, 2-sentence framing (hierarchy line + floor description), primary "Start floor {n}". **§16 H7 rider, binding on 28B:** Dialog is new Radix on a browser-loop path — its activation/dismiss idiom ships in `verify-browser-loop.mjs` in the same session, per the loop-compatibility contract. If the idiom fights the loop, the fallback is an inline (non-modal) intro block at the top of the run view — same content, zero Radix — and the swap is free because the content grammar is identical.

### 1.8 Responsive + a11y

375px: the stack is already single-column; floor cards keep all five anatomy rows (pills wrap under the stats slot); in-run header strip wraps to two lines (label / progress) above the board's own responsive behavior; touch targets ≥44px (whole-card buttons). A11y: floor stack is a `<ul>` of cards; playable floors are buttons with `aria-label` "Floor 2, Sight, up next"; state pills are text (no color-only state); `aria-disabled` on non-interactive floors; the in-run strip is `aria-live`-silent (the board's existing live regions own announcements).

### 1.9 Build notes (28A/28B lift list)

28A (api): floors endpoint in hierarchy order (§1.2); within-floor order + final-pair designation from thesis-facts §8; sequential-unlock semantics; replayability answer. 28B (web): this section + `ladder-floors.html` verbatim; `.ladder-floor` semantic hooks first-position per §5 contract; floor data rendered strictly from API array (teaser entry client-side, marked); Dialog idiom rider (§1.7); KOKE tokens exist since NIL-65 — zero token work; **re-copy the two stale ladder-relevant specs into the repos it touches (§ verification header)**. Neither session invents copy: drafts above go to Nils with the session plan (chrome register, his veto).

---

## 2. Word Mint (NIL-62) — the production surface

### 2.1 How production reads next to the other modes

The other modes *present* a stimulus and collect a reaction; Word Mint presents a **meaning** and collects a form. The design states that inversion with the same grammar the player already knows, so the mode family stays one instrument:

- The **prompt card is raised paper** (`--surface-raised` + `--shadow-card`) — it is the instrument speaking (the elicitation stimulus), exactly as ideophone cards are in Meaning Match. Chrome around it stays washi/flat.
- The **type ladder is the trial board's**: instruction line at `--text-prompt`, the meaning line at `--text-question` with the bolded meaning carrying the weight (house style: bold, no quotes). Production trials thereby *read* like Choosing trials with the poles swapped — same sizes, same emphasis grammar, no new type roles.
- The **reveal reuses FeedbackPanel conventions** (raised + shadow, reserved-feel layout) — but §2.4's verdict-color rule is deliberately broken: production has no verdict.

View shell: `production` `AppView`, mode card `MEASURE · PRODUCTION` / "Word Mint" (adopted slate). One `--container-read` column: status line → prompt card (or reveal card) → actions. Mockup: `word-mint.html`, semantic hooks `.mint-prompt`, `.mint-input`, `.mint-reveal`, `.mint-chip`.

### 2.2 Prompt state (`.mint-prompt`)

1. **Specimen label, tinted:** `MEANING · INTEROCEPTIVE` in the modality color (the prompt DTO carries `modality`). Ruling: showing modality pre-submit is sanctioned — the sensory domain is part of the *meaning* being conveyed (prompt side), not a hint about the *form* to be invented; it also primes the player to reach for the right kind of iconicity, which is the game.
2. **Instruction line** (`--text-prompt`), draft pending Nils's §9 adjudication: "Invent a Japanese-sounding word for the meaning below. Type it in roman letters."
3. **Meaning line** (`--text-question`): "**a racing heartbeat**" — bold, no quotes.
4. **Input** — native `<input type="text">`, raised fill + `--border-mid` per §1's affordance exception, text at `--text-md` body face, `maxlength=24`, and the quality attributes ruled here: `autocapitalize="off" autocorrect="off" spellcheck="false" autocomplete="off"`. The visible instruction line is the label (`aria-labelledby`). **No live character counter** (noise; the maxlength + parse helper carry the constraint). Enter submits.
5. **One-shot notice** — plain specimen label under the input: `ONE TRY PER WORD`, with the support-text draft: "Your first instinct is the data." (semantics frozen per spec §9; register tunable).
6. **Submit** — primary button. Proposed label (chrome, verb+object rule, mode-flavored): **"Mint this word"**.

**Parse failure** (400, attempt not consumed): inline helper under the input in `--negative` ink — a parse failure *is* an input error; the §7 non-match-≠-error rule governs feature chips, not validation. Draft helper (spec §9 draft, F3 tunes): "That doesn't parse as Japanese sounds — stick to romaji like *gorogoro* or *pika*." Input keeps its value, focus returns to it, `aria-describedby` wires the helper.

### 2.3 Reveal state (`.mint-reveal`)

Raised + `--shadow-card`, entering with the §7 feedback choreography (`--motion-reveal` rise, motion-safe gated). Top → bottom:

1. **Top rule: neutral `--border-strong` — never verdict-colored.** Options were (a) neutral always, (b) score-banded color. (b) rejected: banding reinstates grade-ness the spec explicitly avoids ("distance, not grade"); positive/negative are verdict colors and production has no verdict. The score numeral and chips carry all magnitude.
2. **Reveal line** (draft, §9): "The real word is **どきどき**." — kana = `words.canonical_form` verbatim (post-M2 keys per the NIL-62 kickoff), `--text-kana-feedback`, `lang="ja"`; player input is never converted (invariant 1 — displayed as typed, romaji, full stop).
3. **Word meta row:** romaji + gloss (`--text-sm`, muted) + the **replay pill** — the §8 Rating-Lab idiom exactly (icon + visible "Replay" label, secondary pill, same spin-on-activate, `StimulusPlayback` engine). First audio contact happens here, post-submit, by design.
4. **Score block:** numeral at `--text-2xl` display face + specimen label `SIMILARITY · 0–100`. Beside it, muted one-liner giving the numeral a register: draft "78 — your instinct shares most of its shape with the real word." (bands for the one-liner: ≥85 "almost the same word" / 60–84 "shares most of its shape" / 35–59 "some shared bones" / <35 "a different creature — which is also data". Copy drafts, Nils tunes.)
5. **Feature chips** (`.mint-chip` row, wrapping): specimen-label pills. Matched: `--positive` text on `--positive-soft` fill. Unmatched: `--ink-muted` text on `--surface-card` fill, `--border-soft` — *not red, not crossed out*. Chip wording table (chrome, ruled here; romaji/English only — **no kana glyphs in chips**, keeping chrome clean of script content per §10.1's spirit):

| Feature | Chip text |
|---|---|
| `redup` | `DOUBLED SHAPE` |
| `sokuon` | `SHARP CUT · Q` |
| `finalN` | `NASAL ENDING · N` |
| `riSuffix` | `-RI ENDING` |
| `voicedOnset` | `HEAVY ONSET` |
| `heavyVowelRatio` | `VOWEL WEIGHT` (matched at score-time tolerance) |
| `moraCount` | `LENGTH · {n} MORAE` |

   **Chip-row membership rule (found while computing the mockup example):** the chip row shows only features **present in either form** (plus the two continuous features, always) — for pikapika→dokidoki, four of seven features "match" as *shared absences*, and a row of green chips celebrating what neither word has reads as noise. Shared absences still count toward the score and still appear in the table twin; the chips summarize the *shape story*, the table proves the arithmetic.

6. **The breakdown table twin** — "View the full comparison" collapsible (shadcn `Table` inside a native `<details>`/summary or the existing collapse idiom — chrome; if Radix enters a loop path, H7 applies): per feature, *yours* vs *target* vs matched. This is the §2 Observatory data-table-twin pattern applied to the score — the chips summarize, the table proves.
7. **Next action:** primary "Next meaning" + ghost "Back to modes".

### 2.4 Progress, completion, empty

- **Status line above the card** (plain specimen label): `WORD 12 OF 60 · YOUR MEAN 58`. Own-data only, no leak surface. (60 = current scored inventory; count comes from the API, never hardcoded.)
- **Pool exhausted:** completion panel (washi, chrome) with personal mean + per-modality breakdown as three tinted specimen labels + one-line benchmark once triangulation has population data ("Arena mean producibility for these words: 54"). CTA pair: Rating Lab / back to modes.
- **Empty/error states** inherit Rating Lab's shipped patterns verbatim (they already model the register).

### 2.5 Responsive + a11y

375px: single column already; chips wrap to two rows; the breakdown table twin scrolls horizontally inside its details block if needed (table is chrome — allowed); input and buttons full-width, ≥44px. A11y: score announced via the existing `aria-live` polite pattern on reveal mount; chips are text (state not color-only — matched/unmatched differ by fill *and* wording context); `<details>` twin is keyboard-native; replay pill inherits §8 a11y (visible label version).

### 2.6 Build notes (NIL-62 frontend session lift list)

This section + `word-mint.html` verbatim; §9 spec strings go to Nils for adjudication **before** the session (backend ships keys only — kickoff prompt already says so); chip table + score-band one-liners ride the same adjudication message; semantic hooks first-position; no new deps (Table/Button/Dialog already in the kit; `<details>` is native). Backend session re-copies the stale `SPEC-free-form-entry.md` into the api repo (§ verification header).

---

## 3. Landing page (NIL-43) — composition

### 3.1 The arc, extended by one strip

§10.5's seven strips are adopted; the Observatory postdates them (NIL-71) and needs a home; the launcher adds E1 art slots + CC BY attribution. **Ruling: one new strip, position 6** — after Script Lab, before the modes grid — making eight:

| # | Strip | Ground | Content (adopted copy where quoted) |
|---|---|---|---|
| 1 | Hero | page | §10.5.1 verbatim (H1, sub, CTAs) + §10.6 motif: wave rule + L4 practice-kana specimen ガタン (gatan) with romaji+gloss caption — as in the NIL-64 mockup record |
| 2 | The numbers | washi | §10.5.2's three stat cards verbatim (64% / Sound→Sight→Inner states / 36% shobon) — raised *fill*, no shadow (§1 precision) |
| 3 | How it works | page | §10.5.3: three steps + the honesty line "Your guesses join the arena record — the same 30 pairs, still collecting data." |
| 4 | The dissociation | washi | §10.5.4 verbatim incl. the **binding** line "Ratings detect ideophone-ness; guessing doesn't." + ρ footnote; CTA into Rating Lab |
| 5 | Script Lab teaser | page | §10.5.5 verbatim (framing rule; 4.50 vs 4.15) |
| 6 | **The Observatory** (new) | washi | §3.2 below |
| 7 | Modes grid | page | §2.3 shell reused; six cards, honest coming-soon; cards route to auth |
| 8 | Provenance + attribution | page, bordered top | §3.3 below |

Layout mechanics stay §2.5's: full-bleed strips alternating tints, prose on `--container-read`, hero asymmetric ≥860 (copy ~60% left, motif right; stacked below with motif first), no parallax, motion per §7 only. The NIL-64 `landing-hero.html` remains the record for strips 1/2/4; `landing-composition.html` (this session) is the full-arc composition of record and supersedes nothing — it extends.

### 3.2 The Observatory strip (new, ruled here)

Asymmetric two-column mirroring the hero (art left, copy right — the reverse of strip 1, so the page breathes): 

- **Copy side.** Heading: "The arena keeps score of itself." Wave rule. Body draft: "Every guess and rating feeds a public research dashboard — the same charts the thesis drew, redrawn live as players test its claims out of sample. Thesis baselines stay pinned; the live layer grows." CTA: **"Visit the Observatory"** (ghost — the strip is an invitation, not the page's primary action; primary stays the hero's play CTA).
- **Art side: the E1 slot.** One poster-grade static export from NIL-81 (dumbbell or scatter — whichever NIL-81 crops best at ~4:3), rendered as an `<img>` on a raised-fill washi card (no shadow), alt text describing the figure. **Fallback rule (binding):** until NIL-81's export exists, the slot renders the token-styled wave-rule placeholder block in the mockup — the build must not hard-require the asset, and the strip must not ship empty-boxed. Second E1 consumer, og-image, is `<meta>` territory (NIL-81 → web `public/`), not a landing composition element.
- **"Read the research" (hero ghost CTA) destination — ruled:** it anchors to this strip (#observatory), whose CTA then enters the Observatory itself. The Observatory spec's "beyond the stats strip" destination is thereby satisfied with one scroll target and no dead-end: stats strip (2) → narrative (4/5) → Observatory strip (6) is the research read-path.

### 3.3 Provenance + attribution block (strip 8, binding content)

Three stacked lines on the read measure, `--text-sm` muted, the §10.3.4 gem done properly:

1. **Thesis line:** "Built on the author's MA thesis data — every number on this page is from the study." + thesis citation (Paulsson, SPVR01). Whether the thesis PDF itself is linked = deferred (D4, Nils — hosting his own document is his call).
2. **Citations:** Dingemanse (2012) · McLean (2021) · McLean, Dunn & Dingemanse (2023) · Iida & Akita (2023) — one line, no links required at v1 (links fine if trivial).
3. **Data licenses (the CC BY block, mandatory):** "Reference data: McLean, Dunn & Dingemanse (2023), CC BY 4.0 · Iida & Akita (2023) perceptual-strength norms · Winter et al., playful iconicity, CC BY 4.0. This deployment is non-commercial; research data is reproduced with attribution." Exact list mirrors the Observatory's §4.3 vendor table and its attribution footer — landing and Observatory must never disagree about credits; the build lifts the list from the Observatory footer implementation if one exists first.

### 3.4 CTA grammar and the auth boundary

- **Primary path:** hero "Prove it — play a round" → auth (register tab default for logged-out visitors) → **directly into Meaning Match instructions**, not the mode grid — the CTA promised a round; landing on a menu breaks the promise. (Mode grid remains one click away via the header.) Logged-in visitors skip to Meaning Match instructions immediately.
- **Mode cards (strip 7)** route to auth with the chosen mode as the post-auth target — same promise-keeping rule.
- **Header:** public landing header = wordmark + "Log in" (per the NIL-64 mockup). The in-app header's Observatory link (NIL-78, shipped) is unaffected.
- **Observatory public exposure: explicitly deferred, decider = Nils at the NIL-43 build session** (the Observatory spec delegates exactly this). The design works under both answers: if public, strip 6's CTA deep-links straight in; if auth'd, it routes through auth-with-return like the mode cards. Nothing in the composition assumes either.

### 3.5 Responsive + a11y

375px: hero stacks with motif first (order:-1, per the mockup record); stat cards single-column; Observatory strip stacks art-above-copy; modes grid collapses per §2.3; all targets ≥44px; strips keep their alternating tints (no tint collapses to page-on-page). A11y: strips are `<section>`s with headings in order (single h1 in the hero); the E1 image alt describes the chart's claim ("Dumbbell chart: thesis vs live accuracy by modality"), not its filename; kana specimen carries `lang="ja"` + the visible romaji-gloss caption is its accessible text sibling; CTAs are real links/buttons with §6 focus states.

### 3.6 Build notes (NIL-43 build session lift list)

This section + `landing-composition.html` + the NIL-64 `landing-hero.html` record; §10.5 copy verbatim where marked adopted; E1 asset from NIL-81 (fallback rule if absent); attribution list cross-checked against the shipped Observatory footer; the two deferred decisions (D1 public boundary, D4 thesis link) resolved with Nils in the session plan **before** building; landing is bespoke sections + `Button` CTAs only (§5 row) — no new components, no new deps.

---

## 4. Binding rulings ledger (V-ledger, NIL-64 format)

| # | Ruling | Binds |
|---|---|---|
| V1 | Floor order = **Sound → Sight → Touch → Inner states** (McLean hierarchy; four-floor spec §5 wins over UI-SYSTEM §10.2's aside — §10.2 gets a doc-fix when next touched) | 28A/28B |
| V2 | Ladder composition = vertical washi floor stack on the read measure; no tower art, no stepper | 28B |
| V3 | Floor identity = tinted specimen label (`FLOOR {i} · SOUND`) + 3px inline-start modality rule; **no soft-fill card tints**; ordinals render from API array position, never persisted | 28B |
| V4 | Floor states = status pills (`CLEARED · n/10` positive / `UP NEXT` vermillion / `AFTER FLOOR {n}` muted / `IN PREPARATION` muted); **no lock icons, no grayed-out cards** | 28B |
| V5 | Difficulty is structural pre-answer (thesis-facts §8 order; position only); thesis numbers appear **post-answer** (research note) and at floor completion — never before | 28A/28B |
| V6 | Final pair marker = `FINAL RUNG` specimen label; "boss" stays internal vocabulary; coin-flip honesty lives in copy | 28B |
| V7 | Floor completion + summit use the §10.3.2 benchmark grammar (you vs thesis floor mean 6.86/6.42/5.97; Touch: "you're the norming study") — text, no charts | 28B |
| V8 | Touch teaser: client-side, at hierarchy position 3, KOKE label, `IN PREPARATION`, recording-gate honesty copy; removed the day the API serves it | 28B |
| V9 | Floor intro = shadcn Dialog **with its browser-loop idiom shipped same session (§16 H7)**; sanctioned fallback = inline intro block, same content | 28B |
| V10 | Word Mint prompt card = raised + shadow (instrument's voice); trial-board type ladder reused (`--text-prompt` / `--text-question`); modality shown pre-submit as tinted `MEANING · {MODALITY}` label | NIL-62 FE |
| V11 | Input: native, raised fill, `--text-md`, maxlength 24, `autocapitalize/autocorrect/spellcheck/autocomplete` off, instruction line is the label, **no live counter**; Enter submits; parse errors = `--negative` inline helper, value + focus retained | NIL-62 FE |
| V12 | Reveal top rule = neutral `--border-strong`, **never verdict- or score-colored**; score numeral `--text-2xl` + `SIMILARITY · 0–100` specimen label + banded one-liner (drafts §2.3.4) | NIL-62 FE |
| V13 | Feature chips per §2.3.5 wording table — romaji/English only, no kana glyphs; matched = positive/positive-soft, unmatched = muted on card (never negative); **chip row shows present-in-either features only** (shared absences live in the table twin); full comparison = collapsible table twin | NIL-62 FE |
| V14 | Word Mint status line = `WORD {i} OF {n} · YOUR MEAN {m}` specimen label; counts from API | NIL-62 FE |
| V15 | Landing arc = **eight strips**: §10.5's seven + The Observatory at position 6 (washi, art-left/copy-right asymmetric) | NIL-43 |
| V16 | E1 art slot: one poster export in strip 6 on a raised-fill card; **binding fallback** = token-styled wave placeholder until NIL-81 lands; og-image is meta-only | NIL-43 |
| V17 | Attribution block per §3.3: thesis line + citations + CC BY data-license line; must match the Observatory footer's credit list | NIL-43 |
| V18 | CTA grammar: hero primary → auth → Meaning Match instructions (promise-keeping rule; mode cards carry their mode as post-auth target); hero ghost "Read the research" anchors to strip 6, whose ghost CTA enters the Observatory | NIL-43 |
| V19 | Mockup convention: one responsive HTML per surface (desktop + 375px via media queries), H2 token names, semantic hooks first — the NIL-64 pattern continued | all three |

## 5. Explicitly deferred (named deciders)

| # | Question | Decider | When |
|---|---|---|---|
| D1 | Observatory public exposure (public deep-link vs auth-with-return) | Nils | NIL-43 build session plan |
| D2 | ~~Word Mint §9 frozen strings + chip wordings / score-band one-liners / button label~~ **RESOLVED 2026-07-06 as NIL-83 — frozen strings in §8** | Nils | done |
| D3 | ~~XL mode name (Lingua Quest / Polyglot Challenge)~~ **RESOLVED 2026-07-07: Polyglot Challenge** (Nils; ledger row closed at NIL-84 doc-fix pass) | Nils | done |
| D4 | Thesis PDF linked from the landing provenance line | Nils | NIL-43 build session plan |
| D5 | Which E1 export (dumbbell vs scatter) fills strip 6, and its crop | NIL-81 session output; Nils's eye | when NIL-81 lands |
| D6 | Floor replayability after clear (assumed yes) | 28A session (mechanics) | 28A plan |

## 6. What each consuming session takes from here

| Session | Takes | Must not touch |
|---|---|---|
| 28A (api, Opus) | §1.2 order · §1.5 structural difficulty + §8-of-thesis-facts sequences · V5 · D6 · re-copy stale ladder spec to api repo | trial mechanics beyond the ordering map; invariants |
| 28B (web, Opus) | §1 wholesale + `ladder-floors.html` · V1–V9 · Dialog H7 rider · re-copy stale ladder spec to web repo | trial board internals (§2.4 constants, frozen strings); token values |
| NIL-62 FE (Opus) | §2 wholesale + `word-mint.html` · V10–V14 · D2 outcome | §9 semantics without D2; kana derivation (invariant 1); scorer/spec §5 |
| NIL-43 (build) | §3 wholesale + `landing-composition.html` + NIL-64 hero record · V15–V18 · D1/D4/D5 outcomes | frozen strings; stat invention (thesis-facts only); §10.5 adopted copy except where V15 extends it |

## 7. Proof battery for the consuming builds

Standard §11.4 bar (pnpm battery + browser loop at desktop and 375px) plus, per surface: **28B** — floor stack renders 3 floors from a 3-floor API payload and 4 from a 4-floor payload with zero code delta (fixture test); teaser removed-flag asserted; Dialog loop idiom green or fallback exercised; KOKE label pairing present on every haptic-colored element. **NIL-62 FE** — parse-error path leaves the attempt unconsumed (browser proof); reveal shows kana verbatim from the DTO (string-equality assert, invariant 1); chips render both states from fixture. **NIL-43** — all eight strips at both viewports; E1 fallback renders when the asset is absent; attribution list string-matches the Observatory footer's; hero CTA lands on Meaning Match instructions post-auth (loop waypoint).

---

## 8. AMENDMENT — NIL-83 frozen Word Mint strings (adjudicated 2026-07-06, Fable; closes deferral D2)

Nils ruled every group in chat. These strings are **frozen** — NIL-62-FE lands them verbatim in `experimentText.ts` (additive; existing entries untouched). They supersede the drafts in this spec's §2.2/§2.3 and `SPEC-free-form-entry.md` §9. Overriding principle from adjudication: **player copy is language-neutral** — no string names Japanese (XL/cross-linguistic future; the kana reveal and romaji examples carry the language implicitly).

### 8.1 Prompt state

| Key | String |
|---|---|
| Instruction line | Invent a word whose sound fits the meaning below. Type it in roman letters. |
| Meaning line | **{meaning}** (bold, no quotes — house style, unchanged) |
| One-shot label | ONE TRY PER WORD |
| One-shot support | Your first instinct is the data. |
| Submit button | Mint this word |
| Parse-error helper | That didn't read as speakable syllables — try simple roman letters, like *gorogoro* or *pika*. |

### 8.2 Reveal state

| Key | String |
|---|---|
| Reveal line | The real word is **{displayForm}**. |
| Score label | SIMILARITY · 0–100 |
| Band ≥85 | Your instinct is almost the same word. |
| Band 60–84 | Your instinct shares most of its shape with the real word. |
| Band 35–59 | Your word and the real one share some bones. |
| Band <35 | A different creature, which is also data. |
| Primary action | Next meaning |
| Ghost action | Back to modes |

**Amended 2026-07-10 (NIL-85).** The bands were frozen as `{score} — <tail>`, so the numeral opened the sentence
and the caption repeated the large `.score-figure` beside it (§2.3.4). Nils ruled the numeral out of the caption
and the em-dash out of the copy (project-wide ban); `MINT_BAND_DIFFERENT` lost its internal em-dash too. Each band
is now a standalone sentence. `scoreBandTail` was renamed `scoreBandCaption` to match.

### 8.3 Feature chips (§2.3.5 table frozen as written)

`DOUBLED SHAPE` · `SHARP CUT · Q` · `NASAL ENDING · N` · `-RI ENDING` · `HEAVY ONSET` · `VOWEL WEIGHT` · `LENGTH · {n} MORAE`. The `· Q` notation is deliberate (matches scorer normalization; §10.3 hidden-gem hook).

### 8.4 Status/completion

Status line `WORD {i} OF {n} · YOUR MEAN {m}` and empty/error states per §2.4 (Rating Lab patterns verbatim) — no new strings needed beyond the above.
