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


@dataclass(frozen=True)
class Stimulus:
    kana: str
    romaji: str
    gloss: str
    canonical_script: str
    stimulus_file: str
    modality: str


@dataclass(frozen=True)
class Round:
    prompt: str
    left_stimulus_file: str
    right_stimulus_file: str
    correct_stimulus_file: str
    condition_name: str


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


def read_trial_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        return [row for row in reader if row.get("display") == "trial"]


def collect_data() -> tuple[OrderedDict[str, Stimulus], list[Round]]:
    ideophones: OrderedDict[str, Stimulus] = OrderedDict()
    rounds: list[Round] = []

    for _condition_number, condition_name, path in CONDITIONS:
        if not path.exists():
            raise FileNotFoundError(path)

        trial_rows = read_trial_rows(path)
        if len(trial_rows) != 30:
            raise ValueError(f"Expected 30 trial rows in {path.name}, found {len(trial_rows)}")

        for row in trial_rows:
            left = stimulus_from_row(row, "word-a", "meaning-a")
            right = stimulus_from_row(row, "word-b", "meaning-b")
            correct = stimulus_from_row(row, "word-answer", None)

            if correct.stimulus_file not in {left.stimulus_file, right.stimulus_file}:
                raise ValueError(f"Correct answer {row['word-answer']} is not one of the choices in {path.name}")

            for stimulus in (left, right):
                existing = ideophones.get(stimulus.stimulus_file)
                if existing is None:
                    ideophones[stimulus.stimulus_file] = stimulus
                elif existing != stimulus:
                    raise ValueError(f"Conflicting data for {stimulus.stimulus_file}: {existing} vs {stimulus}")

            rounds.append(
                Round(
                    prompt=row["meaning-prompt"],
                    left_stimulus_file=left.stimulus_file,
                    right_stimulus_file=right.stimulus_file,
                    correct_stimulus_file=correct.stimulus_file,
                    condition_name=condition_name,
                )
            )

    validate_unique_constraints(ideophones)
    return ideophones, rounds


def validate_unique_constraints(ideophones: OrderedDict[str, Stimulus]) -> None:
    kana_script_keys: dict[tuple[str, str], str] = {}

    for stimulus in ideophones.values():
        kana_script_key = (stimulus.kana, stimulus.canonical_script)
        existing_file = kana_script_keys.get(kana_script_key)
        if existing_file:
            raise ValueError(
                f"Duplicate kana/script seed key {kana_script_key}: {existing_file} and {stimulus.stimulus_file}"
            )
        kana_script_keys[kana_script_key] = stimulus.stimulus_file


def stimulus_from_row(row: dict[str, str], word_column: str, meaning_column: str | None) -> Stimulus:
    _pairing, script, romaji = parse_stimulus_file(row[word_column])
    kana = romaji_to_hiragana(romaji)
    gloss = row[meaning_column] if meaning_column else ""

    if not gloss:
        if row[word_column] == row["word-a"]:
            gloss = row["meaning-a"]
        elif row[word_column] == row["word-b"]:
            gloss = row["meaning-b"]
        else:
            raise ValueError(f"No meaning found for {row[word_column]}")

    return Stimulus(
        kana=kana,
        romaji=romaji,
        gloss=gloss,
        canonical_script=script.upper(),
        stimulus_file=row[word_column],
        modality=row["modality"].upper(),
    )


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def sql_insert_values(values: tuple[object, ...]) -> str:
    escaped = [str(value) if isinstance(value, int) else sql_string(str(value)) for value in values]
    return "(" + ", ".join(escaped) + ")"


def render_sql(ideophones: OrderedDict[str, Stimulus], rounds: list[Round]) -> str:
    ideophone_ids = {stimulus_file: index for index, stimulus_file in enumerate(ideophones.keys(), start=1)}

    lines = [
        "CREATE DATABASE IF NOT EXISTS ideophone_arena",
        "    DEFAULT CHARACTER SET utf8mb4",
        "    DEFAULT COLLATE utf8mb4_unicode_ci;",
        "",
        "USE ideophone_arena;",
        "",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "DROP TABLE IF EXISTS player_answers;",
        "DROP TABLE IF EXISTS game_sessions;",
        "DROP TABLE IF EXISTS arena_rounds;",
        "DROP TABLE IF EXISTS ideophones;",
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
        "CREATE TABLE ideophones (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    kana VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,",
        "    romaji VARCHAR(100) NOT NULL,",
        "    gloss VARCHAR(255) NOT NULL,",
        "    canonical_script VARCHAR(20) NOT NULL,",
        "    stimulus_file VARCHAR(100) NOT NULL",
        "        UNIQUE,",
        "    modality VARCHAR(50),",
        "",
        "    PRIMARY KEY (id),",
        "    UNIQUE (kana, canonical_script)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "CREATE TABLE arena_rounds (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    prompt VARCHAR(255) NOT NULL,",
        "    left_ideophone_id BIGINT NOT NULL,",
        "    right_ideophone_id BIGINT NOT NULL,",
        "    correct_ideophone_id BIGINT NOT NULL,",
        "    condition_name VARCHAR(50) NOT NULL DEFAULT 'TEXT_ONLY',",
        "    difficulty_level INT NOT NULL DEFAULT 1,",
        "",
        "    PRIMARY KEY (id),",
        "",
        "    CONSTRAINT fk_round_left_ideophone",
        "        FOREIGN KEY (left_ideophone_id)",
        "            REFERENCES ideophones (id),",
        "",
        "    CONSTRAINT fk_round_right_ideophone",
        "        FOREIGN KEY (right_ideophone_id)",
        "            REFERENCES ideophones (id),",
        "",
        "    CONSTRAINT fk_round_correct_ideophone",
        "        FOREIGN KEY (correct_ideophone_id)",
        "            REFERENCES ideophones (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "CREATE TABLE game_sessions (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    session_uuid CHAR(36) NOT NULL",
        "        UNIQUE,",
        "    user_id BIGINT NOT NULL,",
        "    difficulty_level INT NOT NULL DEFAULT 1,",
        "    condition_name VARCHAR(50) NOT NULL DEFAULT 'TEXT_ONLY',",
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
        "CREATE TABLE player_answers (",
        "    id BIGINT NOT NULL AUTO_INCREMENT,",
        "    session_id BIGINT NOT NULL,",
        "    round_id BIGINT NOT NULL,",
        "    selected_ideophone_id BIGINT NOT NULL,",
        "    is_correct BOOLEAN NOT NULL,",
        "    response_time_ms INT,",
        "    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,",
        "",
        "    PRIMARY KEY (id),",
        "    UNIQUE (session_id, round_id),",
        "",
        "    CONSTRAINT fk_answers_session",
        "        FOREIGN KEY (session_id)",
        "            REFERENCES game_sessions (id),",
        "",
        "    CONSTRAINT fk_answers_round",
        "        FOREIGN KEY (round_id)",
        "            REFERENCES arena_rounds (id),",
        "",
        "    CONSTRAINT fk_answers_selected_ideophone",
        "        FOREIGN KEY (selected_ideophone_id)",
        "            REFERENCES ideophones (id)",
        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;",
        "",
        "-- Seed data generated from src/main/resources/condition-*-choosing-sokuon.csv.",
        "INSERT INTO ideophones (id, kana, romaji, gloss, canonical_script, stimulus_file, modality)",
        "VALUES",
    ]

    ideophone_values = []
    for ideophone_id, stimulus in enumerate(ideophones.values(), start=1):
        ideophone_values.append(
            sql_insert_values(
                (
                    ideophone_id,
                    stimulus.kana,
                    stimulus.romaji,
                    stimulus.gloss,
                    stimulus.canonical_script,
                    stimulus.stimulus_file,
                    stimulus.modality,
                )
            )
        )
    lines.extend(with_sql_commas(ideophone_values))
    lines.append("")
    lines.append("INSERT INTO arena_rounds (id, prompt, left_ideophone_id, right_ideophone_id, correct_ideophone_id, condition_name, difficulty_level)")
    lines.append("VALUES")

    round_values = []
    for round_id, round_data in enumerate(rounds, start=1):
        round_values.append(
            sql_insert_values(
                (
                    round_id,
                    round_data.prompt,
                    ideophone_ids[round_data.left_stimulus_file],
                    ideophone_ids[round_data.right_stimulus_file],
                    ideophone_ids[round_data.correct_stimulus_file],
                    round_data.condition_name,
                    1,
                )
            )
        )
    lines.extend(with_sql_commas(round_values))
    lines.append("")

    return "\n".join(lines)


def with_sql_commas(values: list[str]) -> list[str]:
    return [value + ("," if index < len(values) - 1 else ";") for index, value in enumerate(values)]


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate the Ideophone Arena MySQL schema and seed SQL.")
    parser.add_argument("--check", action="store_true", help="verify that ideophone_arena.sql is up to date")
    args = parser.parse_args()

    ideophones, rounds = collect_data()
    rendered = render_sql(ideophones, rounds)

    if args.check:
        current = OUTPUT_PATH.read_text(encoding="utf-8") if OUTPUT_PATH.exists() else ""
        if current != rendered:
            print(f"{OUTPUT_PATH} is not up to date", file=sys.stderr)
            return 1
        print(f"SQL is up to date: {len(ideophones)} ideophones, {len(rounds)} rounds")
        return 0

    OUTPUT_PATH.write_text(rendered, encoding="utf-8")
    print(f"Wrote {OUTPUT_PATH}: {len(ideophones)} ideophones, {len(rounds)} rounds")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
