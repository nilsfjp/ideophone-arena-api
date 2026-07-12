# SPEC - GCP Text-to-Speech synthesis for stimulus expansion

_NIL-87, Opus, 2026-07-08. The distilled, **live-verified** recipe for producing Ideophone Arena expansion audio on Google Cloud Text-to-Speech. Consumes: Nils's audio ruling of record (2026-07-07 - no live recordings ever; GCP TTS is the audio route). Feeds: NIL-86 (Japanese top-600 pipeline; see the addendum on its launcher), NIL-60 (re-scoped "record" → "run the TTS batch"), Polyglot/M6. Companion: `research/xl-dataset-sourcing-manifest.md`._

_Everything marked ✅ was verified this session against project `enduring-sign-501717-f4` (voice list + 21 real syntheses). **Adjudicated by Nils 2026-07-08:** pinned voice, phoneme route, and dataset path are now decided (see §4/§5/§9) - no longer open._

---

## 1. What this unblocks, in one paragraph

Feed a word form (kana / Hangul / native script) to the `text:synthesize` REST endpoint, get back base64 audio, decode to WAV, transcode to `.m4a` under the invariant-2 naming convention, record `tts:gcp:<voice>:<date>` provenance. Cost at our volumes is **effectively zero** (600 words ≈ 2,400 characters, inside the monthly free tier). The pipeline is a thin loop over the corpus sheet. The voice is **pinned to `ja-JP-Wavenet-B`** (§4) and the phoneme route is **dropped** (§5), so the only remaining variable is which languages are reachable at all (§3 - fewer than hoped).

## 2. Voice types - what each supports (the taxonomy that shapes everything)

All GCP voices for our target languages emit **24 kHz** natural audio. The decisive axis is **SSML support** (the `<phoneme>` tag is SSML):

| Voice type | SSML / `<phoneme>` | Price /1M chars | Notes |
|---|---|---|---|
| **Chirp3-HD** | ❌ no SSML at all (no `<phoneme>`, no rate/pitch, no A-Law) | $30 | Newest, most natural; text input only. 30 named voices/lang. |
| **Neural2** | ✅ SSML incl. `<phoneme>` | $16 | General purpose; rate/pitch control. |
| **WaveNet** ← **pinned tier** | ✅ SSML incl. `<phoneme>` | **$4** | Warm/human; now same price as Standard; **the thesis + McLean-2023 voice lives here** (§4). |
| **Standard** | ✅ SSML incl. `<phoneme>` | $4 | Parametric; cost floor. |
| **Studio** | ✅ SSML except `<mark>`/`<emphasis>`/`<prosody pitch>`/`<lang>` | $160 | Narration; few languages - not relevant here. |

Ideophones are fed as **plain native-script text** (kana, Hangul, Tamil script), so **no SSML is required** for a language with a native voice. WaveNet (the pinned tier) additionally *supports* SSML, so `<prosody rate="slow">…</prosody>` is available if a flatter/slower delivery is ever wanted - no voice change needed.

`<phoneme>` syntax (for the record, though the route is dropped - §5): `<speak><phoneme alphabet="ipa" ph="…">fallback</phoneme></speak>`; `alphabet` ∈ {`ipa`, `x-sampa`}, except Japanese = `yomigana`, Chinese = `pinyin`/`jyutping`. ~22 languages have phoneme tables - not Tamil, not any African language.

## 3. Verified voice inventory (✅ live `voices:list`, 2026-07-08)

Full raw JSON: `research/gcloud-tts-docs/voices-list-raw.json` (2,066 voices, 63 language codes). Target-language extract (names in Appendix A):

| Language | BCP-47 | Native voices | Types present | `<phoneme>`? | Reachable by TTS? |
|---|---|---|---|---|---|
| Japanese | `ja-JP` | **41** | Chirp3-HD ×30, Neural2 ×3, WaveNet ×4, Standard ×4 | yes (`yomigana`) | ✅ **yes** |
| Korean | `ko-KR` | **41** | Chirp3-HD ×30, Neural2 ×3, WaveNet ×4, Standard ×4 | yes (`x-sampa`) | ✅ **yes** |
| Tamil | `ta-IN` | **38** | Chirp3-HD ×30, WaveNet ×4, Standard ×4 (no Neural2) | ✗ | ✅ **yes** (native script) |
| Turkish | `tr-TR` | 40 | Chirp3-HD ×30, WaveNet ×5, Standard ×5 | yes | ✅ (no dataset - manifest) |
| Vietnamese | `vi-VN` | 40 | Chirp3-HD ×30, Neural2 ×2, WaveNet ×4, Standard ×4 | ✗ | ✅ (no dataset) |
| Afrikaans | `af-ZA` | ⚠ **1** | Standard ×1 | ✗ | thin |
| Basque | `eu-ES` | ⚠ **1** | Standard ×1 | ✗ | thin |
| Swahili | `sw-KE` | 30 | Chirp3-HD ×30 | ✗ | ✅ (not ideophone-famous) |
| **Ewe / Siwu / Semai** | ee / - / - | **0** | - | ✗ | ❌ **no** |
| **Zulu / Yoruba / Xhosa / Sotho / Hausa** | zu / yo / xh / st / ha | **0** | - | ✗ | ❌ **no** |

**The finding that scopes M6:** the ideophone-rich African languages have **no GCP voice**. TTS expansion is realistically **Japanese + Korean + Tamil** only. African-language audio stays the vendored `triangulating_iconicity` CC BY set.

## 4. Pinned Japanese voice - **DECIDED (Nils, 2026-07-08): `ja-JP-Wavenet-B`**

```json
"voice": { "languageCode": "ja-JP", "name": "ja-JP-Wavenet-B", "ssmlGender": "FEMALE" }
```

**Rationale (this is a research-validity decision, not a naturalness one):** `ja-JP-Wavenet-B` is the **standardized voice used in Nils's thesis and in McLean et al. (2023)**. Synthesizing expansion audio with the same voice makes new stimuli **continuous with the original experimental stimuli** - the strongest possible reason to pin a voice for a research instrument. (This supersedes the session's earlier naturalness-based Chirp3-HD proposal.)

- **Other languages - prefer a similar female WaveNet voice** (Nils's guidance). Proposed: **`ko-KR-Wavenet-A`** (FEMALE), **`ta-IN-Wavenet-A`** (FEMALE). Ratifiable, but follow the "female WaveNet" rule.
- WaveNet supports SSML, so if a lone reduplicated word ever needs flattening, wrap in `<speak><prosody rate="slow">…</prosody></speak>` - same voice.
- One voice is pinned for the **entire** set (one speaker identity = one shared per-word file, invariant 2). The exact voice is recorded in provenance (§6.6).
- Reference clip in the pinned voice: `research/tts-samples/ja-JP/ja-iraira-Wavenet-B.m4a` (the 5 Japanese *singles* were rendered in Chirp3-HD-Aoede **before** this decision - regenerate in Wavenet-B if a fresh reference set is wanted, else NIL-86's batch produces them all).

> **⚠ Provenance implication - flag for NIL-86 (do not silently overturn NIL-86's decision):** if ja-JP-Wavenet-B is the thesis/McLean voice, then the existing **thesis-word audio was itself TTS-synthesized with this voice, not human-recorded** (this corrects an earlier wrong assumption in this spec). If so, thesis words and TTS-expansion words share **identical provenance**, which would **relax NIL-86 decision 4 ("mixed thesis×new pairs = re-pair or drop") for Japanese** - both members are Wavenet-B TTS, i.e. homogeneous under the pair-provenance rule. Confirm the provenance of the app's existing thesis audio at NIL-86 before relying on this. Invariant 2 (no re-rendering of thesis words) is unaffected either way.

## 5. Phoneme route - **DROPPED (Nils, 2026-07-08)**

Not pursued. The empirical basis (kept as the record): four real `<phoneme>` syntheses this session (`research/tts-samples/phoneme-route/`) all returned 200 + audio - **including the hard case** (`kpraɖii`, with labial-velar `kp` and retroflex `ɖ`, neither in the en-US phoneme table). That is the disqualifier: the API **silently approximates or drops** unsupported phonemes with **no error signal**, and rendering an Ewe ideophone through an Italian/Spanish/English voice model is an approximation in a foreign phonetic system, not a research-honest cross-linguistic stimulus.

**Consequence:** languages without a native voice (Ewe, Siwu, Semai, Zulu, Yoruba, Xhosa, Sotho, Hausa) stay on their **authentic vendored audio** (`triangulating_iconicity`, CC BY 4.0). No TTS growth for them.

## 6. Synthesis recipe (bulk pattern)

### 6.1 Auth (per Appendix B)
```sh
TOK=$(gcloud auth print-access-token)          # short-lived (~1h, sometimes ~10 min); refresh per run
PROJECT=enduring-sign-501717-f4
```

### 6.2 List voices (ground-truth check before a batch)
```sh
curl -sS -H "Authorization: Bearer $TOK" -H "x-goog-user-project: $PROJECT" \
  "https://texttospeech.googleapis.com/v1/voices?languageCode=ja-JP"
```

### 6.3 One synthesis (native-script text → WAV master), pinned voice
```sh
curl -sS -H "Authorization: Bearer $TOK" -H "x-goog-user-project: $PROJECT" \
  -H "Content-Type: application/json; charset=utf-8" \
  --data '{"input":{"text":"きらきら"},
           "voice":{"languageCode":"ja-JP","name":"ja-JP-Wavenet-B","ssmlGender":"FEMALE"},
           "audioConfig":{"audioEncoding":"LINEAR16"}}' \
  "https://texttospeech.googleapis.com/v1/text:synthesize" \
| python3 -c "import sys,json,base64; open('out.wav','wb').write(base64.b64decode(json.load(sys.stdin)['audioContent']))"
```
- `audioEncoding: LINEAR16` → WAV master (with header). Response is `{"audioContent": "<base64>"}`; **must base64-decode** before it plays.

### 6.4 Transcode WAV → `.m4a`
```sh
ffmpeg -y -loglevel error -i out.wav -c:a aac -b:a 96k "<3char>-<romaji>.m4a"
```
✅ proven this session on 21 clips (AAC, 24 kHz preserved, 9–17 KB each).

### 6.5 Naming (invariant 2, unchanged)
- **Japanese core:** `/stimuli/audio/<3-char prefix>-<romaji>.m4a`, prefix = first 3 chars of romaji. きらきら → `kir-kirakira.m4a`; いらいら → `ira-iraira.m4a`; どきどき → `dok-dokidoki.m4a`.
- **Cross-linguistic (XL):** `/stimuli/xl/{iso}/{basename}.m4a` per SPEC-cross-linguistic §G5 (invariant 2 untouched).

### 6.6 Provenance (per stimulus)
Record `tts:gcp:<voice>:<YYYY-MM-DD>` - e.g. **`tts:gcp:ja-JP-Wavenet-B:2026-07-08`**. Home = `stimulus_sources` at M5; until then the TTS manifest (`scripts/tts-manifest.json`) + a seed comment. Aggregates must stay splittable by provenance (Observatory integrity strip).

### 6.7 Rate limiting / robustness
Default project quotas are generous (hundreds of requests/min); 600 single-word requests complete in ~1–2 min **serial**. Exponential backoff on HTTP 429 / 5xx (1s→2s→4s, max 5 tries); write a per-word success/fail row to the manifest so a partial batch is resumable. Serial or ≤4-way parallel.

### 6.8 Pair-provenance homogeneity (hard rule, from the audio ruling)
Both members of every 2AFC pair must share audio provenance (both human **or** both TTS). Generator asserts this beside invariant 4. See the §4 flag: if thesis audio is already Wavenet-B TTS, thesis×new Japanese pairs are homogeneous - confirm at NIL-86.

## 7. Cost - effectively zero (✅ pricing fetched 2026-07-08)

Per-character, after the monthly free tier (`cloud.google.com/text-to-speech/pricing`): Standard **$4** / WaveNet **$4** / Neural2 $16 / Chirp3-HD $30 / Studio $160 per 1M chars. Free/month: Standard+WaveNet **4,000,000**; Neural2/Chirp3-HD/Studio 1,000,000.

Ideophones are tiny (a reduplicated kana word ≈ 4 characters; **Japanese multi-byte chars bill as 1 char each**). With the pinned **WaveNet** tier ($4/1M, 4M free):
- **600 Japanese words** × ~4 chars = **~2,400 chars** → **$0.0096** at rate, **$0** under the 4M free tier.
- **Whole LEX-ICON-scale batch** (~8,000 words × ~4 = ~32,000 chars): **$0.13** at rate, **$0** under free tier.
- Re-running the batch 10× during iteration ≈ 24,000 chars ⇒ still free.

**Conclusion: cost is a non-issue.** (Only guardrail: send the word form, never a whole sentence.)

## 8. Sample batch (✅ 21 clips, this session)

`research/tts-samples/` + `research/tts-samples/listening-sheet.md`. Japanese: いらいら across 4 tiers (**incl. the pinned `ja-iraira-Wavenet-B.m4a`**) + 5 top-600 singles (Chirp3-HD-Aoede, pre-decision). Korean: 반짝반짝 across 3 tiers + 5 Hangul singles. Phoneme-route: 4 tests (§5, route dropped). **Korean note:** the sample forms are hand-sourced Hangul, but LEX-ICON's local `ko.json` (§ manifest) is a **4,999-word Hangul-native** source - a real Korean pipeline draws from that, not from the romanized `triangulating` set.

## 9. Decisions & remaining items
1. **Pinned voice - DECIDED:** `ja-JP-Wavenet-B` (FEMALE); other langs female WaveNet (`ko-KR-Wavenet-A`, `ta-IN-Wavenet-A` proposed). §4.
2. **Phoneme route - DROPPED.** §5. African langs stay on vendored audio.
3. **Datasets - path chosen (Nils):** LEX-ICON cloned locally (`research/gcloud-tts-sources/sound-symbolism/`) + Tamil via Chevillard's freely-available subset; proceed for now, **full license verification before deployment** (manifest). No author contact.
4. **NIL-86 flag:** confirm thesis-audio provenance (§4) - may relax the mixed-pair rule for Japanese.
5. Provenance-strip copy line on the Observatory (follow-up rider, next Observatory touch).

---

## Appendix A - raw voice names (target languages, ✅ 2026-07-08)

SSML-capable (non-Chirp3) voices, all 24 kHz:

- **ja-JP:** Neural2-B (F), Neural2-C (M), Neural2-D (M); **Wavenet-A (F), Wavenet-B (F ← pinned), Wavenet-C (M), Wavenet-D (M)**; Standard-A (F), Standard-B (F), Standard-C (M), Standard-D (M). Plus 30 Chirp3-HD.
- **ko-KR:** Neural2-A (F), Neural2-B (F), Neural2-C (M); **Wavenet-A (F ← proposed), Wavenet-B (F), Wavenet-C (M), Wavenet-D (M)**; Standard-A/B (F), Standard-C/D (M). Plus 30 Chirp3-HD.
- **ta-IN:** **Wavenet-A (F ← proposed), Wavenet-B (M), Wavenet-C (F), Wavenet-D (M)**; Standard-A (F), Standard-B (M), Standard-C (F), Standard-D (M); 30 Chirp3-HD. (No Neural2.)
- **af-ZA:** Standard-A (F) only. **eu-ES:** Standard-B (F) only. **sw-KE:** 30 Chirp3-HD only.
- **Negative results (verified absent):** ee (Ewe), Siwu, Semai, zu, yo, xh, st, ha, ta-LK/ta-SG/ta-MY.

Full JSON (2,066 voices, 63 codes): `research/gcloud-tts-docs/voices-list-raw.json`.

## Appendix B - auth + endpoints
- Project: `enduring-sign-501717-f4`. OAuth scope: `https://www.googleapis.com/auth/cloud-platform`.
- Headers: `Authorization: Bearer $(gcloud auth print-access-token)` · `x-goog-user-project: <project>` · `Content-Type: application/json; charset=utf-8`.
- Endpoints: `GET …/v1/voices` · `POST …/v1/text:synthesize` · (`…:synthesizeLongAudio` = async, not needed for single words).
- ⚠ **Access tokens are short-lived** (this session's first lapsed within ~10 min) - regenerate per batch; read the token fresh at start, never cache.
- Encodings: `LINEAR16` (WAV master, recommended) · `MP3` · `OGG_OPUS`.

_Reference docs digested this session: `research/gcloud-tts-docs/` (basics, synthesize, rest, rpc, ssml, phonemes, list-voices-and-types, gcloud-info). Planning artifact - Nils reviews; api/web repos untouched._
