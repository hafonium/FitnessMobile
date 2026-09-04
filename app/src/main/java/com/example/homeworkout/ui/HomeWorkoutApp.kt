package com.example.homeworkout.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.homeworkout.ui.core.chat.ChatOverlay
import com.example.homeworkout.ui.navigation.ScreenNavigator
import com.example.homeworkout.utils.ScreenWrapper

@Composable
fun HomeWorkoutApp() {
    KeepScreenOnEffect()
    Box(modifier = Modifier.fillMaxSize()) {
        ScreenWrapper {
            ScreenNavigator()
        }
        // Global floating chat assistant bubble/popup, above every screen — see
        // docs/chatbot-feature.md.
        ChatOverlay()
    }
}
