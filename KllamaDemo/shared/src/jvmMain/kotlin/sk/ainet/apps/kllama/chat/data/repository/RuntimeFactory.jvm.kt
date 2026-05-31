package sk.ainet.apps.kllama.chat.data.repository

import kotlinx.io.Source
import sk.ainet.apps.kllama.CpuAttentionBackend
import sk.ainet.apps.kllama.GGUFTokenizer
import sk.ainet.context.ExecutionContext
import sk.ainet.models.llama.LlamaRuntime
import sk.ainet.models.llama.LlamaRuntimeWeights
import sk.ainet.lang.types.FP32

actual fun createRuntimeAndTokenizer(
    ctx: ExecutionContext,
    runtimeWeights: LlamaRuntimeWeights<FP32>,
    sourceProvider: () -> Source
): RuntimeCreationResult {
    val backend = CpuAttentionBackend(ctx, runtimeWeights, FP32::class)
    val runtime = LlamaRuntime(ctx, runtimeWeights, backend, FP32::class)
    val tokenizer = GGUFTokenizer.fromSource(sourceProvider())
    return RuntimeCreationResult(runtime = runtime, tokenizer = tokenizer)
}
