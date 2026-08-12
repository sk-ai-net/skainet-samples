package sk.ainet.samples.kernelrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import sk.ainet.samples.kernelrace.vm.ChatViewModel

class MainActivity : ComponentActivity() {

    private val app get() = applicationContext as KernelRaceApp

    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatViewModel(loadModel = { onProgress -> app.engine(onProgress) }) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(
                viewModel = viewModel,
                skainetVersion = BuildConfig.SKAINET_VERSION,
                kernelControls = { busy, vm, currentPrompt -> KernelRaceControls(busy, vm, currentPrompt) },
            )
        }
    }
}
