package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.kllama.chat.domain.model.ChatSession

/**
 * Platform-specific: format a ChatSession into a prompt string using ChatMLTemplate,
 * and encode it to token IDs.
 *
 * @param runtime InferenceRuntime (Any for commonMain compatibility)
 * @param tokenizer GGUFTokenizer (Any for commonMain compatibility)
 * @param session The chat session
 * @return encoded prompt token IDs, or null if platform doesn't support
 */
expect fun platformEncodePrompt(tokenizer: Any, session: ChatSession): IntArray?

/**
 * Platform-specific: decode a single token ID to its text representation.
 *
 * @return decoded text, or null if platform doesn't support
 */
expect fun platformDecodeToken(tokenizer: Any, tokenId: Int): String?

/**
 * Platform-specific: get the EOS token ID from the tokenizer.
 *
 * @return EOS token ID, or -1 if not available
 */
expect fun platformEosTokenId(tokenizer: Any): Int

/**
 * Platform-specific: reset the inference runtime (clear KV cache).
 */
expect fun platformResetRuntime(runtime: Any)

/**
 * Platform-specific: run a single forward pass and sample the next token.
 *
 * @return the sampled token ID
 */
expect fun platformForwardAndSample(runtime: Any, tokenId: Int, temperature: Float): Int
