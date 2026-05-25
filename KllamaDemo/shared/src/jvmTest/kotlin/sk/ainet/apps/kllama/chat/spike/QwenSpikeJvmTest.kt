package sk.ainet.apps.kllama.chat.spike

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Phase-0 spike on JVM: read the embedded Qwen GGUF directly from disk and
 * run a 5-token generation. Verifies the QwenNetworkLoader -> OptimizedLLMRuntime
 * chain actually produces tokens before we wire it through the app.
 *
 * Skips silently if the model file isn't present locally (so CI doesn't fail
 * for contributors who haven't run the fetch script).
 */
class QwenSpikeJvmTest {

    private val modelPath: java.nio.file.Path = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "composeApp",
        "src",
        "commonMain",
        "composeResources",
        "files",
        "qwen3-0.6b-Q3_K_S.gguf",
    ).normalize()

    @Test
    fun loads_model_and_generates_five_tokens() = runBlocking {
        if (!Files.exists(modelPath)) {
            println("[spike] model not present at $modelPath - run scripts/fetch-qwen-model.sh; skipping")
            return@runBlocking
        }

        val bytes = Files.readAllBytes(modelPath)
        println("[spike] loaded ${bytes.size / 1024 / 1024} MB of GGUF bytes")

        when (val result = runQwenSpike(bytes)) {
            is QwenSpikeResult.Success -> {
                println("[spike] OK - tokens=${result.tokenIds} loadMs=${result.loadMillis} genMs=${result.generateMillis}")
                assertTrue(result.tokenIds.size == 5, "expected 5 tokens, got ${result.tokenIds.size}")
            }
            is QwenSpikeResult.Failure -> {
                fail("[spike] failed at stage='${result.stage}': ${result.message}")
            }
        }
    }
}
