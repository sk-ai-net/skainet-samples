package sk.ainet.samples.kernelrace.model

import java.io.File
import kotlin.time.TimeSource
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import sk.ainet.data.source.KtorRemoteDataSourceFetcher
import sk.ainet.samples.kernelrace.platform.logEvent
import kotlinx.io.files.Path as KotlinxPath

/**
 * Desktop has no bundled-asset concept, so this is cache-hit-or-download: the model lands in
 * `~/.skainet-examples/kernelrace/models/` and is reused across runs.
 */
class DesktopModelProvider(
    private val cacheDir: String = defaultCacheDir(),
) : ModelProvider {

    override suspend fun resolve(onProgress: (String) -> Unit): ModelData {
        val dir = File(cacheDir).apply { mkdirs() }
        val target = File(dir, HF_FILE)

        return when (val plan = ModelResolver.plan(target.path, target.exists(), assetAvailable = false)) {
            is ResolutionPlan.UseCached -> {
                logEvent("model_cached", "file" to target.name, "fileMB" to target.length() / 1_000_000)
                ModelData.FilePath(plan.path)
            }
            is ResolutionPlan.UseAsset -> error("desktop has no bundled asset")
            ResolutionPlan.Download -> {
                download(target, onProgress)
                ModelData.FilePath(target.path)
            }
        }
    }

    private suspend fun download(target: File, onProgress: (String) -> Unit) {
        val url = "https://huggingface.co/$HF_REPO/resolve/main/$HF_FILE"
        val tmp = File(target.parentFile, ModelResolver.partPath(target.name))
        val fetcher = KtorRemoteDataSourceFetcher()
        logEvent("download_start", "url" to url)
        val startedAt = TimeSource.Monotonic.markNow()
        var received = 0L
        try {
            val content = fetcher.fetch(url, emptyMap())
            val totalMb = content.sizeBytes?.let { (it / 1_000_000).toString() } ?: "?"
            content.source.use { source ->
                SystemFileSystem.sink(KotlinxPath(tmp.path)).buffered().use { sink ->
                    val chunk = Buffer()
                    var lastReported = -1L
                    while (true) {
                        val n = source.readAtMostTo(chunk, 1024 * 1024)
                        if (n == -1L) break
                        sink.write(chunk, n)
                        received += n
                        val mb = received / 1_000_000
                        if (mb != lastReported) {
                            lastReported = mb
                            onProgress("Downloading model… $mb / $totalMb MB")
                        }
                    }
                }
            }
            check(tmp.renameTo(target)) { "rename failed: $tmp -> $target" }
            logEvent(
                "download_done",
                "fileMB" to received / 1_000_000,
                "durMs" to startedAt.elapsedNow().inWholeMilliseconds,
            )
        } catch (e: Exception) {
            tmp.delete()
            logEvent("download_failed", "receivedMB" to received / 1_000_000, "error" to (e.message ?: e::class.simpleName))
            throw e
        } finally {
            fetcher.close()
        }
    }
}

private fun defaultCacheDir(): String =
    File(System.getProperty("user.home"), ".skainet-examples/kernelrace/models").path
