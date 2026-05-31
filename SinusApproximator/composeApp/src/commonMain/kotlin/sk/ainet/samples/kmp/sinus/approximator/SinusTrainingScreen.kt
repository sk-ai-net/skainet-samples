package sk.ainet.samples.kmp.sinus.approximator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI

@Composable
fun SinusTrainingScreen(viewModel: SinusTrainingViewModel) {
    val trainingState by viewModel.trainingState.collectAsState()
    val trainedModelUpdate by viewModel.trainedModelUpdate.collectAsState()
    
    // We want to show the current model performance on a plot
    // We'll use a fixed angle for demonstration or maybe just show the whole curve
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Model Training",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Progress: ${trainingState.epoch} / ${trainingState.totalEpochs}")
                LinearProgressIndicator(
                    progress = { trainingState.epoch.toFloat() / trainingState.totalEpochs },
                    modifier = Modifier.fillMaxWidth(),
                )
                
                // Manual rounding for KMP common code
                val roundedLoss = try {
                   val factor = 100000.0
                   (kotlin.math.round(trainingState.currentLoss.toDouble() * factor) / factor).toString()
                } catch(e: Exception) {
                   trainingState.currentLoss.toString()
                }
                
                Text("Current Loss: $roundedLoss")
                
                if (trainingState.lossHistory.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loss History", style = MaterialTheme.typography.titleSmall)
                    LossVisualization(
                        lossHistory = trainingState.lossHistory,
                        totalEpochs = trainingState.totalEpochs,
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                }
            }
        }

        // Preview plot
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Trained Model Visualization", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Reusing SinusVisualization
                // For the preview, we'll just show the trained model at a fixed position or ignore the others
                val trainedValue = remember(trainedModelUpdate) { viewModel.trainedCalculator.calculate(PI.toFloat() / 4f) }
                SinusVisualization(
                    sliderValue = (PI.toFloat() / 4f), // Show at 45 degrees
                    actualSinus = kotlin.math.sin(PI / 4.0),
                    approximatedSinusMlp = 0f,
                    approximatedSinusPretrained = 0f,
                    approximatedSinusTrained = trainedValue,
                    modifier = Modifier.height(150.dp)
                )
                
                Row(
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50)))
                    Text("Trained Model", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (!trainingState.isTraining && !trainingState.isCompleted) {
            Button(
                onClick = { viewModel.startTraining() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Training")
            }
        } else if (trainingState.isTraining) {
            Button(
                onClick = { /* Could add stop training */ },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Training in progress...")
            }
        } else if (trainingState.isCompleted) {
            Text(
                "Training Completed!",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = { viewModel.startTraining() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Train Again")
            }
        }
    }
}
