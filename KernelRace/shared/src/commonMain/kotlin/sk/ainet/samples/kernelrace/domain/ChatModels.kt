package sk.ainet.samples.kernelrace.domain

data class UiState(
    val status: String = "Model not loaded",
    val kernelTier: String = "",
    val output: String = "",
    val tokensPerSecond: Double? = null,
    val busy: Boolean = false,
    val scalarMode: Boolean = false,
)
