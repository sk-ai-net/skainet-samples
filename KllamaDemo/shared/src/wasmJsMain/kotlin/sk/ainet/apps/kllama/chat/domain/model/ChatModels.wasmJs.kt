package sk.ainet.apps.kllama.chat.domain.model

/**
 * Returns current time in milliseconds using JavaScript Date.now().
 */
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun currentTimeMillis(): Long = jsDateNow().toLong()
