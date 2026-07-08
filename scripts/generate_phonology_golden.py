#!/usr/bin/env python3
"""Emit docs/research/phonology-golden.json: the committed reference for the
SPEC-free-form-entry section 5 feature/distance metric (ARCHITECTURE ADR-8.2).

This discharges the PYTHON side of the dual-implementation contract: it records the
per-word phonological features and the per-pair foil_distance the generator computes,
so NIL-62's Java PhonologyService can assert byte-for-byte parity against a frozen file
rather than re-deriving the metric from prose. Reuses generate_seed_sql so the golden
always matches the seed. Deterministic; --check verifies it is up to date.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPTS_DIR = REPO_ROOT / "scripts"
GOLDEN_PATH = REPO_ROOT / "docs" / "research" / "phonology-golden.json"

sys.path.insert(0, str(SCRIPTS_DIR))
import generate_seed_sql as seed  # noqa: E402

WEIGHTS = {
    "redup": 0.20,
    "sokuon": 0.15,
    "final_n": 0.10,
    "ri_suffix": 0.10,
    "voiced_onset": 0.15,
    "heavy_vowel": 0.15,
    "mora_count": 0.15,
}


def word_entry(word_id: int, romaji: str) -> dict:
    morae = seed.mora_segments(romaji)
    features = seed.feature_vector(romaji)
    heavy = sum(1 for mora in morae if mora not in ("N", "Q") and mora[-1] in ("o", "u"))
    light = sum(1 for mora in morae if mora not in ("N", "Q") and mora[-1] in ("i", "e"))
    return {
        "word_id": word_id,
        "romaji": romaji,
        "morae": morae,
        "mora_count": features["mora"],
        "redup": features["redup"],
        "sokuon": features["sokuon"],
        "final_n": features["final_n"],
        "ri_suffix": features["ri_suffix"],
        "voiced_onset": features["voiced_onset"],
        "heavy_vowels": heavy,   # numerator of heavy_vowel_ratio (denominator = heavy + light)
        "light_vowels": light,   # ratio is 0.5 when heavy + light == 0
    }


def render() -> str:
    words, _presentations, pairings = seed.collect_data()
    word_ids = seed.word_id_map(words)

    word_rows = [word_entry(word_ids[audio], word.romaji) for audio, word in words.items()]
    word_rows.sort(key=lambda row: row["word_id"])

    pair_rows = []
    for pairing in pairings.values():
        if pairing.source != seed.EXPANSION_SOURCE:
            continue
        romaji_a = words[pairing.word_a_audio].romaji
        romaji_b = words[pairing.word_b_audio].romaji
        pair_rows.append(
            {
                "pair_code": pairing.pair_code,
                "word_a": romaji_a,
                "word_b": romaji_b,
                "foil_distance": f"{pairing.foil_distance:.4f}",
            }
        )

    golden = {
        "spec": "SPEC-free-form-entry section 5 (feature distance); ARCHITECTURE ADR-8.2 golden reference",
        "voiced_onset_letters": ["g", "z", "d", "b"],
        "weights": WEIGHTS,
        "words": word_rows,
        "expansion_pairs": pair_rows,
    }
    return json.dumps(golden, ensure_ascii=False, indent=2) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Emit/verify the phonology golden file.")
    parser.add_argument("--check", action="store_true", help="verify phonology-golden.json is up to date")
    args = parser.parse_args()

    rendered = render()
    if args.check:
        current = GOLDEN_PATH.read_text(encoding="utf-8") if GOLDEN_PATH.exists() else ""
        if current != rendered:
            print(f"{GOLDEN_PATH} is not up to date", file=sys.stderr)
            return 1
        print("phonology-golden.json is up to date")
        return 0

    GOLDEN_PATH.write_text(rendered, encoding="utf-8")
    words = json.loads(rendered)
    print(f"Wrote {GOLDEN_PATH}: {len(words['words'])} words, {len(words['expansion_pairs'])} expansion pairs")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
