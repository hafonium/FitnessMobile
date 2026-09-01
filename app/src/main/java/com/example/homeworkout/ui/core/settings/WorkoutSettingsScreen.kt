package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.SettingsNavRow

/**
 * Settings-tab "Workout Settings" (global defaults) — distinct from the Training-flow sheet of
 * the same name. Static in this pass; a real screen would read/write `user_settings`.
 */
@Composable
fun WorkoutSettingsScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar(title = "Workout Settings", onNavigateBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsNavRow(label = "Gender")
            SettingsNavRow(label = "Music", value = "on")
            SettingsNavRow(label = "Rest timer", value = "Default")
            SettingsNavRow(label = "Prep timer", value = "15 secs")
            SettingsNavRow(label = "Sound options")
            SettingsNavRow(label = "Restart progress")
        }
    }
}
