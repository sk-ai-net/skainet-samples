package sk.ainet.apps.kllama.chat.inference

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.InferenceConfig
import sk.ainet.apps.kllama.chat.domain.model.LoadedModel
import sk.ainet.apps.kllama.chat.domain.port.GeneratedToken
import sk.ainet.apps.kllama.chat.domain.port.InferenceEngine
import sk.ainet.apps.kllama.chat.logging.AppLogger
import kotlin.concurrent.Volatile
import kotlin.random.Random

/**
 * Inference engine for LLM text generation using SKaiNET's published APIs.
 *
 * On JVM, uses:
 * - SKaiNET's [sk.ainet.apps.kllama.LlamaRuntime] with [sk.ainet.apps.kllama.CpuAttentionBackend]
 * - [sk.ainet.apps.kllama.chat.ChatMLTemplate] for prompt formatting
 * - [sk.ainet.apps.kllama.GGUFTokenizer] for proper BPE/SentencePiece tokenization
 * - [sk.ainet.apps.kllama.agent.sampleFromLogits] for sampling
 *
 * Falls back to demo mode when no runtime or tokenizer is available.
 *
 * @param model The loaded model metadata (or null for demo mode)
 * @param runtime SKaiNET InferenceRuntime (stored as Any for commonMain compatibility)
 * @param tokenizer SKaiNET GGUFTokenizer (stored as Any for commonMain compatibility)
 */
class LlamaInferenceEngine(
    private val model: LoadedModel?,
    private val runtime: Any? = null,
    private val tokenizer: Any? = null
) : InferenceEngine {

    @Volatile
    private var shouldStop = false

    private val statisticsCollector = TokenStatisticsCollector()

    override val isReady: Boolean
        get() = model != null && runtime != null && tokenizer != null

    override fun generate(
        session: ChatSession,
        config: InferenceConfig
    ): Flow<GeneratedToken> = flow {
        shouldStop = false

        if (runtime == null || tokenizer == null) {
            AppLogger.warn("Inference", "No runtime/tokenizer available, using demo mode")
            generateDemoTokens(config).collect { emit(it) }
            return@flow
        }

        // Encode prompt using ChatMLTemplate + GGUFTokenizer
        val promptTokens = platformEncodePrompt(tokenizer, session)
        if (promptTokens == null) {
            AppLogger.warn("Inference", "Platform encoding not supported, using demo mode")
            generateDemoTokens(config).collect { emit(it) }
            return@flow
        }

        val eosTokenId = platformEosTokenId(tokenizer)

        AppLogger.info("Inference", "Generation started", mapOf(
            "promptTokens" to "${promptTokens.size}",
            "maxNewTokens" to "${config.maxNewTokens}",
            "temperature" to "${config.temperature}",
            "eosTokenId" to "$eosTokenId"
        ))

        // Stream tokens using forward + sample loop
        generateWithRuntime(promptTokens, config, eosTokenId).collect { emit(it) }
    }

    /**
     * Generate tokens using SKaiNET's runtime with streaming via forward+sample loop.
     *
     * Prefill: forward each prompt token, keeping only the sample from the last one.
     * Decode: forward+sample in a loop, emitting each token for streaming.
     */
    private fun generateWithRuntime(
        promptTokens: IntArray,
        config: InferenceConfig,
        eosTokenId: Int
    ): Flow<GeneratedToken> = flow {
        val rt = runtime ?: return@flow
        val tok = tokenizer ?: return@flow

        platformResetRuntime(rt)
        statisticsCollector.start(promptTokens.size)

        // Prefill: forward all prompt tokens, keep the sampled next token from the last one
        var nextToken = 0
        for (tokenId in promptTokens) {
            if (shouldStop || !currentCoroutineContext().isActive) return@flow
            nextToken = platformForwardAndSample(rt, tokenId, config.temperature)
        }

        statisticsCollector.recordPrefillDone()
        AppLogger.debug("Inference", "Prefill complete", mapOf(
            "prefillTimeMs" to "${statisticsCollector.getPrefillTimeMs()}"
        ))

        // Decode: generate new tokens with streaming
        var generatedCount = 0
        while (generatedCount < config.maxNewTokens && !shouldStop && currentCoroutineContext().isActive) {
            if (nextToken == eosTokenId) break

            val tokenText = platformDecodeToken(tok, nextToken) ?: ""
            statisticsCollector.recordToken()

            emit(GeneratedToken(
                token = tokenText,
                tokenId = nextToken,
                statistics = statisticsCollector.buildStatistics()
            ))

            yield() // Allow cancellation between tokens

            nextToken = platformForwardAndSample(rt, nextToken, config.temperature)
            generatedCount++
        }

        val finalStats = statisticsCollector.buildStatistics()
        AppLogger.info("Inference", "Generation complete", mapOf(
            "tokensGenerated" to "${finalStats.tokensGenerated}",
            "totalTimeMs" to "${finalStats.totalTimeMs}",
            "averageTps" to "${statisticsCollector.getAverageTps()}",
            "peakTps" to "${finalStats.peakTps}"
        ))
    }

    /**
     * Generate demo tokens for testing without a real model.
     */
    private fun generateDemoTokens(config: InferenceConfig): Flow<GeneratedToken> = flow {
        val demoResponse = buildDemoResponse()

        statisticsCollector.start(0)

        for ((index, char) in demoResponse.withIndex()) {
            if (shouldStop || !currentCoroutineContext().isActive) break
            if (index >= config.maxNewTokens) break

            statisticsCollector.recordToken()

            emit(GeneratedToken(
                token = char.toString(),
                tokenId = char.code,
                statistics = statisticsCollector.buildStatistics()
            ))

            kotlinx.coroutines.delay(20 + Random.nextLong(30))
        }
    }

    private fun buildDemoResponse(): String {
        val responses = listOf(
            "Hello! I'm a demo response from the local LLM. The model is loaded and ready for inference. " +
                    "In a real implementation, this would be actual generated text from the neural network. " +
                    "The streaming display shows how tokens would appear one by one during generation.",
            "I see you've loaded a model! This is a demonstration of the chat interface. " +
                    "The real inference engine would perform forward passes through the transformer layers " +
                    "and sample tokens based on the probability distribution.",
            "Thanks for trying the offline chat app! This placeholder text demonstrates the streaming UI. " +
                    "When connected to a real GGUF model with proper weights, you'll see actual AI-generated responses.",
            "This is a test response showing the token-by-token streaming capability. " +
                    "Notice how the text appears progressively, simulating real LLM inference behavior. " +
                    "The statistics panel shows tokens per second and other metrics."
        )
        return responses.random()
    }

    override fun stopGeneration() {
        shouldStop = true
        AppLogger.info("Inference", "Generation stopped by user")
    }
}
