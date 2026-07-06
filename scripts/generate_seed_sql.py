#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
import sys
from collections import OrderedDict
from dataclasses import dataclass
from pathlib import Path


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

    validate_unique_constraints(words, presentations, pairings)
    return words, presentations, pairings


def validate_unique_constraints(
    words: OrderedDict[str, Word],
    presentations: list[Presentation],
    pairings: OrderedDict[str, Pairing],
) -> None:
    if len(words) != 68:
        raise ValueError(f"Expected 68 words, found {len(words)}")
    if len(presentations) != 204:
        raise ValueError(f"Expected 204 presentations, found {len(presentations)}")
    if len(pairings) != 34:
        raise ValueError(f"Expected 34 pairings, found {len(pairings)}")

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
    return sql_string(str(value))


def sql_row(values: tuple[object, ...]) -> str:
    return "(" + ", ".join(sql_literal(value) for value in values) + ")"


def insert_block(table: str, columns: list[str], rows: list[tuple[object, ...]]) -> list[str]:
    lines = [f"INSERT INTO {table} ({', '.join(columns)})", "VALUES"]
    lines.extend(with_sql_commas([sql_row(row) for row in rows]))
    return lines


def render_sql(
    words: OrderedDict[str, Word],
    presentations: list[Presentation],
    pairings: OrderedDict[str, Pairing],
) -> str:
    word_ids = word_id_map(words)
    pairing_ids = {code: index for index, code in enumerate(pairings.keys(), start=1)}

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
                "THESIS",   # source
                None,       # difficulty_prior: thesis pairs use thesis_accuracy
                thesis_accuracy,
                None,       # foil_distance
                None,       # signoff_ref
                None,       # approved_at
            )
        )
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
    lines.append('-- Dev-only admin account. Throwaway password; see docs/demo-runbook.md, "Creating an admin".')
    lines.extend(insert_block(
        "app_users",
        ["id", "username", "email", "password_hash", "role"],
        [(1, ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD_HASH, ADMIN_ROLE)],
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
    rendered = render_sql(words, presentations, pairings)
    summary = f"{len(words)} words, {len(presentations)} presentations, {len(pairings)} pairings/trials"

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
