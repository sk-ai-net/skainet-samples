package com.kkon.kmp.ai.mnist.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.io.Buffer
import sk.ai.net.samples.kmp.mnist.demo.App
import sk.ai.net.samples.kmp.mnist.demo.LoadingState
import sk.ai.net.samples.kmp.mnist.demo.ResourceUtils


import sk.ainet.clean.data.io.ResourceReader
import mnistdemo.composeapp.generated.resources.Res
import sk.ainet.clean.di.ServiceLocator
import sk.ainet.clean.domain.factory.DigitClassifierFactory
import sk.ainet.clean.domain.factory.DigitClassifierFactoryImpl
import sk.ainet.clean.framework.inference.CnnInferenceModuleAdapter
import sk.ainet.clean.framework.inference.MlpInferenceModuleAdapter


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Clean Architecture ServiceLocator for Android platform
        val androidResourceReader = object : ResourceReader {
            override suspend fun read(path: String): ByteArray? = try {
                Res.readBytes(path)
            } catch (e: Exception) {
                null
            }
        }

        val factory: DigitClassifierFactory = DigitClassifierFactoryImpl(
            repositoryProvider = { ServiceLocator.modelWeightsRepository },
            cnnModuleProvider = { CnnInferenceModuleAdapter.create() },
            mlpModuleProvider = { MlpInferenceModuleAdapter.create() }
        )

        // Inject into ServiceLocator once at startup
        ServiceLocator.configure(
            resourceReader = androidResourceReader,
            digitClassifierFactory = factory,
        )

        setContent {

            val resourcePath = "files/mnist_mlp.gguf"

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

            if (loadingState == LoadingState.Success) {
                println("LoadingState.Success, showing App")
                App {
                    // Provide a Source for the mnist.json file
                    ResourceUtils.getSourceFromResource(resourcePath) ?: Buffer()
                }
            } else if (loadingState is LoadingState.Error) {
                // Show error message
                val errorMessage = (loadingState as LoadingState.Error).message
                println("LoadingState.Error: $errorMessage")
                androidx.compose.material.Text("Error loading resource: $errorMessage")
            } else {
                println("LoadingState: $loadingState, showing loading indicator")
                // Show loading indicator
                sk.ainet.ui.components.LoadingIndicator()
            }
        }
    }
}
