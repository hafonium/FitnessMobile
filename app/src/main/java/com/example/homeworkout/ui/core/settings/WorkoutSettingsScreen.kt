package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.SettingsBlue
import com.example.homeworkout.ui.theme.SettingsGreen
import com.example.homeworkout.ui.theme.SettingsOrange
import com.example.homeworkout.ui.theme.SettingsPurple
import com.example.homeworkout.ui.theme.SettingsSlate
import com.example.homeworkout.ui.theme.SettingsTeal

/**
 * Settings-tab "Workout Settings" (global defaults) — distinct from the Training-flow sheet of
 * the same name. Static in this pass; a real screen would read/write `user_settings`.
 */
@Composable
fun WorkoutSettingsScreen(onNavigateBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar(title = "Workout Settings", onNavigateBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsNavRow(label = "Gender", icon = Icons.Default.Wc, iconTint = SettingsPurple)
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(label = "Music", value = "on", icon = Icons.Default.MusicNote, iconTint = SettingsBlue)
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(label = "Rest timer", value = "Default", icon = Icons.Default.Timer, iconTint = SettingsGreen)
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(label = "Prep timer", value = "15 secs", icon = Icons.Default.AvTimer, iconTint = SettingsOrange)
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(label = "Sound options", icon = Icons.Default.VolumeUp, iconTint = SettingsTeal)
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(label = "Restart progress", icon = Icons.Default.RestartAlt, iconTint = SettingsSlate)
                }
            }
        }
    }
}
