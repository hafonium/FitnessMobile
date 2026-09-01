package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow

/** Settings-tab "General Settings". Static in this pass. */
@Composable
fun GeneralSettingsScreen(onNavigateBack: () -> Unit) {
    var dailyReminder by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(true) }

    Scaffold(topBar = { BackTopBar(title = "General Settings", onNavigateBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsSwitchRow(label = "Remind me to work out every day", checked = dailyReminder, onCheckedChange = { dailyReminder = it })
            SettingsNavRow(label = "Metric & Imperial Units", value = "Metric")
            SettingsSwitchRow(label = "Keep the screen on", checked = keepScreenOn, onCheckedChange = { keepScreenOn = it })
            SettingsNavRow(label = "Privacy Policy")
        }
    }
}
