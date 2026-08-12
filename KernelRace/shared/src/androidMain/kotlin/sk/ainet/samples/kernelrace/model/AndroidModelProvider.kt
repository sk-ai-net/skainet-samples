package sk.ainet.samples.kernelrace.model

import android.content.Context
import java.io.File
import java.io.FileNotFoundException
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlin.time.TimeSource
import sk.ainet.data.source.KtorRemoteDataSourceFetcher
import sk.ainet.samples.kernelrace.platform.logEvent
import kotlinx.io.files.Path as KotlinxPath

/**
 * Resolves the GGUF file: bundled asset if present (offline demo builds), otherwise streamed
 * from the Hugging Face Hub via SKaiNET's Ktor fetcher into filesDir. Chunked copy straight to
 * disk — the model never sits on the ART heap, and progress is reported per real received bytes.
 * (Not the resolver's CachePolicy.Bypass path: that buffers the whole artifact in memory before
 * returning — exactly what a 145 MB file on a 256 MB heap must avoid.)
 *
 * Both app processes (NEON and :scalar) share filesDir, so the model is fetched once per device.
 */
class AndroidModelProvider(
    private val context: Context,
    private val hfToken: String = "",
) : ModelProvider {

    override suspend fun resolve(onProgress: (String) -> Unit): ModelData {
        val dir = File(context.filesDir, "models").apply { mkdirs() }
        val target = File(dir, HF_FILE)

        return when (val plan = ModelResolver.plan(target.path, target.exists(), assetAvailable(context))) {
            is ResolutionPlan.UseCached -> {
                logEvent("model_cached", "file" to target.name, "fileMB" to target.length() / 1_000_000)
                ModelData.FilePath(plan.path)
            }
            is ResolutionPlan.UseAsset -> {
                copyFromAssets(context, target)
                logEvent("model_from_assets", "file" to target.name, "fileMB" to target.length() / 1_000_000)
                ModelData.FilePath(plan.path)
            }
            ResolutionPlan.Download -> {
                download(target, onProgress)
                ModelData.FilePath(target.path)
            }
        }
    }

    private fun assetAvailable(context: Context): Boolean = try {
        context.assets.open(HF_FILE).close()
        true
    } catch (_: FileNotFoundException) {
        false
    }

    private fun copyFromAssets(context: Context, target: File) {
        context.assets.open(HF_FILE).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private suspend fun download(target: File, onProgress: (String) -> Unit) {
        val url = "https://huggingface.co/$HF_REPO/resolve/main/$HF_FILE"
        val headers = hfToken.takeIf { it.isNotBlank() }?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()

        val tmp = File(target.parentFile, ModelResolver.partPath(target.name))
        val fetcher = KtorRemoteDataSourceFetcher()
        logEvent("download_start", "url" to url)
        val startedAt = TimeSource.Monotonic.markNow()
        var received = 0L
        try {
            val content = fetcher.fetch(url, headers)
            val totalMb = content.sizeBytes?.let { formatWholeMb(it.toDouble()) } ?: "?"
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
            val durMs = startedAt.elapsedNow().inWholeMilliseconds
            logEvent(
                "download_done",
                "fileMB" to received / 1_000_000,
                "durMs" to durMs,
                "MBps" to if (durMs > 0) formatWholeMb(received / (durMs / 1000.0)) else null,
            )
        } catch (e: Exception) {
            tmp.delete()
            logEvent(
                "download_failed",
                "receivedMB" to received / 1_000_000,
                "durMs" to startedAt.elapsedNow().inWholeMilliseconds,
                "error" to (e.message ?: e::class.simpleName),
            )
            throw e
        } finally {
            fetcher.close()
        }
    }
}

private fun formatWholeMb(bytes: Double): String = (bytes / 1e6).toLong().toString()
