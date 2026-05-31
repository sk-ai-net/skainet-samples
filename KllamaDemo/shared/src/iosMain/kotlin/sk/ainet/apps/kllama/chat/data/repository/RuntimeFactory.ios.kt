package sk.ainet.apps.kllama.chat.data.repository

import kotlinx.io.Source
import sk.ainet.context.ExecutionContext
import sk.ainet.models.llama.LlamaRuntimeWeights
import sk.ainet.lang.types.FP32

actual fun createRuntimeAndTokenizer(
    ctx: ExecutionContext,
    runtimeWeights: LlamaRuntimeWeights<FP32>,
    sourceProvider: () -> Source
): RuntimeCreationResult = RuntimeCreationResult(runtime = null, tokenizer = null)
