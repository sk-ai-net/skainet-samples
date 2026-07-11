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

## Model weights

SKaiNET loads flat **fp16 SafeTensors**. The reference vec2text checkpoints are custom
PyTorch classes on HuggingFace, so they need a one-time offline conversion (done once with any
Python + HuggingFace tooling — this Kotlin repo intentionally ships no conversion scripts).
Drop the resulting files into `models/` (git-ignored, ~1.1 GB):

| Output file | Source checkpoint | Keep these keys |
|---|---|---|
| `gtr_encoder.safetensors` | `sentence-transformers/gtr-t5-base` | `shared.weight`, `encoder.*` |
| `inversion.safetensors` | `jxm/gtr__nq__32` | `embedding_transform.{0,3}.{weight,bias}`, `encoder_decoder.*` |
| `corrector.safetensors` | `jxm/gtr__nq__32__correct` | `embedding_transform_{1,2,3}.{0,3}.*`, `layernorm.{weight,bias}`, `encoder_decoder.*` |
| `tokenizer.json` | `t5-base` | (SentencePiece, shared by all models) |

Conversion notes:
- Cast float tensors to fp16 and `clone()` them — T5 ties `shared.weight` /
  `encoder.embed_tokens.weight` / `decoder.embed_tokens.weight` / `lm_head.weight` to one
  storage, so **keep only `shared.weight`** and drop the aliases (the Kotlin loader feeds it to
  every tied site; SafeTensors also refuses aliased storage).
- The `jxm/*` checkpoints are sharded `pytorch_model-0000N-of-…bin` — merge all shards via the
  `.index.json` before filtering keys.
- Some older corrector checkpoints share one `embedding_transform.*` MLP; remap it into the three
  `embedding_transform_{1,2,3}.*` (mirrors `Corrector._remap_state_dict`).

The parity tests (below) compare against golden tensors dumped from the reference models:
`GtrEmbedderParityTest` needs token ids + the mean-pooled embedding from `gtr-t5-base`;
`Vec2TextRoundTripTest` needs only the converted weights above.

## Run it

Put the converted weights in `models/`, then either:

```bash
# Desktop GUI (Round trip + Vector arithmetic tabs):
./gradlew :app:run

# Or the CLI:
./gradlew :cli:run --args="jack morris is a phd student at cornell tech in new york city"
```

Environment knobs: `VEC2TEXT_MODELS_DIR` (default `../models` for the app, `./models` for the
CLI), `VEC2TEXT_STEPS` (CLI, default 5).

### Build setup

- **SKaiNET core** is consumed from Maven Central (**0.36.0**, pinned by the `sk.ainet:skainet-bom`
  platform) — no local `SKaiNET` checkout needed.
- **`t5` / `vec2text`** are not yet published, so **`SKaiNET-transformers` is a composite build**:
  `settings.gradle.kts` `includeBuild`s `../../SKaiNET-transformers` and maps the two
  `skainet-transformers-inference-*` coordinates to the local `:llm-inference:t5` / `:vec2text`
  projects (their publish artifactId differs from the Gradle project name, so auto-substitution
  can't match them). Uses Gradle 9.6.1 to match that build.

Once `skainet-transformers` is released, drop the composite and depend on the published
coordinates directly.

> Reconstruction quality scales with correction `steps`; greedy + few steps + fp16 can produce
> rough or `<unk>`-laden output on short inputs. Beam search and a decode KV-cache (much faster,
> closer) are the M5 follow-ups.

## Planned Compose Multiplatform demo (`app/`)

A `GloVeEmbeddings`-style Compose app (desktop JVM first) is the next step, reusing the same
composite build. Planned tabs:

- **Round trip** — type text → embed → show the 768-d vector → invert → compare original vs
  reconstruction, with the per-step hypothesis + cosine sparkline.
- **Vector arithmetic** — interpolate two sentence embeddings with a slider and invert the
  midpoint live (why inversion matters for privacy).

## Status

| Milestone | State |
|---|---|
| M0 weight export + golden | ✅ done (scripts here) |
| M1 T5 encoder + GTR embedder | ✅ verified (cosine 0.99999985 vs reference) |
| M2 inversion (single-shot) | ✅ working end-to-end |
| M3 corrector loop | ✅ working end-to-end |
| M4 runnable CLI (composite build) | ✅ `./gradlew :cli:run` |
| M4 Compose desktop app | ✅ `./gradlew :app:run` — Round trip + Vector arithmetic tabs |
| M5 beam search + KV-cache speedup | ⏳ follow-up |

Current decoding is greedy with a no-KV-cache O(L²) loop — correct but slow on CPU. Beam search
and a KV cache (much faster, closer reconstructions) are the main follow-ups.
