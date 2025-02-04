package com.kkon.kmp.ai.sinus.approximator

import SinusSliderScreen
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import okio.BufferedSource
import org.jetbrains.compose.ui.tooling.preview.Preview


@Composable
@Preview
fun App(handleSource: () -> BufferedSource) {
    MaterialTheme {
        SinusSliderScreen(handleSource)
    }
}