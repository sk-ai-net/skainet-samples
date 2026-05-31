package sk.ainet.apps.kllama.chat.inference

import sk.ainet.apps.kllama.chat.domain.model.ChatSession

actual fun platformEncodePrompt(tokenizer: Any, session: ChatSession): IntArray? = null
actual fun platformDecodeToken(tokenizer: Any, tokenId: Int): String? = null
actual fun platformEosTokenId(tokenizer: Any): Int = -1
actual fun platformResetRuntime(runtime: Any) {}
actual fun platformForwardAndSample(runtime: Any, tokenId: Int, temperature: Float): Int = 0
