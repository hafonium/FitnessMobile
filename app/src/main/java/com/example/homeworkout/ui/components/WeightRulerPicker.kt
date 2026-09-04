package com.example.homeworkout.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private const val WeightStepKg = 0.1f

/** A draggable ruler for choosing weight without typing. */
@Composable
fun WeightRulerPicker(
    valueKg: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val tickSpacingPx = with(LocalDensity.current) { 12.dp.toPx() }
    var dragRemainderPx by remember { mutableFloatStateOf(0f) }
    val selectedTick = (valueKg / WeightStepKg).roundToInt()
    val minTick = (valueRange.start / WeightStepKg).roundToInt()
    val maxTick = (valueRange.endInclusive / WeightStepKg).roundToInt()

    fun updateByTicks(delta: Int) {
        val nextTick = (selectedTick + delta).coerceIn(minTick, maxTick)
        onValueChange(nextTick * WeightStepKg)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Weight ruler"
                stateDescription = "${formatWeight(valueKg)} kilograms"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = { updateByTicks(-1) }) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease weight")
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formatWeight(valueKg),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    " kg",
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { updateByTicks(1) }) {
                Icon(Icons.Default.Add, contentDescription = "Increase weight")
            }
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
        val labelColor = MaterialTheme.colorScheme.onSurface
        val rulerBackground = MaterialTheme.colorScheme.surfaceVariant
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(rulerBackground)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { dragDelta ->
                        dragRemainderPx -= dragDelta
                        val tickDelta = (dragRemainderPx / tickSpacingPx).toInt()
                        if (tickDelta != 0) {
                            updateByTicks(tickDelta)
                            dragRemainderPx -= tickDelta * tickSpacingPx
                        }
                    },
                    onDragStopped = { dragRemainderPx = 0f }
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val centerX = size.width / 2f
                val rulerTop = 18.dp.toPx()
                val labelBaseline = 94.dp.toPx()

                for (offset in -24..24) {
                    val tick = selectedTick + offset
                    if (tick !in minTick..maxTick) continue

                    val x = centerX + offset * tickSpacingPx - dragRemainderPx
                    val isMajor = tick % 10 == 0
                    val isMedium = tick % 5 == 0
                    val tickHeight = when {
                        isMajor -> 40.dp.toPx()
                        isMedium -> 29.dp.toPx()
                        else -> 19.dp.toPx()
                    }
                    drawLine(
                        color = tickColor,
                        start = androidx.compose.ui.geometry.Offset(x, rulerTop),
                        end = androidx.compose.ui.geometry.Offset(x, rulerTop + tickHeight),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    if (isMajor) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = labelColor.toArgb()
                            textAlign = Paint.Align.CENTER
                            textSize = 12.sp.toPx()
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            (tick / 10).toString(),
                            x,
                            labelBaseline,
                            paint
                        )
                    }
                }

                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(centerX, 8.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(centerX, 66.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(centerX, 8.dp.toPx())
                )
            }
        }
        Text(
            "Slide the ruler to adjust",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatWeight(value: Float): String = "%.1f".format(value)

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt()
)
