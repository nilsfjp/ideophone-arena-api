# Game-mode roadmap

## Status - 2026-07-02 (F2 / NIL-56): four greenfield spec sheets exist

Engineering-grade specs for the four greenfield mode candidates live in the Cowork project folder, `docs/specs/` (Ideophone Arena project, outside this repo):

- `SPEC-cross-linguistic.md` - `CROSS_LINGUISTIC`, 239 items / 5 languages from `triangulating_iconicity`, published-guessability anchors. Architecture driver for NIL-57; build after NIL-57 + licensing gate.
- `SPEC-free-form-entry.md` - `PRODUCTION`, the third measure (production). Standalone `ratings`-pattern vertical; no NIL-57 dependency, no external gates. **← Build pick (F2, criteria: no new recordings · ≤1 backend + 1 frontend session · vision test). Build slot Jul 6–8 (F5).**
- `SPEC-phoneme-shape.md` - `TEMPLATE_READING`, template-reading (redup / -Q / -ri; -N deferred; voicing excluded per the F1 null). Second in line, after NIL-57 D1 + pair sign-off round.
- `SPEC-four-floor-ladder.md` - Touch floor for the Modality Ladder from the six approved haptic pairs. Gated on the audio-recording decision (10 new recordings) → fails build-pick criterion 1; W31+ contingent on the gate.

Every spec carries a "Deferred to NIL-57" ledger; the shared decisions (mode dispatch D1, unified trials D2, lexeme identity D3, shared item-stats D4, seed-stream registry, `PhonologyService` as shared engine) resolve in the NIL-57 architecture session before any gated build.

Still deferred as modes (unchanged): pseudo-word Foil Arena (gated on recorded audio; TTS ruled out 2026-07-02 - foil *method* lands invariant-safely as the NIL-58 difficulty engine instead), sentence-context mode, campaign / deck-builder / codex structures, Corpus Route.

### Learnability / iconic-bootstrapping mode - designed, deferred (assessed 2026-07-02, OSF-mining session)

Paradigm: Lockwood, Dingemanse & Hagoort 2016 (*Collabra*; data mined from the OSF archive, CC BY 4.0). Teach word–meaning pairs, test retention; iconicity's fingerprint is the real-vs-opposite gap - verified from raw data at **86.7% vs 71.3%, Cohen's dz = 1.07, n = 29** (large and real). A "Memory Lab" mode would be the fourth measure: recognition (Choosing) / reflection (Rating) / generation (Production) / **retention**.

Why deferred, explicitly **not** a build-one candidate (the case is not overwhelming):

1. The measure's power comes from the **opposite arm** - deliberately teaching players false meanings. That is a mode-killer for a research-honest public app (and un-teaching would need its own design).
2. A real-only variant loses the contrast and hits ceiling (.867 mean) - low discriminative resolution, and it measures memory + iconicity confounded.
3. Per-item learnability is empirically thin even in the source data (one condition arm per item, n = 19/arm; learn-acc ~ guessability rho = +.30/−.25, both n.s.) - no per-item difficulty scaffold to build on.
4. Not salvageable as a free-form-entry mechanic: produce-then-recall measures the generation effect, not iconicity.

Salvage that ships anyway: the clash-is-costly finding (with its P3/LPC ERP signature) is vetted research-flavor copy ("your brain fights a word whose sound points the wrong way" - F3 register, citation ready), and the 37-row opposite-gloss sheet (`osf-lockwoodstimuli/`) is a reference for antonymic contrast quality in pairing work. Revisit only if a delayed-retention hook ever matters (e.g. spaced-repetition retention curves as a long-horizon portfolio stat); it would still need an honest design that never teaches falsehoods.

---

## Historical design notes (pre-F2 chat digest, kept for provenance)

Yes. The raw `.png`, `.mp3`, base64 strings, TTS workflow, and spreadsheets are valuable, but they should be treated as an **asset pipeline**, not as the game runtime model.

The design mistake to avoid is letting generated `.mp4` files define the game logic. In the experiment, the videos were a practical Gorilla artifact: Google Cloud TTS audio was combined with images for katakana, hiragana, or triangle placeholders, then uploaded as `.mp4` stimuli. For the web app, the correct model is: audio is one asset, script display is React-rendered state, placeholder display is React-rendered state, and `.mp4` is only a legacy derived asset.

The conceptual spine should be this:

Ideophone Arena is not “guess the Japanese word.” It is “explore how far iconicity carries you before convention takes over.” That gives you a clean structure for many game modes. The thesis already gives the ladder: sound, movement, visual patterns, other sensory perceptions, inner feelings/cognitive states. The uploaded Chapter 2 explicitly frames this as a progression from easier unimodal sound-to-sound mappings toward more abstract cross-modal mappings.

For actual game modes, I would group ideas by implementation cost.

First tier: modes you can add soon.

**Script Lab**. The same choosing task, but the player chooses the presentation condition: audio-only, congruent script, incongruent script. This is closest to the current backend. It should be framed as “presentation changes the experience,” not “matched script helps,” because your results found little group-level effect of orthographic condition on guessing accuracy.

**Modality Ladder**. A short run ordered by modality: auditory, visual, interoceptive. The game text can say “climb from direct sound-to-sound mappings toward more cross-modal and internal meanings.” This uses the thesis result well: participants guessed auditory items most accurately, visual items next, and interoceptive items least accurately.

**Rating Lab**. After a normal guessing session, show a separate reflective task: “How much does this word sound like what it means?” on a 1 to 7 scale. This mirrors the thesis structure, where the Choosing Task measured accuracy and the Rating Task measured subjective iconicity. The thesis explicitly treats them as complementary measures, not interchangeable ones.

Second tier: modes that need more data modeling.

**Foil Arena**. This is where the McLean-style idea becomes useful: present a real ideophone against either a real foil, an artificial foil, or a cross-modal distractor. The game question changes from “Which one means X?” to “Which one feels more like a word for X?” This would let you compare accuracy, confidence, and rating. It is probably the most research-interesting mode after Rating Lab, but it requires a clear `foil_type` model.

**Opposition Duel**. This is your “defeat the opposing ideophone by choosing the most opposite one” idea. It fits your original pair design, since the thesis stimuli were built as contrastive pairs within the same modality. A player sees “kirakira” as the opponent and chooses “donyori” as a semantic counter, or sees a heavy/noisy form and chooses a soft/quiet counter. This is game-like without becoming arbitrary.

**Pattern Hunter**. The player is not asked for a translation, but for the sound-symbolic principle: voiced vs. voiceless, reduplication, sokuon, moraic nasal, high/low vowels, sharp/soft feel. This would turn your research notes into mechanics. It is good for learning, but you need reliable metadata per ideophone.

Third tier: campaign and card-game structures.

**Implicational Climb Campaign**. This is the cleanest campaign metaphor. Each “floor” corresponds to a step on the hierarchy: Sound, Motion, Visual, Haptic/Gustatory, Inner State. Early floors are high-transparency. Later floors become more dependent on convention and learned patterns. This can look like Slay the Spire without copying its mechanics: map nodes are trials, rating nodes, debrief nodes, boss pairs, and corpus-lore nodes.

**Deck Builder**. Each ideophone is a card with attributes: modality, script tendency, phonological features, iconicity rating, transparency, intensity, and maybe “resonance.” Battles can stay semiotic rather than fake elemental combat. Example: an opponent card has meaning “dark, gloomy.” The player wins by selecting a card with a stronger iconic match, an opposite meaning, or a matching modality constraint. This is fun, but it is a much bigger design project.

**Pokemon-style Codex**. Instead of catching monsters, the player “attunes” ideophones. Guess correctly to reveal translation. Rate it to reveal iconicity. Use it in a sentence to unlock contextual usage. Compare scripts to unlock orthographic notes. This is probably better than combat if you want a portfolio project that still feels research-grounded.

Fourth tier: more linguistically serious expansion.

**Context Mode**. Present an ideophone embedded in a real or curated sentence and ask the player to infer its contribution. This is closer to real ideophone research, but harder because you need sentence sources, translations, licensing/citation decisions, and probably Japanese-reading support. It should come later.

**Corpus Route**. Use the top-600 spreadsheet as a content expansion source. The player unlocks “common ideophones,” “katakana-dominant ideophones,” “hiragana-dominant ideophones,” “high perceptual strength,” and so on. Your spreadsheet already has frequency, script frequency, modality, and perceptual-strength columns, so it can become a mode-generation source rather than a manual content dump.

The practical architecture I would aim for is:

`Ideophone` remains the lexical item.
`StimulusAsset` stores audio/image/video/source paths and whether an asset is canonical or generated.
`RoundTemplate` stores pairings, correct answer, foil type, modality, route, difficulty, and source.
`PresentationMode` controls audio-only, congruent script, incongruent script, context sentence, or rating view.
`GameMode` controls the rules: choosing, rating, ladder, duel, campaign.
`Attempt` records choices.
`RatingAttempt` records Likert ratings separately.

That separation keeps the project from becoming a pile of special cases.

The most sensible roadmap is:

Now: make the current game loop feel good. Feedback should be readable, progress should be visible, leaderboard/recent attempts should not pollute active play.

Next: separate stimulus playback from stimulus display. Use `.mp3` or hidden media for sound, React for kana/placeholder/script presentation. Do not rely on video visuals.

Then: add Script Lab. It is the smallest real research-mode expansion.

Then: add Modality Ladder. It gives the game a thesis-shaped progression.

Then: add Rating Lab. This creates the “two measures” design inside the app.

Later: add generated assets, artificial foils, context sentences, campaign/deck mechanics.

The one point I would push back on: do not jump straight to Slay-the-Spire/Pokemon mechanics. That can be the long-term skin, but first you need a clean research-game engine. Once `GameMode`, `PresentationMode`, `RoundTemplate`, and `StimulusAsset` are cleanly separated, the card/campaign ideas become mostly frontend design rather than backend chaos.
