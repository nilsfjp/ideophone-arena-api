# UI-SYSTEM — Ideophone Arena full-app design spec (NIL-64)

_Fable 5 design session, 2026-07-03. Status: **ADOPTED** (ledger L1–L5 resolved in chat with Nils, 2026-07-03; §13) → **§1–§7 LANDED by NIL-65, 2026-07-04**; as-built wiring canonized as **§16 (normative)**. Consumed by: NIL-65 (migration build — done), NIL-63 (replay — next; companion prompt `session-NIL-63-prompt.md`), 28B (ladder UI), NIL-62 (free-form UI), NIL-43 (landing). The consumed NIL-65 prompt lives in the planning folder's `archive/`. Color derivations verified programmatically the design session (OKLab reference implementation + Machado 2009 CVD matrices + WCAG 2.x relative luminance); every number in §4 is computed, not eyeballed._

_Binding context: Tailwind v4 + selective shadcn/ui (settled 2026-07-03); `tokens.css` stays the single source of truth via `@theme`; frozen `experimentText.ts` strings and invariants 1–3 untouchable; kana rendered verbatim — typography may style, never transform. This spec drafted the token values; **NIL-65 landed them (2026-07-04, full proof battery green at 1280px and 375px)**. Second-pass adjudication same day: **harmonize to vanilla Tailwind/shadcn expectations** — §16 is the normative wiring canon (H1–H8 target table; **NIL-69 executes it**), including the H2 token rename map (`--vermillion`, `--modality-*`) applied to this spec's prose 2026-07-04. Until NIL-69 lands, code matches §16's as-built column. Web-repo package manager: **pnpm** (canonical 2026-07-04; all proof commands are pnpm)._

---

## 0. Design thesis

**The problem is composition, not color.** The palette works; the components read as cobbled-together because every container is the same washi card with a 1px border at slightly different paddings, every heading competes at similar weight, and the trial board's sizes are literals that predate the type scale. The fix is a small set of load-bearing rules, applied everywhere:

1. **Surface grammar — "raised paper is the instrument's voice."** Elevation is reserved for the experiment speaking: stimulus cards and the verdict (feedback panel). All chrome sits flat on washi with hairline borders. (§1)
2. **The specimen label** — the small-caps tracked label that already exists in embryo (`.card-side-label`, `.practice-note`, `.mode-card-status`, `.rating-reveal-label`) becomes the app's unifying primitive, reused by every future mode (floor labels, language chips, axis chips). (§3.4)
3. **One type ladder, two scripts** — a Latin UI scale and a kana display scale with named roles; the trial board's odd literals get mapped onto tokens. (§3)
4. **State grammar** — one hover/active/focus/selected/disabled vocabulary across all interactive categories. (§6)
5. **Motion speaks the same dialect everywhere** — a three-step timing ladder, opt-in under `prefers-reduced-motion: no-preference`, never on layout properties. (§7)

Aesthetic direction (unchanged, sharpened): **laboratory ink and paper** — a warm research bench, editorial and scientific. The memorable anchors: the kana as the undisputed star on raised paper, the specimen-label system, and (ledger item L5) a depictive "wave rule" motif for chrome headings — form depicting sound, which is the entire subject.

What this spec deliberately does **not** do: invent a dark theme (tokens are surface-relative; re-mapping stays possible later), redesign the trial board's reserved-slot structure (invariant 5 — the reserved sizes are timing-validity constants, documented in §2.4), or touch any frozen string.

---

## 1. Surface grammar

Three paper levels, each with one job:

| Surface | Token | Role | Border | Shadow |
|---|---|---|---|---|
| Page ground | `--surface-page` | The desk. Shows through as margins; never a component background. | — | — |
| Washi card | `--surface-card` | **All chrome**: mode cards, instructions, script-lab selector, completion panel, leaderboard columns, auth tabs, feedback-choice sub-cards. | `--border-soft` (static) / `--border-mid` (interactive affordance) | none |
| Raised paper | `--surface-raised` | **The instrument's voice only**: ideophone cards, rating stimulus card, feedback panel (the verdict), form inputs (affordance exception), rating reveal (arena record). | `transparent` or `--border-soft` | `--shadow-card` |

Precision (from the critique pass): the grammar's teeth are in the **shadow**, not the fill. `--surface-raised` *fill without shadow* is permitted where washi-on-washi would be mud (inputs, landing stat cards on a washi strip, card hover-lift). `--shadow-card` remains instrument-voice only; `--shadow-raised` overlay-only.

Migration deltas this rule forces (NIL-65):

- `.complete-panel`, `.error-panel`: `--surface-raised` + `--shadow-card` → `--surface-card` + `1px --border-soft`. Chrome doesn't cast shadows.
- `.condition-option`, `.mode-card` hover: keep `--surface-raised` as **hover fill** (lift-on-hover is a state, not a resting elevation) but no shadow at rest.
- `.feedback` keeps raised + shadow (it is the verdict). `.rating-reveal` keeps raised (arena record = instrument output), gains `--shadow-card` for consistency with feedback.
- `--shadow-raised` is reserved for transient overlays only (dialogs, toasts — the shadcn layer). Nothing at rest uses it.

**Why it works:** elevation regains meaning — when something floats off the bench, the experiment is talking to you. This is the single highest-leverage composition fix.

---

## 2. Layout system and spacing rhythm

### 2.1 Containers

| Token (new, `@theme`-mapped) | Value | Role |
|---|---|---|
| `--container-app` | `1120px` | Header bar content width (`.site-main` today) |
| `--container-read` | `760px` | Every panel and the trial stage — the reading measure |
| `--container-form` | `420px` | Auth form, completion summary |

Rule: exactly one `--container-read` wrapper per view; panels never nest containers. (Today `.rating-lab`, `.mode-select`, `.trial-stage` etc. each re-declare `min(100%, 760px)` — the token replaces the copies.)

### 2.2 Vertical rhythm

| Position | Token | Notes |
|---|---|---|
| View top padding | `--space-5` (desktop) / `--space-3` (≤640) | uniform across views (today: 2rem/1.75rem/pad-top-0 mixtures) |
| Between sections within a view | `--space-6` | e.g. instructions copy → script lab → sound check |
| Panel internal padding | `--space-5` / `--space-3` (≤640) | feedback panel, completion, script-lab selector |
| Stack gap inside a panel | `--space-4` | grid `gap` default |
| Label → content | `--space-2` | specimen label to its object |
| Chip/inline padding | `--space-1` `--space-2` | pills, chips |

Rule: **no margin/padding literals in component CSS.** The only sanctioned literals are the trial board's reserved-slot dimensions (§2.4). Everything else is a `--space-*` step. This is checkable and becomes part of the NIL-65 exit proof (§11.4).

### 2.3 The six-mode home shell

`ModeSelect` must scale from 3 to 6 cards (Choosing Task incl. Script Lab, Rating Lab, Modality Ladder, Free-form, Template Reading, Cross-Linguistic) without a code change — cards render from `modes.ts` data only.

- Grid: `repeat(auto-fill, minmax(220px, 1fr))` inside `--container-read` → 3×2 at desktop, 2×3 at ~700px, 1-col ≤640. No hardcoded `repeat(3, …)`.
- Card anatomy (top → bottom): **measure chip** (specimen label, §3.4 — e.g. `MEASURE · GUESSING`), title (`--font-display`, `--text-md`), one-line description (`--text-sm`, `--ink-muted`), status slot (reserved even when empty — cards stay equal-height per row via grid).
- Coming-soon stays the honest pattern already shipped: full-opacity copy, `--ink-muted`, pill badge, `disabled` + `aria-disabled`, **no lock icons, no grayed-out mystery** — the description tells the truth about what the mode will measure.
- Mode cards are the shadcn `Card` beachhead (§5), but the measure chip and status pill are the shared bespoke primitives.

### 2.4 Reserved-slot constants (invariant-5 annex — do not tokenize)

These are **timing-validity constants**, not rhythm: `.trial-stage` min-heights (760/620/0), `.stimulus-row` card dimensions (320×180 desktop; aspect-ratio 16/9 ≤860; min-height 132 ≤640), `.translation-lines` min-height 96, `.question-slot` min-height 54, `.status-line` 1.5em, `.rating-reveal` min-height 9.5rem, fixation-cross overlay. They move only with an explicit invariant-5 discussion. NIL-65 must carry them through the migration byte-identical. They are documented here so nobody "cleans them up" into the spacing scale.

### 2.5 Landing layout (NIL-43 consumer)

Full-bleed horizontal strips alternating page-ground and washi tints; text on the `--container-read` measure. Hero: asymmetric two-column ≥860 (copy left ~60%, motif right — §10.6), stacked below. Stats strip: three specimen-labeled figures on washi cards (§10.5 provides the copy). No parallax; motion per §7 only.

---

## 3. Typography hierarchy

Faces are settled (Nils, 2026-06-13) and survive: **Zen Maru Gothic** display, **LINE Seed JP 400** body, **LINE Seed JP Bold** stimulus kana. What changes: named roles replace ad-hoc sizes, and the trial board's pre-scale literals get tokens.

### 3.1 Latin UI ladder (roles, not just sizes)

| Role | Token | Face/weight | Used by |
|---|---|---|---|
| Page title | `--text-2xl` | display 700 | view `h1` |
| Section head | `--text-lg` | display 700 | feedback verdict `h2`, panel `h2` |
| Sub-head | `--text-md` | display 700 | mode-card title, script-lab `h2`, record `h2` |
| Body | `--text-base`/`--text-md` | body 400 | instructions at `--text-md`, everything else base |
| Control | `--text-ui` | body 700 | buttons, tabs |
| Support | `--text-sm` | body 400 | descriptions, tables, anchors |
| Specimen label | `--text-xs` | body 700 + tracking | §3.4 |

**Weight discipline:** today almost everything chrome is 700. New rule — 700 belongs to display headings, controls, kana, and `<strong>` targets inside frozen lines; body copy and descriptions are 400. (The bolded `{target}` interpolations are part of the instrument and keep their `<strong>` — this rule is about chrome prose around them.)

### 3.2 Trial-board prompt ladder (new tokens; kills the literals)

| New token | Value | Replaces |
|---|---|---|
| `--text-prompt` | `clamp(1.3rem, 2.6vw, 1.9rem)` | `.trial-copy` 2rem / 1.55 / 1.25 breakpoint literals |
| `--text-question` | `clamp(1.25rem, 2.2vw, 1.55rem)` | `.question-text` + `.translation-lines` 1.55 / 1.45 / 1.25 / 1.1 literals |

Hierarchy intent: the frozen listen-instruction leads (`--text-prompt`), the meanings and the canonical question share `--text-question` with the bolded target carrying the emphasis — the question's decision weight comes from the `<strong>` target and its position, not a size jump mid-board. Wording, order, and bolding are untouchable (invariants); sizes are presentation and may land these tokens. Clamp midpoints chosen to keep rendered sizes within ~5% of today's at 375px and 1120px so reserved slots don't need re-derivation (proof step: §11.4 both-viewport screenshots + slot-geometry assertions stay green).

### 3.3 Kana display ladder

| Token | Value | Role |
|---|---|---|
| `--text-kana-card` | `clamp(2.6rem, 7vw, 3.6rem)` | choosing-card kana (unchanged) |
| `--text-kana-feedback` | `1.9rem` | feedback + reveal kana (unchanged) |
| `--text-kana-hero` | `clamp(3.2rem, 9vw, 5.4rem)` — **new, chrome only** | landing hero motif (§10.6), styleguide |
| `--text-hero` | `clamp(2.2rem, 5vw, 3.4rem)` — **new, chrome only** | landing hero H1 (`--text-2xl` is a panel size, not a hero size) |
| `--text-xl-form` | `clamp(1.6rem, 5vw, 2.4rem)` — **new, reserved** | XL long non-kana forms (`ŋɛrŋorŋɛrŋor…`), per SPEC-cross-linguistic §6 |

Reserved face token: `--font-stimuli-latin` — IPA-capable face for XL word forms. **Value now:** fallback-only stack (`"Noto Sans", "DejaVu Sans", sans-serif`); the self-hosted Noto Sans @fontsource install rides the XL build's own dependency gate, not NIL-65. Kana discipline note: these tokens size and set faces; nothing here (or anywhere) converts, detects, or derives kana — display strings arrive from the backend verbatim.

### 3.4 The specimen label (unifying primitive)

`--text-xs` · weight 700 · `letter-spacing: var(--tracking-wide)` · `text-transform: uppercase` · default ink `--ink-muted`.

Variants: **plain** (reveal label, rating progress), **pill** (1px border, `--radius-pill`, `--space-1/2` padding — practice note, mode status), **ring** (circled — card side labels A/B), **tinted** (modality text color — floor labels, feedback research-note label). Consumers today: `.card-side-label`, `.practice-note`, `.mode-card-status`, `.rating-reveal-label`, `.rating-progress`, `.feedback-side`. Consumers tomorrow: measure chips (§2.3), Ladder floor labels (`FLOOR 1 · SOUND` in `--modality-auditory`), XL language chips (small caps, **no flags** — languages ≠ countries), Template axis chips (`SHAPE · REPETITION`), free-form feature chips. One CSS class (or one tiny component in the bespoke layer), six modes fed.

---

## 4. Color — OKLCH token table, ramps, and the haptic adjudication

Method: exact sRGB→OKLab per Ottosson's reference; round-trip verified (`oklch()` → 8-bit hex reproduces every anchor byte-identically, so the migration is lossless). CVD: Machado et al. (2009) severity-1.0 protanopia/deuteranopia matrices in linear sRGB; separations reported as OKLab ΔE (rule of thumb: ≈0.02 barely distinguishable, ≥0.06 comfortably categorical). Contrast: WCAG 2.x. **Perceptual uniformity ≠ CVD safety — every value below was checked, not assumed.**

### 4.1 Anchor conversion table (tokens.css → oklch, lossless)

| Token | Hex (provenance) | `oklch()` |
|---|---|---|
| `--surface-page` | `#e8e3d9` | `oklch(91.70% 0.0145 84.58)` |
| `--surface-card` | `#fbf8f2` | `oklch(97.98% 0.0086 84.57)` |
| `--surface-raised` | `#ffffff` | `oklch(100% 0 0)` |
| `--ink-primary` | `#211e19` | `oklch(23.66% 0.0104 80.60)` |
| `--ink-muted` | `#5e5749` | `oklch(45.92% 0.0237 84.56)` |
| `--ink-inverse` | `#fbf8f2` | `oklch(97.98% 0.0086 84.57)` |
| `--vermillion` | `#c8401f` | `oklch(56.34% 0.1781 34.51)` |
| `--vermillion-hover` | `#b23618` | `oklch(51.42% 0.1654 34.39)` |
| `--vermillion-active` | `#9e3015` | `oklch(47.22% 0.1504 34.55)` |
| `--modality-auditory` | `#8a5512` | `oklch(49.87% 0.1035 65.93)` |
| `--modality-auditory-soft` | `#ebd7b0` | `oklch(88.58% 0.0559 83.97)` |
| `--modality-visual` | `#99454f` | `oklch(50.16% 0.1127 14.70)` |
| `--modality-visual-soft` | `#e9c4c7` | `oklch(85.29% 0.0423 12.49)` |
| `--modality-interoceptive` | `#386376` | `oklch(47.64% 0.0568 228.32)` |
| `--modality-interoceptive-soft` | `#b9d3dc` | `oklch(85.04% 0.0306 220.63)` |
| `--positive` | `#2a6b43` | `oklch(47.44% 0.0930 153.74)` |
| `--positive-soft` | `#adcab8` | `oklch(81.24% 0.0400 158.15)` |
| `--negative` | `#9d2c1a` | `oklch(46.64% 0.1520 31.81)` |
| `--negative-soft` | `#e3c0b8` | `oklch(83.58% 0.0417 33.01)` |
| `--border-strong` | `#211e19` | `oklch(23.66% 0.0104 80.60)` |
| `--border-mid` | `#867f70` | `oklch(59.83% 0.0237 85.96)` |
| `--border-soft` | `#c9c2b4` | `oklch(81.59% 0.0208 84.59)` |

thesis_colors.xml anchors (canonical, for provenance comments): accent1 `#9c6114` = `oklch(54.58% 0.1137 66.49)`, accent2 `#e9c4c7`, accent3 `#b9d3dc`, accent4 `#adcab8`, accent5 `#d6d2c4` = `oklch(86.31% 0.0195 93.69)`, accent6 `#bfb8af` = `oklch(78.58% 0.0149 74.39)`.

NIL-65 rewrites tokens.css values as `oklch()` with the hex kept as a trailing provenance comment per line. (`oklch()` is Baseline-supported; no fallback layer needed for the Vite target matrix.)

### 4.2 The ramp model (measured from the hand-tuned accent, now the formula)

The existing hover/active ramp is **not** a pure L-shift — it darkens *and* de-saturates, which is exactly why it stays in sRGB gamut where a constant-chroma shift clips:

> **hover** = base `L − 0.049`, `C − 0.013`, `H` const · **active** = base `L − 0.091`, `C − 0.028`, `H` const

Verified: this model reproduces the shipped `--vermillion-hover`/`--vermillion-active` (ΔE 0.013/0.028 ≈ imperceptible at interaction speed) and produces in-gamut ramps for every modality color and every haptic candidate (§4.4 table). Disabled state stays `opacity: 0.62` (composited, works on any surface) — no disabled color tokens.

### 4.3 Derived-trio stats (the derivation inputs)

Modality text variants: mean `L 0.4922`, `C 0.0910` (aud 65.9°, vis 14.7°, int 228.3°); all clear AA on both papers (4.85–5.11:1 page, 5.85–6.16:1 card). Soft fills: mean `L 0.8630`, `C 0.0429`.

**Baseline CVD finding (existing palette, disclosed):** under protanopia, `--modality-auditory` vs `--vermillion` separate by only ΔE 0.013, and under deuteranopia aud/vis sit at 0.060. The palette's own floor is therefore ~0.06 under deutan and the aud/accent protan pair is already a known collision. Mitigation is structural and already app policy: **modality color is never the sole carrier** — always paired with a specimen label or text ("Research note: auditory", floor names, chip text). This rule graduates from habit to invariant-adjacent guideline in this spec.

### 4.4 Haptic (fourth modality) candidates — ledger item L1, Nils picks

Derivation rule from the brief: match trio L/C, rotate hue. Two findings before the candidates: (a) the ladder spec's hinted **clay/terracotta at matched L/C is dead on arrival** — at H 55° it lands 11° from the auditory brown and separates by ΔE **0.026 for normal vision** (#8e5730 vs #8a5512 — effectively the same color); (b) under dichromacy the hue wheel collapses to ~2 families and all three existing hue slots are taken, so **no fourth hue can be categorically CVD-distinct at the trio's shared lightness** — lightness is the only channel that survives. Candidate C therefore deliberately breaks the L-match rule and buys its distinctness with darkness; that trade is the pick.

| | **C1 · UME (plum)** | **C2 · KOKE (moss)** | **C3 · TSUCHI (deep clay)** — recommended |
|---|---|---|---|
| `--modality-haptic` | `#855380` · `oklch(51.52% 0.0910 330)` | `#5d6a2a` · `oklch(49.95% 0.0910 120)` | `#743d0e` · `oklch(42.00% 0.0950 55)` |
| Derivation | rule-faithful (L/C matched) | rule-faithful (L/C matched) | hue from the ladder spec's clay hint; **L lowered 0.49→0.42** (disclosed break) |
| Contrast page/card | 4.63 / 5.59 ✓AA | 4.61 / 5.56 ✓AA | **6.80 / 8.20** ✓AA+ |
| Worst ΔE vs trio+accent, normal | 0.080 (vis) ✓ | 0.088 (aud) ✓ | 0.080 (aud) ✓ |
| Worst, deuteranopia | 0.042 (int) ⚠ below palette floor | **0.023 (aud) ✗** | **0.079 (aud) ✓** |
| Worst, protanopia | **0.022 (int) ✗** | 0.028 (accent) ✗ | **0.083 (aud) ✓** |
| Soft (`-haptic-soft`) | `#e3c8e0` · `oklch(86.3% 0.0429 330)` | `#ced7b7` · `oklch(86.3% 0.0429 120)` | `#dfbea9` · `oklch(82.5% 0.0480 55)` (L offset from aud-soft for the same reason) |
| Hover / active (§4.2 model, in-gamut ✓) | `#73496e` / `#62415e` | `#505b26` / `#464f26` | `#61330c` / `#4f2b0e` |
| Semantics | ume-plum; Japanese but not tactile | moss; texture-adjacent | **earth/clay — touch itself**; honors the ladder spec's hint |
| Cost | protan users lose it against the slate blue | deutan users lose it against the auditory brown | visually heavier than the trio (reads "deep floor" — arguably right for the ladder's fourth floor); ΔE vs `--negative` is weak under protan (0.012) but the two never share a role or a screen region (feedback verdict vs floor identity), both always label-paired |
| CVD verdict | fails protan | fails deutan | **only candidate clearing the palette's own floor under all three vision types** |

Killed in testing: matched-L clay (normal-vision collision above), teal H195 (0.028–0.053 vs interoceptive everywhere). Soft-fill separations are small across the board (palest tints); softs are chart/fill colors and always label-paired — disclosed, not fixable at these lightness levels.

**ADOPTED (Nils, 2026-07-03): C2 KOKE.** Rule-faithful derivation wins; the deuteranopia weakness (ΔE 0.023 vs the auditory brown) is accepted with the structural mitigation made mandatory: **haptic-colored UI always carries its text label** (floor names, chips — §4.3 rule, now load-bearing for the Touch floor). TSUCHI and UME not selected; their data stays above for the record.

Adopted tokens (NIL-65 lands them):

```css
--modality-haptic:        #5d6a2a; /* oklch(49.95% 0.0910 120) — KOKE; AA 4.61:1 page, 5.56:1 card */
--modality-haptic-soft:   #ced7b7; /* oklch(86.30% 0.0429 120) — fill/chart tint; ink-on-it 11.09:1 */
--modality-haptic-hover:  #505b26; /* §4.2 ramp model, in gamut */
--modality-haptic-active: #464f26; /* §4.2 ramp model, in gamut */
```

### 4.5 Non-text and utility checks (verified)

Focus ring `--vermillion` on page 3.90:1, card 4.71:1, raised 4.99:1 — clears the 3:1 non-text bar everywhere. **Accent as colored text: card/raised surfaces only, never on the page ground** (3.90 < 4.5 — tokens.css already documents this; restated here because specimen-label `tinted` variants must obey it). Button text `--ink-inverse` on `--vermillion` 4.71:1 ✓AA. `--border-mid` affordance 3.11:1 page / 3.75:1 card ✓. `--ink-muted` 5.59/6.75 ✓. `--positive` 5.00, `--negative` 5.85 on page ✓. No changes needed; the table exists so NIL-65's guard test (§11.3) has canonical expected values.

### 4.6 Tailwind v4 wiring and the shadcn theme map

`tokens.css` stays byte-canonical (values and provenance; names per the §16 H2 map). Tailwind consumes it via **`@theme inline` referencing the existing custom properties** — zero value duplication. **Harmonized wiring (§16 H1, NIL-69 target):** vanilla import with preflight, legacy CSS in `@layer app`:

```css
/* app.css head — harmonized (H1). Vanilla import incl. preflight; layer order
   pre-declared so legacy @layer app sits under utilities. */
@layer theme, base, components, app, utilities;
@import "tailwindcss";
@import "./tokens.css";
@import "./theme.css";          /* @theme inline token → utility map (sibling file) */
@import "./shadcn-bridge.css";  /* shadcn vars ← tokens */
```

`@layer app` is declared before `utilities`, so appended utilities win on shadcn/chrome components while bespoke surfaces (which append no utilities) render from `@layer app` unchanged; preflight sits in `base`, under everything. Compensating base rules go in `@layer app` only where the proof battery shows drift — the invariant-5 geometry waypoints are the hard gate (H1 revert criterion). *(NIL-65 shipped without preflight + a global button reset; superseded by this wiring when NIL-69 lands.)* The `@theme inline` map itself (illustrative shape below, full version in `src/styles/theme.css`):

```css
@theme inline {
  --color-surface-page: var(--surface-page);
  --color-surface-card: var(--surface-card);
  --color-surface-raised: var(--surface-raised);
  --color-ink: var(--ink-primary);
  --color-ink-muted: var(--ink-muted);
  --color-ink-inverse: var(--ink-inverse);
  --color-vermillion: var(--vermillion);    /* + hover/active */
  --color-auditory: var(--modality-auditory); /* + soft; visual, interoceptive, haptic same pattern */
  --color-positive: var(--positive);        /* + soft; negative same */
  --color-border-soft: var(--border-soft);  /* + mid, strong */
  --font-display: var(--font-display);
  --font-body: var(--font-body);
  --font-stimuli: var(--font-stimuli);
  --text-*: …;        /* STANDARD size names only (xs/sm/base/lg/xl/2xl) — H4 */
  --radius-sm/md/lg: …; --shadow-card/raised: …;
}
```

Utilities then read `bg-surface-card`, `text-ink-muted`, `text-auditory`, `bg-vermillion`, `font-stimuli`, `rounded-md`, `shadow-card`. Radius roles: `sm` = chips/inline controls, `md` = controls + cards, `lg` = panels, `pill` = status chips; **`--radius-xl` is aliased to `--radius-lg`** so shadcn Card/Dialog defaults (`rounded-xl`) resolve to the panel radius.

Two rules the map carries (§16 H4/H5):

- **Spacing is deliberately NOT remapped.** `--space-5..8` (1.5/2/3/4rem) diverge from Tailwind's numeric defaults; remapping `--spacing-*` would corrupt every shadcn component's padding, which assumes the default scale. Bespoke rhythm (§2.2) stays enforced in `app.css` via `var(--space-*)`.
- **The utility surface uses only standard font-size names.** Tailwind's `text-*` namespace holds both sizes and colors, and stock `tailwind-merge` only disambiguates the standard size names — so the `@theme` font-size map is restricted to `xs/sm/base/lg/xl/2xl` (values still ours by reference). The non-standard §3 roles (`ui`, `md`, `prompt`, `question`, `kana-*`, `hero`, `xl-form`) stay tokens in tokens.css, consumed via `var()` in `@layer app` semantic classes or as arbitrary-value utilities (`text-[length:var(--text-ui)]`, natively size-classified by tailwind-merge) — **never as named utilities**. No `extendTailwindMerge` config exists; `cn()` is stock `twMerge`.

shadcn variable bridge (its components read these; values are our tokens, again by reference):

| shadcn var | ← token | shadcn var | ← token |
|---|---|---|---|
| `--background` | `--surface-page` | `--muted-foreground` | `--ink-muted` |
| `--foreground` | `--ink-primary` | `--destructive` | `--negative` |
| `--card` | `--surface-card` | `--border` | `--border-soft` |
| `--popover` | `--surface-raised` | `--input` | `--border-mid` |
| `--primary` | `--vermillion` | `--ring` | `--vermillion` |
| `--primary-foreground` | `--ink-inverse` | `--radius` | `--radius-md` (7px) |
| `--secondary` | `--surface-raised` | `--muted` | `--surface-card` |
| `--secondary-foreground` | `--ink-primary` | `--accent`* | `--surface-card` |

*shadcn's `--accent` is its hover-wash var, not our vermillion. The H2 rename (§16) frees the name: with our vermillion token now `--vermillion`, the bridge defines shadcn's `--accent: var(--surface-card)` + `--accent-foreground: var(--ink-primary)` normally, and hand-authored `src/components/ui/*` use stock shadcn `accent` classes — future shadcn CLI output drops in unpatched. (NIL-65 shipped with these two vars omitted and `bg-muted` hand-patched in; NIL-69 restores the stock wiring.) Never wire vermillion into a list-hover wash. The bridge lives in one `shadcn-bridge.css` file next to tokens.css; deleting it detaches shadcn cleanly.

---

## 5. Component inventory — the shadcn/bespoke boundary (ledger item L2)

Principle (from the settled stack decision): **themed shadcn for chrome; experiment surfaces bespoke on the same tokens.** The refinement this spec adds: a *reason per row*, and a hard rule — **no Radix/shadcn dependency inside any component that renders a frozen string, a reserved slot, or stimulus content.** The experiment surface must stay inspectable top to bottom.

| Component / surface | Boundary | shadcn parts | Why |
|---|---|---|---|
| `AuthForm` | **shadcn** | `Tabs`, `Input`, `Label`, `Button` | pure chrome; the biggest instant win for perceived polish |
| Site header | **shadcn-adjacent** | `Button` (ghost) for logout | keep the header bespoke-thin; no nav component needed at this scale |
| `ModeSelect` cards | **shadcn shell** | `Card` | settled in NIL-64 card; measure chip + status pill stay bespoke primitives inside |
| `Leaderboard` table + pager | **shadcn** | `Table`, `Pagination` (or `Button` pair) | chrome; long-username wrap rule carries over |
| Completion panel + tabs | **shadcn** | `Tabs`, `Button`, `Card` | chrome |
| Dialogs (future: logout-mid-session confirm, ladder floor intro) | **shadcn** | `Dialog` | the reason Radix is worth its peers |
| Toasts — chrome-level only (auth expiry, network loss) | **shadcn** | `sonner` | **must never replace in-trial status lines** — those are reserved slots (§2.4) |
| `Instructions` panel frame | **mixed** | `Button`, `Checkbox` (practice toggle) | panel copy/layout bespoke; controls themed |
| Script-lab condition options | **bespoke** | — | rich multi-line pressed-state cards; `aria-pressed` group; ToggleGroup is a poor fit and this selects the experimental condition — keep inspectable |
| `TrialPlayer` board | **bespoke** | — | reserved-slot system, frozen strings, timing (invariant 5) |
| `IdeophoneCard` / `StimulusDisplay` | **bespoke** | — | the instrument itself (invariants 1/4/7) |
| `StimulusPlayback` | **bespoke** | — | invisible audio engine; no visual surface |
| Replay button (NIL-63) | **bespoke** | — | lives inside IdeophoneCard; §8 |
| `FeedbackPanel` + research note | **bespoke** | — | verdict surface; reserved-height grid; microcopy territory §10.4 |
| Rating scale (1–7) | **bespoke** | — | instrument control; `aria-pressed` buttons stay (§9.2 documents why not RadioGroup) |
| `RatingLab` panels (instructions/done/empty) | **mixed** | `Button`, `Table` (lab record) | frozen copy renders verbatim inside bespoke frames; the record table is chrome |
| Progress track | **bespoke** | — | reserved geometry; `role="progressbar"` already correct |
| Landing page (NIL-43) | **bespoke sections** | `Button` CTAs | editorial layout; nothing shadcn does better here |
| Future: `ModalityLadder` floors, `ProductionLab`, `CrossLinguisticTrial`, `TemplateTrial` | **bespoke** | chrome bits may borrow `Button`/`Card` | all four are experiment surfaces per their specs |

Boundary test for any new surface: *does it render frozen text, stimulus content, or a reserved slot, or record a measure?* → bespoke. Otherwise chrome → shadcn first.

**Stable-hook contract (load-bearing for §11):** every component root and every selector the proof battery uses keeps its semantic class as the *first* class in `className`, with Tailwind utilities appended after. The semantic set (waypoint-verified): `.trial-stage`, `.trial-board`, `.stimulus-row`, `.ideophone-card`, `.choice-button`, `.question-slot`, `.question-text`, `.feedback-next-button`, `.fixation-cross`, `.feedback`, `.sound-check`, `.mode-card`, `.rating-scale-button`(+`.selected`), `.rating-next-button`, `.rating-reveal`, `.status-line`, `.stimulus-media`, `#leaderboard-panel`, `.leaderboard-pager`, plus the display classes (`.script-display-text`, `.romaji-display-text`, `.meaning-display-text`, `.feedback-choice-card`, `.slot-hidden`). shadcn components get semantic hooks too (`.auth-tabs`, `.mode-card` on the Card root). CVA/`cn()` composition per the component-design conventions; `cn("ideophone-card", …variants)`.

**Browser-loop compatibility contract (as built NIL-65, binding — §16 H7):** the proof loop drives real DOM, so chrome it must operate either stays native or ships its loop idiom in the same session. Landed idioms: the Instructions practice toggle stays a native `input[type=checkbox]` (Radix Checkbox renders no `input`, which the loop queries); AuthForm inputs stay descendants of native `<label>` (the loop associates by wrapping); mode cards stay `<button class="mode-card">`; the auth/completion Tabs ARE shadcn Radix, and the loop activates them via focus+Enter (`trustedPressEnterOnText`) because Radix ignores synthetic `element.click()` (no mousedown). Any new Radix control on a loop path must add its activation idiom to `verify-browser-loop.mjs` before the session exits.

---

## 6. Interaction states (one grammar)

| State | Buttons (primary/secondary) | Cards-as-buttons (mode, condition, choice) | Scale buttons | Inputs |
|---|---|---|---|---|
| Rest | fill `--vermillion` / raised+`--border-mid` | washi/raised + `--border-mid` (interactive affordance ≥3:1) | raised + `--border-mid` | raised + `--border-mid` |
| Hover | ramp hover fill / border→`--vermillion` | border→`--vermillion`, fill→`--surface-raised` (the "lift") | border→`--vermillion` | border→`--border-strong` |
| Active | ramp active fill | inset ring 2px `--vermillion` | — | — |
| Selected | — | `.active`: inset ring 2px `--vermillion` (condition) | `.selected`: fill `--vermillion`, text `--ink-inverse` — persists through disable (Gorilla-faithful) | — |
| Focus-visible | `--focus-ring` outline, offset 2 | same | same | same, offset 0 |
| Disabled | `opacity: .62`, `cursor: not-allowed` | coming-soon exception: full opacity, muted ink, pill (honest state) | `.62`, keep `.selected` fill | `.62` |

Rules: hover states never move layout (border-color/fill/shadow only — never border-width). Focus ring is always the vermillion outline (§4.5 verified ≥3:1 on every surface) — shadcn's `--ring` maps to it, so both layers focus identically. `cursor: pointer` on all interactive elements.

---

## 7. Motion language

**Policy first: motion is opt-in.** Every `transition`/`animation` lives inside `@media (prefers-reduced-motion: no-preference)` (the rating-scale block already models this) — in Tailwind terms, **`motion-safe:` on every motion utility**, enforced by grep in review (§11.3). Reduced-motion users get instant state changes, full function. Never animate layout properties (width/height/top/left) — transform/opacity/color only; the one exception stays the progress-track width fill, which is decorative feedback on a reserved geometry (180ms, retained).

The timing ladder (tokens, `@theme`-mapped):

| Token | Value | Easing | Used for |
|---|---|---|---|
| `--motion-micro` | `150ms` | `ease-out` | color/border/fill state changes (existing rating value) |
| `--motion-reveal` | `220ms` | `ease-out` | feedback panel entrance, reveal-slot content fade-in, toast in |
| `--motion-spin` | `400ms` | `ease-in-out` | replay one-revolution (§8); exits ~60% of enter per MD |

Choreography:

- **Feedback panel entrance:** opacity 0→1 + `translateY(8px)`→0 over `--motion-reveal`. It mounts *below* the reserved board (document flow after the trial board, gutter already stabilized), so the rise is comment-worthy but layout-safe. Reduced motion: appears instantly.
- **Reveal slots** (romaji/meaning lines, rating reveal content): content fades in `--motion-micro`; the slots themselves never move (they're pre-reserved).
- **Verdict color:** the feedback top rule and heading color arrive with the panel — no separate flash animation. Calm lab, not a quiz show.
- **Progress track:** existing 180ms width ease.
- **Mode cards / buttons:** `--motion-micro` on border/fill only. No scale-on-hover anywhere — paper doesn't stretch; the lift is fill+border.
- **Anti-pattern list:** no stagger-cascades on the mode grid, no kana letter-by-letter animations (stimulus dignity), no parallax, no shimmer skeletons (status lines + reserved slots already handle waiting honestly).

---

## 8. NIL-63 — per-card replay affordance (full design)

Retires Gorilla's four-card workaround (upper pair replayed, lower pair selected — an experiment-builder limitation, not thesis-canonical). Invariant 2 (playback separate from presentation) is what makes this legal.

**Anatomy.** One icon `<button>` per ideophone card, absolutely positioned inside the card's top-right padding (`inset-block-start/inline-end: var(--space-2)`), above card content (`z-index` over the media layer, below nothing). **36px visual circle, 44×44px hit area** via an inset `::before` extension (no-precision rule; the smaller glyph circle keeps headroom over the kana measure — see the wrap guard below); `--radius-pill`, `--surface-raised` fill, 2px `--ink-primary` border — the same DNA as `.media-play-button` and `.placeholder-mark`, so it reads as instrument hardware, not chrome. Icon: circular-arrow SVG (Lucide `rotate-cw` path inlined, stroke 2 — one icon family app-wide), `currentColor`.

**Kana-measure guard:** long stimuli (そろりそろり-class, 6 morae) can wrap to two kana lines inside the 320×180 card; the text face gets `max-width: calc(100% - 2 * (36px + var(--space-2)))` **only when the replay control is mounted**, keeping the wrapped line-box clear of both corner controls symmetrically. Reserved card dimensions unchanged (§2.4); the guard narrows the centered text measure, never the card.

**Behavior.** Click replays that card's word audio via the existing `StimulusPlayback` engine (a replay token bump, exactly like Rating Lab's `replayToken` — one shared per-word audio file, invariant 2). **Replay ≠ select:** the button is a *sibling* element overlaying the card, not a child of the choice `<button>` (nested interactive elements are an a11y fault and a mis-tap trap. IdeophoneCard renders the replay control in its wrapper, outside `mode="button"`'s clickable tree; `stopPropagation` as belt-and-braces). Available in every phase where cards are visible (choice + feedback; hidden during the sequential first plays so the phase timing stays clean — appears with the choice phase).

**Identity symmetry (leak prevention):** both cards get byte-identical affordances — same icon, same position, same enabled states, no per-card styling that could mark the target.

**A11y.** `aria-label="Replay card A"` / `"Replay card B"` (matches the "Choose card A" idiom; never includes the word — pre-feedback the accessibility tree must stay position-only, invariant 7). Keyboard: normal tab stop; tab order is per-card (choose A → replay A → choose B → replay B — grouping beats control-type ordering); Enter/Space activates; focus ring per §6. Announce nothing on completion (the audio is the feedback).

**Motion.** On activation the *icon* (not the button) rotates one revolution: `transform: rotate(360deg)` over `--motion-spin`, then resets. Gated `motion-safe`; reduced-motion users get a `--motion-micro` border→vermillion pulse instead (state change, no movement). Hover: border→`--vermillion` per §6; no idle spin.

**Rating Lab unification.** Rating Lab's replay keeps its frozen visible label (`RATING_REPLAY_BUTTON` = "Replay" — untouchable), restyled as the same circular-arrow icon + label in a pill secondary button: icon left, text right, same spin-on-activate, same tokens. Both affordances read as the same instrument control; the trial-side one is icon-only because no frozen string exists for it (chrome, new).

**Waypoint note:** browser-loop gains a replay waypoint (NIL-63 exit): click `.card-replay-button` on card A during choice → assert a second play of A's audio occurs and phase/selection state unchanged. Spin is decorative; the waypoint asserts audio + state, never animation.

---

## 9. Responsive and accessibility rules

### 9.1 Responsive

- Breakpoints stay 860 / 640 / 400 (established, matched to the board's reserved geometries). Tailwind: custom screens `bp-860` etc. — do **not** adopt Tailwind's defaults mid-migration; the reserved-slot constants are derived against these exact widths.
- **375px is a proof viewport** (alongside desktop) for every consuming build: no horizontal scroll, all targets ≥44px, kana unclipped at `--text-kana-card` floor (2.4rem ≤640 override stays), rating scale fits (7×44 + 6×2px gaps = 320px ≤ 375 − page padding ✓ shipped).
- Six-mode grid: auto-fill collapse per §2.3; header wrap rule (title steps to `--text-md` ≤640) stays.

### 9.2 Accessibility commitments (spec-level)

- Touch targets ≥44×44 everywhere (already policy; replay button included by construction).
- The 1–7 scale stays **native buttons + `aria-pressed`** (WCAG-conformant toggle group). Radix `RadioGroup` (roving tabindex) was considered and rejected: it would put a dependency inside an instrument control, change the DOM the proof battery pins, and alter tab semantics mid-instrument. Post-NIL-65 enhancement (optional, not in scope): add `role="group"` labelled by the frozen question (already shipped) + documented Left/Right key support. Recorded so it isn't re-litigated per build.
- Modality color never sole carrier (§4.3) — every colored surface has a text/label sibling.
- `aria-live` discipline stays as shipped: trial stage `polite`, status lines swap in place, toasts `polite` and never steal focus.
- Kana `lang` attribute: stimulus display elements carry `lang="ja"` so screen readers pick Japanese voices — **rendering attribute, not text transformation; zero string manipulation.** (New, cheap, belongs to NIL-65.)
- Focus is never removed, only styled (§6); focus-visible everywhere including shadcn layer via `--ring`.

---

## 10. Art direction (absorbed F3 scope)

Scope fence, restated because it is the whole game: **chrome and landing only.** `experimentText.ts`, the canonical question wording, and every participant-facing trial string are outside this section's jurisdiction. Script Lab framing rule binds all copy: "presentation changes the experience," never "matched script helps."

### 10.1 Creative voice

**The voice is a good lab notebook: precise, warm, quietly delighted by its own subject.** The app never pretends the player is a "subject," and never pretends the science is a game. It says true things in a way that makes you want to test them.

Register rules:

- **Wit lives in observation, not exclamation.** The material is inherently funny-adjacent (words that sound like what they mean); pointing accurately *is* the joke. No "Nice!!", no "Oops!", no emoji, no anime-flavored romaji slang.
- **Second person, present tense, active.** "Your ear already knows more than you think" — not "users will discover…".
- **Numbers are copy.** Real thesis stats beat adjectives everywhere ("64% — try beating the thesis cohort" over "surprisingly guessable!").
- **Incorrect is interesting, never shameful.** The wrongness of a guess is data; copy treats it with curiosity ("This pair fooled most of the thesis cohort too").
- **English carries meaning; kana are stimuli.** Chrome never uses Japanese for decoration-by-vocabulary ("ganbatte!" is banned). Whether *static authored kana* may appear in chrome at all (logo mark, hero motif) is ledger item **L4** — if yes, the fence is precise: authored literals only, never derived, and **the sanctioned pool is the practice set (p0–p3)** — practice words are revealed to every player by design and are excluded from analysis. Core-pool stimulus words may never appear in chrome next to their meanings (the critique pass caught exactly this: an early hero draft used *kirakira* + gloss, which would pre-answer pair v2 — the class of leak the fence exists for). If L4 = ban, the wave motif (§10.6) carries the identity alone.
- Latin script for romaji in chrome; italics not required; never fake-Japanese typography (no faux-brush fonts).

### 10.2 Mode naming (six modes — ledger item L3, Nils picks per row)

**ADOPTED SLATE (Nils, 2026-07-03)** — Nils renamed beyond the proposal set; the measure chips are his adjusted wording. Player-facing only: mode IDs, API values (`CHOOSING`, `LADDER`, …), and the *measure* terms in docs/specs ("the Choosing Task" as thesis terminology) are untouched.

| Mode (internal) | Player-facing name (adopted) | Measure chip (adopted) |
|---|---|---|
| `choosing` (incl. Script Lab) | **Meaning Match** | `MEASURE · GUESSING` |
| `rating` | **Rating Lab** | `MEASURE · REFLECTION` |
| `ladder` | **Perception Ladder** (floors: Sound → Sight → Inner States → Touch) | `JOURNEY · PERCEPTION` |
| `production` (free-form) | **Word Mint** | `MEASURE · PRODUCTION` |
| `template` (phoneme-shape) | **Word Anatomy** | `STRUCTURE · PATTERNS` |
| `xl` (cross-linguistic) | **Lingua Quest / Polyglot Challenge** — ⚠ *unresolved pair; final pick deferred to XL build kickoff* | `TRANSFER · 5 LANGUAGES` |

**Rename sweep (chrome literals only — rides NIL-65):** `modes.ts` titles/descriptions; `Instructions.tsx` h1 "Choosing Task Instructions" → "Meaning Match Instructions"; `RatingLab.tsx` empty/done-state literals "…play the Choosing Task…" → "…play Meaning Match…". Verified: the term does not occur in `experimentText.ts`; the sweep touches zero frozen strings. Thesis-register docs keep "Choosing Task" as the measure's name — player name ≠ measure name, and the chip bridges them.

### 10.3 Hidden academic gems (placement map)

The principle: reward attention, never gate on it. Each gem is true, sourced, and one sentence.

1. **Feedback research notes** (exists — `researchFlavor.ts` territory): grows per-item thesis stats post-NIL-54 ("In the thesis study this pair was the easiest of all 30 — 94% got it."). Data-driven lines only; vetted numbers from `thesis-facts.md` §4/§5.
2. **Completion panel benchmark:** your session vs the thesis cohort ("Thesis cohort mean: 64%. You: 73%.") + one dissociation teaser when Rating Lab has data for the player.
3. **Mode-card measure chips** (§10.2) — the taxonomy of measures, hiding in plain sight.
4. **Landing footnotes:** real citation line at the strip bottom (Dingemanse 2012; McLean 2021; McLean, Dunn & Dingemanse 2023; Punselie, McLean & Dingemanse 2024 when Babel Run ships — plus the thesis itself).
5. **Empty states as micro-lessons** (§10.4).
6. **Styleguide page** stays a dev-only gem (`/styleguide.html` documents the tokens — it already smoke-tests them).

### 10.4 Feedback-card and chrome microcopy territory

Territory definition: `FeedbackPanel` headings and card titles, research-note lines, status/empty/error states, completion copy, buttons. (The trial strings *between* them are frozen; the panel is chrome.)

| Surface | Register | Examples (voice reference, not frozen) |
|---|---|---|
| Verdict heading | one word, lab-calm | "Correct" / "Incorrect" (keep — the color + rule carry the emotion) |
| Card titles | descriptive | "You chose" / "Correct word" (keep) |
| Research note (correct) | observation | "Auditory pairs like this one sat at the top of the thesis accuracy table." |
| Research note (incorrect) | curiosity, company | "shobon fooled 64% of the thesis cohort — the hardest word in the study." |
| Empty leaderboard | micro-lesson | "No scores yet. Somebody has to be the baseline." |
| Rating Lab empty | direction | (keep shipped copy — it already does this well) |
| Auth error | cause + fix | "That username and password don't match. Check both, or register a new account." |
| Network/status | plain | "Loading next round…" (keep); toasts follow the same voice |
| Buttons | verb + object | "Start rating", "Next round", "Back to modes" (all keep) — never "OK"/"Submit" |

Banned moves, all surfaces: exclamation-mark cheer, "Oops", performative apology ("So sorry!"), fake urgency, praising script matching (framing rule), calling the player a participant/subject, streak-shaming.

### 10.5 Landing narrative arc (NIL-43 consumer)

Spine: **"Explore how far iconicity carries you before convention takes over."** Every stat traces to `thesis-facts.md` (§ refs inline). The corrected dissociation line is **binding**: "ratings detect ideophone-ness; guessing doesn't" — "nearly orthogonal" is retired (scale data: ρ +.44/+.65).

1. **Hero.** H1: "You've never studied Japanese. You'll still get most of these right." Sub: "Ideophone Arena is a live replication of a real experiment on sound symbolism — words whose shape carries their meaning. Guess, rate, and see how your ear compares." CTA: **"Prove it — play a round"** (primary), "Read the research" (ghost, anchors to strip 4). Motif per §10.6.
2. **The numbers strip** (three washi stat cards, specimen-labeled): `64%` "mean guessing accuracy for people with zero Japanese — chance is 50%" (§3/§9) · `Sound → Sight → Inner states` "accuracy slides 6.86 → 6.42 → 5.97 out of 10 as meanings turn inward" (§3) · `36%` "shobon (downhearted): the one word the cohort did worse than a coin flip on" (§4).
3. **How it works** (three steps, 2AFC in plain words + the honesty line): "Your guesses join the arena record — the same 30 pairs, still collecting data."
4. **The dissociation** (the thesis's most game-worthy finding, §3): heading "Your gut and your reflection disagree." Copy: "dokidoki — a racing heartbeat — was rated the most word-like word in the study. People still couldn't reliably guess it. **Ratings detect ideophone-ness; guessing doesn't.**" Footnote: ρ ≈ +.44/+.65 across scales — related, far from interchangeable. CTA into Rating Lab.
5. **Script Lab teaser**, framing rule verbatim territory: "Seeing the script didn't change how well people guessed — it changed how the words *felt* (audio-only raters: 4.50; script raters: 4.15). Presentation changes the experience." (§3)
6. **Modes grid** (§2.3 shell reused) + honest coming-soon.
7. **Provenance footer:** thesis line + citations (§10.3.4) + "built on the author's MA thesis data; every number on this page is from the study."

### 10.6 The identity motif (ledger item L5)

Proposal: the **wave rule** — a short seismograph-like zigzag rule (SVG, `--vermillion`, 2px stroke) replacing the flat underline under landing/section headings and as the hero's right-column motif (large, in `--modality-auditory`→`--modality-interoceptive` gradient stops per §4 trio). Rationale: a line that depicts sound *is* iconicity — the thesis subject drawn as chrome. Cheap, ownable, works at 375px, no kana needed (survives any L4 ruling). If L4 allows chrome kana: the hero motif is a large static **practice-set** specimen (mockup uses ガタン *gatan* "with a bang" — p0's katakana word, revealed in practice by design) with romaji + gloss caption; core-pool words banned per §10.1.

---

## 11. NIL-65 migration plan and test fallout (plan it, don't discover it)

> **Status: LANDED 2026-07-04** — full battery green at 1280px and 375px; see the web repo's `docs/progress-log.md` entry "2026-07-04 (NIL-65)" for the build record and the adversarial-review fixes (button reset, token-purity regex, tailwind-merge font-size registration). This section is kept as the plan-of-record; the as-built deltas and their 2026-07-04 harmonization adjudication are in §16 (NIL-69 executes the harmonized target). Note: the three unimported `@fontsource` packages (kaisei-decol, klee-one, rocknroll-one) were **not** removed — the removal stays pre-approved and may ride NIL-69.

### 11.1 Build sequence (one session)

1. Deps (pre-approved gate only): `tailwindcss@4`, `@tailwindcss/vite`, shadcn CLI + the §5 component set's Radix peers, `class-variance-authority`/`clsx`/`tailwind-merge` (shadcn's standard kit). **Nothing else** — new icons via inline Lucide SVG paths already vendored by shadcn components, no separate icon dep without a fresh gate. Remove-only cleanup allowed: `@fontsource/kaisei-decol`, `@fontsource/klee-one`, `@fontsource/rocknroll-one` are installed but unimported (font-adjudication leftovers) — dropping them is a dep *removal*, flag in handoff.
2. tokens.css: hex→`oklch()` rewrite (§4.1 table, hex provenance comments), add §3.2/§3.3 new tokens + `--modality-haptic` family (ledger L1 pick) + `--motion-*` ladder + `--container-*`; append the `@theme inline` block (or sibling `theme.css`) + `shadcn-bridge.css`.
3. Restyle chrome to shadcn per §5 boundary; restyle bespoke surfaces to Tailwind utilities on the same semantic hooks; apply §1 surface-grammar deltas and §2.2 rhythm.
4. Test-battery repairs (§11.2) in the same commits as the components they pin.
5. Proof battery (§11.4).

Behavior changes: **none** (presentation-layer only), except the two riders NIL-65's card carries: the lab-record romaji/gloss fallback defect (visible when the divergence fetch fails for words rated in earlier visits) and the `lang="ja"` attribute (§9.2).

### 11.2 Known test fallout (exact, verified against current tree)

| Assertion | Location | Why it breaks | Tailwind-aware replacement |
|---|---|---|---|
| `/<article class="feedback-choice-card slot-hidden" aria-hidden="true">/` regex | `FeedbackPanel.test.tsx:70` | exact `class="…"` match dies the moment utilities append | render + query `.feedback-choice-card.slot-hidden[aria-hidden="true"]` via DOM (`classList.contains`), not markup regex |
| `toContain('<span class="script-display-text">カタカタ</span>')` (+5 siblings: lines 48, 53, 71–73, 78–79) | `StimulusDisplay.test.tsx` | same | query `.script-display-text` and assert `textContent` — keeps the verbatim-kana assertion, drops markup coupling |
| `markup.indexOf('class="question-slot"')` ordering probe | `TrialPlayer.test.tsx:172` | same | locate via DOM query, assert document order via `compareDocumentPosition` |
| `countOccurrences(ratingMarkup, 'class="rating-scale-button"') === 7` | `scripts/verify-presentation-logic.mjs:406` | exact-string count breaks with appended utilities | count `class="rating-scale-button` *prefix* occurrences, or regex `/class="rating-scale-button[ "]/g` — stable under the §5 first-position contract |
| `ratingMarkup.includes("rating-reveal slot-hidden")` + `"status-line"` | `verify-presentation-logic.mjs:417–424` | adjacency substring survives **only** under the first-position contract | keep, plus comment pinning the contract; or upgrade to the same prefix-regex idiom |
| Frozen-string presence/uniqueness checks (same script) | various | **unaffected** — they assert text, not classes | no change |
| Browser-loop waypoints (`.mode-card`, `.rating-scale-button(.selected)`, `.rating-next-button`, `.trial-stage/.trial-board/.question-slot/.feedback-next-button/.stimulus-row .ideophone-card/.feedback/.question-text/.fixation-cross/.sound-check/#leaderboard-panel/.leaderboard-pager/.stimulus-media`, `button[aria-pressed='true']`) | `scripts/verify-browser-loop.mjs` | **unaffected by design** — the §5 stable-hook contract exists precisely so these hold | no change; contract test guards them |
| Kana-derivation grep (`scanForForbiddenKanaLogic`) | `verify-presentation-logic.mjs:430` | unaffected (scans TS identifiers) | no change |

Note on the remembered "token-grep check": no literal token-grep exists in the frontend verify scripts today (verified this session) — the fallout is the exact-class assertions above plus `.design-sync/conventions.md`'s token/class enumeration, which needs re-validation after the migration (§12).

### 11.3 New guards the migration must add

1. **Semantic-hook contract test** (vitest): render each proof-battery component; assert each §5 hook exists and is the first class token on its element. This is the migration's replacement for exact-markup pinning — it guards the *waypoints'* substrate, not the styling. **Landed as `src/semanticHooks.test.tsx`.**
2. **Token-purity guard** (script or vitest, the successor to the old intent): grep built CSS/site source for raw hex outside `tokens.css` (allowing the provenance comments) and for `transition|animate` outside `motion-safe`/`no-preference` gates. Keeps "tokens are the only place values live" true under Tailwind. **Landed as `scripts/verify-token-purity.mjs`** (post-review fix: the hex-strip regex was consuming 4/6/8-digit hex and silently skipping `#rrggbb`; now self-tested).
3. **Reserved-slot geometry note:** the browser-loop layout-stability waypoint (`.trial-board` phase geometry) already asserts this at runtime — §2.4 constants must reproduce byte-identically.

### 11.4 Exit proof battery (unchanged bar, plus)

**Canonical commands (pnpm, 2026-07-04):** `pnpm lint` · `pnpm build` · `pnpm vitest run` green (post-repair; incl. the two §11.3 guards) · `node scripts/verify-presentation-logic.mjs` green · `node scripts/verify-token-purity.mjs` green · `node scripts/verify-browser-loop.mjs` full pass at desktop + 375px · §4.5 contrast spot-checks on any surface the migration re-colored · screenshots of trial board at both viewports vs pre-migration (reserved-slot geometry identical) · tree clean for Nils's review, no commits. This battery is the standard exit bar for every consuming build (§15).

---

## 12. Design-sync and styleguide notes

- `.design-sync/` untouched by NIL-65 (its `previews/*.tsx` are component harnesses against the *shipped* bundle; pre-build HTML mockups don't belong there — they live in the planning folder, §13 note). Post-NIL-65 re-sync duties, from `.design-sync/NOTES.md` own risk list: re-run `build-css.mjs` (bundle gains Tailwind layer), re-validate `conventions.md` token/class enumeration, re-grade previews, and fix the stale `componentSrcMap` rows (`HomePage`/`ResultsPage`/`NotFoundPage` point at `src/pages/*` which no longer exists post-27C). **These chores ride NIL-63 as riders (decided 2026-07-04)** — see `session-NIL-63-prompt.md`.
- `/styleguide.html` gains: §3.4 specimen-label variants, §4.4 haptic swatch row, §7 motion ladder demo, the shadcn-themed control row — it remains the token smoke test.

---

## 13. Adjudication ledger (Nils's calls — resolved in chat before adoption)

| # | Decision | Resolution (Nils, in chat, 2026-07-03) |
|---|---|---|
| **L1** | Haptic color | **KOKE moss** (`#5d6a2a` family, §4.4) — rule-faithful derivation preferred over the recommended lightness-broken TSUCHI; deutan trade-off accepted with mandatory label-pairing |
| **L2** | shadcn boundary | **§5 adopted as written** (condition options bespoke; rating scale native buttons; stable-hook contract) |
| **L3** | Mode names | **Full slate renamed** — §10.2 adopted table; Meaning Match / Rating Lab / Perception Ladder / Word Mint / Word Anatomy; XL name still an open pair (Lingua Quest / Polyglot Challenge), final pick at XL kickoff |
| **L4** | Static authored kana in chrome | **Allowed, practice-set (p0–p3) pool only**, romaji+gloss captioned; core-pool words banned next to meanings |
| **L5** | Wave-rule identity motif | **Adopted** (no objection raised) |

## 14. Critique + accessibility pass log (2026-07-03, this session)

Run against the three mockups (`docs/design/NIL-64-mockups/`) and the spec itself; fixes folded before adoption.

| Finding | Severity | Resolution |
|---|---|---|
| Hero kana specimen used *kirakira* + gloss — a live core-pool target (pair v2): answer leak | 🔴 | swapped to practice word ガタン (p0); L4 fence sharpened to name the practice-set pool (§10.1) |
| Landing stat cards cast `--shadow-card` — chrome borrowing the instrument's voice | 🟡 | shadow removed; §1 refined: grammar's teeth are the shadow, raised *fill* allowed on washi strips |
| Replay button at 44px visual could crowd two-line kana wraps (6-mora words) | 🟡 | 36px visual / 44px hit via `::before`; kana-measure guard added (§8) |
| Hero H1 at `--text-2xl` undersized for a landing | 🟢 | `--text-hero` token added (§3.3) |
| Focus-visible styles missing on mockup CTAs; heading/`main` semantics loose in trial mockup | 🟢 | fixed in mockups (app already has global focus rules) |
| Accent-as-text on page ground (3.90:1 < AA) risk via tinted specimen labels | 🟡 | rule restated in §4.5; tinted labels restricted to card/raised |
| WCAG sweep (1.4.3/1.4.11/2.1.1/2.4.7/2.5.5/4.1.2) on §4–§9 values | — | all pass per §4.5 computed table; rating-scale keyboard rationale documented §9.2; replay a11y §8 |
| Modality color legibility under CVD | — | §4.3/§4.4: existing aud/accent protan collision disclosed; label-pairing rule made explicit; haptic pick optimized for it |

## 15. What each consuming session takes from here

| Session | Consumes | Must not touch |
|---|---|---|
| **NIL-65** — ✅ LANDED 2026-07-04 | §1–§7 wholesale; §4 token values; §5 boundary + hook contract; §11 plan/fallout/guards; ledger picks | frozen strings; reserved-slot constants (§2.4); shuffle/API anything |
| **NIL-69** (next; harmonization) | §16 H1–H8 wholesale; §4.6 harmonized wiring; H2 rename map | frozen strings; §2.4 constants; token *values*; visual output (pixel parity) |
| **NIL-63** (after NIL-69) | §8 verbatim; §7 spin token; §16 harmonized canon; **riders: §12 design-sync chores** | phase timing; frozen strings; §16 wiring (extend, don't rework) |
| **28B ladder UI** | §3.4 floor labels; §4.4 **KOKE** haptic tokens (labels mandatory on haptic-colored UI); §2.3 shell (floors-from-data, no hardcoded 3); "Perception Ladder" name + `JOURNEY · PERCEPTION` chip; thesis-facts §8 tiers | pairings/backend |
| **NIL-62 Word Mint UI** | §5 boundary row; feature chips = specimen labels in `--positive`/`--ink-muted` (per its spec §7 — non-match ≠ error); §3 scales | PhonologyService spec |
| **NIL-43 landing** | §2.5 layout; §10.5 arc + stats + corrected line; §10.6 motif; §10.3 footnotes | stat invention — thesis-facts.md only |
| **XL / Template builds** | reserved tokens (§3.3), chip idioms (§3.4) | their own gates |

---

## 16. Wiring canon — harmonization target (adjudicated 2026-07-04 second pass; NIL-69 executes) — NORMATIVE

NIL-65 landed with as-built deviations (recorded verbatim in the web repo's `docs/progress-log.md` entry "2026-07-04 (NIL-65)"). Same-day second-pass adjudication (Nils): **vanilla Tailwind/shadcn alignment is preferred over canonized workarounds wherever a vanilla path exists** — the app's utility surface should read like any stock Tailwind v4 + shadcn project, with all project identity living in `tokens.css` values and `@layer app`. This table is the target; NIL-69 executes it. Until NIL-69 lands, the code matches the as-built column.

| # | Item | NIL-65 as-built | Harmonized target (NIL-69) |
|---|---|---|---|
| **H1** | Preflight | Omitted; global `button` appearance reset in `@layer app` | **Enabled**: `@layer theme, base, components, app, utilities;` then `@import "tailwindcss";` — vanilla import, layer order pre-declared so legacy `@layer app` sits under `utilities`. Button reset deleted (preflight owns it). Compensating base rules added in `@layer app` **only** where the proof battery shows drift. **Hard revert criterion:** any invariant-5 geometry-waypoint or trial-board screenshot regression that can't be fixed with a targeted compensating rule ⇒ revert to no-preflight as-built wiring and report |
| **H2** | Token names | tokens.css owns `--accent` (vermillion) + `--accent-<modality>` families | **Rename map (root custom properties; old names use the accent prefix):** the vermillion trio `accent`/`accent-hover`/`accent-active` → `--vermillion`/`--vermillion-hover`/`--vermillion-active` · the modality families `accent-auditory{,-soft}`, `accent-visual{,-soft}`, `accent-interoceptive{,-soft}`, `accent-haptic{,-soft,-hover,-active}` → `--modality-auditory{,-soft}`, `--modality-visual{,-soft}`, `--modality-interoceptive{,-soft}`, `--modality-haptic{,-soft,-hover,-active}`. Values, provenance hexes, and derivations unchanged. Utility names for the modality trio stay short (`text-auditory` etc. — `--color-auditory: var(--modality-auditory)`); the vermillion trio's utilities become `*-vermillion{,-hover,-active}`. Frees shadcn's `--accent` (H3) |
| **H3** | shadcn bridge `--accent` | Omitted from bridge; ui components hand-patched to `bg-muted` hover wash | **Restored per the original §4.6 table:** `--accent: var(--surface-card)`, `--accent-foreground: var(--ink-primary)`, mapped in `@theme`. Hand-authored ui components revert to stock shadcn `accent` classes — future shadcn CLI output drops in unpatched |
| **H4** | Utility surface / type roles | Custom font-size utilities (`text-ui`, `text-md`, …) + `extendTailwindMerge` registration in `lib/utils.ts` | **Standard names only in the utility surface.** `theme.css` font-size map restricted to Tailwind's standard names (`xs/sm/base/lg/xl/2xl`, values still ours by reference). Non-standard §3 roles (`ui`, `md`, `prompt`, `question`, `kana-*`, `hero`, `xl-form`) remain tokens in tokens.css consumed via `var()` in `@layer app` semantic classes, or as arbitrary-value utilities (`text-[length:var(--text-ui)]` — natively understood by tailwind-merge) — **never as named utilities**. `extendTailwindMerge` deleted; `cn()` uses stock `twMerge`. Pixel parity required (no size substitutions) |
| **H5** | Spacing scale | NOT remapped (Tailwind defaults intact; §2.2 rhythm via `var(--space-*)` in app.css) | **Unchanged — already the vanilla-aligned choice** |
| **H6** | `--radius-xl` alias → `--radius-lg` | Landed | **Keep** — it makes stock shadcn `rounded-xl` resolve to the panel radius; standard `@theme` config, zero cost |
| **H7** | Browser-loop idioms (§5 contract): native practice checkbox, native `<label>` wrapping, focus+Enter for Radix Tabs | Landed | **Keep** — harness-vs-Radix, orthogonal to Tailwind/shadcn naming. New Radix on a loop path still ships its idiom same session |
| **H8** | Package manager | pnpm lockfile added; `package-lock.json` still present; no pin | **Complete the migration:** delete `package-lock.json`; add `"packageManager": "pnpm@<installed version>"` to package.json; commands `pnpm lint` / `pnpm build` / `pnpm vitest run` everywhere |

**Going-forward rule (the point of the pass):** the utility surface uses only vanilla Tailwind/shadcn names — anything project-specific lives in tokens.css values and `@layer app`. No custom classifier config, no bridge omissions, no preflight opt-outs.

Doc note: token names throughout this spec were updated to the H2 map on 2026-07-04; the §4.4 candidate table, §13 ledger, and §14 log are decision/measurement records whose original `--accent-*` names are preserved in git history and the progress log. For the record (not deviations): the three unimported `@fontsource` packages (kaisei-decol, klee-one, rocknroll-one) remain installed; removal stays pre-approved and may ride NIL-69. `Toaster` and `Dialog` are themed and staged but untriggered.



