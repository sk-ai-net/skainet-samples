package sk.ainet.apps.kllama.chat.domain.model

import kotlin.math.pow
import kotlin.math.round
import kotlin.math.abs

/**
 * Formats a decimal number with the specified number of decimal places.
 * Multiplatform-compatible replacement for String.format("%.Xf", value).
 */
private fun formatDecimal(value: Double, decimals: Int = 2): String {
    val factor = 10.0.pow(decimals)
    val rounded = round(value * factor) / factor
    val intPart = rounded.toLong()
    val fracPart = abs(((rounded - intPart) * factor).toLong())
    return "$intPart.${fracPart.toString().padStart(decimals, '0')}"
}

/**
 * Supported model file formats.
 */
enum class ModelFormat {
    GGUF,
    SAFETENSORS,
    UNKNOWN
}

/**
 * Quantization type information.
 */
enum class QuantizationType {
    F32,
    F16,
    Q8_0,
    Q4_K,
    Q4_K_M,
    Q5_K,
    Q5_K_M,
    Q6_K,
    UNKNOWN
}

/**
 * Metadata about a loaded model.
 */
data class ModelMetadata(
    val name: String,
    val parameterCount: Long,
    val sizeBytes: Long,
    val quantization: QuantizationType = QuantizationType.UNKNOWN,
    val contextLength: Int = 2048,
    val vocabSize: Int = 32000,
    val hiddenSize: Int = 0,
    val numLayers: Int = 0,
    val numHeads: Int = 0
) {
    val formattedSize: String
        get() = when {
            sizeBytes >= 1_000_000_000 -> "${formatDecimal(sizeBytes / 1_000_000_000.0)} GB"
            sizeBytes >= 1_000_000 -> "${formatDecimal(sizeBytes / 1_000_000.0)} MB"
            sizeBytes >= 1_000 -> "${formatDecimal(sizeBytes / 1_000.0)} KB"
            else -> "$sizeBytes B"
        }

    val formattedParamCount: String
        get() = when {
            parameterCount >= 1_000_000_000 -> "${formatDecimal(parameterCount / 1_000_000_000.0)}B"
            parameterCount >= 1_000_000 -> "${formatDecimal(parameterCount / 1_000_000.0)}M"
            parameterCount >= 1_000 -> "${formatDecimal(parameterCount / 1_000.0)}K"
            else -> "$parameterCount"
        }
}

/**
 * Represents a fully loaded model ready for inference.
 */
data class LoadedModel(
    val metadata: ModelMetadata,
    val modelPath: String,
    val format: ModelFormat
)

/**
 * Configuration for inference operations.
 */
data class InferenceConfig(
    val maxContextLength: Int = 2048,
    val maxNewTokens: Int = 64,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f
)

/**
 * Real-time statistics during inference.
 */
data class InferenceStatistics(
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Float = 0f,
    val totalTimeMs: Long = 0,
    val promptTokens: Int = 0,
    val peakTps: Float = 0f,
    val prefillTimeMs: Long = 0,
    val timeToFirstTokenMs: Long = 0
) {
    val formattedTps: String
        get() = "${formatDecimal(tokensPerSecond.toDouble())} tok/s"
}
