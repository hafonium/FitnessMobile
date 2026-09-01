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
import com.example.homeworkout.domain.models.enums.VoiceType
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.SettingsRadioRow

/** Settings-tab "Voice Options (TTS)". Static in this pass. */
@Composable
fun VoiceOptionsScreen(onNavigateBack: () -> Unit) {
    var selected by remember { mutableStateOf(VoiceType.NATURAL) }

    Scaffold(topBar = { BackTopBar(title = "Voice Options (TTS)", onNavigateBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SettingsRadioRow(
                label = "Natural Voice",
                subtitle = "Advanced & more fluid voice",
                selected = selected == VoiceType.NATURAL,
                onSelect = { selected = VoiceType.NATURAL }
            )
            SettingsRadioRow(
                label = "Device TTS Engine",
                subtitle = null,
                selected = selected == VoiceType.DEVICE_TTS,
                onSelect = { selected = VoiceType.DEVICE_TTS }
            )
        }
    }
}
