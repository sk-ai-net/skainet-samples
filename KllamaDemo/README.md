# KllamaDemo — Transformer Explainer on SKaiNET

A Compose Multiplatform / KMP / SKaiNET analog of the
[Polo Club Transformer Explainer](https://poloclub.github.io/transformer-explainer/),
running fully in the browser (and on JVM desktop, Android, iOS). The app
ships with an embedded **Qwen3-0.6B** model (Q3_K_S GGUF, ~280 MB) and
visualizes what the transformer is doing as it produces each token.

## What it demonstrates

A multi-tab UI driven by a single loaded model. The headline tab is the
**Visualize** explainer; the other tabs reuse the same runtime to show
related SKaiNET-transformer capabilities.

- **Visualize** *(headline)* — type a prompt, click **Step**, watch the
  next token be generated. See an architecture diagram of the 24-block
  decoder stack, the post-softmax **attention heatmap** for any
  block/head, the **residual stream** values out of any block, and the
  top-10 **next-token probability bars**. Step token-by-token to see
  the state of inference evolve.
- **Tokenizer playground** — type text, see Qwen's BPE breakdown
  into (id, decoded) pairs. Instant.
- **Chat** — Qwen3 ChatML template applied inline, streaming tokens.
- **Streaming completion** — raw prompt, no template, token-by-token.
- **Translation** — en↔zh via a translation system prompt.
- **Tool call** *(experimental)* — two-turn round-trip with a
  `get_current_time` tool.

## One-time setup — fetch the model

The 400 MB GGUF is not committed (`*.gguf` is `.gitignore`'d). Run the
fetch script before building:

```shell
./scripts/fetch-qwen-model.sh
```

The script downloads `Qwen3-0.6B-Q3_K_S.gguf` from
[unsloth/Qwen3-0.6B-GGUF](https://huggingface.co/unsloth/Qwen3-0.6B-GGUF)
into `composeApp/src/commonMain/composeResources/files/`. It's
idempotent and skips on subsequent runs.

## Build and run

### Browser (wasmJs — primary target)

```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

The first page load downloads the full ~295 MB bundle including the
embedded Q3_K_S model. Subsequent loads hit the browser HTTP cache.

### Desktop (JVM)

```shell
./gradlew :composeApp:run
```

Model load takes ~30 s on first launch (FP32 dequantization of 600M
parameters from the smaller Q3_K_S quant). Subsequent chat tokens
stream at ~1-3 tok/s on CPU.

### Android

```shell
./gradlew :androidApp:assembleDebug
```

The debug APK includes the 400 MB model in `assets/` — too large for
Play Store distribution as a single APK. A follow-up will split the
model into an `assetPack` for AAB builds.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode. The model ships as part of the
iOS framework — the same caveat about bundle size applies.

## Model & license

This app bundles **Qwen3-0.6B** by Alibaba Cloud / Qwen team, licensed
under the **Apache License 2.0**.

- Model card: https://huggingface.co/Qwen/Qwen3-0.6B
- GGUF build used:
  [unsloth/Qwen3-0.6B-GGUF](https://huggingface.co/unsloth/Qwen3-0.6B-GGUF)
  (Q3_K_S quantization)
- License text: [THIRD_PARTY_LICENSES/Apache-2.0.txt](./THIRD_PARTY_LICENSES/Apache-2.0.txt)
- Attribution: [THIRD_PARTY_LICENSES/NOTICE](./THIRD_PARTY_LICENSES/NOTICE)

## Known issues

- **Q4_1 quantization is not supported.** Loading a GGUF with Q4_1 tensors logs
  `unsupported quant type Q4_1 ... passing through unchanged` and the forward pass
  then fails at `matmul`. Use a Q3_K_S / Q4_0 / Q8_0 / F16 model instead. Tracked
  upstream in [SKaiNET#654](https://github.com/SKaiNET-developers/SKaiNET/issues/654).

## Project structure

- `composeApp/` — Compose Multiplatform application. The playground UI
  lives under `composeApp/src/commonMain/kotlin/sk/ainet/apps/kllama/chat/playground/`.
- `shared/` — model-loading types, the Phase-0 inference spike
  (`spike/QwenSpike.kt`), and the platform-detection scaffold used by
  the older filesystem-picker chat (now superseded by the playground).
- `server/` — Ktor server (unrelated to the playground).
- `iosApp/` — iOS entry point.
- `scripts/fetch-qwen-model.sh` — model downloader.
- `THIRD_PARTY_LICENSES/` — Apache 2.0 + NOTICE for the bundled model.

## Testing the inference plumbing

A JVM JUnit smoke test under `shared/src/jvmTest/` loads the embedded
GGUF directly from disk and runs a 5-token forward pass — proves the
`QwenNetworkLoader` → `OptimizedLLMRuntime` → `generate(...)` chain
works without any UI involvement:

```shell
./gradlew :shared:jvmTest --tests "*QwenSpike*"
```
