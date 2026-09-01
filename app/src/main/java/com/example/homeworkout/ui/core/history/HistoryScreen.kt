package com.example.homeworkout.ui.core.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseThumbnail

private data class HistoryEntry(val date: String, val title: String, val stats: String)

private val sampleWeek = listOf(
    HistoryEntry("Sep 1, 4:54 PM", "Day 1 – LOWER BODY", "00:03 · 0.3 Kcal"),
    HistoryEntry("Sep 1, 9:46 AM", "Abs Beginner", "00:00 · 0.0 Kcal"),
    HistoryEntry("Sep 1, 9:45 AM", "test", "00:02 · 0.2 Kcal")
)

/** History (from Report -> "All records"). Static sample data for this pass. */
@Composable
fun HistoryScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar(title = "History", onNavigateBack = onNavigateBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = padding.calculateTopPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item {
                Column {
                    Text("Weekly Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Aug 30 – Sep 5 · 3 Workouts · 00:05 · 0.5 Kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(sampleWeek) { entry ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ExerciseThumbnail(size = 44.dp)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(entry.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(entry.stats, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
