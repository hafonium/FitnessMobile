package com.example.homeworkout.ui.core.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.enums.VoiceType
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.SettingsRadioRow
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.services.TtsVoiceOption

/** Settings-tab "Voice Options (TTS)". */
@Composable
fun VoiceOptionsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBrowser by remember { mutableStateOf(false) }

    Scaffold(topBar = { BackTopBar(title = "Voice Options (TTS)", onNavigateBack = onNavigateBack) }) { padding ->
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
            SettingsRadioRow(
                label = "Male Coach Voice",
                subtitle = "Deeper pitch, energetic coaching tone",
                selected = settings.ttsVoiceType == VoiceType.MALE_COACH,
                onSelect = { viewModel.setVoiceType(VoiceType.MALE_COACH) }
            )
            SettingsRadioRow(
                label = "Female Coach Voice",
                subtitle = "Higher pitch, energetic coaching tone",
                selected = settings.ttsVoiceType == VoiceType.FEMALE_COACH,
                onSelect = { viewModel.setVoiceType(VoiceType.FEMALE_COACH) }
            )
            SettingsRadioRow(
                label = "Device TTS Engine",
                subtitle = "Fallback to your device's system voice",
                selected = settings.ttsVoiceType == VoiceType.DEVICE_TTS,
                onSelect = { viewModel.setVoiceType(VoiceType.DEVICE_TTS) }
            )
            Text(
                viewModel.engineDiagnostics(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SettingsRadioRow(
                label = "Custom voice",
                subtitle = settings.customVoiceName ?: "Browse and pick any voice your engine offers",
                selected = settings.ttsVoiceType == VoiceType.CUSTOM,
                onSelect = { showBrowser = true }
            )
            AppButton(
                text = "Preview / Test Voice",
                onClick = { viewModel.previewVoice() },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            if (viewModel.hasLimitedVoiceEngine()) {
                Text(
                    "Your device's current TTS engine has no distinct male/female voices, so " +
                        "Male/Female Coach fall back to a pitch shift. Install or switch to a " +
                        "fuller engine (e.g. Google Text-to-Speech) for real distinct voices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                }) {
                    Text("Open TTS engine settings")
                }
            }
        }
    }

    if (showBrowser) {
        VoiceBrowserSheet(
            voices = viewModel.listVoices(),
            selectedName = settings.customVoiceName,
            onPreview = viewModel::previewVoiceByName,
            onSelect = { name ->
                viewModel.setCustomVoice(name)
                showBrowser = false
            },
            onDismiss = { showBrowser = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceBrowserSheet(
    voices: List<TtsVoiceOption>,
    selectedName: String?,
    onPreview: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                Text(
                    "Available voices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (voices.isEmpty()) {
                item {
                    Text(
                        "No offline voices reported by your TTS engine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(voices, key = { it.name }) { voice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(voice.name) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(voice.label, style = MaterialTheme.typography.bodyLarge)
                        Text(voice.localeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onPreview(voice.name) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview ${voice.label}")
                    }
                    RadioButton(selected = voice.name == selectedName, onClick = { onSelect(voice.name) })
                }
            }
        }
    }
}
