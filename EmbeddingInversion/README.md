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

CLI environment knobs: `VEC2TEXT_MODELS_DIR` (default `../models` for the app, `./models` for
the CLI), `VEC2TEXT_STEPS` (default 5), `VEC2TEXT_BEAM` (sequence beam width, default 1 = greedy),
`VEC2TEXT_TOKEN_BEAMS` (T5 token-level beam, default = `VEC2TEXT_BEAM`). The GUI has a **Beam
width** slider. Example:

```bash
VEC2TEXT_BEAM=3 ./gradlew :cli:run --args="jack morris is a phd student at cornell tech in new york city"
# beam ×3 lifts cosine ~0.77 → ~0.82 vs greedy at one step (and is proportionally slower).
```

### Build setup

SKaiNET core comes from **Maven Central 0.36.0** (aligned by the `sk.ainet:skainet-bom` platform).
The inversion models `sk.ainet.transformers:skainet-transformers-inference-{t5,vec2text}` are used
at **0.37.0** (adds beam search); until that lands on Central it's resolved from the **local Maven
cache** (a scoped `mavenLocal`). To (re)publish it, from a `SKaiNET-transformers` checkout with
`VERSION_NAME=0.37.0`:

```bash
./gradlew :llm-bom:publishToMavenLocal :transformer-core:publishToMavenLocal \
          :llm-core:publishToMavenLocal :llm-inference:t5:publishToMavenLocal \
          :llm-inference:vec2text:publishToMavenLocal -PsignAllPublications=false
```

No composite build, no source checkouts. Once transformers 0.37.0 is on Central, drop the
`mavenLocal` repository from `cli/` and `app/` `build.gradle.kts`.

> Reconstruction quality scales with correction `steps` and beam width; greedy + few steps + fp16
> can produce rough or `<unk>`-laden output on short inputs. A decode KV-cache (much faster,
> compounding with beam) is the remaining M5 follow-up.

## Compose desktop app (`app/`)

`./gradlew :app:run` opens a Compose for Desktop window with two tabs:

- **Round trip** — type text → embed → 768-d embedding strip → invert; the per-step hypotheses
  stream in with a live cosine bar as each correction completes.
- **Vector arithmetic** — interpolate two sentence embeddings with a slider and invert the
  blend (text you never wrote, decoded from a vector — why inversion matters for privacy).

## Status

| Milestone | State |
|---|---|
| M0 weight export + golden | ✅ done (scripts here) |
| M1 T5 encoder + GTR embedder | ✅ verified (cosine 0.99999985 vs reference) |
| M2 inversion (single-shot) | ✅ working end-to-end |
| M3 corrector loop | ✅ working end-to-end |
| M4 runnable CLI | ✅ `./gradlew :cli:run` (Maven Central 0.36.0) |
| M4 Compose desktop app | ✅ `./gradlew :app:run` — Round trip + Vector arithmetic tabs |
| M5 beam search | ✅ `VEC2TEXT_BEAM` / GUI slider (transformers 0.37.0) |
| M5 decode KV-cache speedup | ⏳ follow-up |

Current decoding is greedy with a no-KV-cache O(L²) loop — correct but slow on CPU. Beam search
and a KV cache (much faster, closer reconstructions) are the main follow-ups.
