package com.example.homeworkout.ui.core.editgoal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.enums.WeekDay
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.ui.theme.TileShape

/**
 * "Set your weekly goal": weekly training-day target (1-7) and first day of week, persisted to
 * `user_settings` (shared with the Settings tab). The editable chips are seeded from the stored
 * value exactly once, the first time it arrives, and never reset afterwards — otherwise the flow
 * re-emitting after Save (or the screen briefly showing its placeholder default before the real
 * Room row loads) would clobber whatever the user is mid-editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalScreen(
    viewModel: EditGoalViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()

    var goalDays by remember { mutableIntStateOf(settings.weeklyGoalDays) }
    var firstDay by remember { mutableStateOf(settings.firstDayOfWeek) }
    var seededFromStore by remember { mutableStateOf(false) }
    LaunchedEffect(settings) {
        if (!seededFromStore) {
            goalDays = settings.weeklyGoalDays
            firstDay = settings.firstDayOfWeek
            seededFromStore = true
        }
    }

    Scaffold(topBar = { BackTopBar(title = "Weekly Goal", onNavigateBack = onNavigateBack) }) { padding ->
        if (!isReady) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Set your weekly goal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "We recommend training at least 3 days weekly for a better result.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateGray
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(icon = Icons.Default.GpsFixed, text = "Weekly training days")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    (1..7).forEach { day ->
                        DayCountTile(count = day, selected = goalDays == day, onClick = { goalDays = day })
                    }
                }
            }

            androidx.compose.material3.HorizontalDivider(color = HairlineGray)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(icon = Icons.Default.CalendarMonth, text = "First day of week")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekDay.entries.forEach { day ->
                        WeekDayChip(
                            label = day.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = firstDay == day,
                            onClick = { firstDay = day }
                        )
                    }
                }
            }

            AppButton(
                text = "Save",
                onClick = {
                    viewModel.save(goalDays, firstDay)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = SlateGray, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayCountTile(count: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(TileShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.White)
            .border(1.dp, if (selected) Color.Transparent else HairlineGray, TileShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else InkBlack
        )
    }
}

@Composable
private fun WeekDayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(com.example.homeworkout.ui.theme.PillShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else CloudGray)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else SlateGray
        )
    }
}
