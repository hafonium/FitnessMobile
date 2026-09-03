package com.example.homeworkout.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import com.example.homeworkout.ui.theme.HomeWorkoutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HomeWorkoutTheme {
                HomeWorkoutApp()
            }
        }
    }
}

/** Applies the "Keep the screen on" setting to the window as soon as it changes, app-wide. */
@Composable
fun KeepScreenOnEffect() {
    val app = LocalContext.current.applicationContext as App
    val view = LocalView.current
    LaunchedEffect(Unit) {
        app.settingsRepository.observeSettings().collect { settings ->
            view.keepScreenOn = settings.keepScreenOn
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    HomeWorkoutTheme {
        HomeWorkoutApp()
    }
}
