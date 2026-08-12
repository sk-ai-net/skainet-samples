#!/usr/bin/env bash
set -euo pipefail

# Fetch the SmolLM2-135M-Instruct GGUF (Q8_0, ~145 MB) into the composeApp's commonMain
# composeResources so the bundled wasmJs / desktop artifacts carry the model too. Android keeps
# its own asset-or-download path (AndroidModelProvider) since a bundled 145 MB asset would bloat
# every install; wasm has no filesystem so this bundled copy is its only option. Q8_0 is what the
# NEON kernels also want on Android, so one file serves every target. Destination is
# .gitignore'd via the repo's *.gguf rule.

MODEL_URL="https://huggingface.co/unsloth/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q8_0.gguf"
DEST_DIR="$(cd "$(dirname "$0")/.." && pwd)/composeApp/src/commonMain/composeResources/files"
DEST_FILE="$DEST_DIR/SmolLM2-135M-Instruct-Q8_0.gguf"
MIN_SIZE_BYTES=$((100 * 1024 * 1024))   # sanity floor: 100 MB (Q8_0 is ~145 MB)

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

echo "Fetching SmolLM2-135M-Instruct-Q8_0.gguf (~145 MB) from Hugging Face..."
curl -L --fail --progress-bar -o "$DEST_FILE" "$MODEL_URL"

size=$(stat -f%z "$DEST_FILE" 2>/dev/null || stat -c%s "$DEST_FILE")
if (( size < MIN_SIZE_BYTES )); then
    echo "Downloaded file is suspiciously small ($(( size / 1024 / 1024 )) MB). Aborting." >&2
    rm -f "$DEST_FILE"
    exit 1
fi

echo "Model saved to $DEST_FILE ($(( size / 1024 / 1024 )) MB)."
