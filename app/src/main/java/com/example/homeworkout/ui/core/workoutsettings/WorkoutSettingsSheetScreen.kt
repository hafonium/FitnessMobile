package com.example.homeworkout.ui.core.workoutsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.ui.components.buttons.AppButton

/**
 * The Training-flow "Workout Settings" sheet (music/coach video/timers for the session about to
 * start) — distinct from the Settings-tab "Workout Settings" screen. Static: toggles only affect
 * local state for this screen.
 */
@Composable
fun WorkoutSettingsSheetScreen(onDone: () -> Unit) {
    var coachVideoEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }

    Scaffold(topBar = { BackTopBar(title = "Workout Settings", onNavigateBack = onDone) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = padding.calculateTopPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SettingsSwitchRow(label = "Coach Video", checked = coachVideoEnabled, onCheckedChange = { coachVideoEnabled = it }) }
            item { HorizontalDivider() }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsSwitchRow(label = "Music", checked = musicEnabled, onCheckedChange = { musicEnabled = it })
                    if (musicEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExerciseThumbnail(size = 40.dp)
                            Column {
                                Text("Dancing All Night", fontWeight = FontWeight.Bold)
                                Text("Home Workout Music", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {}) { Icon(Icons.Default.FastRewind, contentDescription = "Previous") }
                            IconButton(onClick = {}) { Icon(Icons.Default.PlayArrow, contentDescription = "Play") }
                            IconButton(onClick = {}) { Icon(Icons.Default.FastForward, contentDescription = "Next") }
                        }
                    }
                }
            }
            item { HorizontalDivider() }
            item { SettingsNavRow(label = "Sound options") }
            item { SettingsNavRow(label = "Rest timer", value = "Default") }
            item { SettingsNavRow(label = "Prep timer", value = "15 secs") }
            item { AppButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth()) }
        }
    }
}
