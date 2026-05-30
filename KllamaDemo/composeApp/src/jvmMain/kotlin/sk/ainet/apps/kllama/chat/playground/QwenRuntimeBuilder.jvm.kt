package sk.ainet.apps.kllama.chat.playground

import java.lang.foreign.Arena
import java.nio.file.Files
import java.nio.file.Path
import sk.ainet.apps.llm.OptimizedLLMMode
import sk.ainet.apps.llm.OptimizedLLMRuntime
import sk.ainet.context.ExecutionContext
import sk.ainet.io.JvmRandomAccessSource
import sk.ainet.io.model.QuantPolicy
import sk.ainet.lang.types.FP32
import sk.ainet.models.llama.DecoderGgufMemSegConverter
import sk.ainet.models.llama.DecoderGgufWeightLoader
import sk.ainet.models.qwen.QwenNetworkLoader

private val QWEN_ARCHS = setOf("qwen2", "qwen3", "qwen35")

internal actual suspend fun buildQwenRuntime(
    ctx: ExecutionContext,
    bytes: ByteArray,
    bosTokenId: Int,
): OptimizedLLMRuntime<FP32> {
    // DecoderGgufWeightLoader + DecoderGgufMemSegConverter both need
    // random-access reads, but we got our bytes via Res.readBytes() from
    // Compose Resources — an in-memory ByteArray. Stage it to a temp file
    // so JvmRandomAccessSource (which uses FileChannel) can serve it.
    // The temp file is reused across the loader's metadata + tensor passes,
    // then deleted on JVM exit.
    val tempFile: Path = stageBytesToTempFile(bytes)

    val loader = DecoderGgufWeightLoader(
        randomAccessProvider = { JvmRandomAccessSource.open(tempFile.toString()) },
        quantPolicy = QuantPolicy.NATIVE_OPTIMIZED,
        acceptedArchitectures = QWEN_ARCHS,
    )

    val rawWeights = loader.loadToMapStreaming<FP32, Float>(ctx)

    // Wrap Q4_0 / Q8_0 tensors as Q4/Q8 MemorySegmentTensorData. Embedded
    // K-quants (Q4_K, Q5_K, Q6_K) and token_embd are dequantized to FP32 by
    // the converter, but matmul-bound layers stay packed.
    val convertedWeights = if (rawWeights.quantTypes.isNotEmpty()) {
        val arena = Arena.ofShared()
        DecoderGgufMemSegConverter.convert(rawWeights, ctx, arena)
    } else {
        rawWeights
    }

    val model = QwenNetworkLoader.fromWeights(convertedWeights)
    return OptimizedLLMRuntime(
        model = model,
        ctx = ctx,
        mode = OptimizedLLMMode.DIRECT,
        dtype = FP32::class,
        bos = convertedWeights.metadata.bosTokenId.takeIf { it >= 0 } ?: bosTokenId,
    )
}

private fun stageBytesToTempFile(bytes: ByteArray): Path {
    // createTempFile already creates an empty file; Files.write will
    // overwrite it. deleteOnExit removes it at JVM shutdown so we don't
    // accumulate 380 MB temp files across launches.
    val tempFile = Files.createTempFile("kllamademo-qwen-", ".gguf")
    Files.write(tempFile, bytes)
    tempFile.toFile().deleteOnExit()
    return tempFile
}
