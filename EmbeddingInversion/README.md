# EmbeddingInversion — vec2text in the SKaiNET ecosystem

Decode sentence embeddings **back into text**, in pure Kotlin. This is a Kotlin port of
[vec2text](https://github.com/vec2text/vec2text) (Morris et al.) inference, running on
[SKaiNET](https://github.com/SKaiNET) — the KotlinDL-successor multiplatform DL engine.

Given only the 768-d embedding vector of a sentence (from `sentence-transformers/gtr-t5-base`),
the models reconstruct an approximation of the original text:

```
original:      jack morris is a phd student at cornell tech in new york city
step 0 (invert):  cos 0.71  "jack morris is a neophyte at COL Tech ... New York City University in"
step 3 (correct): cos 0.83  "jack morris is a ph.D. tech at neorthell alumnus at Cornell University in"
```

It is a striking privacy demonstration: **embeddings are not anonymous** — text can be
recovered from them.

## How it works

Three models (all `t5-base` scale), from the `jxm/gtr__nq__32` checkpoints:

1. **GTR embedder** — `sentence-transformers/gtr-t5-base` T5 encoder + mean pooling → target embedding.
2. **Inversion model** ("hypothesizer", `jxm/gtr__nq__32`) — projects the embedding to 16
   pseudo-tokens, feeds them to a T5 encoder-decoder, greedily decodes a first guess.
3. **Corrector** (`jxm/gtr__nq__32__correct`) — given the target, the current hypothesis and their
   difference, generates a refined guess. Iterated; the hypothesis with the highest cosine
   similarity to the target wins.

The Kotlin models live upstream in **SKaiNET-transformers**:
- `llm-inference:t5` — the T5 encoder-decoder runtime + GTR embedder.
- `llm-inference:vec2text` — inversion model, corrector, and the iterative `Vec2TextInverter`.

## Fetch the model weights

The checkpoints (~1.1 GB) are converted from HuggingFace to flat fp16 SafeTensors that SKaiNET
loads directly. Requires Python 3.10+ with `torch safetensors huggingface_hub`:

```bash
scripts/fetch-vec2text-models.sh          # → models/{gtr_encoder,inversion,corrector}.safetensors + tokenizer
```

Optional golden dumps for the parity tests (needs `transformers`, and `vec2text` for the full dump):

```bash
python scripts/dump_embedder_golden.py    # embedder golden (needs transformers)
python scripts/dump_golden_tensors.py     # full golden trace (needs vec2text installed)
```

## Run it today (via the SKaiNET-transformers tests)

Until the `t5`/`vec2text` modules are published as artifacts, run the end-to-end round-trip from
the SKaiNET-transformers repo:

```bash
cd /path/to/SKaiNET-transformers
VEC2TEXT_MODELS_DIR=/path/to/EmbeddingInversion/models \
  ./gradlew :llm-inference:vec2text:jvmTest --tests '*Vec2TextRoundTripTest'
# GTR embedder parity (cos > 0.999 vs reference):
VEC2TEXT_MODELS_DIR=/path/to/EmbeddingInversion/models \
  ./gradlew :llm-inference:t5:jvmTest --tests '*GtrEmbedderParityTest'
```

## Planned Compose Multiplatform demo (`app/`)

A `GloVeEmbeddings`-style Compose app (desktop JVM first) is the next step. It depends on the
`t5` / `vec2text` modules being available as dependencies — either published in a
`skainet-transformers` release **> 0.34.1**, or wired via a composite build. Planned tabs:

- **Round trip** — type text → embed → show the 768-d vector → invert → compare original vs
  reconstruction, with the per-step hypothesis + cosine sparkline.
- **Vector arithmetic** — interpolate two sentence embeddings with a slider and invert the
  midpoint live (why inversion matters for privacy).

Recommended JVM args (large models): `-Xmx4g`, plus SKaiNET's SIMD flags for speed.

## Status

| Milestone | State |
|---|---|
| M0 weight export + golden | ✅ done (scripts here) |
| M1 T5 encoder + GTR embedder | ✅ verified (cosine 0.99999985 vs reference) |
| M2 inversion (single-shot) | ✅ working end-to-end |
| M3 corrector loop | ✅ working end-to-end |
| M4 Compose demo app | ⏳ pending module publish |
| M5 beam search + KV-cache speedup | ⏳ follow-up |

Current decoding is greedy with a no-KV-cache O(L²) loop — correct but slow on CPU. Beam search
and a KV cache (much faster, closer reconstructions) are the main follow-ups.
