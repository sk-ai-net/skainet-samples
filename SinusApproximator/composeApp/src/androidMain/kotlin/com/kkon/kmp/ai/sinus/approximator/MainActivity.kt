package com.kkon.kmp.ai.sinus.approximator

import android.R
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import okio.buffer
import okio.source
import java.io.InputStream


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Suppose we have a file to read from:
        val inputStream: InputStream? = assets.open("sinus.json");

        setContent {
            inputStream?.let {
                App {
                    it.source().buffer()
                }
            }
        }
    }

    fun getAsset(name: String): InputStream? {
        return assets.open(name)
    }
}
