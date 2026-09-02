package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.enums.UserGender
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ConfirmDialog
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.ui.components.SingleChoiceDialog

private val REST_TIMER_OPTIONS_SEC = listOf(15, 20, 30, 45, 60, 90)
private val PREP_TIMER_OPTIONS_SEC = listOf(5, 10, 15, 20, 30)

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

    var showGenderDialog by remember { mutableStateOf(false) }
    var showRestTimerDialog by remember { mutableStateOf(false) }
    var showPrepTimerDialog by remember { mutableStateOf(false) }
    var showSoundOptions by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar(title = "Workout Settings", onNavigateBack = onNavigateBack) }) { padding ->
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
                value = "${settings.restTimerSec} secs",
                onClick = { showRestTimerDialog = true }
            )
            SettingsNavRow(
                label = "Prep timer",
                value = "${settings.prepTimerSec} secs",
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
        SingleChoiceDialog(
            title = "Rest timer",
            options = REST_TIMER_OPTIONS_SEC.map { "$it secs" to it },
            selected = settings.restTimerSec,
            onSelect = viewModel::setRestTimerSec,
            onDismiss = { showRestTimerDialog = false }
        )
    }

    if (showPrepTimerDialog) {
        SingleChoiceDialog(
            title = "Prep timer",
            options = PREP_TIMER_OPTIONS_SEC.map { "$it secs" to it },
            selected = settings.prepTimerSec,
            onSelect = viewModel::setPrepTimerSec,
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
