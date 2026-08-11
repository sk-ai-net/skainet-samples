package sk.ainet.demo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import sk.ainet.ui.theme.SKaiNETTheme

/**
 * The "before" half of the split-screen race. Runs in the `:scalar`
 * process, where SkainetDemoApp pins ScalarKernelProvider — same APK,
 * same model file, same code, no NEON.
 *
 * Preloads the engine on launch and starts generating when the main
 * window broadcasts [ACTION_RACE], so both sides fire simultaneously
 * from a single button.
 */
class ScalarActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

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
        setContent { SKaiNETTheme { ChatScreen(viewModel) } }
    }

    override fun onDestroy() {
        unregisterReceiver(raceReceiver)
        super.onDestroy()
    }
}
