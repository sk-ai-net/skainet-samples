package sk.ainet.samples.kernelrace.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PromptTemplateTest {

    @Test
    fun wrapsPromptInChatMlEnvelope() {
        val envelope = chatMlEnvelope("Explain NEON in two sentences.")

        assertEquals(
            "<|im_start|>user\nExplain NEON in two sentences.<|im_end|>\n<|im_start|>assistant\n",
            envelope,
        )
    }

    @Test
    fun preservesEmbeddedNewlines() {
        val envelope = chatMlEnvelope("line one\nline two")

        assertEquals(
            "<|im_start|>user\nline one\nline two<|im_end|>\n<|im_start|>assistant\n",
            envelope,
        )
    }
}
