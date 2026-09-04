package com.example.homeworkout.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.homeworkout.domain.models.WeightRecord
import com.example.homeworkout.ui.theme.BrandBlueLight
import kotlin.math.max
import kotlin.math.min

/** Lightweight time-series chart shared by Report and Weight; intentionally dependency-free. */
@Composable
fun WeightLineChart(
    records: List<WeightRecord>,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    showLatestTooltip: Boolean = false,
    lineColor: Color = BrandBlueLight,
    gridColor: Color = Color(0xFF34363C),
    pointFillColor: Color = Color.White
) {
    Canvas(modifier = modifier) {
        if (records.isEmpty()) return@Canvas

        val horizontalPadding = 12.dp.toPx()
        val tooltipSpace = if (showLatestTooltip) 34.dp.toPx() else 8.dp.toPx()
        val bottomPadding = 10.dp.toPx()
        val chartWidth = (size.width - horizontalPadding * 2).coerceAtLeast(1f)
        val chartHeight = (size.height - tooltipSpace - bottomPadding).coerceAtLeast(1f)
        val weights = records.map { it.weightKg.toFloat() }
        val rawMin = weights.minOrNull() ?: 0f
        val rawMax = weights.maxOrNull() ?: rawMin
        val padding = max((rawMax - rawMin) * 0.2f, 0.5f)
        val minWeight = rawMin - padding
        val maxWeight = rawMax + padding
        val range = (maxWeight - minWeight).coerceAtLeast(1f)

        if (showGrid) {
            repeat(4) { index ->
                val y = tooltipSpace + chartHeight * index / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(horizontalPadding, y),
                    end = Offset(size.width - horizontalPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 7f))
                )
            }
        }

        fun point(index: Int): Offset {
            val x = if (records.size == 1) {
                size.width / 2f
            } else {
                horizontalPadding + chartWidth * index / records.lastIndex.toFloat()
            }
            val normalized = (weights[index] - minWeight) / range
            val y = tooltipSpace + chartHeight * (1f - normalized)
            return Offset(x, y)
        }

        val path = Path()
        records.indices.forEach { index ->
            val p = point(index)
            if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        if (records.size > 1) {
            drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        records.indices.forEach { index ->
            val p = point(index)
            drawCircle(color = pointFillColor, radius = 4.dp.toPx(), center = p)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = p, style = Stroke(2.dp.toPx()))
        }

        if (showLatestTooltip) {
            val latest = point(records.lastIndex)
            val text = "%.1f".format(records.last().weightKg)
            val tooltipWidth = 48.dp.toPx()
            val tooltipHeight = 25.dp.toPx()
            val left = min(
                max(latest.x - tooltipWidth / 2f, 0f),
                size.width - tooltipWidth
            )
            val top = (latest.y - tooltipHeight - 9.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(
                color = Color(0xFF3A3C43),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = 12.sp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            drawContext.canvas.nativeCanvas.drawText(
                text,
                left + tooltipWidth / 2f,
                top + tooltipHeight * 0.68f,
                paint
            )
        }
    }
}
