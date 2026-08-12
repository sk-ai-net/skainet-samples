# Kernel Race

On-device LLM chat, accelerated by SKaiNET's hand-written ARM NEON kernels on Android — with a
built-in **NEON vs scalar** comparison so you can measure the difference on your own phone. The
same Kotlin codebase also runs on Desktop and in the browser (WebAssembly), where it shows the
*other* kernel tiers SKaiNET dispatches to when there's no NEON hardware to target.

SmolLM2-135M (Q8_0 GGUF) decodes fully on-device through `skainet-backend-jni-cpu` on Android:
hand-written NEON matmul kernels behind a JNI bridge, with two `.so` tiers (`armv8-a` and
`armv8.2-a+fp16+dotprod`) selected at runtime per device.

This example began as the Android-only **AndroidNeonLlmDemo** (still available as a frozen
snapshot at the git tag [`2026_08_arm_android`](../../tree/2026_08_arm_android/AndroidNeonLlmDemo))
and was converted into a proper Kotlin Multiplatform sample: shared engine/view-model logic, a
Compose Multiplatform UI, and platform-specific runtime construction for Android, Desktop and
Wasm. iOS is deferred until the multiplatform structure is proven out.

## What it demonstrates

- **~5-line integration**: `DecoderGgufWeightLoader` → `OptimizedLLMRuntime` →
  `generateUntilStop`, streaming tokens into Compose (see `shared/.../engine/LlmEngine.kt` and
  the per-platform `LlamaRuntimeBuilder.*.kt` actuals).
- **NEON | SCALAR switch** (Android only): two chips re-pin the kernel registry (engine reloads
  on the next run) — same APK, same model, full-device A/B with a live tok/s counter.
- **Split-screen race** (Android only): one button launches a second process with the scalar
  provider pinned and starts both generations simultaneously.

  ![Split-screen race: NEON at 44.7 tok/s vs scalar at 9.3 tok/s](docs/screenshots/split_race.png)
- **Cross-platform kernel tiers**: the same Kotlin `LlmEngine` runs on three different kernel
  paths — Android's ARM NEON JNI kernels, Desktop's native-optimized file-based load, and Wasm's
  in-memory FP32 fallback (browsers have no filesystem, so the model is bundled at build time
  instead of downloaded).
- **Model delivery**: Android downloads the GGUF from the Hugging Face Hub on first run
  (SKaiNET's Ktor fetcher, streamed to disk with progress) or uses a bundled asset if present;
  Desktop downloads to a local cache dir; Wasm bundles the model into the production build via
  `scripts/fetch-model.sh` (see the Web section below for the size trade-off this implies).
- **SKaiNET design system** from the shared [`../skainet-ui`](../skainet-ui) module (theme,
  logo, `FadingRingLoader`).

## Run

### Android (the full experience)

Real ARM64 hardware shows the point best (an x86 emulator falls back to scalar):

```sh
./gradlew :composeApp:installDebug
```

Tap **Generate on-device** — the model (~145 MB) downloads on first use. Then flip the
**SCALAR** chip and generate again to see the difference, or tap **Race against scalar** for the
side-by-side version. To go fully offline, place `SmolLM2-135M-Instruct-Q8_0.gguf` in
`composeApp/src/androidMain/assets/` before building.

Reference numbers (SmolLM2-135M Q8_0, SKaiNET 0.39.1): ~6.4× decode-kernel throughput NEON vs
scalar on a Pixel 8a.

### Desktop

```sh
./gradlew :composeApp:run
```

Downloads the model to `~/.skainet-examples/kernelrace/models/` on first run. No NEON kernels
here — this is SKaiNET's file-based native-optimized load path on plain JVM.

### Web (Wasm)

```sh
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

The first build runs `scripts/fetch-model.sh`, which bundles the ~145 MB GGUF straight into the
production JS/Wasm bundle (browsers have no persistent filesystem to download into at runtime).
That's a deliberately large asset for a web page — acceptable for a local dev run or a
maintainer-approved deploy, but worth knowing about before wiring this into CI or a public
samples page.

## Requirements

- Android: ARM64 device (minSdk 24, one APK covers armv8.0 through armv9), JDK 21+
- Desktop: JDK 21+ with the JDK Vector API (incubator) enabled — wired automatically by the
  Gradle build
- Web: a Chromium-based browser for the wasm GC runtime

## Architecture

```
KernelRace/
├── shared/                    # Engine, view-model, model resolution — platform-neutral
│   └── src/
│       ├── commonMain/        # LlmEngine, ChatViewModel, ModelResolver (pure, unit-tested)
│       ├── androidMain/       # AndroidModelProvider, NEON-aware LlamaRuntimeBuilder actual
│       ├── jvmMain/           # DesktopModelProvider, file-based LlamaRuntimeBuilder actual
│       └── wasmJsMain/        # Bytes-only LlamaRuntimeBuilder actual (no filesystem)
└── composeApp/                 # Compose Multiplatform UI + platform entry points
    └── src/
        ├── commonMain/         # App/ChatScreen (skainet-ui themed), kernelControls slot
        ├── androidMain/        # KernelRaceApp (kernel pinning), race UI, manifest
        ├── jvmMain/            # Desktop window entry point
        └── wasmJsMain/         # Browser entry point + bundled model resource
```

The race mechanics (multi-process kernel pinning, `KernelRegistry`, the split-screen button) are
entirely Android-specific and live in `composeApp/androidMain`. Common code only sees a
`kernelControls` composable slot and a `GenerativeEngine` interface, which is what makes
`shared`'s `ChatViewModel` unit-testable with a fake on every target — see `shared/src/*Test/`.
