package com.example.homeworkout.ui.core.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.WorkoutHistoryRecord
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.SlateGray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class CalendarDay(
    val dayOfMonth: Int,
    val isToday: Boolean,
    val hasCompletedWorkout: Boolean
)

private fun currentMonthGrid(completedAt: List<Long>): List<CalendarDay?> {
    val todayCalendar = Calendar.getInstance()
    val currentYear = todayCalendar.get(Calendar.YEAR)
    val currentMonth = todayCalendar.get(Calendar.MONTH)
    val today = todayCalendar.get(Calendar.DAY_OF_MONTH)
    val completedDays = completedAt.mapNotNull { timestamp ->
        Calendar.getInstance().apply { timeInMillis = timestamp }.let { calendar ->
            if (calendar.get(Calendar.YEAR) == currentYear && calendar.get(Calendar.MONTH) == currentMonth) {
                calendar.get(Calendar.DAY_OF_MONTH)
            } else null
        }
    }.toSet()

    val month = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    val cells = mutableListOf<CalendarDay?>()
    repeat(month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) { cells.add(null) }
    for (day in 1..month.getActualMaximum(Calendar.DAY_OF_MONTH)) {
        cells.add(CalendarDay(day, day == today, day in completedDays))
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthGrid = remember(state.records) { currentMonthGrid(state.records.map { it.endedAt }) }

    Scaffold(topBar = { BackTopBar(title = "History", onNavigateBack = onNavigateBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp,
                top = padding.calculateTopPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { HistoryCalendar(monthGrid) }
            item { WeeklySummary(state) }

            when {
                state.isLoading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.errorMessage != null -> item {
                    Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                state.records.isEmpty() -> item {
                    Text(
                        "No completed workouts yet.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGray
                    )
                }
                else -> items(state.records, key = { it.sessionId }) { record -> HistoryRecordRow(record) }
            }
        }
    }
}

@Composable
private fun HistoryCalendar(monthGrid: List<CalendarDay?>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray,
                    modifier = Modifier.size(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        monthGrid.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day -> CalendarCell(day) }
            }
        }
    }
}

@Composable
private fun WeeklySummary(state: HistoryUiState) {
    val totalSeconds = state.weekRecords.sumOf { it.durationSeconds.toLong() }
    val recordedCalories = state.weekRecords.mapNotNull { it.caloriesBurned }
    val caloriesText = if (recordedCalories.isEmpty()) "— Kcal" else "${"%.1f".format(recordedCalories.sum())} Kcal"

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Weekly Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${formatDay(state.weekStartMillis)} – ${formatDay(state.weekEndMillis - 1)} · " +
                    "${state.weekRecords.size} Workouts · ${formatDuration(totalSeconds)} · $caloriesText",
                style = MaterialTheme.typography.bodySmall,
                color = SlateGray
            )
        }
    }
}

@Composable
private fun HistoryRecordRow(record: WorkoutHistoryRecord) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExerciseThumbnail(size = 44.dp, imageUrl = record.coverImageUrl)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(formatDateTime(record.endedAt), style = MaterialTheme.typography.bodySmall, color = SlateGray)
            Text(record.displayTitle(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                "${formatDuration(record.durationSeconds.toLong())} · ${formatCalories(record.caloriesBurned)}",
                style = MaterialTheme.typography.bodySmall,
                color = SlateGray
            )
        }
    }
}

@Composable
private fun CalendarCell(day: CalendarDay?) {
    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (day == null) return@Box
        if (day.isToday) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text("${day.dayOfMonth}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Text("${day.dayOfMonth}", style = MaterialTheme.typography.bodyMedium, color = InkBlack)
        }
        if (day.hasCompletedWorkout) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (day.isToday) Color.White else MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun WorkoutHistoryRecord.displayTitle(): String =
    if (!dayTitle.isNullOrBlank()) "Day $dayNumber – $dayTitle" else planTitle

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))

private fun formatDay(timestamp: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun formatCalories(calories: Double?): String =
    calories?.let { "%.1f Kcal".format(it) } ?: "— Kcal"
