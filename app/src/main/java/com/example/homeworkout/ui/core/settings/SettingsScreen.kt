package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.utils.ScreenWrapper

/** Settings tab landing screen. */
@Composable
fun SettingsScreen(
    onOpenPlanSetup: () -> Unit,
    onOpenWorkoutSettings: () -> Unit,
    onOpenGeneralSettings: () -> Unit,
    onOpenVoiceOptions: () -> Unit
) {
    var healthConnect by remember { mutableStateOf(false) }

    ScreenWrapper {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { Text("SETTINGS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { SettingsNavRow(label = "Set up my plan", onClick = onOpenPlanSetup) }
            item { SettingsNavRow(label = "Workout Settings", onClick = onOpenWorkoutSettings) }
            item { SettingsNavRow(label = "General Settings", onClick = onOpenGeneralSettings) }
            item { SettingsNavRow(label = "Voice Options (TTS)", onClick = onOpenVoiceOptions) }
            item { SettingsNavRow(label = "Suggest Other Features") }
            item { SettingsNavRow(label = "Language Options", value = "Default") }
            item { SettingsSwitchRow(label = "Sync to Health Connect", checked = healthConnect, onCheckedChange = { healthConnect = it }) }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsNavRow(label = "Share with friends") }
            item { SettingsNavRow(label = "Rate us") }
            item { SettingsNavRow(label = "Feedback") }
            item { SettingsNavRow(label = "Remove Ads") }
        }
    }
}
