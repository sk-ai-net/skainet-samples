package sk.ainet.samples.kernelrace.model

/**
 * Where the GGUF weights ended up. [FilePath] platforms (Android, desktop) get random-access
 * reads straight off disk; [Bytes] platforms (wasm — no filesystem) hold the whole model
 * in memory.
 */
sealed interface ModelData {
    data class FilePath(val path: String) : ModelData
    data class Bytes(val bytes: ByteArray) : ModelData
}

const val HF_REPO = "unsloth/SmolLM2-135M-Instruct-GGUF"
const val HF_FILE = "SmolLM2-135M-Instruct-Q8_0.gguf"
