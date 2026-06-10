#!/usr/bin/env bash
# Regenerates the 60 per-word .m4a audio assets from the audio-only mp4 variants.
#
# Provenance (progress-log S1, 2026-06-10): the seed's stimulus_file column points at
# audio/<p1><p2><p3>-<romaji>.m4a, one shared audio file per word across all three
# condition rows. Each .m4a is a stream copy (ffmpeg -vn -c:a copy, bit-identical AAC)
# of the audio track of that word's audio-only mp4 variant, i.e. the file whose
# 4-character prefix ends in u (hiragana-canonical) or d (katakana-canonical).
# The output name drops pos4: displayed script lives in ideophones.display_form,
# not in filenames. The audio outputs are gitignored in the frontend repo; this
# script exists so they stay reproducible.
#
# Usage: scripts/extract-audio.sh [source-dir] [dest-dir]

set -euo pipefail

SRC_DIR="${1:-../../js/ideophone-arena-web/stimuli/final-stimuli/final-sokuon}"
DEST_DIR="${2:-../../js/ideophone-arena-web/stimuli/audio}"

mkdir -p "$DEST_DIR"

count=0
for mp4 in "$SRC_DIR"/[avip][0-9][hk][ud]-*.mp4; do
    base="$(basename "$mp4" .mp4)"
    prefix="${base%%-*}"          # e.g. a0hu
    romaji="${base#*-}"           # e.g. gosogoso
    out="$DEST_DIR/${prefix:0:3}-${romaji}.m4a"
    ffmpeg -hide_banner -loglevel error -y -i "$mp4" -vn -c:a copy "$out"
    count=$((count + 1))
done

echo "Extracted $count audio files into $DEST_DIR (expected 60)."
