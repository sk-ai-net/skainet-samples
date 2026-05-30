package sk.ainet.apps.kllama.chat.playground.explainer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Vertical block stack showing the Qwen3 decoder architecture.
 * Click a block to select it for the heatmap + residual panels.
 *
 *      ┌──────────────┐
 *      │ token_embed  │
 *      └──────────────┘
 *             │
 *      ┌──────────────┐
 *      │   blk.0      │  ← selected has a thick stroke
 *      └──────────────┘
 *             │
 *           ... N blocks
 *             │
 *      ┌──────────────┐
 *      │  output (lm) │
 *      └──────────────┘
 */
@Composable
fun ArchitectureDiagram(
    arch: ArchitectureSummary,
    selectedBlock: Int,
    onBlockSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Qwen3 — ${arch.numLayers} decoder blocks · dim=${arch.hiddenDim} · vocab=${arch.vocabSize}",
            style = MaterialTheme.typography.labelMedium,
        )
        val rowHeightDp = 22.dp
        // Token embed + N blocks + output = (N+2) rows.
        val totalRows = arch.numLayers + 2
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeightDp * totalRows)
                .pointerInput(arch.numLayers) {
                    detectTapGestures { offset ->
                        val rowH = size.height / totalRows
                        val rowIdx = (offset.y / rowH).toInt()
                        // row 0 = token_embed, rows [1..N] = blocks, row N+1 = output
                        val blockIdx = rowIdx - 1
                        if (blockIdx in 0 until arch.numLayers) onBlockSelected(blockIdx)
                    }
                },
        ) {
            val rowH = size.height / totalRows
            val boxW = size.width * 0.6f
            val boxX = (size.width - boxW) / 2

            fun drawRow(row: Int, label: String, selected: Boolean, isBlock: Boolean) {
                val color = when {
                    selected -> Color(0xFFFFE082)
                    isBlock -> Color(0xFFB3E5FC)
                    else -> Color(0xFFCFD8DC)
                }
                drawRect(
                    color = color,
                    topLeft = Offset(boxX, row * rowH + 2f),
                    size = Size(boxW, rowH - 4f),
                )
                drawRect(
                    color = if (selected) Color(0xFFFF8F00) else Color(0xFF455A64),
                    topLeft = Offset(boxX, row * rowH + 2f),
                    size = Size(boxW, rowH - 4f),
                    style = Stroke(width = if (selected) 2.5f else 1f),
                )
                // small "rail" line between rows
                if (row < totalRows - 1) {
                    drawLine(
                        color = Color(0xFF607D8B),
                        start = Offset(size.width / 2, row * rowH + rowH - 2f),
                        end = Offset(size.width / 2, (row + 1) * rowH + 2f),
                        strokeWidth = 1.5f,
                    )
                }
            }

            drawRow(0, "token_embed", selected = false, isBlock = false)
            for (i in 0 until arch.numLayers) {
                drawRow(i + 1, "blk.$i", selected = i == selectedBlock, isBlock = true)
            }
            drawRow(arch.numLayers + 1, "output", selected = false, isBlock = false)
        }
        Text(
            "Selected: blk.$selectedBlock",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
