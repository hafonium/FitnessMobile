package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.utils.ScreenWrapper

/** Settings tab landing screen. */
@Composable
fun SettingsScreen(
    onOpenWorkoutSettings: () -> Unit,
    onOpenGeneralSettings: () -> Unit,
    onOpenVoiceOptions: () -> Unit
) {
    ScreenWrapper {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { Text("SETTINGS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                SettingsNavRow(
                    label = "Workout Settings",
                    icon = Icons.Default.KeyboardArrowDown,
                    onClick = onOpenWorkoutSettings
                )
            }
            item {
                SettingsNavRow(
                    label = "General Settings",
                    icon = Icons.Default.Settings,
                    onClick = onOpenGeneralSettings
                )
            }
            item {
                SettingsNavRow(
                    label = "Voice Options (TTS)",
                    icon = Icons.Default.Mic,
                    onClick = onOpenVoiceOptions
                )
            }
        }
    }
}
