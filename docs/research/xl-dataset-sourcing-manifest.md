# Cross-linguistic dataset sourcing manifest — TTS-gated

_NIL-87, Opus, 2026-07-08 (updated same day for Nils's dataset adjudications). Vetted candidate ideophone/mimetic datasets for growing the stimulus inventory **beyond Japanese**, for the Polyglot Challenge / M6 cross-linguistic work. Companion to `docs/specs/SPEC-tts-synthesis.md` (the audio-supply recipe). Every external URL was resolved with web tools this session; the two adopted sources are now **cloned locally** by Nils (paths below)._

**Adjudication of record (Nils, 2026-07-08):** LEX-ICON is cloned into `research/gcloud-tts-sources/` and Chevillard's Tamil analysis is downloaded there too; **proceed with both now, verify licenses fully before deployment** (Nils will not email the LEX-ICON authors — the data is freely available; the Chevillard subset is open and author-linked). Attribution mandatory, non-commercial deploy stands.

## The gate that reorders everything: can we even synthesize the language?

Expansion audio comes from **GCP TTS only** (Nils's audio ruling, 2026-07-07 — no live recordings). So a dataset is only actionable for a *new* language if GCP has a voice for it. The live `voices:list` inventory (2026-07-08, in `SPEC-tts-synthesis.md` §3/App. A) is the first filter, ahead of dataset quality:

| Language | GCP native voices? | Verdict for TTS expansion |
|---|---|---|
| **Japanese** `ja-JP` | ✅ 41 | **Green** — top-600 source (NIL-86); LEX-ICON ja = bonus |
| **Korean** `ko-KR` | ✅ 41 | **Green** — LEX-ICON `ko.json` = 4,999 Hangul words in hand (below) |
| **Tamil** `ta-IN` | ✅ 38 (no Neural2) | **Green (voice)** — Chevillard/Madras-Lexicon subset (below) |
| Turkish `tr-TR`, Vietnamese `vi-VN` | ✅ 40 each | Green (voice) — **no ideophone dataset found** |
| Afrikaans `af-ZA`, Basque `eu-ES` | ⚠ 1 each | Thin (single Standard voice) |
| Swahili `sw-KE` | ✅ 30 (Chirp3-HD) | Green (voice) — not ideophone-famous, no dataset |
| **Ewe / Siwu / Semai / Zulu / Yoruba / Xhosa / Sotho / Hausa** | ❌ none | **TTS-dead** — vendored CC BY audio only |

**Consequence — the launcher's African targets (Zulu, Yoruba, Xhosa, Ewe, Sotho, Hausa) are all TTS-unreachable.** The ideophone-rich languages the literature loves are exactly the ones GCP does not voice. Those stay on the **already-vendored** `triangulating_iconicity` CC BY 4.0 set (Ewe 42, Siwu 32, Semai 38, Korean 46, Japanese 81), with no growth beyond it (human recordings are ruled out). So TTS expansion = **Japanese + Korean + Tamil**.

---

## Ranked candidate datasets

**Verification key:** ✅ = URL resolved this session · ⛁ = cloned locally by Nils · ⚠ = license caveat (proceed + verify pre-deploy).

| # | Language | Source | Location | Size | Gloss | License status | Verdict |
|---|---|---|---|---|---|---|---|
| 1 | **Japanese** | top-600 NINJAL-LWP/BCCWJ | `research/data/ideophone-dataset/top600-…xlsx` (local) | 595 | needs JMDict | freq data; forms are facts | **ADOPT** — the NIL-86 source |
| 2 | **Korean** | **LEX-ICON `ko.json`** | ⛁ `research/gcloud-tts-sources/sound-symbolism/data/processed/nat/ko.json` | **4,999** | **English (`en_meaning`)** + IPA + romanization | ⚠ no license file; defs from Standard Korean Dict | **PROCEED + verify** — the at-scale Hangul Korean source |
| 3 | **Japanese** | **LEX-ICON `ja.json`** | ⛁ same clone, `nat/ja.json` | **1,418** | English (`en_meaning`) + IPA + romanization | ⚠ same | bonus/superset of top-600 |
| 4 | **Tamil** | **Chevillard, X-eṉal expressives** (subset of Madras Tamil Lexicon) | ⛁ `research/gcloud-tts-sources/NewKolam 9+10_ Ideophones in Tamil (Jean-Luc Chevillard).htm` | subset of 646 | English | ⚠ open/author-linked (archive.org); assume OK, verify | **PROCEED + verify** |
| 5 | **Korean** | `triangulating_iconicity` (46) | local (NIL-59) | 46 | English ✅ | **CC BY 4.0** ✅ | clean but small; ships **romanization**, keep with its vendored audio |
| 6 | **Tamil (full)** | Madras Tamil Lexicon (DSAL) | https://dsal.uchicago.edu/dictionaries/tamil-lex ✅ | 646 X-eṉal | English ✅ | ⚠ **CC BY-NC-ND 2.0** | reference for the full set; ND restricts gloss redistribution |

---

## Per-source detail

### 2 + 3. LEX-ICON (AAAI-26) — ⛁ cloned, PROCEED + verify pre-deploy
Clone: `research/gcloud-tts-sources/sound-symbolism/` (github `jjhsnail0822/sound-symbolism`, "AAAI-26 Oral"). Paper: `research/papers/Jeong_Lee_Lee_Han_Yu_2026_Do_Language_Models_Associate_Sound_with_Meaning.pdf`. Per-language word lists at `data/processed/nat/{en,fr,ja,ko}.json` (verified counts this session: **en 826 · fr 809 · ja 1,418 · ko 4,999 = 8,052**). Each record carries `word`, `meaning`, `ipa`, `romanization`, `en_meaning`, plus the 25 binary semantic dimensions in `…/semantic_dimension/semantic_dimension_binary_gt.json`.
- **Korean (`ko.json`, 4,999):** Hangul `word` (e.g. `가닐가닐`), Korean `definitions` from the **Standard Korean Dictionary** (`stdict.korean.go.kr`), **plus English `en_meaning`**, IPA, romanization. **This is the at-scale, Hangul-native, English-glossed Korean ideophone source** — it dissolves the "Korean ships only romanization" problem (row 5). Curation: filter to true mimetics, QA the English glosses (dictionary/MT-derived), pick a ~600 subset to match the Japanese scale.
- **Japanese (`ja.json`, 1,418):** kana `word` (e.g. `あーん`), meaning from **Nihon Kokugo Daijiten** (`nikkoku`), `romanization`, `en_meaning`, IPA. A superset of the top-600 sheet — a candidate cross-check, not the primary (top-600 stays primary for NIL-86).
- ⚠ **License:** repo has **no LICENSE file**; glosses/definitions derive from copyrighted dictionaries (Standard Korean Dictionary, Nihon Kokugo Daijiten, OED, Petit Robert). **Nils's call (2026-07-08): proceed for the non-commercial portfolio build, verify fully before deployment.** The word *forms* are uncopyrightable facts; the 25-dimension annotations are the authors' original work; the *glosses* are the license-sensitive part — safest to treat LEX-ICON forms + annotations as the spine and re-source/re-write glosses if the pre-deploy check flags the dictionary text.
- Citation: Jeong, Lee, Lee, Han & Yu (2026), *Do Language Models Associate Sound with Meaning?*, AAAI 40(37):31247–31255, DOI 10.1609/aaai.v40i37.40387.

### 4 + 6. Tamil — Chevillard X-eṉal subset — ⛁ downloaded, PROCEED + verify pre-deploy
`research/gcloud-tts-sources/NewKolam 9+10_ Ideophones in Tamil (Jean-Luc Chevillard).htm` (216 KB, saved from **archive.org**; ~1,570 `eṉal` mentions, 7 tables). Chevillard's scholarly analysis catalogues the Tamil **X-eṉal expressives** (ஒலிக்குறிப்பு), drawing on a **subset of the Madras Tamil Lexicon** (646 total X-eṉal entries), with English glosses and Tamil script (feeds `ta-IN` voices directly, no phoneme step). **Nils's call (2026-07-08): use the Chevillard subset now, assume the license is fine (it is freely available and linked by the author himself), verify completely before deployment.** The full Madras Tamil Lexicon (row 6, `dsal.uchicago.edu/dictionaries/tamil-lex`, **CC BY-NC-ND 2.0** ✅ quoted) is the fallback reference for the complete set; ND restricts redistributing its glosses, so Chevillard's author-published subset is the cleaner entry.

### 5. Korean — `triangulating_iconicity` (46) — clean but superseded for scale
Already local (NIL-59), **CC BY 4.0**, 46 Korean ideophones with English glosses + published guessability. Ships **romanized** Korean (`ŏngŏng`), not Hangul → **keep these 46 with their existing vendored audio**; do not re-synthesize (romanization→Hangul is derivation, invariant 1). Superseded as a *scale* source by LEX-ICON `ko.json` (row 2), which is Hangul-native.

### 1. Japanese — top-600 NINJAL-LWP/BCCWJ — ADOPT (the NIL-86 source)
`research/data/ideophone-dataset/top600-ninjal-lwp-for-bccwj.xlsx`, 595 rows (kana + Hepburn + BCCWJ freq), profiled in `research/f1-dataset-profile.md`. Glosses via JMDict + adjudication. The clean, at-scale Japanese source; unchanged by today's adjudications.

---

## Scale-match assessment (vs the ~600 Japanese corpus)

| Language | At-scale source in hand? | Best available now | Gloss work | License posture |
|---|---|---|---|---|
| Japanese | ✅ | top-600 (595); LEX-ICON ja 1,418 bonus | JMDict lookup | clean (top-600) |
| **Korean** | ✅ **now** | **LEX-ICON `ko.json` 4,999 Hangul + English** | QA/curate en glosses | proceed, verify pre-deploy |
| Tamil | ⚠ subset | Chevillard X-eṉal subset (of 646) | present (English) | proceed, verify pre-deploy |
| Turkish / Vietnamese | ❌ | voice only, no dataset | — | n/a |

**Bottom line:** TTS unblocked audio; today's adjudications unblock **Korean at scale** (4,999 Hangul words with English glosses, locally in hand) and **Tamil** (Chevillard's open subset). Both carry one shared condition — **full license verification before the public deploy** (Nils's explicit gate). Japanese proceeds immediately (NIL-86). The African ideophone languages remain TTS-dead → vendored CC BY audio only.

---

## License summary + flags
- **Clean (adopt with attribution):** `triangulating_iconicity` — CC BY 4.0 (Korean 46 + Ewe/Siwu/Semai/Japanese; already local). top-600 Japanese — frequency data; forms are facts.
- **Proceed now, verify before deploy (Nils, 2026-07-08):** **LEX-ICON** (no license file; dictionary-derived glosses) — cloned locally; use forms + annotations as spine, re-source glosses if flagged. **Chevillard Tamil subset** (open/author-linked, archive.org) — use now, verify.
- **Reference only:** Madras Tamil Lexicon full set — CC BY-NC-ND 2.0 (ND blocks derived gloss redistribution). NIKL 우리말샘 / Standard Korean Dict — the upstream of LEX-ICON's Korean; KOGL, relevant if re-sourcing glosses.
- **Out of reach:** Ewe, Siwu, Semai, Zulu, Yoruba, Xhosa, Sotho, Hausa — no GCP voice → cannot TTS-grow; capped at vendored audio.

---

## What changed / verification log
1. **Voice availability is the primary filter** — live `voices:list`, 2026-07-08. Dataset quality is moot where no voice exists.
2. **LEX-ICON cloned + inspected locally** (Nils) — real counts en 826 / fr 809 / ja 1,418 / ko 4,999; Korean is **Hangul + English + IPA + romanization** → the scale-match for Korean. No license file → proceed + verify pre-deploy (Nils's call; no author email).
3. **Tamil = Chevillard's archive.org subset** (Nils) — open/author-linked; use now, verify; full Madras Lexicon (CC BY-NC-ND) is the reference fallback.
4. **Korean "romanization-not-Hangul" problem resolved** — the local LEX-ICON `ko.json` is Hangul-native; the 46 romanized `triangulating` words keep their vendored audio and are no longer the Korean scale path.
5. Turkish/Vietnamese have voices but no ideophone dataset surfaced — data-thin, not adopted.

_Planning artifact — for Nils's review. Not committed (planning folder = Claude-managed; api/web repos untouched)._
