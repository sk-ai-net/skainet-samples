# MnistJavaDemo

A CLI MNIST digit detector written in Java 21, demonstrating JVM interoperability with [SKaiNET](../../SKaiNET), a Kotlin Multiplatform ML framework. This is the first Java example in SKaiNET-examples.

## Prerequisites

- **Java 21+** (with preview features)
- **SKaiNET 0.40.1** -- resolved directly from Maven Central, no local publish needed
- **GGUF model file** -- a trained MNIST CNN model in GGUF format

## Usage

```bash
# Show help
./gradlew run --args="--help"

# Detect a digit from an image file
./gradlew run --args="detect --model mnist_cnn.gguf digit.png"

# Detect with inverted colors (black digit on white background)
./gradlew run --args="detect --model mnist_cnn.gguf digit.png --invert"

# Run on MNIST test dataset samples
./gradlew run --args="test --model mnist_cnn.gguf --count 20"
```

### Example output

```
Predicted digit: 7
Confidence: 98.3%

Probability distribution:
  [0]   0.1%
  [1]   0.2%
  [2]   0.3%
  [3]   0.1%
  [4]   0.0%
  [5]   0.1%
  [6]   0.0%
  [7]  98.3% ####################################### <--
  [8]   0.5%
  [9]   0.4%
```

## Architecture

The project uses a **pipeline pattern** for orchestration, with SKaiNET's `Transform<I,O>` API handling image preprocessing:

```
                  +-----------------+     +-------------------+     +--------------------+
  image path ---> | ImagePipeline   | --> | InferencePipeline | --> | DetectionResult    |
                  | (SKaiNET        |     | (CNN forward pass |     | (digit, confidence,|
                  |  transforms)    |     |  + softmax)       |     |  probabilities)    |
                  +-----------------+     +-------------------+     +--------------------+

                  +-----------------+     +-------------------+
  dataset cfg --> | DatasetPipeline | --> | InferencePipeline | --> accuracy report
                  | (MNIST download)|     | (per-sample)      |
                  +-----------------+     +-------------------+
```

### Source layout

```
src/main/java/sk/ainet/examples/mnist/
  MnistDetector.java          # Main entry point, CLI dispatch
  CliArgs.java                 # Sealed interface + records for CLI parsing
  DetectionResult.java         # Result record
  interop/
    KotlinBridge.java          # Coroutine runBlocking bridge
  pipeline/
    Pipeline.java              # Generic Pipeline<I,O> orchestration abstraction
    ImagePipeline.java         # Image loading + SKaiNET mnistPreprocessing()
    DatasetPipeline.java       # MNIST dataset download via SKaiNET
    InferencePipeline.java     # Model forward pass + softmax + argmax

src/main/kotlin/sk/ainet/examples/mnist/
  ModelFactory.kt              # Kotlin bridge for inline reified APIs
```

## Java 21 Features Showcased

| Feature | Where |
|---|---|
| **Sealed interfaces** | `CliArgs` -- exhaustive pattern matching |
| **Records** | `CliArgs.Help`, `DetectFromImage`, `DetectFromDataset`, `DetectionResult`, `InferenceInput`, `DatasetConfig` |
| **Pattern matching switch** | `MnistDetector.main()` -- dispatch on `CliArgs` variants |
| **Text blocks** | Help message in `MnistDetector.printHelp()` |
| **`var`** | Throughout for local type inference |

## Java-Kotlin Interop Patterns

This project demonstrates several patterns for calling Kotlin/SKaiNET APIs from Java:

| Kotlin Pattern | Java Equivalent |
|---|---|
| Top-level function `mnistPreprocessing(ctx)` | `ImageTransformDslKt.mnistPreprocessing(ctx)` |
| Extension function `tensor.softmax(dim)` | `TensorExtensionsKt.softmax(tensor, dim)` |
| `object MNIST` singleton | `MNIST.INSTANCE.loadTest(config, continuation)` |
| `suspend fun` (coroutines) | `BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, block)` |
| `inline reified fun sequential()` | Bridged via `ModelFactory.kt` (cannot call from Java) |
| `PlatformBitmapImage` typealias | `BufferedImage` (transparent on JVM) |
| Kotlin default parameters | Must pass all arguments, or use Kotlin bridge helpers |
| `Shape(1, 1, 28, 28)` vararg companion invoke | `new Shape(new int[]{1, 1, 28, 28})` |

### Why one Kotlin file?

SKaiNET's `sequential` DSL uses `inline reified` type parameters -- a Kotlin compiler feature that inlines the function body and captures type information at the call site. These functions have no regular bytecode method and **cannot be called from Java**. `ModelFactory.kt` wraps these calls into plain functions callable from Java, keeping the project 95% Java.

## SKaiNET Dependencies

All artifacts are consumed from Maven Central with the `-jvm` suffix (Kotlin Multiplatform JVM targets):

- `skainet-lang-core-jvm` -- Tensor operations, Shape, DType
- `skainet-lang-models-jvm` -- Module, sequential DSL
- `skainet-backend-cpu-jvm` -- CPU execution context
- `skainet-data-transform-jvm` -- `Transform<I,O>`, `mnistPreprocessing()`, image transforms
- `skainet-io-image-jvm` -- `PlatformBitmapImage` / image-to-tensor conversion
- `skainet-io-gguf-jvm` -- GGUF model file reader
- `skainet-data-simple-jvm` -- MNIST dataset loader
- `skainet-compile-dag-jvm` -- Graph execution context for inference

## Running Tests

```bash
./gradlew test
```

Tests cover CLI argument parsing (`CliArgsTest`) and pipeline composition (`PipelineTest`).
