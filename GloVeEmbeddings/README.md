# GloVe Embeddings — SKaiNET example

Companion code for the article `glove.md` ("Explaining GloVe Embeddings with SKaiNET and
Kotlin"), plus an offline Compose Multiplatform app that explores word vectors.

## Modules

- **`:glove`** — the teaching library. The full GloVe pipeline in plain Kotlin
  (`Vocabulary`, `Cooccurrence`, `GloVePlain`) with a SKaiNET-tensor counterpart
  (`GloVeTensors`), plus a reusable read-only vector store (`Embeddings`) supporting cosine
  similarity, nearest-neighbour search and vector algebra (`analogy`), and a
  `GloVeTextReader` for loading pretrained vectors in GloVe text format.
- **`:app`** — a Compose Multiplatform UI that loads vectors **bundled in the app** (fully
  offline) and lets you explore them:
  - **Neighbours** — type a word, see its nearest neighbours with similarity bars.
  - **Analogy** — vector algebra `A − B + C`, prefilled with the classic
    `king − man + woman ≈ queen`.

## Run

```bash
./gradlew :glove:runDemo                      # the article pipeline, end to end (JVM)
./gradlew :app:run                            # desktop UI (needs a display)
./gradlew :app:wasmJsBrowserDevelopmentRun    # web UI in the browser
./gradlew :androidApp:assembleDebug           # Android APK (needs an Android SDK)
```

iOS targets are declared and build on a macOS host (`:app` produces a `GloVeApp` framework);
they cannot be compiled from Linux/Windows.

## Tests

```bash
./gradlew :glove:jvmTest :glove:jsNodeTest :glove:wasmJsNodeTest
```

## About the bundled vectors

`app/src/commonMain/composeResources/files/mini-glove.50d.txt` is a **small, didactic** vector
set (38 words, 50-d) constructed so the showcased analogies resolve exactly — it is *not* real
pretrained GloVe. The format is standard GloVe text (`word v1 v2 … vN` per line), so you can
drop in a trimmed slice of the real `glove.6B.50d.txt` (or any GloVe file) by replacing that
resource; `GloVeTextReader` and the UI consume it unchanged.
