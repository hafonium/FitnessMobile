package com.example.homeworkout.ui.core.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.enums.UnitSystemType
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ClockWheelPicker
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.ui.components.SingleChoiceDialog

// TODO: point this at the real hosted privacy policy before release.
private const val PRIVACY_POLICY_URL = "https://example.com/privacy-policy"

/**
 * Settings-tab "General Settings". The daily reminder here is intentionally hint-free — see
 * `docs/notifications.md` for the full permission/OS-reliability model (notification permission,
 * exact-alarm permission, OEM background-kill behavior) instead of in-UI warning text.
 */
@Composable
fun GeneralSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setDailyReminder(true, settings.dailyReminderTime)
            if (settings.dailyReminderTime == null) showTimePicker = true
        }
        // Denied: never persist enabled=true, so the switch simply stays off.
    }

    fun enableReminder() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setDailyReminder(true, settings.dailyReminderTime)
            if (settings.dailyReminderTime == null) showTimePicker = true
        }
    }

    Scaffold(topBar = { BackTopBar(title = "General Settings", onNavigateBack = onNavigateBack) }) { padding ->
        if (!isReady) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsSwitchRow(
                label = "Remind me to work out every day",
                checked = settings.dailyReminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) enableReminder() else viewModel.setDailyReminder(false, settings.dailyReminderTime)
                }
            )
            if (settings.dailyReminderEnabled) {
                SettingsNavRow(
                    label = "Reminder time",
                    value = settings.dailyReminderTime?.let(::formatTimeLabel) ?: "Set time",
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

private fun formatTimeLabel(time24: String): String {
    val (hour, minute) = parseTimeOrDefault(time24)
    return "%02d:%02d".format(hour, minute)
}

@Composable
private fun ReminderTimePickerDialog(
    initialTime: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var time by remember { mutableStateOf(initialTime ?: "07:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder time") },
        text = {
            ClockWheelPicker(time24 = time, onChange = { time = it })
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(time); onDismiss() }) { Text("Set") }
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
