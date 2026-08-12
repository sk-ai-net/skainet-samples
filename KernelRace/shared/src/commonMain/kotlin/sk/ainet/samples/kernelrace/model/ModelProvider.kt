package sk.ainet.samples.kernelrace.model

/**
 * Resolves the GGUF onto this platform. Android and desktop implementations
 * ([AndroidModelProvider], [DesktopModelProvider]) do real file IO (cache hit → bundled asset →
 * streamed Hugging Face download); wasm has no filesystem, so composeApp reads the bundled
 * Compose resource bytes directly and wraps them as [ModelData.Bytes] — no provider needed there.
 */
interface ModelProvider {
    suspend fun resolve(onProgress: (String) -> Unit): ModelData
}
