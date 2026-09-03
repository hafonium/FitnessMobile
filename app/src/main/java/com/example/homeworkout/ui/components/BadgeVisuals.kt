package com.example.homeworkout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.homeworkout.domain.models.BadgeIcon
import com.example.homeworkout.domain.models.BadgeMetric
import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.models.BadgeTier
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.SlateGray
import java.text.DateFormat
import java.util.Date

private val Bronze = Color(0xFFB87333)
private val BronzeLight = Color(0xFFF4D6B8)
private val Silver = Color(0xFF88909A)
private val SilverLight = Color(0xFFE1E5EA)
private val Gold = Color(0xFFE3A008)
private val GoldLight = Color(0xFFFFE8A3)
private val Platinum = Color(0xFF5D63D8)
private val PlatinumLight = Color(0xFFDDE0FF)

private data class TierPalette(val strong: Color, val soft: Color)

private fun BadgeTier.palette(): TierPalette = when (this) {
    BadgeTier.BRONZE -> TierPalette(Bronze, BronzeLight)
    BadgeTier.SILVER -> TierPalette(Silver, SilverLight)
    BadgeTier.GOLD -> TierPalette(Gold, GoldLight)
    BadgeTier.PLATINUM -> TierPalette(Platinum, PlatinumLight)
}

private fun BadgeIcon.imageVector(): ImageVector = when (this) {
    BadgeIcon.DUMBBELL -> Icons.Default.FitnessCenter
    BadgeIcon.TRENDING_UP -> Icons.AutoMirrored.Filled.TrendingUp
    BadgeIcon.STARS -> Icons.Default.Stars
    BadgeIcon.MEDAL -> Icons.Default.MilitaryTech
    BadgeIcon.PREMIUM -> Icons.Default.WorkspacePremium
    BadgeIcon.FIRE -> Icons.Default.LocalFireDepartment
    BadgeIcon.BOLT -> Icons.Default.Bolt
    BadgeIcon.CALENDAR -> Icons.Default.CalendarMonth
    BadgeIcon.CLOCK -> Icons.Default.Timer
    BadgeIcon.TROPHY -> Icons.Default.EmojiEvents
}

@Composable
fun BadgeMedallion(
    badge: BadgeProgress,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val palette = badge.definition.tier.palette()
    val unlocked = badge.isUnlocked
    val outer = if (unlocked) palette.strong else SlateGray.copy(alpha = 0.45f)
    val inner = if (unlocked) palette.soft else CloudGray

    Box(
        modifier = modifier.size(size).clip(CircleShape).background(outer),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(size * 0.78f).clip(CircleShape).background(inner),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badge.definition.icon.imageVector(),
                contentDescription = badge.definition.title,
                tint = if (unlocked) outer else SlateGray,
                modifier = Modifier.size(size * 0.42f)
            )
        }
        if (!unlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.34f)
                    .clip(CircleShape)
                    .background(InkBlack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.18f)
                )
            }
        }
    }
}

@Composable
fun BadgeDetailDialog(badge: BadgeProgress, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { BadgeMedallion(badge = badge, size = 92.dp) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    badge.definition.title,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    badge.definition.tier.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = badge.definition.tier.palette().strong
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    badge.definition.description,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                LinearProgressIndicator(
                    progress = { badge.progressFraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = badge.definition.tier.palette().strong,
                    trackColor = CloudGray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Progress", style = MaterialTheme.typography.bodySmall, color = SlateGray)
                    Text(
                        badge.progressLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                badge.unlockedAt?.let { unlockedAt ->
                    Text(
                        "Unlocked ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(unlockedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = badge.definition.tier.palette().strong,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun BadgeUnlockedDialog(badge: BadgeProgress, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        icon = { BadgeMedallion(badge = badge, size = 104.dp) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "BADGE UNLOCKED",
                    style = MaterialTheme.typography.labelLarge,
                    color = badge.definition.tier.palette().strong,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    badge.definition.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                badge.definition.description,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Awesome", fontWeight = FontWeight.Bold)
            }
        }
    )
}

fun BadgeProgress.progressLabel(): String = when (definition.metric) {
    BadgeMetric.COMPLETED_SESSIONS -> "$currentValue / ${definition.targetValue} workouts"
    BadgeMetric.BEST_STREAK_DAYS -> "$currentValue / ${definition.targetValue} days"
    BadgeMetric.TOTAL_DURATION_SECONDS -> {
        val currentMinutes = currentValue / 60
        val targetHours = definition.targetValue / 3600
        "${currentMinutes / 60}h ${currentMinutes % 60}m / ${targetHours}h"
    }
    BadgeMetric.COMPLETED_PLANS -> "$currentValue / ${definition.targetValue} plans"
}
