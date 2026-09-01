package com.example.homeworkout.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    HomeWorkoutTheme {
        HomeWorkoutApp()
    }
}
