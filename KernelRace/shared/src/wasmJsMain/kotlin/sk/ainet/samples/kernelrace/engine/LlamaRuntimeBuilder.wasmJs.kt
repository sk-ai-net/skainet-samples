package sk.ainet.samples.kernelrace.engine

import sk.ainet.context.ExecutionContext
import sk.ainet.samples.kernelrace.model.ModelData

/** No filesystem in the browser — the bytes were bundled at build time and read by composeApp
 *  via `Res.readBytes(...)`, handed down as [ModelData.Bytes]. */
actual suspend fun buildLlamaComponents(ctx: ExecutionContext, model: ModelData): LlamaComponents =
    buildLlamaComponentsFallback(ctx, (model as ModelData.Bytes).bytes)
