package com.example.homeworkout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.domain.models.RecoveryScore
import com.example.homeworkout.domain.models.enums.RecoveryTier
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PillShape
import com.example.homeworkout.ui.theme.SlateGray

private val OptimalGreen = Color(0xFF10B981)
private val ModerateAmber = Color(0xFFF59E0B)
private val HighStrainCoral = Color(0xFFEF4444)

private fun RecoveryTier.ringColor(): Color = when (this) {
    RecoveryTier.OPTIMAL -> OptimalGreen
    RecoveryTier.MODERATE -> ModerateAmber
    RecoveryTier.HIGH_STRAIN -> HighStrainCoral
}

/**
 * Offline "Readiness & Recovery Score Card" - advisory only. The score and CTA are computed
 * entirely from local history ([com.example.homeworkout.domain.usecases.home.RecoveryCalculator]);
 * tapping a suggested action is always an explicit opt-in, never automatic.
 */
@Composable
fun RecoveryScoreCard(
    recovery: RecoveryScore,
    onApplySuggestion: () -> Unit,
    onKeepOriginal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = recovery.tier.ringColor()

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ScoreRing(score = recovery.score, color = ringColor)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        recovery.tier.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = InkBlack
                    )
                    Text(
                        recovery.tier.rationale,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (recovery.badges.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recovery.badges.forEach { badge -> MetricBadge(text = badge, tint = ringColor) }
                }
            }

            if (recovery.tier == RecoveryTier.OPTIMAL) {
                AppButton(
                    text = "Start Workout",
                    onClick = onKeepOriginal,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    recovery.tier.ctaLabel?.let { cta ->
                        AppButton(
                            text = cta,
                            onClick = onApplySuggestion,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    AppButton(
                        text = "Keep Original Workout",
                        onClick = onKeepOriginal,
                        variant = AppButtonVariant.Outlined,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int, color: Color, modifier: Modifier = Modifier) {
    val sweep = remember(score) { 360f * (score / 100f) }
    Box(modifier = modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = CloudGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            )
        }
        Text("$score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = InkBlack)
    }
}

@Composable
private fun MetricBadge(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .clip(PillShape)
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tint)
    }
}
