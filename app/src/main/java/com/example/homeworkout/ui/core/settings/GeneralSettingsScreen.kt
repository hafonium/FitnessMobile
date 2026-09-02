package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.SettingsBlue
import com.example.homeworkout.ui.theme.SettingsOrange
import com.example.homeworkout.ui.theme.SettingsSlate
import com.example.homeworkout.ui.theme.SettingsTeal

/** Settings-tab "General Settings". Static in this pass. */
@Composable
fun GeneralSettingsScreen(onNavigateBack: () -> Unit) {
    var dailyReminder by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(true) }

    Scaffold(topBar = { BackTopBar(title = "General Settings", onNavigateBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSwitchRow(
                        label = "Remind me to work out every day",
                        checked = dailyReminder,
                        onCheckedChange = { dailyReminder = it },
                        icon = Icons.Default.Notifications,
                        iconTint = SettingsOrange
                    )
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(label = "Metric & Imperial Units", value = "Metric", icon = Icons.Default.Straighten, iconTint = SettingsBlue)
                    HorizontalDivider(color = HairlineGray)
                    SettingsSwitchRow(
                        label = "Keep the screen on",
                        checked = keepScreenOn,
                        onCheckedChange = { keepScreenOn = it },
                        icon = Icons.Default.BrightnessHigh,
                        iconTint = SettingsTeal
                    )
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(label = "Privacy Policy", icon = Icons.Default.PrivacyTip, iconTint = SettingsSlate)
                }
            }
        }
    }
}
