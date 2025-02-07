package com.kkon.kmp.ai.sinus.approximator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.io.asSource
import sk.ai.net.samples.kmp.sinus.approximator.App
import java.io.InputStream
import kotlinx.io.buffered



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Suppose we have a file to read from:
        val inputStream: InputStream? = assets.open("sinus.json");

        setContent {
            inputStream?.let {
                App {
                    it.asSource().buffered()
                }
            }
        }
    }
}
