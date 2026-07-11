#!/usr/bin/env python3
"""Minimal golden dump for the GTR embedder parity test (needs only torch + transformers).

For each test string, writes token ids (T5 SentencePiece, max_length=32 + EOS, no padding)
and the raw-encoder mean-pooled embedding [768] — exactly the vec2text embedder path
(AutoModel(...).encoder + mean_pool, NO Dense, NO L2). The Kotlin T5 embedder test feeds
these exact ids and compares its embedding to `embedding`.
"""
import argparse
import json
from pathlib import Path

import torch
from transformers import AutoModel, AutoTokenizer

REPO = "sentence-transformers/gtr-t5-base"
STRINGS = [
    "jack morris is a phd student at cornell tech in new york city",
    "the quick brown fox jumps over the lazy dog",
]


def mean_pool(last_hidden_state, attention_mask):
    mask = attention_mask.unsqueeze(-1).float()
    summed = (last_hidden_state * mask).sum(dim=1)
    counts = mask.sum(dim=1).clamp(min=1e-9)
    return summed / counts


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="models/embedder_golden.json")
    args = ap.parse_args()

    tok = AutoTokenizer.from_pretrained(REPO)
    encoder = AutoModel.from_pretrained(REPO).encoder.eval()

    records = []
    for s in STRINGS:
        enc = tok([s], return_tensors="pt", max_length=32, truncation=True)
        with torch.no_grad():
            hidden = encoder(
                input_ids=enc["input_ids"], attention_mask=enc["attention_mask"]
            ).last_hidden_state
            emb = mean_pool(hidden, enc["attention_mask"])[0]
        records.append({
            "text": s,
            "input_ids": enc["input_ids"][0].tolist(),
            "embedding": emb.float().tolist(),
        })
        print(f"  {s[:40]!r}: ids={enc['input_ids'][0].tolist()[:8]}... dim={emb.shape[0]}")

    Path(args.out).write_text(json.dumps({"records": records}, indent=2))
    print(f"wrote {args.out}")


if __name__ == "__main__":
    main()
