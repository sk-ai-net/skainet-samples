package sk.ainet.samples.kernelrace.engine

import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import sk.ainet.context.DirectCpuExecutionContext
import sk.ainet.samples.kernelrace.model.HF_FILE
import sk.ainet.samples.kernelrace.model.ModelData

/**
 * Loads the real SmolLM2 GGUF and runs a couple of forward passes end to end. Not a hermetic
 * unit test — it's gated on the model already sitting in the desktop cache dir (the same place
 * [sk.ainet.samples.kernelrace.model.DesktopModelProvider] downloads it to), so a clean CI
 * checkout with no cached model skips it rather than failing the build.
 */
class GgufSmokeTest {

    private val cachedModel = File(
        System.getProperty("user.home"),
        ".skainet-examples/kernelrace/models/$HF_FILE",
    )

    @Test
    fun tokenizesAndGeneratesAFewTokens() = runTest {
        if (!cachedModel.exists()) {
            println("Skipping GgufSmokeTest: no cached model at $cachedModel")
            return@runTest
        }

        val ctx = DirectCpuExecutionContext()
        val engine = LlmEngine.load(ctx, ModelData.FilePath(cachedModel.path))

        val tokens = mutableListOf<String>()
        val text = engine.generate("Say hi in three words.", maxTokens = 8) { tokens.add(it) }

        check(tokens.isNotEmpty()) { "expected at least one streamed token" }
        check(text.isNotBlank()) { "expected non-blank generated text" }
    }
}
