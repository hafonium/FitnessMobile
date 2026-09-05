package com.example.homeworkout.ui.core.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.WorkoutHistoryRecord
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.PlanThumbnail
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.PageBackground
import com.example.homeworkout.ui.theme.SlateGray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class CalendarDay(
    val dayStartMillis: Long,
    val dayOfMonth: Int,
    val isSelected: Boolean,
    val hasCompletedWorkout: Boolean
)

private fun currentMonthGrid(completedAt: List<Long>, selectedDayStartMillis: Long): List<CalendarDay?> {
    val currentMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val year = currentMonth.get(Calendar.YEAR)
    val month = currentMonth.get(Calendar.MONTH)
    val completedDays = completedAt.mapNotNull { timestamp ->
        Calendar.getInstance().apply { timeInMillis = timestamp }.let { calendar ->
            if (calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month) {
                calendar.get(Calendar.DAY_OF_MONTH)
            } else null
        }
    }.toSet()

    val cells = mutableListOf<CalendarDay?>()
    repeat(currentMonth.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) { cells.add(null) }
    for (day in 1..currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)) {
        val dayStart = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
        cells.add(
            CalendarDay(
                dayStartMillis = dayStart,
                dayOfMonth = day,
                isSelected = dayStart == selectedDayStartMillis,
                hasCompletedWorkout = day in completedDays
            )
        )
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
    val monthGrid = remember(state.records, state.selectedDayStartMillis) {
        currentMonthGrid(state.records.map { it.endedAt }, state.selectedDayStartMillis)
    }

    Scaffold(
        containerColor = PageBackground,
        topBar = { BackTopBar(title = "History", onNavigateBack = onNavigateBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                CalendarCard(
                    monthGrid = monthGrid,
                    onDaySelected = viewModel::selectDay
                )
            }
            item {
                Text(
                    "Weekly Summary",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                WeeklySummaryCard(
                    state = state,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarCard(
    monthGrid: List<CalendarDay?>,
    onDaySelected: (Long) -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            monthGrid.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { day -> CalendarCell(day, onDaySelected) }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(day: CalendarDay?, onDaySelected: (Long) -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .then(if (day == null) Modifier else Modifier.clickable { onDaySelected(day.dayStartMillis) }),
        contentAlignment = Alignment.Center
    ) {
        if (day == null) return@Box
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (day.isSelected) BrandBlue else Color.Transparent)
                .then(
                    if (!day.isSelected && day.hasCompletedWorkout) {
                        Modifier.border(1.dp, BrandBlue, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isSelected || day.hasCompletedWorkout) FontWeight.Bold else FontWeight.Normal,
                color = if (day.isSelected) Color.White else SlateGray
            )
        }
        if (day.hasCompletedWorkout && !day.isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(BrandBlue)
            )
        }
    }
}

@Composable
private fun WeeklySummaryCard(state: HistoryUiState, modifier: Modifier = Modifier) {
    val totalSeconds = state.weekRecords.sumOf { it.durationSeconds.toLong() }

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "${formatDay(state.weekStartMillis)} - ${formatDay(state.weekEndMillis - 1)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${state.weekRecords.size} Workout" + if (state.weekRecords.size == 1) "" else "s",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Metric(icon = Icons.Default.Timer, value = formatDuration(totalSeconds), tint = BrandBlue)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = HairlineGray)

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.errorMessage != null -> Text(
                    state.errorMessage.orEmpty(),
                    modifier = Modifier.padding(vertical = 20.dp),
                    color = MaterialTheme.colorScheme.error
                )

                state.weekRecords.isEmpty() -> Text(
                    "No completed workouts in this week.",
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateGray
                )

                else -> state.weekRecords.forEachIndexed { index, record ->
                    HistoryRecordRow(record)
                    if (index < state.weekRecords.lastIndex) HorizontalDivider(color = HairlineGray)
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordRow(record: WorkoutHistoryRecord) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlanThumbnail(planId = record.planId, coverImageUrl = record.coverImageUrl, size = 52.dp)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(formatDateTime(record.endedAt), style = MaterialTheme.typography.bodySmall, color = SlateGray)
            Text(
                record.displayTitle(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Metric(icon = Icons.Default.Timer, value = formatDuration(record.durationSeconds.toLong()), tint = BrandBlue)
            }
        }
    }
}

@Composable
private fun Metric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

private fun WorkoutHistoryRecord.displayTitle(): String =
    if (!dayTitle.isNullOrBlank()) "Day $dayNumber - $dayTitle" else planTitle

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
