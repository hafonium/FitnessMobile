package com.example.homeworkout.ui.core.report

import androidx.compose.foundation.background
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
import com.example.homeworkout.ui.components.SectionHeader
import com.example.homeworkout.ui.components.StatTile
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.ui.theme.SuccessGreen
import com.example.homeworkout.ui.theme.TileShape
import com.example.homeworkout.utils.ScreenWrapper

/**
 * Report tab. "Day Streak" / "Personal Best" are real, from [ReportViewModel] /
 * [com.example.homeworkout.domain.usecases.report.GetStreakUseCase]. Everything else (Workouts/Kcal/Minute,
 * the weekly calendar row, Weight, BMI) is still static sample figures matching the storyboard —
 * wiring those to `workout_sessions` / `user_weight_logs` remains out of scope for this pass.
 */
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onOpenHistory: () -> Unit,
    onOpenAchievements: () -> Unit
) {
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
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
                        StatTile(Icons.Default.FitnessCenter, "3", "Workouts")
                        StatTile(Icons.Default.LocalFireDepartment, "1", "Kcal")
                        StatTile(Icons.Default.Timer, "0", "Minute")
                    }
                }
            }

            item { SectionHeader(title = "History", actionText = "All records", onActionClick = onOpenHistory) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { index, day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(day, style = MaterialTheme.typography.bodySmall, color = SlateGray)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 3) MaterialTheme.colorScheme.primary else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${30 + index}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == 3) Color.White else InkBlack
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
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Weight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            AppButton(text = "Log", onClick = {}, variant = AppButtonVariant.Tonal)
                        }
                        Text("142.2 kg", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Heaviest 142.2 · Lightest 142.2",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGray
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(TileShape)
                                .background(CloudGray),
                            contentAlignment = Alignment.Center
                        ) { Text("weight trend", style = MaterialTheme.typography.bodySmall, color = SlateGray) }
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("BMI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            AppButton(text = "Edit", onClick = {}, variant = AppButtonVariant.Tonal)
                        }
                        Text("22.8", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Healthy weight", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                        Text("Height 250 cm", style = MaterialTheme.typography.bodySmall, color = SlateGray)
                    }
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
