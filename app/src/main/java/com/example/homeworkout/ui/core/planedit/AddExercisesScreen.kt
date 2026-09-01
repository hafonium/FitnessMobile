package com.example.homeworkout.ui.core.planedit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.example.homeworkout.ui.components.BackTopBar

/** "Add Exercises": browse and add exercises from the real library to the current plan. */
@Composable
fun AddExercisesScreen(
    viewModel: ExerciseBrowserViewModel,
    onNavigateBack: () -> Unit,
    onOpenFilter: () -> Unit,
    onExerciseInfo: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            BackTopBar(title = "Add Exercises", onNavigateBack = onNavigateBack) {
                IconButton(onClick = onOpenFilter) { Icon(Icons.Default.FilterList, contentDescription = "Filter") }
            }
        }
    ) { padding ->
        ExerciseBrowserContent(viewModel = viewModel, topPadding = padding.calculateTopPadding(), onExerciseInfo = onExerciseInfo)
    }
}
