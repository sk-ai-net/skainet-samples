package sk.ainet.apps.kllama.chat.data.repository

import kotlinx.io.Source
import sk.ainet.context.ExecutionContext
import sk.ainet.models.llama.LlamaRuntimeWeights
import sk.ainet.lang.types.FP32

/**
 * Result of creating a LLM runtime and tokenizer from model weights.
 *
 * @property runtime SKaiNET InferenceRuntime (LlamaRuntime<FP32> on JVM, null on other platforms)
 * @property tokenizer SKaiNET Tokenizer (GGUFTokenizer on JVM, null on other platforms)
 */
data class RuntimeCreationResult(
    val runtime: Any?,
    val tokenizer: Any?
)

/**
 * Platform-specific factory for creating LLM runtime and tokenizer from GGUF weights.
 *
 * On JVM: creates a real LlamaRuntime with CpuAttentionBackend and a GGUFTokenizer.
 * On other platforms: returns nulls (no inference support yet).
 */
expect fun createRuntimeAndTokenizer(
    ctx: ExecutionContext,
    runtimeWeights: LlamaRuntimeWeights<FP32>,
    sourceProvider: () -> Source
): RuntimeCreationResult
