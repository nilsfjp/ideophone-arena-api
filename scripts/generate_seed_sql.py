#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
import sys
import uuid
from collections import OrderedDict
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_EVEN, getcontext
from pathlib import Path

# Ample precision for the Decimal foil-distance arithmetic; the value is quantized
# to 4 places at the end, so --check stays byte-stable regardless of platform.
getcontext().prec = 50


REPO_ROOT = Path(__file__).resolve().parents[1]
RESOURCE_DIR = REPO_ROOT / "src" / "main" / "resources"
OUTPUT_PATH = RESOURCE_DIR / "db" / "init" / "ideophone_arena.sql"

CONDITIONS = (
    (1, "CONDITION_1_SOKUON", RESOURCE_DIR / "condition-1-choosing-sokuon.csv"),
    (2, "CONDITION_2_SOKUON", RESOURCE_DIR / "condition-2-choosing-sokuon.csv"),
    (3, "CONDITION_3_SOKUON", RESOURCE_DIR / "condition-3-choosing-sokuon.csv"),
)

STIMULUS_RE = re.compile(r"^(?P<pairing>[a-z]\d+)(?P<script>[a-z]{2})-(?P<romaji>.+)\.mp4$")

KANA_TOKENS = {
    "kya": "きゃ",
    "kyu": "きゅ",
    "kyo": "きょ",
    "sya": "しゃ",
    "syu": "しゅ",
    "syo": "しょ",
    "zya": "じゃ",
    "zyu": "じゅ",
    "zyo": "じょ",
    "tya": "ちゃ",
    "tyu": "ちゅ",
    "tyo": "ちょ",
    "nya": "にゃ",
    "nyu": "にゅ",
    "nyo": "にょ",
    "hya": "ひゃ",
    "hyu": "ひゅ",
    "hyo": "ひょ",
    "mya": "みゃ",
    "myu": "みゅ",
    "myo": "みょ",
    "rya": "りゃ",
    "ryu": "りゅ",
    "ryo": "りょ",
    "gya": "ぎゃ",
    "gyu": "ぎゅ",
    "gyo": "ぎょ",
    "bya": "びゃ",
    "byu": "びゅ",
    "byo": "びょ",
    "pya": "ぴゃ",
    "pyu": "ぴゅ",
    "pyo": "ぴょ",
    "a": "あ",
    "i": "い",
    "u": "う",
    "e": "え",
    "o": "お",
    "ka": "か",
    "ki": "き",
    "ku": "く",
    "ke": "け",
    "ko": "こ",
    "sa": "さ",
    "si": "し",
    "su": "す",
    "se": "せ",
    "so": "そ",
    "ta": "た",
    "ti": "ち",
    "tu": "つ",
    "te": "て",
    "to": "と",
    "na": "な",
    "ni": "に",
    "nu": "ぬ",
    "ne": "ね",
    "no": "の",
    "ha": "は",
    "hi": "ひ",
    "hu": "ふ",
    "he": "へ",
    "ho": "ほ",
    "ma": "ま",
    "mi": "み",
    "mu": "む",
    "me": "め",
    "mo": "も",
    "ya": "や",
    "yu": "ゆ",
    "yo": "よ",
    "ra": "ら",
    "ri": "り",
    "ru": "る",
    "re": "れ",
    "ro": "ろ",
    "wa": "わ",
    "wo": "を",
    "ga": "が",
    "gi": "ぎ",
    "gu": "ぐ",
    "ge": "げ",
    "go": "ご",
    "za": "ざ",
    "zi": "じ",
    "zu": "ず",
    "ze": "ぜ",
    "zo": "ぞ",
    "da": "だ",
    "di": "ぢ",
    "du": "づ",
    "de": "で",
    "do": "ど",
    "ba": "ば",
    "bi": "び",
    "bu": "ぶ",
    "be": "べ",
    "bo": "ぼ",
    "pa": "ぱ",
    "pi": "ぴ",
    "pu": "ぷ",
    "pe": "ぺ",
    "po": "ぽ",
}


# The stimulus PNGs render these two long-vowel words with the chouonpu in both
# scripts; the lemma kana column keeps the plain-vowel dictionary spelling.
LONG_VOWEL_FORMS = {
    "zyaazyaa": ("じゃーじゃー", "ジャージャー"),
    "kyaakyaa": ("きゃーきゃー", "キャーキャー"),
}

# Source-spreadsheet typo kept out of the seed (ids 47/107/167).
GLOSS_FIXES = {
    "refreshingly, with a feeling fo relief": "refreshingly, with a feeling of relief",
}

# The single v1 language. Cross-linguistic languages (kor/ewe/akp/sea) arrive
# with the XL ingestion session (M5/M6); every current word backfills to jpn.
JPN_LANGUAGE_ID = 1
LANGUAGES = (
    # (id, iso_code, name, family, player_note)
    (JPN_LANGUAGE_ID, "jpn", "Japanese", "Japonic", None),
)

# Per-pairing Choosing accuracy from the thesis raw dataset (n=36, correct/36),
# thesis-facts §4 (Figure 10, exact values verified 2026-06-12). Keyed by the
# stimulus pairing prefix. The 4 practice pairs (p0-p3, Appendix B) carry no
# per-pair thesis measure and are absent here -> thesis_accuracy NULL.
THESIS_N = 36
THESIS_PAIR_ACCURACY = {
    "a9": 34, "a7": 30, "v2": 30, "i3": 29, "v8": 27, "a2": 26, "v0": 26,
    "a0": 25, "v7": 25, "i5": 25, "a3": 24, "a6": 24, "v5": 24, "i9": 24,
    "a4": 23, "i0": 23, "a8": 22, "a1": 21, "v1": 21, "v3": 21, "i4": 21,
    "v6": 20, "v9": 20, "i1": 20, "i6": 20, "i7": 20, "i8": 20,
    "a5": 18, "v4": 17, "i2": 13,
}

# Dev-only admin account seeded for local demos. The hash is a frozen BCrypt
# digest of a throwaway dev password (documented in docs/demo-runbook.md,
# "Creating an admin"); it must stay constant so --check stays reproducible.
# Produced once via jshell with spring-security-crypto on the class path:
#   BCrypt.hashpw(<dev password>, BCrypt.gensalt())
ADMIN_USERNAME = "arena_admin"
ADMIN_EMAIL = "arena_admin@example.invalid"
ADMIN_PASSWORD_HASH = "$2a$10$AWmwnu11Xi/MVcBlbRLB8OUYrJ7kmfjW9Qzy6tCAk38/Kw0EUGzaK"
ADMIN_ROLE = "ROLE_ADMIN"

# --- NIL-54: thesis Gorilla tidy-data ingestion ---------------------------
# The thesis's own experiment data (36 participants: a 2AFC "choosing" task and
# a 7-point "rating" task), ingested as generator-emitted seed rows so the
# Observatory runs on real human data. Reconstruction key: trials.correct_word_id
# (== the thesis fixed target). Sessions carry completed_at = NULL and the cohort
# is fenced off from the live research aggregates by a reserved username prefix
# (thesis_p01..thesis_p36), exactly like browser_loop_% (Rider A precedent).
TIDY_DIR = REPO_ROOT / "docs" / "research" / "data"
CHOOSING_CSV = TIDY_DIR / "gorilla-tidy-choosing.csv"
RATING_CSV = TIDY_DIR / "gorilla-tidy-rating.csv"

THESIS_USER_COUNT = 36
THESIS_ROW_COUNT = 1080  # 36 participants x 30 trials, per task

CONDITION_BY_SPREADSHEET = {
    "condition-1": "CONDITION_1_SOKUON",
    "condition-2": "CONDITION_2_SOKUON",
    "condition-3": "CONDITION_3_SOKUON",
}

# The condition CSVs (the word source) and the Gorilla tidy export disagree on
# the romanisation of four geminate (sokuon) ideophones: the word source uses a
# Q-truncated token (romaji_to_hiragana folds Q -> small tsu), the tidy export
# spells the full doubled-consonant + "to" form. Both name the SAME word; the
# kana differ by one mora, so resolution keys on this alias, not on kana.
#   tidy-export romaji -> words.romaji
SOKUON_ROMAJI_ALIASES = {
    "sakutto": "sakuQ",
    "kiritto": "kiriQ",
    "hotto": "hoQ",
    "katitto": "katiQ",
}

# thesis_p## accounts never authenticate; a single frozen BCrypt digest of a
# throwaway password keeps --check reproducible (runtime hashing would salt
# differently each run). Minted once via jshell + spring-security-crypto, the
# same way ADMIN_PASSWORD_HASH was, of "thesis-cohort-no-login-nil54".
THESIS_PASSWORD_HASH = "$2a$10$l2O7BFb4JhRRBxGhHQDmJOFi8XTVpdf6y59IfXOiEyCCabMdOZgba"
THESIS_ROLE = "ROLE_USER"
# Fixed namespace so uuid5 session uuids are deterministic (RFC 4122 example NS).
THESIS_UUID_NAMESPACE = uuid.UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

# --- NIL-86: stimulus-expansion ingestion (dark inventory) -----------------
# The 21 signed-off expansion pairs (workbook Pairs sheet, minus H2/H3 which reuse
# thesis words on a mismatched floor -- deferred). Ingested as words/presentations/
# pairings but NO trials, so they sit as inventory in zero live pools (ADR-3:
# a pairing without a trial is unserved) until their TTS audio is generated.
EXPANSION_PAIRS_CSV = TIDY_DIR / "stimulus-expansion-pairs.csv"
EXPANSION_CANDIDATES_CSV = TIDY_DIR / "stimulus-expansion-candidates.csv"
EXPANSION_SOURCE = "EXPANSION"
EXPANSION_WORKBOOK = "stimulus-expansion-signoff.xlsx"
# Approval of record: fable-week-plan.md sign-off (ARCHITECTURE ADR-6); the workbook
# sign_off column is blank, so signoff_ref cites the workbook pair and approved_at
# carries the plan date. Hardcoded (not date.today()) so --check stays reproducible.
EXPANSION_APPROVED_AT = "2026-07-02"
EXPANSION_CONDITION_NAMES = ("CONDITION_1_SOKUON", "CONDITION_2_SOKUON", "CONDITION_3_SOKUON")

# Audio provenance per word (SPEC-tts-synthesis 6.6/6.8). Every current jpn stimulus
# is one synthesized voice (thesis audio is itself ja-JP-Wavenet-B TTS), so all pairs
# are provenance-homogeneous; the override map is where future vendored/human audio
# (XL/M6) would diverge and trip the homogeneity assertion. Provenance's schema home
# is stimulus_sources at M5; until then it lives in scripts/tts-manifest.json + here.
TTS_VOICE_PROVENANCE = "tts:gcp:ja-JP-Wavenet-B"
WORD_PROVENANCE_OVERRIDES: dict[str, str] = {}  # romaji -> provenance

# Hepburn (workbook) -> Kunrei/Nihon-shiki (words.romaji key) fold. A validator only:
# the CSV carries the explicit romaji key; this cross-checks it (the syllabic-n rule
# below keeps n before y/vowel, which matches the minted set; ambiguous reuse targets
# like doNyori are resolved from the explicit key, never folded here).
HEPBURN_FOLDS = (
    ("sha", "sya"), ("shu", "syu"), ("sho", "syo"), ("shi", "si"),
    ("cha", "tya"), ("chu", "tyu"), ("cho", "tyo"), ("chi", "ti"),
    ("tsu", "tu"),
    ("ja", "zya"), ("ju", "zyu"), ("jo", "zyo"), ("ji", "zi"),
    ("fu", "hu"),
)


def to_katakana(hiragana: str) -> str:
    return "".join(
        chr(ord(ch) + 0x60) if 0x3041 <= ord(ch) <= 0x3096 else ch for ch in hiragana
    )


@dataclass(frozen=True)
class Stimulus:
    kana: str
    display_form: str
    canonical_form: str
    romaji: str
    gloss: str
    canonical_script: str
    stimulus_file: str
    audio_file: str
    modality: str


@dataclass(frozen=True)
class Word:
    audio_file: str            # words.stimulus_file: the per-word shared audio (invariant 2)
    romaji: str
    kana: str
    canonical_form: str
    canonical_script: str      # 'H' | 'K' -- word-level script (first letter of the 2-letter code)
    gloss: str
    modality: str


@dataclass(frozen=True)
class Presentation:
    audio_file: str            # -> word
    condition_name: str
    display_form: str
    script_code: str           # legacy 2-letter provenance code (HU/KD/HH/KK/HK/KH)


@dataclass(frozen=True)
class Pairing:
    pair_code: str
    word_a_audio: str          # h-position word (word-a column)
    word_b_audio: str          # k-position word (word-b column)
    correct_audio: str         # thesis fixed target (word-answer column)
    modality: str
    practice: bool
    # NIL-86 expansion fields; thesis/practice pairings keep the defaults so their
    # emitted rows stay byte-identical. Expansion pairings are dark (no trial).
    source: str = "THESIS"
    difficulty_prior: str | None = None
    foil_distance: Decimal | None = None
    signoff_ref: str | None = None
    approved_at: str | None = None


@dataclass(frozen=True)
class ChoosingTrial:
    participant: str           # Gorilla "Participant Private ID", e.g. "13318377.0"
    condition_name: str        # CONDITION_{1,2,3}_SOKUON (from Current Spreadsheet)
    pair_code: str             # a0..i9 (Spreadsheet: pairing) -> pairings.pair_code
    selected_file: str         # Response .mp4 (always word-a or word-b)
    answer_file: str           # word-answer .mp4 (the thesis fixed target)
    correct: bool              # CSV Correct == "1"
    response_time_ms: int      # round(Reaction Time); stored verbatim (may exceed 600000)


@dataclass(frozen=True)
class RatingRecord:
    participant: str
    pair_code: str
    rated_file: str            # rated-word .mp4 (a pairing member; the target)
    rating: int                # 1-7
    response_time_ms: int


def romaji_to_hiragana(romaji: str) -> str:
    kana = []
    index = 0

    while index < len(romaji):
        if romaji[index] == "N":
            kana.append("ん")
            index += 1
            continue

        if romaji[index] == "Q":
            kana.append("っ")
            index += 1
            continue

        if (
            index + 1 < len(romaji)
            and romaji[index] == romaji[index + 1]
            and romaji[index] not in {"a", "e", "i", "o", "u", "n"}
        ):
            kana.append("っ")
            index += 1
            continue

        for width in (3, 2, 1):
            token = romaji[index : index + width]
            if token in KANA_TOKENS:
                kana.append(KANA_TOKENS[token])
                index += width
                break
        else:
            raise ValueError(f"Cannot convert romaji token near {romaji[index:]} in {romaji}")

    return "".join(kana)


def parse_stimulus_file(filename: str) -> tuple[str, str, str]:
    match = STIMULUS_RE.match(filename)
    if not match:
        raise ValueError(f"Unexpected stimulus filename: {filename}")
    romaji = match.group("romaji")
    return match.group("pairing"), match.group("script"), romaji


def read_display_rows(path: Path, display: str) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        return [row for row in reader if row.get("display") == display]


def collect_rows(
    words: OrderedDict[str, Word],
    presentations: list[Presentation],
    pairings: OrderedDict[str, Pairing],
    rows: list[dict[str, str]],
    condition_name: str,
    path: Path,
    practice: bool,
) -> None:
    for row in rows:
        left = stimulus_from_row(row, "word-a", "meaning-a")
        right = stimulus_from_row(row, "word-b", "meaning-b")
        correct = stimulus_from_row(row, "word-answer", None)

        if correct.audio_file not in {left.audio_file, right.audio_file}:
            raise ValueError(f"Correct answer {row['word-answer']} is not one of the choices in {path.name}")

        # Split each condition row into its word (shared across the 3 conditions,
        # keyed by audio_file = invariant 2) and its presentation (this condition's
        # script manipulation).
        for stimulus in (left, right):
            if stimulus.stimulus_file.startswith("p") != practice:
                raise ValueError(
                    f"Pairing prefix of {stimulus.stimulus_file} disagrees with display={row['display']} in {path.name}"
                )
            upsert_word(words, stimulus, path)
            presentations.append(
                Presentation(
                    audio_file=stimulus.audio_file,
                    condition_name=condition_name,
                    display_form=stimulus.display_form,
                    script_code=stimulus.canonical_script,
                )
            )

        pair_code = row["pairing"]
        if left.modality != right.modality:
            raise ValueError(
                f"Pairing {pair_code} members disagree on modality "
                f"({left.modality} vs {right.modality}) in {path.name}"
            )
        pairing = Pairing(
            pair_code=pair_code,
            word_a_audio=left.audio_file,
            word_b_audio=right.audio_file,
            correct_audio=correct.audio_file,
            modality=left.modality,
            practice=practice,
        )
        existing_pairing = pairings.get(pair_code)
        if existing_pairing is None:
            pairings[pair_code] = pairing
        elif existing_pairing != pairing:
            raise ValueError(f"Conflicting pairing data for {pair_code}: {existing_pairing} vs {pairing}")


def upsert_word(words: OrderedDict[str, Word], stimulus: Stimulus, path: Path) -> None:
    word = Word(
        audio_file=stimulus.audio_file,
        romaji=stimulus.romaji,
        kana=stimulus.kana,
        canonical_form=stimulus.canonical_form,
        canonical_script=stimulus.canonical_script[0],  # 2-letter code -> word-level 'H' | 'K'
        gloss=stimulus.gloss,
        modality=stimulus.modality,
    )
    existing = words.get(stimulus.audio_file)
    if existing is None:
        words[stimulus.audio_file] = word
    elif existing != word:
        raise ValueError(f"Conflicting word data for {stimulus.audio_file}: {existing} vs {word} in {path.name}")


def word_id_map(words: OrderedDict[str, Word]) -> dict[str, int]:
    return {audio_file: index for index, audio_file in enumerate(words.keys(), start=1)}


def hepburn_to_kunrei(hepburn: str) -> str:
    text = hepburn
    for source_form, target_form in HEPBURN_FOLDS:
        text = text.replace(source_form, target_form)
    text = re.sub(r"n(?![aiueoy])", "N", text)  # syllabic/final n (n before y/vowel stays)
    text = re.sub(r"q$", "Q", text)             # final sokuon
    return text


def mora_segments(romaji: str) -> list[str]:
    # Segment a Kunrei romaji into morae, mirroring romaji_to_hiragana but returning
    # romaji tokens; a doubled non-vowel consonant becomes a 'Q' (sokuon) mora, so the
    # feature extraction sees the geminate that words.romaji spells as a doubled letter.
    morae: list[str] = []
    index = 0
    while index < len(romaji):
        char = romaji[index]
        if char == "N":
            morae.append("N")
            index += 1
            continue
        if char == "Q":
            morae.append("Q")
            index += 1
            continue
        if (
            index + 1 < len(romaji)
            and romaji[index] == romaji[index + 1]
            and char not in {"a", "e", "i", "o", "u", "n"}
        ):
            morae.append("Q")
            index += 1
            continue
        for width in (3, 2, 1):
            token = romaji[index : index + width]
            if token in KANA_TOKENS:
                morae.append(token)
                index += width
                break
        else:
            raise ValueError(f"Cannot segment romaji {romaji} near {romaji[index:]}")
    return morae


def feature_vector(romaji: str) -> dict[str, object]:
    # The 7 whole-form features of SPEC-free-form-entry section 5 (the metric behind
    # the rho=-.155 validation result). voicedOnset is pinned to the first mora's
    # leading letter in {g,z,d,b} (so palatalized zy/gy/by/dz count as voiced).
    morae = mora_segments(romaji)
    count = len(morae)
    heavy = light = 0
    for mora in morae:
        if mora in ("N", "Q"):
            continue
        vowel = mora[-1]
        if vowel in ("o", "u"):
            heavy += 1
        elif vowel in ("i", "e"):
            light += 1
    heavy_ratio = Decimal("0.5") if heavy + light == 0 else Decimal(heavy) / Decimal(heavy + light)
    redup = count >= 4 and count % 2 == 0 and morae[: count // 2] == morae[count // 2 :]
    return {
        "mora": count,
        "redup": redup,
        "sokuon": "Q" in morae,
        "final_n": morae[-1] == "N",
        "ri_suffix": morae[-1] == "ri" and not redup,
        "voiced_onset": morae[0][0] in {"g", "z", "d", "b"},
        "heavy_ratio": heavy_ratio,
    }


def foil_distance(romaji_a: str, romaji_b: str) -> Decimal:
    # Symmetric featural distance in [0, 1]: 1 - the SPEC section 5 weighted similarity.
    # A validation-only covariate (rho=-.155 n.s. on the thesis pairs) -- never an
    # ordering/difficulty input for is_core content (ARCHITECTURE ADR-6).
    a = feature_vector(romaji_a)
    b = feature_vector(romaji_b)

    def agree(key: str) -> Decimal:
        return Decimal(1) if a[key] == b[key] else Decimal(0)

    similarity = (
        Decimal("0.20") * agree("redup")
        + Decimal("0.15") * agree("sokuon")
        + Decimal("0.10") * agree("final_n")
        + Decimal("0.10") * agree("ri_suffix")
        + Decimal("0.15") * agree("voiced_onset")
        + Decimal("0.15") * (Decimal(1) - abs(a["heavy_ratio"] - b["heavy_ratio"]))
        + Decimal("0.15")
        * (Decimal(1) - abs(Decimal(a["mora"]) - Decimal(b["mora"])) / Decimal(max(a["mora"], b["mora"])))
    )
    return (Decimal(1) - similarity).quantize(Decimal("0.0001"), rounding=ROUND_HALF_EVEN)


def read_expansion_candidates(path: Path) -> dict[str, dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        return {row["romaji"]: row for row in csv.DictReader(source)}


def read_expansion_pairs(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        return list(csv.DictReader(source))


def mint_expansion_word(
    words: OrderedDict[str, Word],
    presentations: list[Presentation],
    romaji: str,
    candidate: dict[str, str],
    modality: str,
    audio_prefix: str,
) -> str:
    kana = romaji_to_hiragana(romaji)  # raises on a malformed Kunrei key
    if hepburn_to_kunrei(candidate["romaji_hepburn"]) != romaji:
        raise ValueError(f"Hepburn {candidate['romaji_hepburn']!r} does not fold to Kunrei {romaji!r}")
    if candidate["pool"] == "A" and candidate["workbook_hira"] and kana != candidate["workbook_hira"]:
        raise ValueError(f"Derived kana {kana!r} for {romaji!r} != workbook hira {candidate['workbook_hira']!r}")

    kata_share = candidate["kata_share"]
    canonical_script = "K" if (kata_share and float(kata_share) > 0.5) else "H"  # ADR-6: kata_share > 0.5
    hiragana_form, katakana_form = LONG_VOWEL_FORMS.get(romaji, (kana, to_katakana(kana)))
    canonical_form = hiragana_form if canonical_script == "H" else katakana_form
    incongruent_form = katakana_form if canonical_script == "H" else hiragana_form
    audio_file = f"audio/{audio_prefix}{canonical_script.lower()}-{romaji}.m4a"
    if audio_file in words:
        raise ValueError(f"Expansion word audio_file already present: {audio_file}")

    words[audio_file] = Word(
        audio_file=audio_file,
        romaji=romaji,
        kana=kana,
        canonical_form=canonical_form,
        canonical_script=canonical_script,
        gloss=candidate["gloss"],
        modality=modality,
    )
    other_script = "K" if canonical_script == "H" else "H"
    # CONDITION_1 audio-only (canonical reveal, U/D code); CONDITION_2 congruent
    # (canonical script); CONDITION_3 incongruent (opposite script) -- invariants 1/3.
    condition_specs = (
        (EXPANSION_CONDITION_NAMES[0], canonical_form, canonical_script + ("U" if canonical_script == "H" else "D")),
        (EXPANSION_CONDITION_NAMES[1], canonical_form, canonical_script + canonical_script),
        (EXPANSION_CONDITION_NAMES[2], incongruent_form, canonical_script + other_script),
    )
    for condition_name, display_form, script_code in condition_specs:
        presentations.append(
            Presentation(
                audio_file=audio_file,
                condition_name=condition_name,
                display_form=display_form,
                script_code=script_code,
            )
        )
    return audio_file


def collect_expansion(
    words: OrderedDict[str, Word],
    presentations: list[Presentation],
    pairings: OrderedDict[str, Pairing],
) -> None:
    # Appends after the thesis+practice pass so thesis words keep ids 1-68 and thesis
    # pairings 1-34 (the shuffle/trial-id contract). Expansion words -> ids 69+, expansion
    # pairings -> 35+ with NO trials (dark). word_a is always the lower-id member so the
    # word_a_id < word_b_id shuffle-order invariant holds (reused word before new word).
    if not EXPANSION_PAIRS_CSV.exists() or not EXPANSION_CANDIDATES_CSV.exists():
        raise FileNotFoundError(f"Expansion CSVs missing: {EXPANSION_PAIRS_CSV}, {EXPANSION_CANDIDATES_CSV}")
    candidates = read_expansion_candidates(EXPANSION_CANDIDATES_CSV)
    romaji_to_audio = {word.romaji: audio for audio, word in words.items()}

    for row in read_expansion_pairs(EXPANSION_PAIRS_CSV):
        pair_id = row["pair_id"]
        pair_code = "exp-" + pair_id.lower()
        modality = row["modality"]
        audio_prefix = row["audio_prefix"]
        word_1 = row["word_1"]
        word_2 = row["word_2"]

        if word_1 not in candidates:
            raise ValueError(f"Expansion word_1 {word_1!r} ({pair_id}) missing from candidates")
        word_1_audio = mint_expansion_word(words, presentations, word_1, candidates[word_1], modality, audio_prefix)

        if row["word_2_is_existing"] == "1":
            if word_2 not in romaji_to_audio:
                raise ValueError(f"Expansion reuse target {word_2!r} ({pair_id}) is not an existing word")
            word_2_audio = romaji_to_audio[word_2]
        else:
            if word_2 not in candidates:
                raise ValueError(f"Expansion word_2 {word_2!r} ({pair_id}) missing from candidates")
            word_2_audio = mint_expansion_word(words, presentations, word_2, candidates[word_2], modality, audio_prefix)

        romaji_to_audio[word_1] = word_1_audio
        romaji_to_audio.setdefault(word_2, word_2_audio)

        ids = word_id_map(words)
        word_a_audio, word_b_audio = sorted((word_1_audio, word_2_audio), key=lambda audio: ids[audio])
        if pair_code in pairings:
            raise ValueError(f"Duplicate expansion pair_code {pair_code}")
        pairings[pair_code] = Pairing(
            pair_code=pair_code,
            word_a_audio=word_a_audio,
            word_b_audio=word_b_audio,
            correct_audio=word_a_audio,  # placeholder member; expansion pairs are dark (no trial)
            modality=modality,
            practice=False,
            source=EXPANSION_SOURCE,
            difficulty_prior=row["difficulty_prior"],
            foil_distance=foil_distance(words[word_a_audio].romaji, words[word_b_audio].romaji),
            signoff_ref=f"{EXPANSION_WORKBOOK}#{pair_id}",
            approved_at=EXPANSION_APPROVED_AT,
        )


def word_provenance(word: Word) -> str:
    return WORD_PROVENANCE_OVERRIDES.get(word.romaji, TTS_VOICE_PROVENANCE)


def validate_pair_provenance(
    words: OrderedDict[str, Word], pairings: OrderedDict[str, Pairing]
) -> None:
    # Pair-provenance homogeneity (SPEC-tts-synthesis 6.8): both members of every 2AFC
    # pair must share audio provenance. Trivially true today (all ja-JP-Wavenet-B TTS);
    # fences future vendored/human x TTS mixes beside the invariant-4 modality check.
    for pairing in pairings.values():
        provenance_a = word_provenance(words[pairing.word_a_audio])
        provenance_b = word_provenance(words[pairing.word_b_audio])
        if provenance_a != provenance_b:
            raise ValueError(
                f"Pairing {pairing.pair_code} members disagree on audio provenance "
                f"({provenance_a} vs {provenance_b})"
            )


def collect_data() -> tuple[OrderedDict[str, Word], list[Presentation], OrderedDict[str, Pairing]]:
    words: OrderedDict[str, Word] = OrderedDict()
    presentations: list[Presentation] = []
    pairings: OrderedDict[str, Pairing] = OrderedDict()

    # Trials first, condition-major: trial words -> ids 1-60, trial pairings ->
    # 1-30 (the scored base-list order the deterministic shuffle depends on),
    # presentations 1-180. This mirrors the pre-M2 ideophone/round layout so the
    # shuffle replay is unchanged.
    for _condition_number, condition_name, path in CONDITIONS:
        if not path.exists():
            raise FileNotFoundError(path)

        trial_rows = read_display_rows(path, "trial")
        if len(trial_rows) != 30:
            raise ValueError(f"Expected 30 trial rows in {path.name}, found {len(trial_rows)}")
        collect_rows(words, presentations, pairings, trial_rows, condition_name, path, practice=False)

    # Practice pairs (p0-p3, Appendix B) after all trial rows: practice words ->
    # ids 61-68, practice pairings 31-34, practice presentations 181-204.
    for _condition_number, condition_name, path in CONDITIONS:
        practice_rows = read_display_rows(path, "practice")
        if len(practice_rows) != 4:
            raise ValueError(f"Expected 4 practice rows in {path.name}, found {len(practice_rows)}")
        collect_rows(words, presentations, pairings, practice_rows, condition_name, path, practice=True)

    # NIL-86: expansion pairs append last (dark inventory, no trials).
    collect_expansion(words, presentations, pairings)

    validate_unique_constraints(words, presentations, pairings)
    validate_pair_provenance(words, pairings)
    return words, presentations, pairings


def validate_unique_constraints(
    words: OrderedDict[str, Word],
    presentations: list[Presentation],
    pairings: OrderedDict[str, Pairing],
) -> None:
    # 68 thesis/practice + 34 expansion words; 204 + 102 presentations; 34 thesis/practice
    # pairings + 21 expansion pairings (the expansion pairings carry no trials -- dark).
    if len(words) != 102:
        raise ValueError(f"Expected 102 words, found {len(words)}")
    if len(presentations) != 306:
        raise ValueError(f"Expected 306 presentations, found {len(presentations)}")
    if len(pairings) != 55:
        raise ValueError(f"Expected 55 pairings, found {len(pairings)}")

    # words UNIQUE(language_id, romaji): one language in v1, so romaji is the key.
    romaji_keys: dict[str, str] = {}
    for word in words.values():
        existing = romaji_keys.get(word.romaji)
        if existing:
            raise ValueError(f"Duplicate word romaji {word.romaji!r}: {existing} and {word.audio_file}")
        romaji_keys[word.romaji] = word.audio_file

    # presentations UNIQUE(word_id, condition_name): every word has exactly 3.
    conditions_by_word: dict[str, set[str]] = {}
    for presentation in presentations:
        if presentation.audio_file not in words:
            raise ValueError(f"Presentation references unknown word {presentation.audio_file}")
        seen = conditions_by_word.setdefault(presentation.audio_file, set())
        if presentation.condition_name in seen:
            raise ValueError(
                f"Duplicate presentation for {presentation.audio_file} in {presentation.condition_name}"
            )
        seen.add(presentation.condition_name)
    for audio_file, seen in conditions_by_word.items():
        if len(seen) != len(CONDITIONS):
            raise ValueError(f"Word {audio_file} has {len(seen)} presentations, expected {len(CONDITIONS)}")

    # pairings: UNIQUE(pair_code) is structural; assert the is_core invariant
    # (ADR-6: contrastive, same-modality real words) and the shuffle-order
    # invariant that "pair second = higher word id" resolves to word_b (k-word).
    word_ids = word_id_map(words)
    for pairing in pairings.values():
        for audio in (pairing.word_a_audio, pairing.word_b_audio, pairing.correct_audio):
            if audio not in words:
                raise ValueError(f"Pairing {pairing.pair_code} references unknown word {audio}")
        a = words[pairing.word_a_audio]
        b = words[pairing.word_b_audio]
        if not a.modality or a.modality != b.modality:
            raise ValueError(
                f"is_core pairing {pairing.pair_code} members disagree on modality "
                f"({a.modality} vs {b.modality})"
            )
        if a.gloss == b.gloss:
            raise ValueError(f"is_core pairing {pairing.pair_code} members are not contrastive (same gloss)")
        if word_ids[pairing.word_a_audio] >= word_ids[pairing.word_b_audio]:
            raise ValueError(f"Pairing {pairing.pair_code} word_a id must be < word_b id (shuffle-order invariant)")
        if pairing.correct_audio not in (pairing.word_a_audio, pairing.word_b_audio):
            raise ValueError(f"Pairing {pairing.pair_code} correct word is not a member")


def stimulus_from_row(row: dict[str, str], word_column: str, meaning_column: str | None) -> Stimulus:
    pairing, script, romaji = parse_stimulus_file(row[word_column])
    kana = romaji_to_hiragana(romaji)
    gloss = row[meaning_column] if meaning_column else ""

    if not gloss:
        if row[word_column] == row["word-a"]:
            gloss = row["meaning-a"]
        elif row[word_column] == row["word-b"]:
            gloss = row["meaning-b"]
        else:
            raise ValueError(f"No meaning found for {row[word_column]}")
    gloss = GLOSS_FIXES.get(gloss, gloss)

    hiragana_form, katakana_form = LONG_VOWEL_FORMS.get(romaji, (kana, to_katakana(kana)))
    code = script.upper()
    canonical_form = hiragana_form if code[0] == "H" else katakana_form
    if code[1] == "H":
        display_form = hiragana_form
    elif code[1] == "K":
        display_form = katakana_form
    else:
        # u/d audio-only rows: the word is only ever shown at feedback reveal.
        display_form = canonical_form

    return Stimulus(
        kana=kana,
        display_form=display_form,
        canonical_form=canonical_form,
        romaji=romaji,
        gloss=gloss,
        canonical_script=code,
        stimulus_file=row[word_column],
        audio_file=f"audio/{pairing}{code[0].lower()}-{romaji}.m4a",
        modality=row["modality"].upper(),
    )


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_literal(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):            # bool before int (bool is a subclass of int)
        return "1" if value else "0"
    if isinstance(value, int):
        return str(value)
    if isinstance(value, float):           # thesis_accuracy DECIMAL(5,4)
        return f"{value:.4f}"
    if isinstance(value, Decimal):         # foil_distance DECIMAL(6,4)
        return f"{value:.4f}"
    return sql_string(str(value))


def sql_row(values: tuple[object, ...]) -> str:
    return "(" + ", ".join(sql_literal(value) for value in values) + ")"


def insert_block(table: str, columns: list[str], rows: list[tuple[object, ...]]) -> list[str]:
    lines = [f"INSERT INTO {table} ({', '.join(columns)})", "VALUES"]
    lines.extend(with_sql_commas([sql_row(row) for row in rows]))
    return lines


def read_choosing(path: Path) -> list[ChoosingTrial]:
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        trials = [
            ChoosingTrial(
                participant=row["Participant Private ID"],
                condition_name=CONDITION_BY_SPREADSHEET[row["Current Spreadsheet"]],
                pair_code=row["Spreadsheet: pairing"],
                selected_file=row["Response"],
                answer_file=row["Spreadsheet: word-answer"],
                correct=row["Correct"] == "1",
                response_time_ms=round(float(row["Reaction Time"])),
            )
            for row in csv.DictReader(source)
        ]
    if len(trials) != THESIS_ROW_COUNT:
        raise ValueError(f"Expected {THESIS_ROW_COUNT} choosing rows in {path.name}, found {len(trials)}")
    return trials


def read_rating(path: Path) -> list[RatingRecord]:
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        records = [
            RatingRecord(
                participant=row["Participant Private ID"],
                pair_code=row["Spreadsheet: pairing"],
                rated_file=row["Spreadsheet: rated-word"],
                rating=int(row["rating"]),
                response_time_ms=round(float(row["Reaction Time"])),
            )
            for row in csv.DictReader(source)
        ]
    if len(records) != THESIS_ROW_COUNT:
        raise ValueError(f"Expected {THESIS_ROW_COUNT} rating rows in {path.name}, found {len(records)}")
    return records


def participant_order(choosing: list[ChoosingTrial]) -> list[str]:
    # Distinct participants sorted numerically by their Gorilla private id ->
    # ordinal n in 1..36: username thesis_p{n:02d}, session id n, user id n + 1.
    # Deterministic (no randomness/datetime), so --check stays byte-exact.
    ordered = sorted({trial.participant for trial in choosing}, key=float)
    if len(ordered) != THESIS_USER_COUNT:
        raise ValueError(f"Expected {THESIS_USER_COUNT} thesis participants, found {len(ordered)}")
    return ordered


def resolve_word_id(stimulus_file: str, id_by_romaji: dict[str, int]) -> int:
    # A Gorilla stimulus .mp4 -> the word id it names. The 4 sokuon words carry a
    # doubled-consonant romaji in the export that the alias folds to words.romaji.
    _pairing, _script, romaji = parse_stimulus_file(stimulus_file)
    romaji = SOKUON_ROMAJI_ALIASES.get(romaji, romaji)
    return id_by_romaji[romaji]


def validate_thesis(
    choosing: list[ChoosingTrial],
    ratings: list[RatingRecord],
    words: OrderedDict[str, Word],
    pairings: OrderedDict[str, Pairing],
) -> None:
    # Self-validating ingestion: every choosing row must reconstruct a pairing
    # member for its selection, the thesis fixed target (== trials.correct_word_id)
    # for its answer, and a derived correctness that agrees with the CSV Correct
    # column; every rated word must be a pairing member. Any future data/ordering
    # drift fails the generator loudly rather than seeding a wrong row.
    word_ids = word_id_map(words)
    id_by_romaji = {word.romaji: word_ids[audio_file] for audio_file, word in words.items()}
    members = {code: {word_ids[p.word_a_audio], word_ids[p.word_b_audio]} for code, p in pairings.items()}
    correct_word = {code: word_ids[p.correct_audio] for code, p in pairings.items()}

    order = participant_order(choosing)
    condition_by_participant: dict[str, str] = {}
    for trial in choosing:
        seen = condition_by_participant.setdefault(trial.participant, trial.condition_name)
        if seen != trial.condition_name:
            raise ValueError(f"Participant {trial.participant} spans multiple conditions")

    for trial in choosing:
        if trial.pair_code not in members:
            raise ValueError(f"Choosing pairing {trial.pair_code!r} has no seeded pairing")
        selected = resolve_word_id(trial.selected_file, id_by_romaji)
        target = resolve_word_id(trial.answer_file, id_by_romaji)
        if selected not in members[trial.pair_code]:
            raise ValueError(f"Selected {trial.selected_file} is not a member of {trial.pair_code}")
        if target != correct_word[trial.pair_code]:
            raise ValueError(f"Answer {trial.answer_file} is not the trial target for {trial.pair_code}")
        if (selected == target) != trial.correct:
            raise ValueError(f"Derived is_correct disagrees with CSV Correct for {trial.pair_code}")

    rating_participants = {record.participant for record in ratings}
    if not rating_participants <= set(order):
        raise ValueError("Rating participants are not a subset of the choosing participants")
    for record in ratings:
        if record.pair_code not in members:
            raise ValueError(f"Rating pairing {record.pair_code!r} has no seeded pairing")
        word = resolve_word_id(record.rated_file, id_by_romaji)
        if word not in members[record.pair_code]:
            raise ValueError(f"Rated {record.rated_file} is not a member of {record.pair_code}")
        if not (1 <= record.rating <= 7):
            raise ValueError(f"Rating {record.rating} out of range for {record.rated_file}")


def build_thesis_app_user_rows(order: list[str]) -> list[tuple[object, ...]]:
    return [
        (
            ordinal + 1,                                    # user id (admin is 1)
            f"thesis_p{ordinal:02d}",
            f"thesis_p{ordinal:02d}@thesis.invalid",
            THESIS_PASSWORD_HASH,
            THESIS_ROLE,
        )
        for ordinal, _participant in enumerate(order, start=1)
    ]


def build_thesis_session_rows(
    order: list[str], condition_by_participant: dict[str, str]
) -> list[tuple[object, ...]]:
    rows = []
    for ordinal, participant in enumerate(order, start=1):
        session_uuid = str(uuid.uuid5(THESIS_UUID_NAMESPACE, f"thesis_p{ordinal:02d}"))
        # shuffle_seed is required NOT NULL but inert here: only position-bias
        # replays it, and the thesis cohort is excluded from that aggregate. The
        # participant's private id gives a stable, unique BIGINT.
        rows.append((ordinal, session_uuid, ordinal + 1, condition_by_participant[participant], int(float(participant))))
    return rows


def build_thesis_answer_rows(
    choosing: list[ChoosingTrial],
    session_by_participant: dict[str, int],
    pairing_ids: dict[str, int],
    id_by_romaji: dict[str, int],
) -> list[tuple[object, ...]]:
    by_session: dict[int, list[tuple[int, int, int, bool, int]]] = {}
    for trial in choosing:
        selected = resolve_word_id(trial.selected_file, id_by_romaji)
        target = resolve_word_id(trial.answer_file, id_by_romaji)
        session_id = session_by_participant[trial.participant]
        trial_id = pairing_ids[trial.pair_code]  # trial id == pairing id (1:1 for core)
        by_session.setdefault(session_id, []).append(
            (trial_id, selected, target, trial.correct, trial.response_time_ms)
        )
    rows = []
    answer_id = 1
    for session_id in sorted(by_session):
        for trial_id, selected, target, correct, response_time_ms in sorted(by_session[session_id]):
            rows.append((answer_id, session_id, trial_id, selected, target, correct, response_time_ms))
            answer_id += 1
    return rows


def build_thesis_rating_rows(
    ratings: list[RatingRecord],
    session_by_participant: dict[str, int],
    id_by_romaji: dict[str, int],
) -> list[tuple[object, ...]]:
    by_session: dict[int, list[tuple[int, int, int]]] = {}
    for record in ratings:
        word = resolve_word_id(record.rated_file, id_by_romaji)
        session_id = session_by_participant[record.participant]
        by_session.setdefault(session_id, []).append((word, record.rating, record.response_time_ms))
    rows = []
    rating_id = 1
    for session_id in sorted(by_session):
        for word, rating, response_time_ms in sorted(by_session[session_id]):
            # user id = session id + 1 (admin is user 1); session_id kept for
            # provenance (nullable, unread by any aggregate).
            rows.append((rating_id, session_id + 1, word, session_id, rating, response_time_ms))
            rating_id += 1
    return rows


def render_sql(
    words: OrderedDict[str, Word],
    presentations: list[Presentation],
    pairings: OrderedDict[str, Pairing],
    choosing: list[ChoosingTrial],
    ratings: list[RatingRecord],
) -> str:
    word_ids = word_id_map(words)
    pairing_ids = {code: index for index, code in enumerate(pairings.keys(), start=1)}
    id_by_romaji = {word.romaji: word_ids[audio_file] for audio_file, word in words.items()}
    order = participant_order(choosing)
    session_by_participant = {participant: ordinal for ordinal, participant in enumerate(order, start=1)}
    condition_by_participant = {trial.participant: trial.condition_name for trial in choosing}

    lines = [
        "CREATE DATABASE IF NOT EXISTS ideophone_arena",
        "    DEFAULT CHARACTER SET utf8mb4",
        "    DEFAULT COLLATE utf8mb4_unicode_ci;",
        "",
        "USE ideophone_arena;",
        "",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "DROP TABLE IF EXISTS ratings;",
        "DROP TABLE IF EXISTS player_answers;",
        "DROP TABLE IF EXISTS game_sessions;",
        "DROP TABLE IF EXISTS trials;",
        "DROP TABLE IF EXISTS pairings;",
        "DROP TABLE IF EXISTS presentations;",
        "DROP TABLE IF EXISTS words;",
        "DROP TABLE IF EXISTS languages;",
        "DROP TABLE IF EXISTS app_users;",
        "SET FOREIGN_KEY_CHECKS = 1;",
        "",
        "CREATE TABLE app_users (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    username VARCHAR(50) NOT NULL",
        "        UNIQUE,",
        "    email VARCHAR(255) NOT NULL",
        "        UNIQUE,",
        "    password_hash VARCHAR(255) NOT NULL,",
        "    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',",
        "    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,",
        "",
        "    PRIMARY KEY (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- ADR-1: language as a first-class entity; every v1 word backfills to jpn.",
        "CREATE TABLE languages (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    iso_code VARCHAR(8) NOT NULL",
        "        UNIQUE,",
        "    name VARCHAR(50) NOT NULL,",
        "    family VARCHAR(50),",
        "    player_note VARCHAR(255),",
        "",
        "    PRIMARY KEY (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- ADR-0: word identity as an entity. One row per word (68); the per-word",
        "-- shared audio (invariant 2) lives here once, not duplicated per condition.",
        "CREATE TABLE words (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    language_id BIGINT NOT NULL,",
        "    romaji VARCHAR(100) NOT NULL,",
        "    kana VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,",
        "    canonical_form VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,",
        "    canonical_script VARCHAR(20) NOT NULL,",
        "    gloss VARCHAR(255) NOT NULL,",
        "    modality VARCHAR(50),",
        "    semantic_category VARCHAR(20),",
        "    stimulus_file VARCHAR(100) NOT NULL,",
        "",
        "    PRIMARY KEY (id),",
        "    UNIQUE (language_id, romaji),",
        "",
        "    CONSTRAINT fk_words_language",
        "        FOREIGN KEY (language_id)",
        "            REFERENCES languages (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- ADR-0: the script manipulation, and nothing else. One row per word x",
        "-- scripted condition (204); display_form is verbatim (invariant 1).",
        "CREATE TABLE presentations (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    word_id BIGINT NOT NULL,",
        "    condition_name VARCHAR(50) NOT NULL,",
        "    display_form VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,",
        "    script_code VARCHAR(2) NOT NULL,",
        "",
        "    PRIMARY KEY (id),",
        "    UNIQUE (word_id, condition_name),",
        "",
        "    CONSTRAINT fk_presentations_word",
        "        FOREIGN KEY (word_id)",
        "            REFERENCES words (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- ADR-6: the durable pair-level inventory. Members are FKs to words; the",
        "-- thesis backfill sets is_core, source, and per-pair thesis_accuracy.",
        "CREATE TABLE pairings (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    pair_code VARCHAR(20) NOT NULL",
        "        UNIQUE,",
        "    language_id BIGINT NOT NULL,",
        "    word_a_id BIGINT NOT NULL,",
        "    word_b_id BIGINT NOT NULL,",
        "    modality VARCHAR(50),",
        "    is_core BOOLEAN NOT NULL,",
        "    source VARCHAR(30) NOT NULL,",
        "    difficulty_prior VARCHAR(10),",
        "    thesis_accuracy DECIMAL(5,4),",
        "    foil_distance DECIMAL(6,4),",
        "    signoff_ref VARCHAR(100),",
        "    approved_at DATE,",
        "",
        "    PRIMARY KEY (id),",
        "",
        "    CONSTRAINT fk_pairings_language",
        "        FOREIGN KEY (language_id)",
        "            REFERENCES languages (id),",
        "",
        "    CONSTRAINT fk_pairings_word_a",
        "        FOREIGN KEY (word_a_id)",
        "            REFERENCES words (id),",
        "",
        "    CONSTRAINT fk_pairings_word_b",
        "        FOREIGN KEY (word_b_id)",
        "            REFERENCES words (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- ADR-3: one trials table, condition collapsed (102 rounds -> 34 trials).",
        "-- correct_word_id documents the thesis fixed target (unread for CHOOSING).",
        "CREATE TABLE trials (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    round_type VARCHAR(20) NOT NULL DEFAULT 'CHOOSING',",
        "    pairing_id BIGINT NOT NULL,",
        "    correct_word_id BIGINT NULL,",
        "    feature_axis VARCHAR(10) NULL,",
        "    is_practice BOOLEAN NOT NULL DEFAULT FALSE,",
        "",
        "    PRIMARY KEY (id),",
        "",
        "    CONSTRAINT fk_trials_pairing",
        "        FOREIGN KEY (pairing_id)",
        "            REFERENCES pairings (id),",
        "",
        "    CONSTRAINT fk_trials_correct_word",
        "        FOREIGN KEY (correct_word_id)",
        "            REFERENCES words (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "CREATE TABLE game_sessions (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    session_uuid CHAR(36) NOT NULL",
        "        UNIQUE,",
        "    user_id BIGINT NOT NULL,",
        "    difficulty_level INT NOT NULL DEFAULT 1,",
        "    condition_name VARCHAR(50) NOT NULL DEFAULT 'TEXT_ONLY',",
        "    include_practice BOOLEAN NOT NULL DEFAULT FALSE,",
        "    practice_answered INT NOT NULL DEFAULT 0,",
        "    shuffle_seed BIGINT NOT NULL,",
        "    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,",
        "    completed_at TIMESTAMP NULL,",
        "",
        "    PRIMARY KEY (id),",
        "",
        "    CONSTRAINT fk_game_sessions_user",
        "        FOREIGN KEY (user_id)",
        "            REFERENCES app_users (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- ADR-0/ADR-3: answers re-key to word grain (selected/target words) and",
        "-- reference the collapsed trial. UNIQUE(session_id, trial_id) carries the 409.",
        "CREATE TABLE player_answers (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    session_id BIGINT NOT NULL,",
        "    trial_id BIGINT NOT NULL,",
        "    selected_word_id BIGINT NOT NULL,",
        "    target_word_id BIGINT NOT NULL,",
        "    is_correct BOOLEAN NOT NULL,",
        "    response_time_ms INT,",
        "    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,",
        "",
        "    PRIMARY KEY (id),",
        "    UNIQUE (session_id, trial_id),",
        "",
        "    CONSTRAINT fk_answers_session",
        "        FOREIGN KEY (session_id)",
        "            REFERENCES game_sessions (id),",
        "",
        "    CONSTRAINT fk_answers_trial",
        "        FOREIGN KEY (trial_id)",
        "            REFERENCES trials (id),",
        "",
        "    CONSTRAINT fk_answers_selected_word",
        "        FOREIGN KEY (selected_word_id)",
        "            REFERENCES words (id),",
        "",
        "    CONSTRAINT fk_answers_target_word",
        "        FOREIGN KEY (target_word_id)",
        "            REFERENCES words (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- Standalone 1-7 iconicity rating per word per user (thesis Rating Task).",
        "-- ADR-0: word-keyed UNIQUE(user_id, word_id) -- the grain the instrument",
        "-- means -- so one rating per word holds across conditions at the DB layer;",
        "-- the nullable session_id keeps provenance without coupling to a session.",
        "CREATE TABLE ratings (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    user_id BIGINT NOT NULL,",
        "    word_id BIGINT NOT NULL,",
        "    session_id BIGINT NULL,",
        "    rating SMALLINT NOT NULL,",
        "    response_time_ms INT,",
        "    rated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,",
        "",
        "    PRIMARY KEY (id),",
        "    UNIQUE (user_id, word_id),",
        "",
        "    CONSTRAINT fk_ratings_user",
        "        FOREIGN KEY (user_id)",
        "            REFERENCES app_users (id),",
        "",
        "    CONSTRAINT fk_ratings_word",
        "        FOREIGN KEY (word_id)",
        "            REFERENCES words (id),",
        "",
        "    CONSTRAINT fk_ratings_session",
        "        FOREIGN KEY (session_id)",
        "            REFERENCES game_sessions (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- Reference data + trial content generated from src/main/resources/condition-*-choosing-sokuon.csv.",
        "-- NIL-86: 21 expansion pairs (docs/research/data/stimulus-expansion-*.csv) seeded as dark",
        "-- inventory -- words/presentations/pairings but no trials, so they serve in zero live pools",
        "-- until their TTS audio lands. Every jpn stimulus is ja-JP-Wavenet-B TTS; per-stimulus",
        "-- provenance lives in scripts/tts-manifest.json until stimulus_sources (M5).",
    ]

    language_rows = list(LANGUAGES)
    word_rows = [
        (
            word_ids[audio_file],
            JPN_LANGUAGE_ID,
            word.romaji,
            word.kana,
            word.canonical_form,
            word.canonical_script,
            word.gloss,
            word.modality,
            None,             # semantic_category: jpn v1 has none (never unified with modality)
            word.audio_file,  # stimulus_file: the per-word shared audio
        )
        for audio_file, word in words.items()
    ]
    presentation_rows = [
        (
            index,
            word_ids[presentation.audio_file],
            presentation.condition_name,
            presentation.display_form,
            presentation.script_code,
        )
        for index, presentation in enumerate(presentations, start=1)
    ]
    pairing_rows = []
    for code, pairing in pairings.items():
        correct_count = THESIS_PAIR_ACCURACY.get(code)
        thesis_accuracy = None if correct_count is None else correct_count / THESIS_N
        pairing_rows.append(
            (
                pairing_ids[code],
                code,
                JPN_LANGUAGE_ID,
                word_ids[pairing.word_a_audio],
                word_ids[pairing.word_b_audio],
                pairing.modality,
                True,       # is_core: real contrastive same-modality pairs (invariant 4)
                pairing.source,             # THESIS (default) or EXPANSION
                pairing.difficulty_prior,   # thesis pairs None (use thesis_accuracy); expansion sets it
                thesis_accuracy,            # None for expansion codes
                pairing.foil_distance,      # None for thesis; computed for expansion
                pairing.signoff_ref,        # None for thesis; workbook ref for expansion
                pairing.approved_at,        # None for thesis; sign-off date for expansion
            )
        )
    # Expansion pairings emit NO trial (dark): a pairing without a trial is unserved
    # (ADR-3), so round generation -- which is trial-driven -- never reaches them.
    trial_rows = [
        (
            pairing_ids[code],  # trial id == pairing id (1:1 today, same order)
            "CHOOSING",
            pairing_ids[code],
            word_ids[pairing.correct_audio],
            None,               # feature_axis: TEMPLATE only
            pairing.practice,   # is_practice
        )
        for code, pairing in pairings.items()
        if pairing.source != EXPANSION_SOURCE
    ]

    lines.extend(insert_block("languages", ["id", "iso_code", "name", "family", "player_note"], language_rows))
    lines.append("")
    lines.extend(insert_block(
        "words",
        ["id", "language_id", "romaji", "kana", "canonical_form", "canonical_script", "gloss", "modality",
         "semantic_category", "stimulus_file"],
        word_rows,
    ))
    lines.append("")
    lines.extend(insert_block(
        "presentations",
        ["id", "word_id", "condition_name", "display_form", "script_code"],
        presentation_rows,
    ))
    lines.append("")
    lines.extend(insert_block(
        "pairings",
        ["id", "pair_code", "language_id", "word_a_id", "word_b_id", "modality", "is_core", "source",
         "difficulty_prior", "thesis_accuracy", "foil_distance", "signoff_ref", "approved_at"],
        pairing_rows,
    ))
    lines.append("")
    lines.extend(insert_block(
        "trials",
        ["id", "round_type", "pairing_id", "correct_word_id", "feature_axis", "is_practice"],
        trial_rows,
    ))
    lines.append("")
    lines.append('-- Dev-only admin account (id 1). Throwaway password; see docs/demo-runbook.md,')
    lines.append('-- "Creating an admin". Followed by the NIL-54 thesis cohort (thesis_p01..thesis_p36,')
    lines.append("-- ids 2-37): accounts that never authenticate (shared frozen digest) and are fenced")
    lines.append("-- off from the live Rider A research aggregates by the reserved thesis_p% prefix.")
    app_user_rows = [(1, ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD_HASH, ADMIN_ROLE)]
    app_user_rows.extend(build_thesis_app_user_rows(order))
    lines.extend(insert_block(
        "app_users",
        ["id", "username", "email", "password_hash", "role"],
        app_user_rows,
    ))
    lines.append("")
    lines.append("-- NIL-54: thesis Gorilla tidy-data (36 participants, 30 trials each). Sessions carry")
    lines.append("-- completed_at NULL (never completed) so answers feed divergence but never the")
    lines.append("-- leaderboard; started_at/completed_at/answered_at/rated_at take their DDL defaults.")
    lines.extend(insert_block(
        "game_sessions",
        ["id", "session_uuid", "user_id", "condition_name", "shuffle_seed"],
        build_thesis_session_rows(order, condition_by_participant),
    ))
    lines.append("")
    lines.extend(insert_block(
        "player_answers",
        ["id", "session_id", "trial_id", "selected_word_id", "target_word_id", "is_correct", "response_time_ms"],
        build_thesis_answer_rows(choosing, session_by_participant, pairing_ids, id_by_romaji),
    ))
    lines.append("")
    lines.extend(insert_block(
        "ratings",
        ["id", "user_id", "word_id", "session_id", "rating", "response_time_ms"],
        build_thesis_rating_rows(ratings, session_by_participant, id_by_romaji),
    ))
    lines.append("")

    return "\n".join(lines)


def with_sql_commas(values: list[str]) -> list[str]:
    return [value + ("," if index < len(values) - 1 else ";") for index, value in enumerate(values)]


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate the Ideophone Arena MySQL schema and seed SQL.")
    parser.add_argument("--check", action="store_true", help="verify that ideophone_arena.sql is up to date")
    args = parser.parse_args()

    words, presentations, pairings = collect_data()
    choosing = read_choosing(CHOOSING_CSV)
    ratings = read_rating(RATING_CSV)
    validate_thesis(choosing, ratings, words, pairings)
    rendered = render_sql(words, presentations, pairings, choosing, ratings)
    expansion_pairs = [p for p in pairings.values() if p.source == EXPANSION_SOURCE]
    thesis_pairs = [p for p in pairings.values() if p.source != EXPANSION_SOURCE]
    thesis_member_audios = {a for p in thesis_pairs for a in (p.word_a_audio, p.word_b_audio)}
    mixed = sum(
        1 for p in expansion_pairs
        if p.word_a_audio in thesis_member_audios or p.word_b_audio in thesis_member_audios
    )
    summary = (
        f"{len(words)} words, {len(presentations)} presentations, {len(pairings)} pairings "
        f"({len(expansion_pairs)} expansion dark, {mixed} mixed thesis x expansion, provenance-homogeneous), "
        f"{len(thesis_pairs)} trials, {THESIS_USER_COUNT} thesis users, "
        f"{len(choosing)} answers, {len(ratings)} ratings"
    )

    if args.check:
        current = OUTPUT_PATH.read_text(encoding="utf-8") if OUTPUT_PATH.exists() else ""
        if current != rendered:
            print(f"{OUTPUT_PATH} is not up to date", file=sys.stderr)
            return 1
        print(f"SQL is up to date: {summary}")
        return 0

    OUTPUT_PATH.write_text(rendered, encoding="utf-8")
    print(f"Wrote {OUTPUT_PATH}: {summary}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
