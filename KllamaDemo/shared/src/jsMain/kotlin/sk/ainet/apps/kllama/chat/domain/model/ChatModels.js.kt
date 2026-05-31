package sk.ainet.apps.kllama.chat.domain.model

import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()
