#!/usr/bin/env python3
"""Export vec2text gtr-base checkpoints to flat SafeTensors for SKaiNET.

Produces in --out-dir:
  gtr_encoder.safetensors   sentence-transformers/gtr-t5-base T5 encoder (+ shared embedding)
  inversion.safetensors     jxm/gtr__nq__32 (embedding_transform.* + encoder_decoder.*)
  corrector.safetensors     jxm/gtr__nq__32__correct (embedding_transform_{1,2,3}.*, layernorm.*, encoder_decoder.*)
  tokenizer.json, spiece.model, tokenizer_config.json   (t5-base SentencePiece, shared by all models)
  <name>.config.json        per-model dims/hyperparams needed by the Kotlin loaders

Only needs: torch, safetensors, huggingface_hub. No vec2text install required —
this is pure state-dict surgery. Checkpoint key layout verified against
vec2text/models/inversion.py and corrector_encoder.py (incl. the legacy
shared-MLP remap from trainers/corrector.py::_remap_state_dict).
"""

import argparse
import json
import shutil
from pathlib import Path

import torch
from huggingface_hub import snapshot_download
from safetensors.torch import save_file

GTR_REPO = "sentence-transformers/gtr-t5-base"
INVERSION_REPO = "jxm/gtr__nq__32"
CORRECTOR_REPO = "jxm/gtr__nq__32__correct"


def load_state_dict(repo_dir: Path) -> dict:
    """Load a checkpoint state dict from a downloaded HF snapshot.

    Handles single-file and sharded layouts, safetensors and pytorch_model.bin.
    """
    # Single-file variants.
    for name in ("model.safetensors", "pytorch_model.bin"):
        path = repo_dir / name
        if path.exists():
            if name.endswith(".safetensors"):
                from safetensors.torch import load_file

                return load_file(str(path))
            return torch.load(str(path), map_location="cpu", weights_only=True)

    # Sharded variants: read the index and merge all referenced shards.
    for index_name, is_safe in (
        ("model.safetensors.index.json", True),
        ("pytorch_model.bin.index.json", False),
    ):
        index_path = repo_dir / index_name
        if index_path.exists():
            index = json.loads(index_path.read_text())
            shards = sorted(set(index["weight_map"].values()))
            sd: dict = {}
            for shard in shards:
                shard_path = repo_dir / shard
                if is_safe:
                    from safetensors.torch import load_file

                    sd.update(load_file(str(shard_path)))
                else:
                    sd.update(
                        torch.load(str(shard_path), map_location="cpu", weights_only=True)
                    )
            return sd

    raise FileNotFoundError(f"no model weights found in {repo_dir}")


def to_dtype(sd: dict, dtype: torch.dtype) -> dict:
    # .clone() breaks any shared storage (T5 ties shared/embed_tokens/lm_head to the
    # same tensor); safetensors refuses to serialize aliased storage.
    return {
        k: (v.to(dtype) if v.is_floating_point() else v).contiguous().clone()
        for k, v in sd.items()
    }


def drop_tied_embeddings(sd: dict) -> dict:
    # T5 ties word embeddings: encoder/decoder embed_tokens and lm_head all alias
    # `shared.weight` (or `encoder_decoder.shared.weight`). Keep only the `shared`
    # copy; the Kotlin loader feeds it to every tied site.
    tied_suffixes = (
        "encoder.embed_tokens.weight",
        "decoder.embed_tokens.weight",
        "lm_head.weight",
    )
    return {
        k: v
        for k, v in sd.items()
        if not any(k.endswith(s) for s in tied_suffixes)
    }


def remap_legacy_corrector_keys(sd: dict) -> dict:
    """Mirror Corrector._remap_state_dict: old checkpoints share one MLP for all three."""
    if {"embedding_transform.3.weight", "embedding_transform.3.bias"} <= sd.keys():
        for idx in ("0", "3"):
            w = sd.pop(f"embedding_transform.{idx}.weight")
            b = sd.pop(f"embedding_transform.{idx}.bias")
            for n in ("1", "2", "3"):
                sd[f"embedding_transform_{n}.{idx}.weight"] = w.clone()
                sd[f"embedding_transform_{n}.{idx}.bias"] = b.clone()
    return sd


def export_gtr_encoder(out_dir: Path, dtype: torch.dtype) -> None:
    repo = Path(snapshot_download(GTR_REPO))
    sd = load_state_dict(repo)
    # vec2text uses AutoModel(...).encoder: keep the shared embedding + encoder stack only.
    kept = {
        k: v
        for k, v in sd.items()
        if k == "shared.weight" or k.startswith("encoder.")
    }
    assert "shared.weight" in kept or "encoder.embed_tokens.weight" in kept, sorted(kept)[:5]
    if "shared.weight" not in kept:
        kept["shared.weight"] = sd["encoder.embed_tokens.weight"]
    kept = drop_tied_embeddings(kept)
    save_file(to_dtype(kept, dtype), str(out_dir / "gtr_encoder.safetensors"))

    cfg = json.loads((repo / "config.json").read_text())
    (out_dir / "gtr_encoder.config.json").write_text(
        json.dumps(
            {
                "d_model": cfg["d_model"],
                "num_layers": cfg["num_layers"],
                "num_heads": cfg["num_heads"],
                "d_kv": cfg["d_kv"],
                "d_ff": cfg["d_ff"],
                "vocab_size": cfg["vocab_size"],
                "relative_attention_num_buckets": cfg["relative_attention_num_buckets"],
                "relative_attention_max_distance": cfg.get(
                    "relative_attention_max_distance", 128
                ),
                "layer_norm_epsilon": cfg["layer_norm_epsilon"],
                "feed_forward_proj": cfg.get("feed_forward_proj", "relu"),
                "eos_token_id": cfg.get("eos_token_id", 1),
                "pad_token_id": cfg.get("pad_token_id", 0),
                "max_seq_length": 32,
            },
            indent=2,
        )
    )
    # The t5 tokenizer files travel with the gtr snapshot.
    for f in ("spiece.model", "tokenizer.json", "tokenizer_config.json"):
        src = repo / f
        if src.exists():
            shutil.copy(src, out_dir / f)


def export_vec2text_model(
    repo_id: str, out_name: str, out_dir: Path, dtype: torch.dtype, corrector: bool
) -> None:
    repo = Path(snapshot_download(repo_id))
    sd = load_state_dict(repo)
    if corrector:
        sd = remap_legacy_corrector_keys(sd)
        prefixes = ("embedding_transform_1.", "embedding_transform_2.",
                    "embedding_transform_3.", "layernorm.", "encoder_decoder.")
    else:
        prefixes = ("embedding_transform.", "encoder_decoder.")
    kept = {k: v for k, v in sd.items() if k.startswith(prefixes)}
    missing = [p for p in prefixes if not any(k.startswith(p) for k in kept)]
    assert not missing, f"{repo_id}: missing key groups {missing}; has {sorted(sd)[:10]}"
    # Ensure the tied word-embedding source survives, then drop its aliases.
    shared_key = "encoder_decoder.shared.weight"
    if shared_key not in kept:
        for cand in ("encoder_decoder.encoder.embed_tokens.weight",
                     "encoder_decoder.decoder.embed_tokens.weight"):
            if cand in sd:
                kept[shared_key] = sd[cand]
                break
    kept = drop_tied_embeddings(kept)
    save_file(to_dtype(kept, dtype), str(out_dir / f"{out_name}.safetensors"))

    cfg = json.loads((repo / "config.json").read_text())
    (out_dir / f"{out_name}.config.json").write_text(
        json.dumps(
            {
                "embedder_dim": cfg.get("embedder_dim", 768),
                "num_repeat_tokens": cfg.get("num_repeat_tokens", 16),
                "max_seq_length": cfg.get("max_seq_length", 32),
                "use_ln": cfg.get("use_ln", True),
            },
            indent=2,
        )
    )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-dir", default="models")
    ap.add_argument("--dtype", choices=("fp16", "fp32"), default="fp16")
    args = ap.parse_args()
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    dtype = torch.float16 if args.dtype == "fp16" else torch.float32

    print(f"exporting to {out_dir.resolve()} ({args.dtype})")
    export_gtr_encoder(out_dir, dtype)
    print("  gtr_encoder.safetensors done")
    export_vec2text_model(INVERSION_REPO, "inversion", out_dir, dtype, corrector=False)
    print("  inversion.safetensors done")
    export_vec2text_model(CORRECTOR_REPO, "corrector", out_dir, dtype, corrector=True)
    print("  corrector.safetensors done")
    for f in sorted(out_dir.iterdir()):
        print(f"  {f.name:36} {f.stat().st_size / 1e6:8.1f} MB")


if __name__ == "__main__":
    main()
