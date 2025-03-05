package sk.ai.net.samples.kmp.sinus.approximator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kkon.kmp.ai.sinus.approximator.ASinusCalculator
import kotlinx.coroutines.launch
import kotlinx.io.Source
import kotlin.math.sin

@Composable
fun DrawingScreen(handleSource: () -> Source) {
    var paths by remember { mutableStateOf(listOf<Path>()) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPathRef by remember { mutableStateOf(1) }
    var lastOffset by remember { mutableStateOf<Offset?>(null) }

    var calculator by remember { mutableStateOf(ASinusCalculator(handleSource)) }

    // State für die Modellberechnung und ob das Modell geladen wurde
    var isModelLoaded by remember { mutableStateOf(false) }
    var classificationResult by remember { mutableStateOf<String?>(null) }

    // Sinus des aktuellen Wertes berechnen
    val sinusValue = sin(0.0)
    val modelSinusValue = calculator.calculate(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Instruction Text
        Text(
            text = "Draw a digit (0-9) in the canvas below.\nPress 'Classify' to recognize the digit or 'Clear' to start over.",
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Drawing Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = Path()
                            currentPath?.moveTo(offset.x, offset.y)
                            currentPathRef += 1
                            lastOffset = offset
                        },
                        onDrag = { _, offset ->
                            if (lastOffset != null) {
                                val newOffset = Offset(
                                    lastOffset!!.x + offset.x,
                                    lastOffset!!.y + offset.y
                                )
                                currentPath?.lineTo(newOffset.x, newOffset.y)
                                currentPathRef += 1
                                lastOffset = newOffset
                            }
                        },
                        onDragEnd = {
                            currentPath.let { value ->
                                if (value != null) {
                                    paths = paths + value
                                    currentPath = null
                                    currentPathRef = 0
                                }
                            }
                        }
                    )
                },
            onDraw = {
                paths.forEach { drawPath(path = it, color = Color.Black, style = Stroke(10f)) }

                if (currentPath != null && currentPathRef > 0) {
                    drawPath(path = currentPath!!, color = Color.Black, style = Stroke(10f))
                }
            }
        )

        // Buttons and Result Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val coroutineScope = rememberCoroutineScope()

                if (!isModelLoaded) {
                    Button(
                        onClick = {
                            isModelLoaded = true
                            coroutineScope.launch {
                                calculator.loadModel()
                            }
                        },
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text("Load Model")
                    }
                } else {
                    Button(
                        onClick = {
                            // TODO: Implement classification logic
                            classificationResult = "Predicted digit: 5" // Placeholder result
                        }
                    ) {
                        Text("Classify")
                    }
                }

                Button(
                    onClick = {
                        paths = emptyList()
                        currentPath = null
                        classificationResult = null // Clear the result when canvas is cleared
                    }
                ) {
                    Text("Clear")
                }
            }

            // Classification Result
            classificationResult?.let { result ->
                Text(
                    text = result,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
