package sk.ainet.demo

import android.content.Context
import java.io.File
import java.io.FileNotFoundException
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import sk.ainet.data.source.KtorRemoteDataSourceFetcher
import kotlinx.io.files.Path as KotlinxPath

const val HF_REPO = "unsloth/SmolLM2-135M-Instruct-GGUF"
const val HF_FILE = "SmolLM2-135M-Instruct-Q8_0.gguf"

/**
 * Resolves the GGUF file: bundled asset if present (offline demo builds),
 * otherwise streamed from the Hugging Face Hub via SKaiNET's Ktor fetcher
 * into filesDir. Chunked copy straight to disk — the model never sits on
 * the ART heap, and progress is reported per real received bytes.
 * (Not the resolver's CachePolicy.Bypass path: that buffers the whole
 * artifact in memory before returning — exactly what a 145 MB file on a
 * 256 MB heap must avoid.)
 *
 * Both app processes (NEON and :scalar) share filesDir, so the model is
 * fetched once per device.
 */
object ModelSource {

    suspend fun resolve(context: Context, onProgress: (String) -> Unit): File {
        val dir = File(context.filesDir, "models").apply { mkdirs() }
        val target = File(dir, HF_FILE)
        if (target.exists()) return target

        copyFromAssetsOrNull(context, target)?.let { return it }

        return download(target, onProgress)
    }

    private fun copyFromAssetsOrNull(context: Context, target: File): File? = try {
        context.assets.open(HF_FILE).use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target
    } catch (_: FileNotFoundException) {
        null
    }

    private suspend fun download(target: File, onProgress: (String) -> Unit): File {
        val url = "https://huggingface.co/$HF_REPO/resolve/main/$HF_FILE"
        // HF_TOKEN comes from local.properties via BuildConfig (gated repos only).
        val headers = BuildConfig.HF_TOKEN.takeIf { it.isNotBlank() }
            ?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()

        // Stream to a .part sibling and rename so an interrupted download
        // retries instead of leaving a truncated GGUF behind.
        val tmp = File(target.parentFile, target.name + ".part")
        val fetcher = KtorRemoteDataSourceFetcher()
        try {
            val content = fetcher.fetch(url, headers)
            val totalMb = content.sizeBytes?.let { "%.0f".format(it / 1e6) } ?: "?"
            content.source.use { source ->
                SystemFileSystem.sink(KotlinxPath(tmp.path)).buffered().use { sink ->
                    val chunk = Buffer()
                    var received = 0L
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
            return target
        } catch (e: Exception) {
            tmp.delete()
            throw e
        } finally {
            fetcher.close()
        }
    }
}
