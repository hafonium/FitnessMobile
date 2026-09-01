package com.example.homeworkout.ui.core.editgoal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.domain.models.enums.WeekDay
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.buttons.AppButton

/**
 * "Set your weekly goal": weekly training-day target (1-7) and first day of week. Static — Save
 * just navigates back; nothing is persisted to `user_settings` in this pass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalScreen(onNavigateBack: () -> Unit) {
    var goalDays by remember { mutableIntStateOf(6) }
    var firstDay by remember { mutableStateOf(WeekDay.SUNDAY) }

    Scaffold(topBar = { BackTopBar(title = "Weekly Goal", onNavigateBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Set your weekly goal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "We recommend training at least 3 days weekly for a better result.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Weekly training days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..7).forEach { day ->
                    FilterChip(selected = goalDays == day, onClick = { goalDays = day }, label = { Text("$day") })
                }
            }

            Text("First day of week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeekDay.entries.forEach { day ->
                    FilterChip(
                        selected = firstDay == day,
                        onClick = { firstDay = day },
                        label = { Text(day.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            AppButton(
                text = "Save",
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
