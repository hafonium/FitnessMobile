package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.SettingsBlue
import com.example.homeworkout.ui.theme.SettingsGreen
import com.example.homeworkout.ui.theme.SettingsOrange
import com.example.homeworkout.utils.ScreenWrapper

/** Settings tab landing screen. */
@Composable
fun SettingsScreen(
    onOpenPlanSetup: () -> Unit,
    onOpenWorkoutSettings: () -> Unit,
    onOpenGeneralSettings: () -> Unit,
    onOpenVoiceOptions: () -> Unit
) {
    ScreenWrapper {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("SETTINGS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsNavRow(label = "Set up my plan", onClick = onOpenPlanSetup, icon = Icons.Default.Flag, iconTint = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(color = HairlineGray)
                        SettingsNavRow(label = "Workout Settings", onClick = onOpenWorkoutSettings, icon = Icons.Default.WaterDrop, iconTint = SettingsGreen)
                        HorizontalDivider(color = HairlineGray)
                        SettingsNavRow(label = "General Settings", onClick = onOpenGeneralSettings, icon = Icons.Default.Settings, iconTint = SettingsBlue)
                        HorizontalDivider(color = HairlineGray)
                        SettingsNavRow(label = "Voice Options (TTS)", onClick = onOpenVoiceOptions, icon = Icons.Default.Mic, iconTint = SettingsOrange)
                    }
                }
            }
        }
    }
}
