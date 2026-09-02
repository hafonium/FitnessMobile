package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.enums.UserGender
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ConfirmDialog
import com.example.homeworkout.ui.components.DurationWheelPicker
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.ui.components.SingleChoiceDialog

private const val MIN_TIMER_SEC = 0
private const val MAX_TIMER_SEC = 59 * 60 + 59

private fun formatTimer(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

/**
 * Settings-tab "Workout Settings" (global defaults) — distinct from the Training-flow sheet of
 * the same name.
 */
@Composable
fun WorkoutSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()

    var showGenderDialog by remember { mutableStateOf(false) }
    var showRestTimerDialog by remember { mutableStateOf(false) }
    var showPrepTimerDialog by remember { mutableStateOf(false) }
    var showSoundOptions by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar(title = "Workout Settings", onNavigateBack = onNavigateBack) }) { padding ->
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
            SettingsNavRow(
                label = "Gender",
                value = settings.gender?.let { it.name.lowercase().replaceFirstChar(Char::uppercase) } ?: "Not set",
                onClick = { showGenderDialog = true }
            )
            SettingsSwitchRow(label = "Music", checked = settings.musicEnabled, onCheckedChange = viewModel::setMusicEnabled)
            SettingsNavRow(
                label = "Rest timer",
                value = formatTimer(settings.restTimerSec),
                onClick = { showRestTimerDialog = true }
            )
            SettingsNavRow(
                label = "Prep timer",
                value = formatTimer(settings.prepTimerSec),
                onClick = { showPrepTimerDialog = true }
            )
            SettingsNavRow(label = "Sound options", onClick = { showSoundOptions = true })
            SettingsNavRow(label = "Restart progress", onClick = { showResetConfirm = true })
        }
    }

    if (showGenderDialog) {
        SingleChoiceDialog(
            title = "Gender",
            options = listOf("Male" to UserGender.MALE, "Female" to UserGender.FEMALE),
            selected = settings.gender ?: UserGender.MALE,
            onSelect = viewModel::setGender,
            onDismiss = { showGenderDialog = false }
        )
    }

    if (showRestTimerDialog) {
        TimerWheelDialog(
            title = "Rest timer",
            initialSeconds = settings.restTimerSec,
            onConfirm = viewModel::setRestTimerSec,
            onDismiss = { showRestTimerDialog = false }
        )
    }

    if (showPrepTimerDialog) {
        TimerWheelDialog(
            title = "Prep timer",
            initialSeconds = settings.prepTimerSec,
            onConfirm = viewModel::setPrepTimerSec,
            onDismiss = { showPrepTimerDialog = false }
        )
    }

    if (showSoundOptions) {
        AlertDialog(
            onDismissRequest = { showSoundOptions = false },
            title = { Text("Sound options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsSwitchRow(label = "Music", checked = settings.musicEnabled, onCheckedChange = viewModel::setMusicEnabled)
                    Slider(
                        value = settings.musicVolume,
                        onValueChange = viewModel::setMusicVolume,
                        enabled = settings.musicEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SettingsSwitchRow(label = "Sound effects", checked = settings.soundEnabled, onCheckedChange = viewModel::setSoundEnabled)
                    Slider(
                        value = settings.soundVolume,
                        onValueChange = viewModel::setSoundVolume,
                        enabled = settings.soundEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showSoundOptions = false }) { Text("Done") } }
        )
    }

    if (showResetConfirm) {
        ConfirmDialog(
            title = "Restart progress",
            message = "Are you sure you want to reset all workout progress?",
            confirmLabel = "Reset",
            onConfirm = viewModel::resetWorkoutProgress,
            onDismiss = { showResetConfirm = false }
        )
    }
}

/**
 * `MM:SS` wheel picker dialog for Rest/Prep timer, `00:05`-`59:59`. Edits are held locally and only
 * committed to [onConfirm] when the user taps "Set" — dragging a wheel must not persist mid-scroll.
 */
@Composable
private fun TimerWheelDialog(
    title: String,
    initialSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var seconds by remember { mutableIntStateOf(initialSeconds.coerceIn(MIN_TIMER_SEC, MAX_TIMER_SEC)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            DurationWheelPicker(
                totalSeconds = seconds,
                onChange = { seconds = it },
                minSeconds = MIN_TIMER_SEC,
                maxSeconds = MAX_TIMER_SEC
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(seconds); onDismiss() }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
