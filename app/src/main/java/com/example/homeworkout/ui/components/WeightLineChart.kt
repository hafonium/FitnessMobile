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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.homeworkout.domain.models.WeightRecord
import com.example.homeworkout.ui.theme.BrandBlueLight
import com.example.homeworkout.ui.theme.StreakRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight time-series chart shared by Report and Weight; intentionally dependency-free.
 * Points are positioned by their real elapsed time (not by list index), so unevenly-spaced
 * records — e.g. one every 5 days vs. the forecast's fixed 7-day steps — plot with the correct
 * relative gaps instead of being squeezed into equal-width slots.
 */
@Composable
fun WeightLineChart(
    records: List<WeightRecord>,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    showLatestTooltip: Boolean = false,
    lineColor: Color = BrandBlueLight,
    gridColor: Color = Color(0xFF34363C),
    pointFillColor: Color = Color.White,
    /** A dashed continuation series (e.g. the TDEE weight forecast) drawn from the last [records] point onward. */
    forecastRecords: List<WeightRecord> = emptyList(),
    forecastLineColor: Color = StreakRed,
    /** Draws weight values next to each gridline and a handful of dates along the bottom. */
    showAxisLabels: Boolean = false,
    axisLabelColor: Color = Color(0xFF8B8D98)
) {
    Canvas(modifier = modifier) {
        if (records.isEmpty()) return@Canvas

        val leftInset = if (showAxisLabels) 34.dp.toPx() else 12.dp.toPx()
        val rightPadding = 12.dp.toPx()
        val xAxisHeight = if (showAxisLabels) 16.dp.toPx() else 0f
        val tooltipSpace = if (showLatestTooltip) 34.dp.toPx() else 8.dp.toPx()
        val bottomPadding = 10.dp.toPx() + xAxisHeight
        val chartWidth = (size.width - leftInset - rightPadding).coerceAtLeast(1f)
        val chartHeight = (size.height - tooltipSpace - bottomPadding).coerceAtLeast(1f)
        val weights = records.map { it.weightKg.toFloat() }
        val allWeights = weights + forecastRecords.map { it.weightKg.toFloat() }
        val rawMin = allWeights.minOrNull() ?: 0f
        val rawMax = allWeights.maxOrNull() ?: rawMin
        val padding = max((rawMax - rawMin) * 0.2f, 0.5f)
        val minWeight = rawMin - padding
        val maxWeight = rawMax + padding
        val range = (maxWeight - minWeight).coerceAtLeast(1f)

        val totalPoints = records.size + forecastRecords.size
        val allTimestamps = records.map { it.loggedAt } + forecastRecords.map { it.loggedAt }
        val minTime = allTimestamps.min()
        val timeRange = (allTimestamps.max() - minTime).coerceAtLeast(1L)

        val axisLabelPaint = if (showAxisLabels) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = axisLabelColor.toArgb()
                textSize = 10.sp.toPx()
            }
        } else null

        if (showGrid) {
            repeat(4) { index ->
                val y = tooltipSpace + chartHeight * index / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(leftInset, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 7f))
                )
                if (axisLabelPaint != null) {
                    val value = maxWeight - range * index / 3f
                    axisLabelPaint.textAlign = Paint.Align.RIGHT
                    drawContext.canvas.nativeCanvas.drawText(
                        "%.0f".format(value),
                        leftInset - 6.dp.toPx(),
                        y + 3.dp.toPx(),
                        axisLabelPaint
                    )
                }
            }
        }

        fun xForTime(time: Long): Float = if (totalPoints <= 1) {
            size.width / 2f
        } else {
            leftInset + chartWidth * (time - minTime).toFloat() / timeRange.toFloat()
        }

        fun yForWeight(weight: Float): Float {
            val normalized = (weight - minWeight) / range
            return tooltipSpace + chartHeight * (1f - normalized)
        }

        fun offsetFor(record: WeightRecord): Offset = Offset(xForTime(record.loggedAt), yForWeight(record.weightKg.toFloat()))

        if (axisLabelPaint != null && totalPoints > 1) {
            val labelCount = 4
            val y = size.height - 4.dp.toPx()
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            (0 until labelCount).forEach { i ->
                val time = minTime + timeRange * i / (labelCount - 1)
                axisLabelPaint.textAlign = when (i) {
                    0 -> Paint.Align.LEFT
                    labelCount - 1 -> Paint.Align.RIGHT
                    else -> Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText(
                    dateFormat.format(Date(time)),
                    xForTime(time),
                    y,
                    axisLabelPaint
                )
            }
        }

        val path = Path()
        records.forEachIndexed { index, record ->
            val p = offsetFor(record)
            if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        if (records.size > 1) {
            drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        }
        records.forEach { record ->
            val p = offsetFor(record)
            drawCircle(color = pointFillColor, radius = 4.dp.toPx(), center = p)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = p, style = Stroke(2.dp.toPx()))
        }

        if (forecastRecords.isNotEmpty()) {
            val forecastPath = Path()
            val start = offsetFor(records.last())
            forecastPath.moveTo(start.x, start.y)
            forecastRecords.forEach { record ->
                val p = offsetFor(record)
                forecastPath.lineTo(p.x, p.y)
            }
            drawPath(
                forecastPath,
                color = forecastLineColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            )
            forecastRecords.forEach { record ->
                val p = offsetFor(record)
                drawCircle(color = pointFillColor, radius = 3.dp.toPx(), center = p)
                drawCircle(color = forecastLineColor, radius = 3.dp.toPx(), center = p, style = Stroke(2.dp.toPx()))
            }
        }

        if (showLatestTooltip) {
            val latest = offsetFor(records.last())
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
