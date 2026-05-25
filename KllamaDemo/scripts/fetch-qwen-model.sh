#!/usr/bin/env bash
set -euo pipefail

# Fetch the Qwen3-0.6B GGUF (Q4_0, ~380 MB) into the composeApp's
# commonMain composeResources so the bundled wasmJs / desktop / Android / iOS
# artifacts all carry the model. Q4_0 is the SIMD-friendly packed quant —
# DecoderGgufMemSegConverter wraps these tensors as Q4MemorySegmentTensorData
# so the CPU matmul stays in packed-byte form (4-bit weights), giving a
# much faster forward pass than the heavier K-quants which fully dequantize
# to FP32. The destination is .gitignore'd via the top-level *.gguf rule.

MODEL_URL="https://huggingface.co/unsloth/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q4_0.gguf"
DEST_DIR="$(cd "$(dirname "$0")/.." && pwd)/composeApp/src/commonMain/composeResources/files"
DEST_FILE="$DEST_DIR/qwen3-0.6b-Q4_0.gguf"
MIN_SIZE_BYTES=$((300 * 1024 * 1024))   # sanity floor: 300 MB (Q4_0 is ~380 MB)

mkdir -p "$DEST_DIR"

if [[ -f "$DEST_FILE" ]]; then
    size=$(stat -f%z "$DEST_FILE" 2>/dev/null || stat -c%s "$DEST_FILE")
    if (( size > MIN_SIZE_BYTES )); then
        echo "Model already present at $DEST_FILE ($(( size / 1024 / 1024 )) MB) — skipping."
        exit 0
    fi
    echo "Existing $DEST_FILE looks truncated ($(( size / 1024 / 1024 )) MB) — re-downloading."
    rm -f "$DEST_FILE"
fi

echo "Fetching Qwen3-0.6B-Q4_0.gguf (~380 MB, packed SIMD-friendly quant) from Hugging Face..."
curl -L --fail --progress-bar -o "$DEST_FILE" "$MODEL_URL"

size=$(stat -f%z "$DEST_FILE" 2>/dev/null || stat -c%s "$DEST_FILE")
if (( size < MIN_SIZE_BYTES )); then
    echo "Downloaded file is suspiciously small ($(( size / 1024 / 1024 )) MB). Aborting." >&2
    rm -f "$DEST_FILE"
    exit 1
fi

echo "Model saved to $DEST_FILE ($(( size / 1024 / 1024 )) MB)."
