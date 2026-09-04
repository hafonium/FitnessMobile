package com.example.homeworkout.ui.core.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.WorkoutHistoryEntry
import com.example.homeworkout.domain.models.WorkoutHistorySummary
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.WorkoutCalendar
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = "History", onNavigateBack = onNavigateBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                WorkoutCalendar(
                    visibleMonthStartMillis = state.visibleMonthStartMillis,
                    days = state.calendarDays,
                    onPreviousMonth = viewModel::showPreviousMonth,
                    onNextMonth = viewModel::showNextMonth,
                    onDaySelected = viewModel::selectDay
                )
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.errorMessage != null) {
                item {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                            AppButton(
                                text = "Retry",
                                onClick = viewModel::retry,
                                variant = AppButtonVariant.Tonal
                            )
                        }
                    }
                }
            } else {
                item {
                    WeeklySummaryCard(
                        fromMillis = state.weekFromMillis,
                        toMillisExclusive = state.weekToMillis,
                        summary = state.weeklySummary
                    )
                }

                if (state.weeklySessions.isEmpty()) {
                    item {
                        AppCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "No completed workouts this week.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Complete a workout to see it here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.weeklySessions, key = { it.sessionId }) { session ->
                        HistorySessionRow(session)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(
    fromMillis: Long,
    toMillisExclusive: Long,
    summary: WorkoutHistorySummary
) {
    val endInclusive = toMillisExclusive - 1
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val calories = summary.totalCaloriesBurned?.let { "${formatDecimal(it)} Kcal" } ?: "— Kcal"
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Weekly Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${dateFormat.format(Date(fromMillis))} – ${dateFormat.format(Date(endInclusive))} · " +
                    "${summary.workoutCount} ${if (summary.workoutCount == 1) "Workout" else "Workouts"} · " +
                    "${formatDuration(summary.totalDurationSeconds)} · $calories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistorySessionRow(session: WorkoutHistoryEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExerciseThumbnail(size = 48.dp, imageUrl = session.imageUrl)
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(session.completedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(session.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            val calories = session.caloriesBurned?.let { formatDecimal(it) } ?: "—"
            Text(
                "${formatDuration(session.durationSeconds?.toLong() ?: 0L)} · $calories Kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun formatDecimal(value: Double): String = "%.1f".format(value)
