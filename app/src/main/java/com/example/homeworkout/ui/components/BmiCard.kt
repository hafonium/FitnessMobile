package com.example.homeworkout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.domain.models.WeightDashboard
import com.example.homeworkout.ui.theme.CardShape

private val BmiColors = listOf(
    Color(0xFF3F51B5),
    Color(0xFF2196F3),
    Color(0xFF00AFAF),
    Color(0xFFF9A825),
    Color(0xFFEF6C00),
    Color(0xFFD81B60)
)

/** BMI summary shared by the Report and Weight screens. */
@Composable
fun BmiCard(
    data: WeightDashboard,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    showEditIcon: Boolean = false,
    showBmiValue: Boolean = false
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "BMI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (actionLabel != null && onActionClick != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onActionClick)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            actionLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (showEditIcon) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = actionLabel,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            if (showBmiValue) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your BMI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    data.bmi?.let { "%.1f".format(it) } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            BmiSpectrum(data.bmi)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(bmiColor(data.bmi), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    data.bmiCategory?.label ?: "Add weight and height to calculate BMI",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            data.heightCm?.let { heightCm ->
                Spacer(Modifier.height(6.dp))
                Text(
                    "Height ${formatDecimal(heightCm)} cm",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BmiSpectrum(bmi: Double?) {
    val fraction = (((bmi ?: 15.0) - 15.0) / 25.0).coerceIn(0.0, 1.0).toFloat()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(65.dp)) {
        if (bmi != null) {
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth - 44.dp) * fraction)
                    .width(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "%.1f".format(bmi),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val weights = listOf(1f, 2.5f, 6.5f, 5f, 5f, 5f)
            BmiColors.forEachIndexed { index, color ->
                Box(
                    Modifier
                        .weight(weights[index])
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("15", "16", "18.5", "25", "30", "35", "40").forEach { marker ->
                Text(
                    marker,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun bmiColor(bmi: Double?): Color = when {
    bmi == null -> Color(0xFF8B8D98)
    bmi < 16.0 -> BmiColors[0]
    bmi < 18.5 -> BmiColors[1]
    bmi < 25.0 -> BmiColors[2]
    bmi < 30.0 -> BmiColors[3]
    bmi < 35.0 -> BmiColors[4]
    else -> BmiColors[5]
}

private fun formatDecimal(value: Double): String = "%.1f".format(value)
