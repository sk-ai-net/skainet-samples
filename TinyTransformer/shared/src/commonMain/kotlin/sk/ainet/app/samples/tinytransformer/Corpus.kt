package sk.ainet.app.samples.tinytransformer

/**
 * Default training corpus — the six German example sentences from the original
 * KI-ENNA page (https://statistical-thinking.de/ki-enna-transformer.html).
 * Small on purpose: the whole point is that a transformer can be trained on it
 * live, in seconds, on any device.
 */
val DEFAULT_CORPUS: List<String> = listOf(
    "Der Hund bellt laut",
    "Der Hund frisst Knochen",
    "Der Fisch schwimmt ruhig",
    "Der Fisch knabbert Futter",
    "Der Hamster rennt schnell",
    "Der Hamster mag Körner",
)

const val DEFAULT_MAX_VOCAB: Int = 40
const val DEFAULT_CONTEXT_LEN: Int = 8
const val DEFAULT_EPOCHS: Int = 120
const val DEFAULT_LEARNING_RATE: Float = 0.05f
const val EMBEDDING_DIM: Int = 12
