package sk.ainet.samples.kernelrace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sk.ainet.samples.kernelrace.platform.SamplePlatform
import sk.ainet.samples.kernelrace.platform.currentSamplePlatform

/**
 * One chip per platform this sample runs on, with whichever one is currently running
 * highlighted — a quick visual "this sample is genuinely multiplatform" signal, independent of
 * [sk.ainet.samples.kernelrace.platform.kernelTierLabel] (which describes the kernel *inside*
 * the current platform, not the platform list itself). Purely informational — selection is
 * driven by [currentSamplePlatform], not by taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformChips(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (platform in SamplePlatform.entries) {
            FilterChip(
                selected = platform == currentSamplePlatform,
                onClick = {},
                label = { Text(platform.label) },
            )
        }
    }
}
