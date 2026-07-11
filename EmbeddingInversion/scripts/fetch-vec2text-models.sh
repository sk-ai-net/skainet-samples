#!/usr/bin/env bash
# Fetch + convert the vec2text gtr-base checkpoints into flat SafeTensors that
# SKaiNET can load. Idempotent: skips work if the outputs already exist.
#
# Usage: scripts/fetch-vec2text-models.sh [OUT_DIR]
#   OUT_DIR defaults to <repo>/models
#
# Requires python3 with: torch safetensors huggingface_hub
# (the export step is pure state-dict surgery; vec2text itself is only needed
#  for the optional golden-tensor dump used by the parity tests.)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="${1:-$SCRIPT_DIR/../models}"
DTYPE="${VEC2TEXT_DTYPE:-fp16}"

mkdir -p "$OUT_DIR"

if [[ -f "$OUT_DIR/inversion.safetensors" && -f "$OUT_DIR/corrector.safetensors" && -f "$OUT_DIR/gtr_encoder.safetensors" ]]; then
  echo "vec2text models already present in $OUT_DIR — skipping. (delete them to re-fetch)"
  exit 0
fi

echo "==> exporting vec2text gtr-base weights to $OUT_DIR ($DTYPE)"
python3 "$SCRIPT_DIR/export_vec2text_weights.py" --out-dir "$OUT_DIR" --dtype "$DTYPE"

echo "==> done. Files in $OUT_DIR:"
ls -lh "$OUT_DIR"
