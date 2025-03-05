package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import kotlinx.io.Source
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App(handleSource: () -> Source) {
    MaterialTheme {
        SinusSliderScreen(handleSource)
    }
}