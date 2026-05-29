package sk.ainet.samples.glove

import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.context.ExecutionContext
import sk.ainet.context.data
import sk.ainet.lang.tensor.Tensor
import sk.ainet.lang.types.FP32
import kotlin.random.Random

/**
 * The SKaiNET-tensor counterpart of [GloVePlain], using the *real* SKaiNET DSL and ops
 * (steps 3–5 of `glove.md`). This class mirrors those sections of the article one-to-one:
 *
 * ```
 * val ctx = DirectCpuExecutionContext()
 * val X = data<FP32, Float>(ctx) { tensor { shape(v, v) { fromArray(values) } } }
 * val scores = ctx.ops.matmul(W, ctx.ops.transpose(C))
 * ```
 *
 * This class focuses on representation + scoring with tensors. The gradient training loop
 * stays in the transparent [GloVePlain] path; a SKaiNET-autodiff trainer is a stretch goal.
 */
public class GloVeTensors(
    public val vocab: Vocabulary,
    public val cooccurrences: Map<PairKey, Float>,
    public val embeddingDim: Int = 4,
    seed: Int = 42,
    private val ctx: ExecutionContext = DirectCpuExecutionContext(),
) {
    private val vocabSize = vocab.size

    /**
     * Step 3: the dense co-occurrence matrix `X[i, j]` as a real SKaiNET tensor.
     * In a real GloVe trainer this would be sparse; dense is fine for teaching.
     */
    public val cooccurrenceTensor: Tensor<FP32, Float> = run {
        val values = FloatArray(vocabSize * vocabSize)
        for ((pair, count) in cooccurrences) {
            values[pair.center * vocabSize + pair.context] = count
        }
        data<FP32, Float>(ctx) {
            tensor { shape(vocabSize, vocabSize) { fromArray(values) } }
        }
    }

    /** Step 4: center-word embedding table W = [vocabSize, embeddingDim]. */
    public val wordVectors: Tensor<FP32, Float> = data<FP32, Float>(ctx) {
        tensor { shape(vocabSize, embeddingDim) { randn(std = 0.01f, random = Random(seed)) } }
    }

    /** Step 4: context-word embedding table C = [vocabSize, embeddingDim]. */
    public val contextVectors: Tensor<FP32, Float> = data<FP32, Float>(ctx) {
        tensor { shape(vocabSize, embeddingDim) { randn(std = 0.01f, random = Random(seed + 1)) } }
    }

    /**
     * Step 5: word-context compatibility matrix `scores = W matMul Cᵀ`, shape
     * [vocabSize, vocabSize]. `scores[i, j]` is how strongly word i matches context word j.
     */
    public fun scores(): Tensor<FP32, Float> =
        ctx.ops.matmul(wordVectors, ctx.ops.transpose(contextVectors))

    /** Read a single compatibility score back out of the tensor (by word). */
    public fun score(centerWord: String, contextWord: String): Float =
        scores().data[vocab.idOf(centerWord), vocab.idOf(contextWord)]
}
