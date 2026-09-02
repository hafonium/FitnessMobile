package com.example.homeworkout.ui.core.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.enums.UnitSystemType
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.ui.components.SingleChoiceDialog

// TODO: point this at the real hosted privacy policy before release.
private const val PRIVACY_POLICY_URL = "https://example.com/privacy-policy"

/** Settings-tab "General Settings". */
@Composable
fun GeneralSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar(title = "General Settings", onNavigateBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsSwitchRow(
                label = "Remind me to work out every day",
                checked = settings.dailyReminderEnabled,
                onCheckedChange = { enabled ->
                    viewModel.setDailyReminder(enabled, settings.dailyReminderTime)
                    if (enabled && settings.dailyReminderTime == null) showTimePicker = true
                }
            )
            if (settings.dailyReminderEnabled) {
                SettingsNavRow(
                    label = "Reminder time",
                    value = settings.dailyReminderTime ?: "Set time",
                    onClick = { showTimePicker = true }
                )
            }
            SettingsNavRow(
                label = "Metric & Imperial Units",
                value = if (settings.unitSystem == UnitSystemType.METRIC) "Metric (kg/cm)" else "Imperial (lbs/in)",
                onClick = { showUnitDialog = true }
            )
            SettingsSwitchRow(
                label = "Keep the screen on",
                checked = settings.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn
            )
            SettingsNavRow(
                label = "Privacy Policy",
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))) }
            )
        }
    }

    if (showUnitDialog) {
        SingleChoiceDialog(
            title = "Units",
            options = listOf("Metric (kg/cm)" to UnitSystemType.METRIC, "Imperial (lbs/in)" to UnitSystemType.IMPERIAL),
            selected = settings.unitSystem,
            onSelect = viewModel::setUnitSystem,
            onDismiss = { showUnitDialog = false }
        )
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialTime = settings.dailyReminderTime,
            onConfirm = { time -> viewModel.setDailyReminder(true, time) },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialTime: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val (initHour, initMinute) = remember(initialTime) { parseTimeOrDefault(initialTime) }
    val state = rememberTimePickerState(initialHour = initHour, initialMinute = initMinute, is24Hour = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm("%02d:%02d".format(state.hour, state.minute))
                onDismiss()
            }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun parseTimeOrDefault(time: String?): Pair<Int, Int> {
    val parts = time?.split(":")
    val hour = parts?.getOrNull(0)?.toIntOrNull() ?: 7
    val minute = parts?.getOrNull(1)?.toIntOrNull() ?: 0
    return hour to minute
}
