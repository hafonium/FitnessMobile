package com.example.homeworkout.ui.core.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BadgeMedallion
import com.example.homeworkout.ui.components.BadgeUnlockedDialog
import com.example.homeworkout.ui.components.BmiCard
import com.example.homeworkout.ui.components.SectionHeader
import com.example.homeworkout.ui.components.StatTile
import com.example.homeworkout.ui.components.WeightLineChart
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.utils.ScreenWrapper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Report tab backed by Room: completed-workout totals/calendar, streaks, weight/BMI, and badges.
 */
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onOpenHistory: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenWeight: () -> Unit
) {
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val weightDashboard by viewModel.weightDashboard.collectAsStateWithLifecycle()
    val workoutSummary by viewModel.workoutSummary.collectAsStateWithLifecycle()
    val weeklyProgress by viewModel.weeklyProgress.collectAsStateWithLifecycle()
    val unseenBadge = badges.firstOrNull { it.isUnlocked && !it.isSeen }

    ScreenWrapper {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Text("REPORT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatTile(Icons.Default.FitnessCenter, workoutSummary.completedWorkouts.toString(), "Workouts")
                        StatTile(
                            Icons.Default.LocalFireDepartment,
                            workoutSummary.totalCalories?.let(::formatCompactDecimal) ?: "—",
                            "Kcal"
                        )
                        StatTile(Icons.Default.Timer, formatMinutes(workoutSummary.totalDurationSeconds), "Minutes")
                    }
                }
            }

            item { SectionHeader(title = "History", actionText = "All records", onActionClick = onOpenHistory) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    weeklyProgress.days.forEach { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                SimpleDateFormat("EEEEE", Locale.ENGLISH).format(Date(day.dayStartMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGray
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (day.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .then(
                                        if (day.isToday && !day.isCompleted) {
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${day.dayOfMonth}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.isCompleted) Color.White else InkBlack
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LabeledValue("Day Streak", "🔥 ${streak.currentStreak}")
                    LabeledValue("Personal Best", "${streak.bestStreak} day" + if (streak.bestStreak == 1) "" else "s")
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenWeight)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("View details", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        val weight = weightDashboard
                        if (weight == null) {
                            Text("Loading weight…", color = SlateGray)
                        } else if (weight.currentWeightKg == null) {
                            Text("No weight recorded yet", color = SlateGray)
                            Text("Tap to record your first measurement.", style = MaterialTheme.typography.bodySmall, color = SlateGray)
                        } else {
                            Text("${formatDecimal(weight.currentWeightKg)} kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            weight.currentLoggedAt?.let {
                                Text(
                                    "Updated ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateGray
                                )
                            }
                            Text(
                                "Heaviest ${formatDecimal(weight.heaviestWeightKg)} · Lightest ${formatDecimal(weight.lightestWeightKg)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGray
                            )
                            WeightLineChart(
                                records = weight.chartRecords,
                                modifier = Modifier.fillMaxWidth().height(96.dp),
                                showGrid = false
                            )
                        }
                    }
                }
            }

            item {
                val weight = weightDashboard
                if (weight == null) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text("BMI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Loading BMI…", color = SlateGray)
                        }
                    }
                } else {
                    BmiCard(
                        data = weight,
                        actionLabel = "Details",
                        onActionClick = onOpenWeight,
                        modifier = Modifier.clickable(onClick = onOpenWeight),
                        showBmiValue = true
                    )
                }
            }

            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenAchievements)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Achievements",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${badges.count { it.isUnlocked }} of ${badges.size} badges unlocked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateGray
                                )
                            }
                            AppButton(
                                text = "View all",
                                onClick = onOpenAchievements,
                                variant = AppButtonVariant.Tonal
                            )
                        }

                        val featured = badges.filter { it.isUnlocked }
                            .sortedByDescending { it.unlockedAt }
                            .take(4)
                            .ifEmpty { badges.take(4) }
                        if (featured.isEmpty()) {
                            Text(
                                "Complete a workout to begin earning badges.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateGray
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                featured.forEach { badge ->
                                    Column(
                                        modifier = Modifier.width(68.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        BadgeMedallion(badge = badge, size = 58.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            badge.definition.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    unseenBadge?.let { badge ->
        BadgeUnlockedDialog(
            badge = badge,
            onDismiss = { viewModel.markBadgeSeen(badge.definition.id) }
        )
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SlateGray)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatDecimal(value: Double?): String = value?.let { "%.1f".format(it) } ?: "—"

private fun formatCompactDecimal(value: Double): String =
    if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)

private fun formatMinutes(totalSeconds: Long): String = when {
    totalSeconds <= 0 -> "0"
    totalSeconds < 60 -> "<1"
    else -> (totalSeconds / 60).toString()
}
