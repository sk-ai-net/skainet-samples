package sk.ainet.samples.kernelrace.model

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.asSource
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import sk.ainet.samples.kernelrace.platform.logEvent

/**
 * No `skainet-data-source`/`AndroidModelProvider`-style bundled-asset copy step needed here in
 * the same shape: iOS app bundles are read-only, so a bundled GGUF is used directly from
 * `NSBundle.mainBundle` rather than copied into the cache dir first. Otherwise the same
 * cache-hit-or-download shape as [DesktopModelProvider].
 */
@OptIn(ExperimentalForeignApi::class)
class IosModelProvider(
    private val cacheDir: String = defaultCacheDir(),
) : ModelProvider {

    override suspend fun resolve(onProgress: (String) -> Unit): ModelData {
        SystemFileSystem.createDirectories(Path(cacheDir))
        val target = Path(cacheDir, HF_FILE)

        return when (val plan = ModelResolver.plan(target.toString(), targetExists(target), bundledAssetPath() != null)) {
            is ResolutionPlan.UseCached -> {
                logEvent("model_cached", "file" to HF_FILE, "fileMB" to sizeOf(target) / 1_000_000)
                ModelData.FilePath(plan.path)
            }
            is ResolutionPlan.UseAsset -> {
                val assetPath = checkNotNull(bundledAssetPath()) { "bundled asset disappeared between check and use" }
                logEvent("model_from_bundle", "file" to HF_FILE)
                ModelData.FilePath(assetPath)
            }
            ResolutionPlan.Download -> {
                download(target, onProgress)
                ModelData.FilePath(target.toString())
            }
        }
    }

    private fun bundledAssetPath(): String? =
        NSBundle.mainBundle.pathForResource(HF_FILE.substringBeforeLast('.'), HF_FILE.substringAfterLast('.'))

    private fun targetExists(path: Path): Boolean = SystemFileSystem.exists(path)

    private fun sizeOf(path: Path): Long = SystemFileSystem.metadataOrNull(path)?.size ?: 0L

    private suspend fun download(target: Path, onProgress: (String) -> Unit) {
        val url = "https://huggingface.co/$HF_REPO/resolve/main/$HF_FILE"
        val tmp = Path(cacheDir, ModelResolver.partPath(HF_FILE))
        val client = HttpClient(Darwin) {
            expectSuccess = true
            install(HttpTimeout) {
                requestTimeoutMillis = 600_000
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 600_000
            }
        }
        logEvent("download_start", "url" to url)
        val startedAt = TimeSource.Monotonic.markNow()
        var received = 0L
        try {
            val response = client.get(url) { header(HttpHeaders.Accept, "*/*") }
            val totalMb = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()?.let { it / 1_000_000 }
            response.bodyAsChannel().asSource().buffered().use { source ->
                SystemFileSystem.sink(tmp).buffered().use { sink ->
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
                            onProgress("Downloading model… $mb / ${totalMb ?: "?"} MB")
                        }
                    }
                }
            }
            SystemFileSystem.atomicMove(tmp, target)
            val durMs = startedAt.elapsedNow().inWholeMilliseconds
            logEvent(
                "download_done",
                "fileMB" to received / 1_000_000,
                "durMs" to durMs,
                "MBps" to if (durMs > 0) (received / (durMs / 1000.0) / 1e6).toLong() else null,
            )
        } catch (e: Exception) {
            if (SystemFileSystem.exists(tmp)) SystemFileSystem.delete(tmp)
            logEvent(
                "download_failed",
                "receivedMB" to received / 1_000_000,
                "durMs" to startedAt.elapsedNow().inWholeMilliseconds,
                "error" to (e.message ?: "unknown"),
            )
            throw e
        } finally {
            client.close()
        }
    }

    companion object {
        fun defaultCacheDir(): String {
            val caches = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
                .firstOrNull() as? String
            return "${caches ?: NSFileManager.defaultManager.currentDirectoryPath}/kernelrace/models"
        }
    }
}
