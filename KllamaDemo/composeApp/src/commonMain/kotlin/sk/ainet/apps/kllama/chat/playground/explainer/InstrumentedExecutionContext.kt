package sk.ainet.apps.kllama.chat.playground.explainer

import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.ExecutionContext
import sk.ainet.context.ExecutionObserver
import sk.ainet.lang.nn.hooks.ForwardHooks
import sk.ainet.lang.nn.topology.ModuleNode
import sk.ainet.lang.tensor.Tensor

/**
 * Wraps a [DirectCpuExecutionContext] with [ForwardHooks] + [ExecutionObserver]
 * capture. Used by the Transformer Explainer to peek inside the forward pass
 * one token at a time.
 *
 * Usage:
 * ```kotlin
 * val instr = InstrumentedExecutionContext()
 * val runtime = OptimizedLLMRuntime(model, instr.ctx, ..., bos = ...)
 *
 * instr.reset()
 * runtime.forward(tokenId)
 * val attentions = instr.attentionCaptures()
 * val residuals = instr.residualCaptures()
 * ```
 *
 * - **Residuals** are captured via [ForwardHooks.onForwardEnd] when the module
 *   name matches `blk.N`. The hook receives the block's output tensor; we
 *   downsample to [MAX_RESIDUAL_BARS] floats.
 * - **Attentions** are captured via [ExecutionObserver.onOpEnd] when the op is
 *   `softmax`. The Qwen attention path emits per-block softmax with shape
 *   `[batch, heads, seqQ, seqKV]`. We snapshot every one and keep them in
 *   order — that order corresponds to layer order in DIRECT mode.
 *
 * If the SDPA kernel fuses the softmax (no discrete op visible to the
 * observer), `attentionCaptures()` will be empty and the UI surfaces a
 * "fused SDPA kernel" notice instead of the heatmap.
 */
class InstrumentedExecutionContext {

    /**
     * When false (default), hooks and observer short-circuit immediately.
     * Set true around `runtime.forward(...)` only when the explainer needs
     * the snapshot — that way the other playground tabs don't pay the
     * per-block copy overhead during regular chat / completion.
     */
    var captureEnabled: Boolean = false

    private val hooks = CapturingForwardHooks(::captureEnabled)
    private val observer = CapturingExecutionObserver(::captureEnabled)

    val ctx: ExecutionContext = DirectCpuExecutionContext(_hooks = hooks)

    init {
        ctx.registerObserver(observer)
    }

    fun reset() {
        hooks.reset()
        observer.reset()
    }

    fun residualCaptures(): List<ResidualCapture> = hooks.residuals.toList()
    fun attentionCaptures(): List<AttentionCapture> = observer.attentions.toList()
}

private class CapturingForwardHooks(private val enabled: () -> Boolean) : ForwardHooks {
    val residuals: MutableList<ResidualCapture> = mutableListOf()

    fun reset() = residuals.clear()

    override fun onForwardBegin(module: ModuleNode, input: Any) = Unit

    override fun onForwardEnd(module: ModuleNode, input: Any, output: Any) {
        if (!enabled()) return
        val name = module.name
        // The decoder DSL emits per-block names like "blk.0", "blk.1", ...
        if (!name.startsWith("blk.")) return
        val idx = name.removePrefix("blk.").toIntOrNull() ?: return
        val tensor = output as? Tensor<*, *> ?: return
        val flat = tensor.data.copyToFloatArray()
        val dim = flat.size
        val downsampled = downsample(flat, MAX_RESIDUAL_BARS)
        residuals += ResidualCapture(
            blockIndex = idx,
            originalDim = dim,
            downsampledValues = downsampled,
        )
    }

    private fun downsample(src: FloatArray, target: Int): FloatArray {
        if (src.size <= target) return src.copyOf()
        // Bucket the source into `target` slots and take the slot mean.
        val out = FloatArray(target)
        val ratio = src.size.toDouble() / target.toDouble()
        for (i in 0 until target) {
            val start = (i * ratio).toInt()
            val end = ((i + 1) * ratio).toInt().coerceAtMost(src.size)
            var sum = 0f
            var n = 0
            for (j in start until end) {
                sum += src[j]
                n++
            }
            out[i] = if (n > 0) sum / n else 0f
        }
        return out
    }
}

private class CapturingExecutionObserver(private val enabled: () -> Boolean) : ExecutionObserver {
    val attentions: MutableList<AttentionCapture> = mutableListOf()
    private var nextBlockIndex = 0

    fun reset() {
        attentions.clear()
        nextBlockIndex = 0
    }

    override fun onOpEnd(context: ExecutionContext, opName: String, result: Any?) {
        if (!enabled()) return
        if (opName != "softmax") return
        val tensor = result as? Tensor<*, *> ?: return
        // Expected shape from MultiHeadAttention: [batch, heads, seqQ, seqKV].
        // Drop attention heatmaps with other ranks (rare softmax callsites).
        val dims = tensor.shape.dimensions
        if (dims.size != 4) return
        val heads = dims[1]
        val seqQ = dims[2]
        val seqKV = dims[3]
        if (seqQ != seqKV) return
        val flat = tensor.data.copyToFloatArray()
        attentions += AttentionCapture(
            blockIndex = nextBlockIndex++,
            heads = heads,
            seqLen = seqQ,
            values = flat,
        )
    }
}
