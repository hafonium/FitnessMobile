package com.example.homeworkout.ui

import androidx.compose.runtime.Composable
import com.example.homeworkout.ui.navigation.ScreenNavigator
import com.example.homeworkout.utils.ScreenWrapper

@Composable
fun HomeWorkoutApp() {
    KeepScreenOnEffect()
    ScreenWrapper {
        ScreenNavigator()
    }
}
