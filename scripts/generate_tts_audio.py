#!/usr/bin/env python3
"""Generate the per-word TTS audio for Ideophone Arena stimuli (GCP Text-to-Speech).

This session EMITS this script; it does NOT run the batch. Nils runs the batch himself
with his own GCP credentials (SPEC-tts-synthesis, NIL-87). The voice is pinned to
ja-JP-Wavenet-B (FEMALE) -- the standardized thesis/McLean voice -- so expansion audio
is continuous with the original experimental stimuli (invariant 2: one shared file per
word). Input text is words.canonical_form (verbatim kana; never derived/converted).

Modes
  --build-manifest   (re)build scripts/tts-manifest.json from the seed word data.
                     Existing words (audio already shipped) are recorded as "present";
                     the 34 expansion words are "pending". Deterministic; no network.
  --dry-run          print the planned synthesis requests for pending words; no network.
  --run              actually synthesize the pending words (calls GCP + ffmpeg). Reads a
                     FRESH access token each run (gcloud auth print-access-token) -- tokens
                     lapse in ~10 min, so they are never cached. Resumable: each word's
                     status is written back to the manifest as "ok"/"fail".
  --only <romaji>    restrict --dry-run/--run to a single word.
  (no args)          print a status summary of the manifest.

Stdlib only (urllib/subprocess/base64/json); no new dependencies.
"""
from __future__ import annotations

import argparse
import base64
import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPTS_DIR = REPO_ROOT / "scripts"
MANIFEST_PATH = SCRIPTS_DIR / "tts-manifest.json"

# Pinned voice + project (SPEC-tts-synthesis, ratified 2026-07-08). One voice for the
# whole set: one speaker identity across conditions and words.
VOICE = {"languageCode": "ja-JP", "name": "ja-JP-Wavenet-B", "ssmlGender": "FEMALE"}
PROJECT = "enduring-sign-501717-f4"
SYNTHESIZE_URL = "https://texttospeech.googleapis.com/v1/text:synthesize"

# Where per-word .m4a files are served from (cross-repo coupling, app.stimuli.locations).
AUDIO_DIR = Path("/code/js/ideophone-arena-web/stimuli/audio")

# Provenance strings (tts:gcp:<voice>:<YYYY-MM-DD>). The existing thesis audio is itself
# ja-JP-Wavenet-B TTS (2025-03-27 generation); the expansion batch is dated 2026-07-08.
EXISTING_PROVENANCE = "tts:gcp:ja-JP-Wavenet-B:2025-03-27"
EXPANSION_PROVENANCE = "tts:gcp:ja-JP-Wavenet-B:2026-07-08"
THESIS_WORD_COUNT = 68  # ids 1-68 already have shipped audio; ids 69+ are the expansion batch

MAX_TRIES = 5
BACKOFF_BASE_SECONDS = 1


def build_manifest() -> dict:
    # Reuse the seed generator so the word list, canonical_form (input text) and
    # stimulus_file always match the emitted seed exactly.
    sys.path.insert(0, str(SCRIPTS_DIR))
    import generate_seed_sql as seed

    words, _presentations, _pairings = seed.collect_data()
    word_ids = seed.word_id_map(words)

    entries = []
    for audio_file, word in words.items():
        word_id = word_ids[audio_file]
        existing = word_id <= THESIS_WORD_COUNT
        entries.append(
            {
                "word_id": word_id,
                "romaji": word.romaji,
                "stimulus_file": word.audio_file,
                "input_text": word.canonical_form,
                "char_count": len(word.canonical_form),
                "status": "present" if existing else "pending",
                "provenance": EXISTING_PROVENANCE if existing else EXPANSION_PROVENANCE,
            }
        )
    entries.sort(key=lambda entry: entry["word_id"])
    return {
        "voice": VOICE,
        "project": PROJECT,
        "audio_dir": str(AUDIO_DIR),
        "note": "This session emits the manifest; Nils runs the batch. status=present means "
        "audio already shipped; status=pending awaits synthesis. Provenance is splittable per row.",
        "words": entries,
    }


def load_manifest() -> dict:
    if not MANIFEST_PATH.exists():
        raise FileNotFoundError(f"{MANIFEST_PATH} missing -- run --build-manifest first")
    return json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))


def save_manifest(manifest: dict) -> None:
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def access_token() -> str:
    # Read a FRESH token per run; tokens lapse in ~10 min so they are never cached.
    result = subprocess.run(
        ["gcloud", "auth", "print-access-token"], capture_output=True, text=True, check=True
    )
    return result.stdout.strip()


def synthesize(text: str, token: str) -> bytes:
    payload = json.dumps(
        {"input": {"text": text}, "voice": VOICE, "audioConfig": {"audioEncoding": "LINEAR16"}}
    ).encode("utf-8")
    request = urllib.request.Request(
        SYNTHESIZE_URL,
        data=payload,
        headers={
            "Authorization": f"Bearer {token}",
            "x-goog-user-project": PROJECT,
            "Content-Type": "application/json; charset=utf-8",
        },
        method="POST",
    )
    last_error: Exception | None = None
    for attempt in range(MAX_TRIES):
        try:
            with urllib.request.urlopen(request) as response:
                body = json.loads(response.read().decode("utf-8"))
            return base64.b64decode(body["audioContent"])
        except urllib.error.HTTPError as error:
            last_error = error
            if error.code in (429, 500, 502, 503, 504) and attempt < MAX_TRIES - 1:
                time.sleep(BACKOFF_BASE_SECONDS * (2 ** attempt))  # 1s, 2s, 4s, 8s
                continue
            raise
        except urllib.error.URLError as error:
            last_error = error
            if attempt < MAX_TRIES - 1:
                time.sleep(BACKOFF_BASE_SECONDS * (2 ** attempt))
                continue
            raise
    raise RuntimeError(f"synthesis failed after {MAX_TRIES} tries: {last_error}")


def transcode(wav_bytes: bytes, out_path: Path) -> None:
    out_path.parent.mkdir(parents=True, exist_ok=True)
    tmp_wav = out_path.with_suffix(".tmp.wav")
    tmp_wav.write_bytes(wav_bytes)
    try:
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error", "-i", str(tmp_wav),
             "-c:a", "aac", "-b:a", "96k", str(out_path)],
            check=True,
        )
    finally:
        tmp_wav.unlink(missing_ok=True)


def pending_entries(manifest: dict, only: str | None) -> list[dict]:
    entries = [entry for entry in manifest["words"] if entry["status"] == "pending"]
    if only:
        entries = [entry for entry in entries if entry["romaji"] == only]
    return entries


def run_batch(manifest: dict, only: str | None, dry_run: bool) -> int:
    entries = pending_entries(manifest, only)
    if not entries:
        print("Nothing pending.")
        return 0
    if dry_run:
        for entry in entries:
            print(f"[dry-run] {entry['romaji']:14s} text={entry['input_text']} -> {entry['stimulus_file']}")
        print(f"{len(entries)} word(s) would be synthesized with {VOICE['name']}.")
        return 0

    token = access_token()
    failures = 0
    for entry in entries:
        out_path = AUDIO_DIR / Path(entry["stimulus_file"]).name
        try:
            wav = synthesize(entry["input_text"], token)
            transcode(wav, out_path)
            entry["status"] = "ok"
            print(f"ok   {entry['romaji']:14s} -> {out_path}")
        except Exception as error:  # noqa: BLE001 -- record and continue (resumable)
            entry["status"] = "fail"
            entry["error"] = str(error)
            failures += 1
            print(f"FAIL {entry['romaji']:14s}: {error}", file=sys.stderr)
        save_manifest(manifest)  # persist after each word so a partial run resumes
    print(f"Done: {len(entries) - failures} ok, {failures} failed.")
    return 1 if failures else 0


def summarize(manifest: dict) -> None:
    counts: dict[str, int] = {}
    for entry in manifest["words"]:
        counts[entry["status"]] = counts.get(entry["status"], 0) + 1
    print(f"{MANIFEST_PATH.name}: " + ", ".join(f"{status}={count}" for status, count in sorted(counts.items())))
    print(f"voice={VOICE['name']}  audio_dir={manifest['audio_dir']}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate per-word TTS audio (GCP).")
    parser.add_argument("--build-manifest", action="store_true", help="rebuild tts-manifest.json from the seed")
    parser.add_argument("--dry-run", action="store_true", help="print planned requests; no network")
    parser.add_argument("--run", action="store_true", help="synthesize pending words (calls GCP)")
    parser.add_argument("--only", metavar="ROMAJI", help="restrict to a single word")
    args = parser.parse_args()

    if args.build_manifest:
        manifest = build_manifest()
        save_manifest(manifest)
        summarize(manifest)
        return 0
    if args.dry_run or args.run:
        return run_batch(load_manifest(), args.only, dry_run=args.dry_run)
    summarize(load_manifest())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
