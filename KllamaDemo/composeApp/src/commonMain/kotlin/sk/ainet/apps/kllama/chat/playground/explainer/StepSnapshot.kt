package sk.ainet.apps.kllama.chat.playground.explainer

/**
 * Architecture description captured once after the model is loaded.
 * Feeds the static "block stack" visualization in [ArchitectureDiagram].
 */
data class ArchitectureSummary(
    val numLayers: Int,
    val hiddenDim: Int,
    val vocabSize: Int,
    val maxSeqLen: Int,
    val blockNames: List<String>,
)

/**
 * Captures one attention layer's post-softmax probabilities.
 * Stored flat (`heads * seqLen * seqLen`) so it can be rendered as
 * a `heads`-stack of `seqLen × seqLen` heatmaps.
 */
data class AttentionCapture(
    val blockIndex: Int,
    val heads: Int,
    val seqLen: Int,
    val values: FloatArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is AttentionCapture &&
                blockIndex == other.blockIndex &&
                heads == other.heads &&
                seqLen == other.seqLen &&
                values.contentEquals(other.values))

    override fun hashCode(): Int {
        var r = blockIndex
        r = 31 * r + heads
        r = 31 * r + seqLen
        r = 31 * r + values.contentHashCode()
        return r
    }
}

/**
 * Captures the residual stream after a transformer block. The values are
 * already downsampled to at most [MAX_RESIDUAL_BARS] entries for rendering.
 */
data class ResidualCapture(
    val blockIndex: Int,
    val originalDim: Int,
    val downsampledValues: FloatArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is ResidualCapture &&
                blockIndex == other.blockIndex &&
                originalDim == other.originalDim &&
                downsampledValues.contentEquals(other.downsampledValues))

    override fun hashCode(): Int {
        var r = blockIndex
        r = 31 * r + originalDim
        r = 31 * r + downsampledValues.contentHashCode()
        return r
    }
}

/** Top candidate next token: (id, decoded text, softmax probability). */
data class TopKEntry(val id: Int, val text: String, val probability: Float)

/**
 * Everything captured during one `runtime.forward(tokenId)` call. Read by the
 * UI after the forward call returns.
 */
class StepSnapshot(
    val inputTokenId: Int,
    val tokensSoFar: IntArray,
    val sampledTokenId: Int,
    val sampledTokenText: String,
    val topK: List<TopKEntry>,
    val perLayerAttention: List<AttentionCapture>,
    val perLayerResidual: List<ResidualCapture>,
    val elapsedMillis: Long,
)

internal const val MAX_RESIDUAL_BARS = 64
