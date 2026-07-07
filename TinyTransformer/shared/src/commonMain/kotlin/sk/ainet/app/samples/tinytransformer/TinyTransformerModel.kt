package sk.ainet.app.samples.tinytransformer

import sk.ainet.context.ExecutionContext
import sk.ainet.lang.nn.Module
import sk.ainet.lang.nn.topology.ModuleParameter
import sk.ainet.lang.nn.topology.ModuleParameters
import sk.ainet.lang.tensor.Shape
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.tensor.matmul
import sk.ainet.lang.tensor.plus
import sk.ainet.lang.tensor.softmax
import sk.ainet.lang.tensor.t
import sk.ainet.lang.tensor.times
import sk.ainet.lang.types.DType
import sk.ainet.lang.types.FP32
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The KI-ENNA transformer, faithfully minimal — built with the SKaiNET NN DSL
 * (a custom [Module] composed from differentiable tensor ops):
 *
 * ```
 * X      = E[tokenIds] + P                      token + positional embeddings [T, d]
 * A      = softmax(X·Xᵀ/√d + causalMask)        single-head attention, no Q/K/V  [T, T]
 * O      = A·X                                  attended values                  [T, d]
 * logits = O·W + b                              projection to the vocabulary     [T, V]
 * ```
 *
 * All four parameters (E, P, W, b) are trainable [ModuleParameter]s picked up by
 * `trainableParameters()`; every op in the forward pass has a backward rule on the
 * SKaiNET gradient tape, including the `indexSelect` embedding lookup.
 *
 * The input is a rank-1 FP32 tensor of length [contextLen] holding token ids
 * (as floats — the CPU backend reads indices numerically).
 */
class TinyTransformer(
    val vocabSize: Int,
    val contextLen: Int,
    val dModel: Int = EMBEDDING_DIM,
    ctx: ExecutionContext,
    seed: Int = 42,
) : Module<FP32, Float>(), ModuleParameters<FP32, Float> {

    override val name: String = "TinyTransformer"
    override val modules: List<Module<FP32, Float>> = emptyList()

    private val tokenEmbedding = ModuleParameter.WeightParameter(
        "token_embedding.weight", uniform(ctx, Shape(vocabSize, dModel), Random(seed))
    )
    private val positionEmbedding = ModuleParameter.WeightParameter(
        "position_embedding.weight", uniform(ctx, Shape(contextLen, dModel), Random(seed + 1))
    )
    private val outputWeight = ModuleParameter.WeightParameter(
        "output_projection.weight", uniform(ctx, Shape(dModel, vocabSize), Random(seed + 2))
    )
    private val outputBias = ModuleParameter.BiasParameter(
        "output_projection.bias",
        ctx.fromFloatArray<FP32, Float>(Shape(vocabSize), FP32::class, FloatArray(vocabSize))
    )

    override val params: List<ModuleParameter<FP32, Float>> =
        listOf(tokenEmbedding, positionEmbedding, outputWeight, outputBias)

    // Additive causal mask: 0 on/below the diagonal, -30 above. -30 is enough to
    // zero out an FP32 softmax weight while keeping the tape's softmax backward
    // free of -Inf/NaN (unlike a -1e9 mask).
    private val causalMask: Tensor<FP32, Float> = ctx.fromFloatArray(
        Shape(contextLen, contextLen), FP32::class,
        FloatArray(contextLen * contextLen) { idx ->
            if (idx % contextLen > idx / contextLen) -30f else 0f
        }
    )

    private val attentionScale: Float = 1f / sqrt(dModel.toFloat())

    /** Row-major [contextLen]×[contextLen] attention weights of the most recent forward pass. */
    var lastAttention: FloatArray = FloatArray(contextLen * contextLen)
        private set

    override fun onForward(input: Tensor<FP32, Float>, ctx: ExecutionContext): Tensor<FP32, Float> {
        // Embedding lookup + learned positions: X = E[ids] + P  →  [T, d]
        @Suppress("UNCHECKED_CAST")
        val indices = input as Tensor<DType, *>
        val embedded = ctx.ops.indexSelect(tokenEmbedding.value, indices, 0)
        val x = embedded + positionEmbedding.value

        // Attention without Q/K/V projections: A = softmax(X·Xᵀ/√d + mask)  →  [T, T]
        val scores = x.matmul(x.t()) * attentionScale
        val attention = (scores + causalMask).softmax(-1)

        // Copy the weights out eagerly for the heatmap; later parameter updates
        // and tape operations must not be able to mutate the snapshot.
        val t = contextLen
        lastAttention = FloatArray(t * t) { idx -> attention.data.get(idx / t, idx % t) as Float }

        // O = A·X, logits = O·W + b  →  [T, V]
        val attended = attention.matmul(x)
        return attended.matmul(outputWeight.value) + outputBias.value
    }

    /** Current embedding vector of one token (first [dims] components), for the UI table. */
    fun embeddingRow(tokenId: Int, dims: Int = 6): FloatArray {
        val table = tokenEmbedding.value
        return FloatArray(minOf(dims, dModel)) { j -> table.data.get(tokenId, j) as Float }
    }

    private fun uniform(ctx: ExecutionContext, shape: Shape, random: Random): Tensor<FP32, Float> {
        // Same init as the reference: U(-1/√d, 1/√d)
        val limit = 1f / sqrt(dModel.toFloat())
        val values = FloatArray(shape.volume) { random.nextFloat() * 2f * limit - limit }
        return ctx.fromFloatArray(shape, FP32::class, values)
    }
}
