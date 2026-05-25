#!/usr/bin/env bash
set -euo pipefail

# Fetch the Qwen3-0.6B GGUF (Q4_K_M, ~400 MB) into the composeApp's
# commonMain composeResources so the bundled wasmJs / desktop / Android / iOS
# artifacts all carry the model. The destination is .gitignore'd via the
# top-level *.gguf rule.

MODEL_URL="https://huggingface.co/unsloth/Qwen3-0.6B-GGUF/resolve/main/Qwen3-0.6B-Q3_K_S.gguf"
DEST_DIR="$(cd "$(dirname "$0")/.." && pwd)/composeApp/src/commonMain/composeResources/files"
DEST_FILE="$DEST_DIR/qwen3-0.6b-Q3_K_S.gguf"
MIN_SIZE_BYTES=$((200 * 1024 * 1024))   # sanity floor: 200 MB (Q3_K_S is ~280 MB)

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

echo "Fetching Qwen3-0.6B-Q3_K_S.gguf (~280 MB) from Hugging Face..."
curl -L --fail --progress-bar -o "$DEST_FILE" "$MODEL_URL"

size=$(stat -f%z "$DEST_FILE" 2>/dev/null || stat -c%s "$DEST_FILE")
if (( size < MIN_SIZE_BYTES )); then
    echo "Downloaded file is suspiciously small ($(( size / 1024 / 1024 )) MB). Aborting." >&2
    rm -f "$DEST_FILE"
    exit 1
fi

echo "Model saved to $DEST_FILE ($(( size / 1024 / 1024 )) MB)."
