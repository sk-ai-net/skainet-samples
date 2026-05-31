package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.kllama.GGUFTokenizer
import sk.ainet.apps.kllama.agent.InferenceRuntime
import sk.ainet.apps.kllama.agent.sampleFromLogits
import sk.ainet.apps.kllama.chat.ChatMLTemplate
import sk.ainet.apps.kllama.chat.ChatMessage
import sk.ainet.apps.kllama.chat.ChatRole
import sk.ainet.apps.kllama.chat.domain.model.ChatSession
import sk.ainet.apps.kllama.chat.domain.model.MessageRole
import sk.ainet.lang.types.FP32

actual fun platformEncodePrompt(tokenizer: Any, session: ChatSession): IntArray? {
    val tok = tokenizer as GGUFTokenizer
    val prompt = ChatMLTemplate().apply(buildSKaiNETMessages(session), emptyList(), true)
    return tok.encode(prompt)
}

actual fun platformDecodeToken(tokenizer: Any, tokenId: Int): String? {
    return (tokenizer as GGUFTokenizer).decode(tokenId)
}

actual fun platformEosTokenId(tokenizer: Any): Int {
    return (tokenizer as GGUFTokenizer).eosId
}

@Suppress("UNCHECKED_CAST")
actual fun platformResetRuntime(runtime: Any) {
    (runtime as InferenceRuntime<FP32>).reset()
}

@Suppress("UNCHECKED_CAST")
actual fun platformForwardAndSample(runtime: Any, tokenId: Int, temperature: Float): Int {
    val rt = runtime as InferenceRuntime<FP32>
    val logits = rt.forward(tokenId)
    return sampleFromLogits<FP32>(logits, temperature)
}

private fun buildSKaiNETMessages(session: ChatSession): List<ChatMessage> {
    val messages = mutableListOf<ChatMessage>()

    if (session.systemPrompt.isNotBlank()) {
        messages.add(ChatMessage(role = ChatRole.SYSTEM, content = session.systemPrompt))
    }

    for (msg in session.messages) {
        if (msg.content.isBlank() && msg.isStreaming) continue
        val role = when (msg.role) {
            MessageRole.USER -> ChatRole.USER
            MessageRole.ASSISTANT -> ChatRole.ASSISTANT
            MessageRole.SYSTEM -> ChatRole.SYSTEM
        }
        messages.add(ChatMessage(role = role, content = msg.content))
    }

    return messages
}
