package sk.ainet.apps.kllama.chat.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import sk.ainet.apps.kllama.chat.domain.model.currentTimeMillis

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: Map<String, String> = emptyMap()
)

object AppLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(level: LogLevel, tag: String, message: String, details: Map<String, String> = emptyMap()) {
        val entry = LogEntry(
            timestamp = currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            details = details
        )
        _logs.value = _logs.value + entry
        println("[${level.name}] [$tag] $message${if (details.isNotEmpty()) " $details" else ""}")
    }

    fun debug(tag: String, message: String, details: Map<String, String> = emptyMap()) =
        log(LogLevel.DEBUG, tag, message, details)

    fun info(tag: String, message: String, details: Map<String, String> = emptyMap()) =
        log(LogLevel.INFO, tag, message, details)

    fun warn(tag: String, message: String, details: Map<String, String> = emptyMap()) =
        log(LogLevel.WARN, tag, message, details)

    fun error(tag: String, message: String, details: Map<String, String> = emptyMap()) =
        log(LogLevel.ERROR, tag, message, details)

    fun clear() {
        _logs.value = emptyList()
    }
}
