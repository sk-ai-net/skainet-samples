# KllamaDemo — Qwen3-0.6B browser playground

A Compose Multiplatform showcase of SKaiNET-transformers. The app ships
with an embedded **Qwen3-0.6B** model (Q4_K_M GGUF, ~400 MB) and runs in
the browser, on JVM desktop, on Android, and on iOS.

## What it demonstrates

Tabbed UI driven by a single loaded model:

- **Tokenizer playground** — type text, see how Qwen's BPE tokenizer
  splits it into token IDs. No inference, instant feedback.
- **Chat** — Qwen3 ChatML template applied inline, streaming tokens
  produced via `OptimizedLLMRuntime.generate`.

More tabs (streaming raw completion, en↔zh translation, tool calling
with `get_current_time`) land in follow-up commits.

## One-time setup — fetch the model

The 400 MB GGUF is not committed (`*.gguf` is `.gitignore`'d). Run the
fetch script before building:

```shell
./scripts/fetch-qwen-model.sh
```

The script downloads `Qwen3-0.6B-Q4_K_M.gguf` from
[unsloth/Qwen3-0.6B-GGUF](https://huggingface.co/unsloth/Qwen3-0.6B-GGUF)
into `composeApp/src/commonMain/composeResources/files/`. It's
idempotent and skips on subsequent runs.

## Build and run

### Browser (wasmJs — primary target)

```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

The first page load downloads the full ~400 MB bundle including the
embedded model. Subsequent loads hit the browser HTTP cache.

### Desktop (JVM)

```shell
./gradlew :composeApp:run
```

Model load takes ~40 s on first launch (FP32 dequantization of 600M
parameters). Subsequent chat tokens stream at ~1-3 tok/s on CPU.

### Android

```shell
./gradlew :composeApp:assembleDebug
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
  (Q4_K_M quantization)
- License text: [THIRD_PARTY_LICENSES/Apache-2.0.txt](./THIRD_PARTY_LICENSES/Apache-2.0.txt)
- Attribution: [THIRD_PARTY_LICENSES/NOTICE](./THIRD_PARTY_LICENSES/NOTICE)

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
