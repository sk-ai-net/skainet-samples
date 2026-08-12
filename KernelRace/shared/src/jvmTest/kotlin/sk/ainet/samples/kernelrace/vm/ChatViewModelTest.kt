package sk.ainet.samples.kernelrace.vm

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import sk.ainet.samples.kernelrace.engine.GenerativeEngine

/**
 * [androidx.lifecycle.ViewModel.viewModelScope] launches on `Dispatchers.Main`, and
 * `ChatViewModel.generate` hops to its injectable `generationDispatcher` for the actual
 * generation call. Both need to run on the SAME virtual-time scheduler as the test body for
 * `runTest` to see the launched work complete — an [UnconfinedTestDispatcher] tied to
 * `runTest`'s own `testScheduler` does that: everything (Main + generation) executes eagerly,
 * on the one scheduler `runTest` already knows how to wait for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class FakeEngine(private val piecesToEmit: List<String>) : GenerativeEngine {
        var generateCalls = 0
        override suspend fun generate(prompt: String, maxTokens: Int, onToken: (String) -> Unit): String {
            generateCalls++
            piecesToEmit.forEach(onToken)
            return piecesToEmit.joinToString("")
        }
    }

    private fun viewModel(
        loadModel: suspend (onProgress: (String) -> Unit) -> GenerativeEngine,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
    ) = ChatViewModel(loadModel = loadModel, generationDispatcher = dispatcher)

    @Test
    fun generateStreamsOutputAndEndsIdle() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val engine = FakeEngine(listOf("Hel", "lo"))
        val vm = viewModel(loadModel = { engine }, dispatcher = dispatcher)

        vm.generate("hi")

        val state = vm.state.value
        assertEquals("Hello", state.output)
        assertFalse(state.busy)
        assertTrue(state.status.startsWith("Done"))
        assertEquals(1, engine.generateCalls)
    }

    @Test
    fun blankPromptIsIgnored() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val engine = FakeEngine(listOf("x"))
        val vm = viewModel(loadModel = { engine }, dispatcher = dispatcher)

        vm.generate("   ")

        assertEquals(0, engine.generateCalls)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun secondCallWhileBusyIsIgnored() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        // A load that never returns keeps the ViewModel busy for the duration of this test.
        val engine = FakeEngine(listOf("a"))
        val vm = viewModel(loadModel = { awaitCancellation() }, dispatcher = dispatcher)

        vm.generate("first")
        vm.generate("second") // dropped: still busy, load hasn't resolved

        assertEquals(0, engine.generateCalls)
        assertTrue(vm.state.value.busy)
    }

    @Test
    fun loadFailureSurfacesAsAnErrorStatus() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val vm = viewModel(loadModel = { error("boom") }, dispatcher = dispatcher)

        vm.generate("hi")

        val state = vm.state.value
        assertFalse(state.busy)
        assertTrue(state.status.contains("Error"))
    }

    @Test
    fun engineIsCachedAcrossGenerations() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val engine = FakeEngine(listOf("x"))
        var loadCalls = 0
        val vm = viewModel(loadModel = { loadCalls++; engine }, dispatcher = dispatcher)

        vm.generate("first")
        vm.generate("second")

        assertEquals(1, loadCalls)
        assertEquals(2, engine.generateCalls)
    }

    @Test
    fun kernelModeChangeDropsTheCachedEngineAndClearsOutput() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val engine = FakeEngine(listOf("x"))
        var loadCalls = 0
        val vm = viewModel(loadModel = { loadCalls++; engine }, dispatcher = dispatcher)

        vm.generate("first")

        vm.onKernelModeChanged(newKernelTier = "SCALAR", scalarMode = true)
        assertEquals("", vm.state.value.output)
        assertEquals("SCALAR", vm.state.value.kernelTier)
        assertTrue(vm.state.value.scalarMode)

        vm.generate("second")

        assertEquals(2, loadCalls) // engine reloaded after the mode change
    }
}
