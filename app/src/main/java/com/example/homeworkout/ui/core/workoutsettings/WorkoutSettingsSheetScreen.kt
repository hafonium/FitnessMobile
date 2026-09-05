package com.example.homeworkout.ui.core.workoutsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.SettingsNavRow
import com.example.homeworkout.ui.components.SettingsSwitchRow
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.core.settings.SettingsViewModel
import com.example.homeworkout.ui.core.settings.SoundOptionsDialog
import com.example.homeworkout.ui.core.settings.TimerWheelDialog
import com.example.homeworkout.ui.core.settings.formatTimer
import com.example.homeworkout.ui.theme.HairlineGray
import com.example.homeworkout.ui.theme.SettingsGreen
import com.example.homeworkout.ui.theme.SettingsOrange
import com.example.homeworkout.ui.theme.SettingsPurple
import com.example.homeworkout.ui.theme.SettingsTeal

/**
 * The Training-flow "Workout Settings" sheet (coach video/timers for the session about to
 * start) — distinct from the Settings-tab "Workout Settings" screen, but Rest timer/Prep timer/
 * Sound options read and write the exact same `user_settings` row via the shared
 * [SettingsViewModel] (same [TimerWheelDialog]/[SoundOptionsDialog] used there), so a change made
 * here is visible on the Settings tab and vice versa. Coach Video has no backing settings field
 * yet, so it stays local-only state for this screen.
 */
@Composable
fun WorkoutSettingsSheetScreen(viewModel: SettingsViewModel, onDone: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()

    var coachVideoEnabled by remember { mutableStateOf(true) }
    var showRestTimerDialog by remember { mutableStateOf(false) }
    var showPrepTimerDialog by remember { mutableStateOf(false) }
    var showSoundOptions by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar(title = "Workout Settings", onNavigateBack = onDone) }) { padding ->
        if (!isReady) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = padding.calculateTopPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { HorizontalDivider(color = HairlineGray) }
            item {
                SettingsNavRow(
                    label = "Sound options",
                    onClick = { showSoundOptions = true },
                    icon = Icons.Default.VolumeUp,
                    iconTint = SettingsTeal
                )
            }
            item {
                SettingsNavRow(
                    label = "Rest timer",
                    value = formatTimer(settings.restTimerSec),
                    onClick = { showRestTimerDialog = true },
                    icon = Icons.Default.Timer,
                    iconTint = SettingsGreen
                )
            }
            item {
                SettingsNavRow(
                    label = "Prep timer",
                    value = formatTimer(settings.prepTimerSec),
                    onClick = { showPrepTimerDialog = true },
                    icon = Icons.Default.AvTimer,
                    iconTint = SettingsOrange
                )
            }
            item { AppButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth()) }
        }
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
}
