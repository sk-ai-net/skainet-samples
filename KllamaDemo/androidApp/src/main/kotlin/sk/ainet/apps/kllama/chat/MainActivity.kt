package sk.ainet.apps.kllama.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import sk.ainet.apps.kllama.chat.data.repository.CommonModelLoader
import sk.ainet.apps.kllama.chat.di.ServiceLocator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize ServiceLocator with common model loader
        if (!ServiceLocator.isInitialized) {
            ServiceLocator.configure(loader = CommonModelLoader())
        }

        setContent {
            SetupAndroidFilePicker()
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
