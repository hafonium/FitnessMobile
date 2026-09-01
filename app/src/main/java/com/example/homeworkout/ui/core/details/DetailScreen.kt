package com.example.homeworkout.ui.core.details

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
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenWrapper {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // TODO: Render uiState (Loading / Success / Error) with the workout's full details.
            Text(text = "Detail Screen")
        }
    }
}
