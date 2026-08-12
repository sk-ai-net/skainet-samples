package sk.ainet.samples.kernelrace.engine

/**
 * SmolLM2-Instruct is a ChatML model — a raw prompt makes it emit
 * `<|im_end|>` immediately. Wrap it in the ChatML envelope it was trained on.
 */
fun chatMlEnvelope(prompt: String): String =
    "<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
