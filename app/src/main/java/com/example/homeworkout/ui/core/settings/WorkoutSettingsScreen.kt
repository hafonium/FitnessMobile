package com.example.homeworkout.ui.core.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ConfirmDialog
import com.example.homeworkout.ui.components.DurationWheelPicker
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.ui.components.SingleChoiceDialog
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.SettingsGreen
import com.example.homeworkout.ui.theme.SettingsOrange
import com.example.homeworkout.ui.theme.SettingsPurple
import com.example.homeworkout.ui.theme.SettingsSlate
import com.example.homeworkout.ui.theme.SettingsTeal

internal const val MIN_TIMER_SEC = 0
internal const val MAX_TIMER_SEC = 59 * 60 + 59

/** Also reused by the Training-flow `WorkoutSettingsSheetScreen` — keep this the single formatter for `restTimerSec`/`prepTimerSec`. */
internal fun formatTimer(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsNavRow(
                        label = "Gender",
                        value = settings.gender?.let { it.name.lowercase().replaceFirstChar(Char::uppercase) } ?: "Not set",
                        onClick = { showGenderDialog = true },
                        icon = Icons.Default.Wc,
                        iconTint = SettingsPurple
                    )
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(
                        label = "Rest timer",
                        value = formatTimer(settings.restTimerSec),
                        onClick = { showRestTimerDialog = true },
                        icon = Icons.Default.Timer,
                        iconTint = SettingsGreen
                    )
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(
                        label = "Prep timer",
                        value = formatTimer(settings.prepTimerSec),
                        onClick = { showPrepTimerDialog = true },
                        icon = Icons.Default.AvTimer,
                        iconTint = SettingsOrange
                    )
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(
                        label = "Sound options",
                        onClick = { showSoundOptions = true },
                        icon = Icons.Default.VolumeUp,
                        iconTint = SettingsTeal
                    )
                    HorizontalDivider(color = HairlineGray)
                    SettingsNavRow(
                        label = "Restart progress",
                        onClick = { showResetConfirm = true },
                        icon = Icons.Default.RestartAlt,
                        iconTint = SettingsSlate
                    )
                }
            }
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
        SoundOptionsDialog(
            soundEnabled = settings.soundEnabled,
            soundVolume = settings.soundVolume,
            onSoundEnabledChange = viewModel::setSoundEnabled,
            onSoundVolumeChange = viewModel::setSoundVolume,
            onDismiss = { showSoundOptions = false }
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
 * "Sound effects" mute switch + volume slider — also reused by the Training-flow
 * `WorkoutSettingsSheetScreen`'s "Sound options" row so both surfaces edit the same
 * `soundEnabled`/`soundVolume` settings through one dialog implementation.
 */
@Composable
internal fun SoundOptionsDialog(
    soundEnabled: Boolean,
    soundVolume: Float,
    onSoundEnabledChange: (Boolean) -> Unit,
    onSoundVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sound options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSwitchRow(label = "Sound effects", checked = soundEnabled, onCheckedChange = onSoundEnabledChange)
                Slider(
                    value = soundVolume,
                    onValueChange = onSoundVolumeChange,
                    enabled = soundEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

/**
 * `MM:SS` wheel picker dialog for Rest/Prep timer, `00:05`-`59:59`. Edits are held locally and only
 * committed to [onConfirm] when the user taps "Set" — dragging a wheel must not persist mid-scroll.
 * Also reused by the Training-flow `WorkoutSettingsSheetScreen`'s Rest/Prep timer rows.
 */
@Composable
internal fun TimerWheelDialog(
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
