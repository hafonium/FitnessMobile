package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import com.example.homeworkout.ui.theme.SettingsPurple
import com.example.homeworkout.ui.theme.SettingsSlate
import com.example.homeworkout.ui.theme.SettingsTeal
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
                        HorizontalDivider(color = HairlineGray)
                        SettingsNavRow(label = "Suggest Other Features", icon = Icons.Default.Forum, iconTint = SettingsTeal)
                        HorizontalDivider(color = HairlineGray)
                        SettingsNavRow(label = "Language Options", value = "Default", icon = Icons.Default.Language, iconTint = SettingsPurple)
                    }
                }
            }

            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SettingsNavRow(label = "Share with friends", icon = Icons.Default.Share, iconTint = SettingsSlate)
                        HorizontalDivider(color = HairlineGray)
                        SettingsNavRow(label = "Rate us", icon = Icons.Default.Star, iconTint = SettingsSlate)
                        HorizontalDivider(color = HairlineGray)
                        SettingsNavRow(label = "Feedback", icon = Icons.Default.Edit, iconTint = SettingsSlate)
                        HorizontalDivider(color = HairlineGray)
                        SettingsNavRow(label = "Remove Ads", icon = Icons.Default.Block, iconTint = SettingsSlate)
                    }
                }
            }
        }
    }
}
