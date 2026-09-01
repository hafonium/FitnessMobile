package com.example.homeworkout.ui.core.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.utils.ScreenWrapper

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToDetails: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenWrapper {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // TODO: Render uiState (Loading / Success / Error) as a list of workouts,
            // navigating via onNavigateToDetails(workout.id) on tap.
            Text(text = "Home Screen")
        }
    }
}
