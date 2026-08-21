package sk.ai.net.samples.kmp.mnist.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.io.Buffer
// These classes are in the same package, but importing them explicitly for clarity
import sk.ai.net.samples.kmp.mnist.demo.App
import sk.ai.net.samples.kmp.mnist.demo.LoadingState
import sk.ai.net.samples.kmp.mnist.demo.ResourceUtils

class MainActivity : ComponentActivity() {
    // Path to the default model weights file (MLP)
    private val resourcePath = "files/mnist_mlp.gguf"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val loadingState by ResourceUtils.loadingState.collectAsState()

            // Load the mnist.json resource when the app starts
            LaunchedEffect(Unit) {
                println("LaunchedEffect triggered, loading resource")
                try {
                    ResourceUtils.loadResource(resourcePath)
                    println("Resource loaded successfully")
                } catch (e: Exception) {
                    println("Error loading resource: ${e.message ?: "Unknown error (null message)"}")
                    e.printStackTrace()
                }
            }

            when (loadingState) {
                LoadingState.Success -> {
                    println("LoadingState.Success, showing App")
                    App {
                        // Provide a Source for the mnist.json file
                        ResourceUtils.getSourceFromResource(resourcePath) ?: Buffer()
                    }
                }
                is LoadingState.Error -> {
                    // Show error message
                    val errorMessage = (loadingState as LoadingState.Error).message
                    println("LoadingState.Error: $errorMessage")
                    androidx.compose.material.Text("Error loading resource: $errorMessage")
                }

                else -> {
                    println("LoadingState: $loadingState, showing loading indicator")
                    // Show loading indicator
                    sk.ainet.ui.components.LoadingIndicator()
                }
            }
        }
    }
}
