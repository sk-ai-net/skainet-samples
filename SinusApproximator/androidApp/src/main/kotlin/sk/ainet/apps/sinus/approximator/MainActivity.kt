package sk.ainet.apps.sinus.approximator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import sk.ainet.samples.kmp.sinus.approximator.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
