#!/usr/bin/env python3
"""Dump golden intermediate tensors from vec2text for SKaiNET parity tests.

Requires vec2text installed (`pip install vec2text` or `pip install -e path/to/vec2text`).
Writes golden.json in --out-dir with, per test string:
  - embedder token ids (max_length=32, with EOS/pad) and attention mask
  - GTR mean-pooled embedding (768)
  - inversion MLP output (16 x 768) = the encoder inputs_embeds
  - inversion encoder memory (16 x 768)
  - first-step decoder logits top-20 (id, value)
  - greedy hypothesis token ids and decoded text
  - corrector inputs_embeds after LayerNorm (52+hypLen x 768) for the first step
  - per-step cosine similarity trace for a short recursive run

The Kotlin BertNumericalAccuracyTest-style tests assert against these to a
relative tolerance (see EmbeddingInversion module tests).
"""

import argparse
import json
from pathlib import Path

import torch
import vec2text
from vec2text import analyze_utils  # noqa: F401  (ensures registry import side-effects)

TEST_STRINGS = [
    "jack morris is a phd student at cornell tech in new york city",
    "the quick brown fox jumps over the lazy dog",
    "embeddings are not anonymous",
]


def tolist(t: torch.Tensor):
    return t.detach().float().cpu().reshape(-1).tolist()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-dir", default="models")
    ap.add_argument("--steps", type=int, default=5)
    args = ap.parse_args()
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    torch.manual_seed(0)
    corrector = vec2text.load_pretrained_corrector("gtr-base")
    inv_trainer = corrector.inversion_trainer
    model = inv_trainer.model
    model.eval()
    emb_tok = model.embedder_tokenizer
    t5_tok = model.tokenizer

    records = []
    for s in TEST_STRINGS:
        enc = emb_tok(
            [s], return_tensors="pt", max_length=model.config.max_seq_length,
            truncation=True, padding="max_length",
        )
        with torch.no_grad():
            frozen = model.call_embedding_model(
                input_ids=enc["input_ids"], attention_mask=enc["attention_mask"]
            )
            inputs_embeds, attn = model.embed_and_project(
                embedder_input_ids=enc["input_ids"],
                embedder_attention_mask=enc["attention_mask"],
                frozen_embeddings=None,
            )
            enc_out = model.encoder_decoder.encoder(
                inputs_embeds=inputs_embeds, attention_mask=attn
            ).last_hidden_state
            dec_start = torch.full((1, 1), model.encoder_decoder.config.decoder_start_token_id)
            logits = model.encoder_decoder(
                encoder_outputs=(enc_out,), attention_mask=attn,
                decoder_input_ids=dec_start,
            ).logits[0, 0]
            top = torch.topk(logits, 20)
            hyp = model.generate(
                {"frozen_embeddings": frozen},
                {"min_length": 1, "max_length": 128, "num_beams": 1, "do_sample": False},
            )

        # short recursive run for cosine trace
        traces = []
        for n in range(1, args.steps + 1):
            with torch.no_grad():
                out = vec2text.invert_embeddings(
                    embeddings=frozen, corrector=corrector, num_steps=n,
                )
            re_emb = model.call_embedding_model(
                **emb_tok(out, return_tensors="pt",
                          max_length=model.config.max_seq_length,
                          truncation=True, padding="max_length")
            )
            cos = torch.nn.functional.cosine_similarity(frozen, re_emb).item()
            traces.append({"step": n, "text": out[0], "cosine": cos})

        records.append({
            "text": s,
            "embedder_input_ids": enc["input_ids"][0].tolist(),
            "embedder_attention_mask": enc["attention_mask"][0].tolist(),
            "embedding": tolist(frozen),
            "inversion_inputs_embeds": tolist(inputs_embeds),
            "inversion_encoder_memory": tolist(enc_out),
            "first_step_logits_topk": [
                {"id": int(i), "value": float(v)}
                for i, v in zip(top.indices.tolist(), top.values.tolist())
            ],
            "hypothesis_ids": hyp[0].tolist(),
            "hypothesis_text": t5_tok.decode(hyp[0], skip_special_tokens=True),
            "cosine_trace": traces,
        })
        print(f"  {s[:40]!r}: hyp={records[-1]['hypothesis_text'][:50]!r}")

    (out_dir / "golden.json").write_text(json.dumps(
        {"max_seq_length": model.config.max_seq_length,
         "num_repeat_tokens": model.config.num_repeat_tokens,
         "records": records}, indent=2))
    print(f"wrote {out_dir/'golden.json'}")


if __name__ == "__main__":
    main()
