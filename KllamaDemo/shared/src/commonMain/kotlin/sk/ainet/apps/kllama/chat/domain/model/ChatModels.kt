package sk.ainet.apps.kllama.chat.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents the role of a message in a chat conversation.
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Represents a single message in a chat conversation.
 */
@OptIn(ExperimentalUuidApi::class)
data class ChatMessage(
    val id: String = Uuid.random().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = currentTimeMillis(),
    val isStreaming: Boolean = false,
    val tokenCount: Int = 0
)

/**
 * Represents a chat session with a history of messages.
 */
@OptIn(ExperimentalUuidApi::class)
data class ChatSession(
    val id: String = Uuid.random().toString(),
    val messages: List<ChatMessage> = emptyList(),
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant."
    }

    fun addMessage(message: ChatMessage): ChatSession =
        copy(messages = messages + message)

    fun updateLastMessage(content: String, isStreaming: Boolean = false, tokenCount: Int = 0): ChatSession {
        if (messages.isEmpty()) return this
        val updatedMessages = messages.dropLast(1) + messages.last().copy(
            content = content,
            isStreaming = isStreaming,
            tokenCount = tokenCount
        )
        return copy(messages = updatedMessages)
    }

    fun clearMessages(): ChatSession =
        copy(messages = emptyList())
}

/**
 * Platform-specific time function.
 */
expect fun currentTimeMillis(): Long
