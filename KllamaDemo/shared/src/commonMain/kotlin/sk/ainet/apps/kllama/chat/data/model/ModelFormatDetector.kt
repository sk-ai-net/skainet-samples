package sk.ainet.apps.kllama.chat.data.model

import sk.ainet.apps.kllama.chat.domain.model.ModelFormat
import sk.ainet.apps.kllama.chat.domain.model.QuantizationType

/**
 * Utility object for detecting model file formats.
 */
object ModelFormatDetector {

    // GGUF magic bytes: "GGUF" in little-endian
    private val GGUF_MAGIC = byteArrayOf(0x47, 0x47, 0x55, 0x46) // "GGUF"

    /**
     * Detect the model format from the file extension.
     *
     * @param path File path
     * @return Detected ModelFormat
     */
    fun detectFromPath(path: String): ModelFormat {
        val lowercasePath = path.lowercase()
        return when {
            lowercasePath.endsWith(".gguf") -> ModelFormat.GGUF
            lowercasePath.endsWith(".safetensors") -> ModelFormat.SAFETENSORS
            else -> ModelFormat.UNKNOWN
        }
    }

    /**
     * Detect the model format from magic bytes.
     *
     * @param header First 4+ bytes of the file
     * @return Detected ModelFormat
     */
    fun detectFromHeader(header: ByteArray): ModelFormat {
        if (header.size < 4) return ModelFormat.UNKNOWN

        // Check for GGUF magic
        if (header.take(4).toByteArray().contentEquals(GGUF_MAGIC)) {
            return ModelFormat.GGUF
        }

        // SafeTensors starts with a JSON-like structure (begins with '{')
        if (header[0] == '{'.code.toByte()) {
            return ModelFormat.SAFETENSORS
        }

        return ModelFormat.UNKNOWN
    }

    /**
     * Detect quantization type from GGUF filename conventions.
     *
     * @param filename Filename to analyze
     * @return Detected QuantizationType
     */
    fun detectQuantization(filename: String): QuantizationType {
        val upper = filename.uppercase()
        return when {
            upper.contains("F32") || upper.contains("FP32") -> QuantizationType.F32
            upper.contains("F16") || upper.contains("FP16") -> QuantizationType.F16
            upper.contains("Q8_0") -> QuantizationType.Q8_0
            upper.contains("Q4_K_M") -> QuantizationType.Q4_K_M
            upper.contains("Q4_K") -> QuantizationType.Q4_K
            upper.contains("Q5_K_M") -> QuantizationType.Q5_K_M
            upper.contains("Q5_K") -> QuantizationType.Q5_K
            upper.contains("Q6_K") -> QuantizationType.Q6_K
            else -> QuantizationType.UNKNOWN
        }
    }
}
