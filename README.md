# SKaiNET Examples

Sample applications built with [**SKaiNET**](https://github.com/SKaiNET-developers/SKaiNET) —
the Kotlin Multiplatform AI framework. Each example is a self-contained project;
most are Compose Multiplatform apps that run on Android, iOS, Desktop and the web
from a single Kotlin codebase.

> [!IMPORTANT]
> **About the name**
>
> “SKaiNET” is a **working project name** chosen early in the project’s life as part of a personal learning and experimentation effort, before any trademark considerations were known.
>
> The name is **not intended to reference, infringe, or imply association with any existing trademarks, companies, or products**. It is not a commercial brand and is **not claimed or assignable** to any company or organization that contributors may be affiliated with.
>
> If a naming conflict arises, the project name may be changed in the future.

## Examples

### 🌊 Sinus Approximator

A tiny neural network that learns to approximate `sin(x)` — and you can **train it
live in the app**. Visualises the target curve, the model's prediction, and the
network architecture. One Kotlin codebase running on Android, iOS, Desktop (JVM)
and WebAssembly.

<img src="SinusApproximator/docs/screenshots/jvm.png" alt="Sinus Approximator — in-app training screen" width="640" />

📂 [`SinusApproximator/`](SinusApproximator/) · runs on Android · iOS · Desktop · Wasm

---

### ✏️ MNIST Demo

Handwritten-digit recognition: draw a digit and a convolutional network classifies
it on the spot. A clean-architecture Compose Multiplatform app that loads a
pretrained GGUF model and can also retrain it in-app.

<img src="MNISTDemo/docs/screenshots/wasm.png" alt="MNIST Demo — draw and classify a digit" width="640" />

📂 [`MNISTDemo/`](MNISTDemo/) · runs on Android · iOS · Desktop · Wasm

---

### 💬 Kllama Demo

A local LLM **chat app** — runs a GGUF Llama model fully on-device, no server, no
API key. Compose Multiplatform UI with a streaming chat view and live token
statistics, targeting Android, iOS, Web, Desktop and Server.

📂 [`KllamaDemo/`](KllamaDemo/) · runs on Android · iOS · Desktop · Web · Server

---

### ☕ MNIST Java Demo

The same MNIST digit detector as a **pure-Java CLI** — proof of SKaiNET's
first-class Java interop from a Kotlin Multiplatform engine.

```text
Predicted digit: 7
Confidence: 98.3%
```

📂 [`MnistJavaDemo/`](MnistJavaDemo/) · command-line · Java 21+

## New here?

If this is your first SKaiNET example, start with **Sinus Approximator** — it is
the smallest end-to-end story (define a network, train it, see it predict) and
runs on every platform. Then try **MNIST Demo** for a real convolutional model,
and **Kllama Demo** to run a local LLM.

Each example folder has its own `README.md` with build and run instructions.
