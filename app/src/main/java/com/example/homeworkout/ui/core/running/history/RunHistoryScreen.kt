package com.example.homeworkout.ui.core.running.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.running.RunActivityType
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PageBackground
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.ui.theme.StreakRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RunHistoryScreen(
    viewModel: RunHistoryViewModel,
    onNavigateBack: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<RunSession?>(null) }

    Scaffold(topBar = { BackTopBar("Walking & running history", onNavigateBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(PageBackground)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = BrandBlue)
                state.sessions.isEmpty() -> EmptyHistory(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item("summary") { SummaryCard(state) }
                    item("title") {
                        Text(
                            "Saved sessions (${state.sessions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(state.sessions, key = { it.id }) { session ->
                        SessionCard(session, { onOpenDetail(session.id) }, { pendingDelete = session })
                    }
                }
            }
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete saved session?") },
            text = { Text("The session and its recorded GPS route will be permanently removed.") },
            confirmButton = {
                AppButton("DELETE", {
                    viewModel.delete(session.id)
                    pendingDelete = null
                }, variant = AppButtonVariant.Outlined)
            },
            dismissButton = { AppButton("CANCEL", { pendingDelete = null }, variant = AppButtonVariant.Tonal) }
        )
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = SlateGray, modifier = Modifier.size(44.dp))
        Text("No completed sessions yet", color = InkBlack, fontWeight = FontWeight.Bold)
        Text("Finished walking and running sessions will appear here.", color = SlateGray)
    }
}

@Composable
private fun SummaryCard(state: RunHistoryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, HairlineGray, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrandBlueTint)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("TOTAL ACTIVITY", color = SlateGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(formatDistance(state.totalDistanceKilometers), color = BrandBlue, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = HairlineGray)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric("Time", formatDuration(state.totalDurationSeconds))
                SummaryMetric("Avg pace", formatPace(state.averagePaceMinutesPerKilometer))
                SummaryMetric("Calories", String.format(Locale.getDefault(), "%.0f kcal", state.totalCalories))
            }
        }
    }
}

@Composable private fun SummaryMetric(label: String, value: String) {
    Column {
        Text(label, color = SlateGray, fontSize = 11.sp)
        Text(value, color = InkBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SessionCard(session: RunSession, onClick: () -> Unit, onDelete: () -> Unit) {
    val isWalking = session.activityType == RunActivityType.WALKING
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().border(1.dp, HairlineGray, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActivityIcon(if (isWalking) Icons.AutoMirrored.Filled.DirectionsWalk else Icons.AutoMirrored.Filled.DirectionsRun)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(session.title ?: if (isWalking) "Walking session" else "Running session", color = InkBlack, fontWeight = FontWeight.Bold)
                    Text(formatTimestamp(session.startedAt), color = SlateGray, fontSize = 12.sp)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Delete session", tint = StreakRed, modifier = Modifier.size(19.dp))
                }
            }
            HorizontalDivider(color = HairlineGray)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDistance(session.distanceMeters / 1_000.0), color = BrandBlue, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                SummaryMetric("Time", formatDuration(session.durationSeconds))
                SummaryMetric("Pace", formatPace(session.averagePaceMinutesPerKilometer))
            }
        }
    }
}

@Composable private fun ActivityIcon(icon: ImageVector) {
    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(25.dp))
    }
}

private fun formatTimestamp(timestamp: Long) = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(timestamp))
private fun formatDistance(kilometers: Double) = String.format(Locale.getDefault(), "%.2f km", kilometers)
private fun formatDuration(seconds: Long) = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
private fun formatPace(pace: Double): String {
    if (!pace.isFinite() || pace <= 0.0) return "--:-- /km"
    val minutes = pace.toInt()
    val seconds = ((pace - minutes) * 60).toInt()
    return String.format(Locale.getDefault(), "%d:%02d /km", minutes, seconds)
}
