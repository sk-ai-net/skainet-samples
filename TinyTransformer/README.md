# Tiny Transformer — KI-ENNA (Kotlin Multiplatform Sample)

Train a **tiny decoder-only transformer language model live in the app** — on Android, iOS,
Desktop, and directly **in the browser** (Kotlin/Wasm and JS). Everything runs on-device with
[SKaiNET](https://github.com/SKaiNET-developers/SKaiNET): the word-level tokenizer, the
attention model, the training loop, and next-word prediction. No server, no Python, no
pre-trained weights.

This sample is a Kotlin/SKaiNET port of the German educational page
[KI-ENNA: (E)in (N)euronales (N)etz zum (A)usprobieren — Transformer](https://statistical-thinking.de/ki-enna-transformer.html)
("a neural network to try out"), with an English/German language toggle.

## What it teaches

1. **Data** — edit a tiny corpus (default: six German sentences), tokenize it with a
   word-level tokenizer (`<pad>`/`<unk>`/`<eos>` specials, frequency-capped vocabulary),
   and slice it into sliding next-token windows.
2. **Training** — a minimal transformer, faithfully small:
   - token embedding `E [V, 12]` + learned positional embedding `P [T, 12]`
   - single-head causal self-attention **without** Q/K/V projections:
     `A = softmax(X·Xᵀ/√d + causal mask)`, `O = A·X`
   - output projection `logits = O·W + b`
   - cross-entropy loss (pad positions masked) + plain SGD
   Watch the **attention heatmap** and the **loss curve** update live while it trains.
3. **Embeddings** — inspect the learned token vectors.
4. **Prediction** — type a prompt and let the freshly trained model predict the next word
   (top-5 with probabilities).

## SKaiNET APIs showcased

- The **NN DSL**: a custom `Module<FP32, Float>` built from differentiable `TensorOps`
  (`indexSelect` for embedding lookup, `matmul`, `transpose`, `softmax`), trained with the
  `training { model / loss / optimizer }` runner
- `DefaultGraphExecutionContext(phase = Phase.TRAIN)` + `DefaultGradientTape` autodiff
- `CrossEntropyLoss` with soft (one-hot) targets and the `sgd` optimizer
- Only **`sk.ainet.core:*`** artifacts (via `sk.ainet:skainet-bom`) — the transformer is
  built from scratch; no `sk.ainet.transformers` dependency

## Project structure

- `/shared` — platform-agnostic logic: tokenizer, model, trainer (Flow-based), predictor.
- `/composeApp` — Compose Multiplatform UI for all targets.
- `/iosApp` — iOS entry point.

## Running

| Target  | Command |
|---------|---------|
| Desktop | `./gradlew :composeApp:run` |
| Web (Wasm) | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| Web (JS)   | `./gradlew :composeApp:jsBrowserDevelopmentRun` |
| Android | `./gradlew :androidApp:assembleDebug` |
| iOS     | open `/iosApp` in Xcode and run |
| Tests   | `./gradlew :shared:jvmTest` |

### Web via Docker

```bash
docker build -t skainet/tinytransformer .
docker run -p 8080:80 skainet/tinytransformer
```

## Credits

Concept and original implementation: [KI-ENNA — statistical-thinking.de](https://statistical-thinking.de/)
Ported to Kotlin Multiplatform + SKaiNET to showcase on-device, multi-platform
training of a transformer from first principles.
