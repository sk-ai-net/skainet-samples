package sk.ainet.samples.kernelrace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sk.ainet.samples.kernelrace.vm.ChatViewModel

/**
 * The "before" half of the split-screen race. Runs in the `:scalar` process, where
 * [KernelRaceApp] pins [sk.ainet.exec.kernel.ScalarKernelProvider] — same APK, same model file,
 * same code, no NEON.
 *
 * Preloads the engine on launch and starts generating when the main window broadcasts
 * [ACTION_RACE], so both sides fire simultaneously from a single button.
 */
class ScalarActivity : ComponentActivity() {

    private val app get() = applicationContext as KernelRaceApp

    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(loadModel = { onProgress -> app.engine(onProgress) }) as T
        }
    }

    private val raceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.generate(intent?.getStringExtra(EXTRA_PROMPT) ?: return)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this, raceReceiver, IntentFilter(ACTION_RACE), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        viewModel.preload()
        setContent {
            App(
                viewModel = viewModel,
                skainetVersion = BuildConfig.SKAINET_VERSION,
                kernelControls = { busy, vm, currentPrompt -> KernelRaceControls(busy, vm, currentPrompt) },
            )
        }
    }

    override fun onDestroy() {
        unregisterReceiver(raceReceiver)
        super.onDestroy()
    }
}
