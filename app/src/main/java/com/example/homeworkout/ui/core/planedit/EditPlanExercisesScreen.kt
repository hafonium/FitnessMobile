package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseRow
import com.example.homeworkout.ui.core.details.DetailUiState
import com.example.homeworkout.ui.core.details.DetailViewModel

/**
 * "Edit plan": lets you add/remove exercises and tweak reps/duration for the exercise list
 * loaded from [DetailViewModel]. Static — changes here live only in this screen's local state
 * and are not written back to the database.
 */
@Composable
fun EditPlanExercisesScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onAlterExercise: () -> Unit,
    onAddExercises: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // planExerciseId -> locally edited reps, purely for on-screen stepper feedback.
    val repsOverrides = remember { mutableStateMapOf<Long, Int>() }

    Scaffold(
        topBar = { BackTopBar(title = "Edit plan", onNavigateBack = onNavigateBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExercises) { Icon(Icons.Default.Add, contentDescription = "Add exercise") }
        }
    ) { padding ->
        when (val state = uiState) {
            is DetailUiState.Success -> {
                val exercises = state.detail.days.flatMap { it.exercises }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 24.dp,
                        top = padding.calculateTopPadding() + 8.dp
                    )
                ) {
                    items(exercises, key = { it.planExerciseId }) { exercise ->
                        val reps = repsOverrides[exercise.planExerciseId] ?: exercise.targetReps
                        ExerciseRow(
                            title = exercise.title,
                            subtitle = exercise.subtitleText(reps)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (exercise.targetReps != null) {
                                    IconButton(onClick = {
                                        val current = reps ?: 0
                                        if (current > 1) repsOverrides[exercise.planExerciseId] = current - 1
                                    }) { Icon(Icons.Default.Remove, contentDescription = "Decrease") }
                                    Text("${reps ?: 0}", style = MaterialTheme.typography.bodyLarge)
                                    IconButton(onClick = {
                                        repsOverrides[exercise.planExerciseId] = (reps ?: 0) + 1
                                    }) { Icon(Icons.Default.Add, contentDescription = "Increase") }
                                }
                                IconButton(onClick = onAlterExercise) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Replace exercise")
                                }
                                Icon(Icons.Default.DragHandle, contentDescription = "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            is DetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("This workout could not be found.") }
        }
    }
}

private fun PlanExerciseSummary.subtitleText(overrideReps: Int?): String = when {
    overrideReps != null -> "x$overrideReps"
    targetDurationSec != null -> "${targetDurationSec}s"
    else -> ""
}
