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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.SlateGray
import java.util.Calendar

private data class HistoryEntry(val date: String, val title: String, val stats: String)

private val sampleWeek = listOf(
    HistoryEntry("Sep 1, 4:54 PM", "Day 1 – LOWER BODY", "00:03 · 0.3 Kcal"),
    HistoryEntry("Sep 1, 9:46 AM", "Abs Beginner", "00:00 · 0.0 Kcal"),
    HistoryEntry("Sep 1, 9:45 AM", "test", "00:02 · 0.2 Kcal")
)

private data class CalendarDay(val dayOfMonth: Int, val isToday: Boolean)

private fun currentMonthGrid(): List<CalendarDay?> {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val leadingBlanks = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = mutableListOf<CalendarDay?>()
    repeat(leadingBlanks) { cells.add(null) }
    for (d in 1..daysInMonth) {
        cells.add(CalendarDay(dayOfMonth = d, isToday = d == today))
    }
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

/** History (from Report -> "All records"). Static sample data for this pass. */
@Composable
fun HistoryScreen(onNavigateBack: () -> Unit) {
    val monthGrid = remember { currentMonthGrid() }

    Scaffold(topBar = { BackTopBar(title = "History", onNavigateBack = onNavigateBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = padding.calculateTopPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
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

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Weekly Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Aug 30 – Sep 5 · 3 Workouts · 00:05 · 0.5 Kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGray
                        )
                    }
                }
            }

            items(sampleWeek) { entry ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumbnail(size = 44.dp)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(entry.date, style = MaterialTheme.typography.bodySmall, color = SlateGray)
                        Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(entry.stats, style = MaterialTheme.typography.bodySmall, color = SlateGray)
                    }
                }
            }
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
    }
}
