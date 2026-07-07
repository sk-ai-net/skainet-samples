package sk.ainet.app.samples.tinytransformer

/**
 * A snapshot of the causal self-attention matrix for one window.
 *
 * @param tokens the window's tokens (row/column labels), length [size]
 * @param matrix row-major `size × size` attention weights (each row sums to 1)
 */
data class AttentionSnapshot(
    val tokens: List<String>,
    val matrix: FloatArray,
    val size: Int,
)

data class TrainingProgress(
    val epoch: Int,
    val loss: Float,
    val attention: AttentionSnapshot?,
    val isCompleted: Boolean = false,
)
