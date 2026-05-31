package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import sk.ainet.app.samples.sinus.BuildInfo
import kotlin.math.PI
import kotlin.math.abs

@Composable
fun SinusSliderScreen(
    viewModel: SinusSliderViewModel,
    trainingViewModel: SinusTrainingViewModel? = null
) {
    val trainedModelUpdate by (trainingViewModel?.trainedModelUpdate ?: MutableStateFlow(0)).collectAsState()
    
    val trainedValue = remember(viewModel.sliderValue, trainedModelUpdate) { 
        trainingViewModel?.trainedCalculator?.calculate(viewModel.sliderValue) 
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visualization
        SinusVisualization(
            sliderValue = viewModel.sliderValue,
            actualSinus = viewModel.sinusValue,
            approximatedSinusMlp = viewModel.modelSinusValueMlp,
            approximatedSinusPretrained = viewModel.modelSinusValuePretrained,
            approximatedSinusTrained = trainedValue,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Slider (directly under the plot)
        Slider(
            value = viewModel.sliderValue,
            onValueChange = { viewModel.updateSliderValue(it) },
            valueRange = 0f..(PI.toFloat() / 2),
            modifier = Modifier.fillMaxWidth()
        )

        // Values display
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header values
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Angle: ${viewModel.formattedAngle}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Actual sin: ${viewModel.formattedSinusValue}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // First Row: MLP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "MLP approximated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = viewModel.formattedModelSinusValueMlp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Error: ${viewModel.formattedErrorValueMlp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Second Row: Pretrained and Trained
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pretrained",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF673AB7)
                        )
                        Text(
                            text = viewModel.formattedModelSinusValuePretrained,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF673AB7)
                        )
                        Text(
                            text = "Error: ${viewModel.formattedErrorValuePretrained}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF673AB7)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val trainedColor = Color(0xFF4CAF50)
                        Text(
                            text = "Trained (Local)",
                            style = MaterialTheme.typography.labelSmall,
                            color = trainedColor
                        )
                        Text(
                            text = trainedValue?.let { viewModel.formatValue(it) } ?: "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            color = trainedColor
                        )
                        Text(
                            text = trainedValue?.let { "Error: ${viewModel.formatValue(abs(viewModel.sinusValue - it))}" } ?: "Error: N/A",
                            style = MaterialTheme.typography.bodySmall,
                            color = trainedColor
                        )
                    }
                }
            }
        }

        // Powered by SKaiNET
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Powered by SKaiNET ${BuildInfo.SKAINET_VERSION}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
