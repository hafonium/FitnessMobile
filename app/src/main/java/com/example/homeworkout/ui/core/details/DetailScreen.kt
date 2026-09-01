package com.example.homeworkout.ui.core.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseRow
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.utils.ScreenWrapper

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onEditExercises: (Long) -> Unit,
    onOpenExerciseInfo: (Long) -> Unit,
    onOpenWorkoutSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenWrapper {
        Scaffold(
            topBar = {
                BackTopBar(title = "Workout", onNavigateBack = onNavigateBack) {
                    IconButton(onClick = onOpenWorkoutSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Workout settings")
                    }
                }
            }
        ) { padding ->
            when (val state = uiState) {
                is DetailUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is DetailUiState.NotFound -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("This workout could not be found.") }

                is DetailUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("Couldn't load workout: ${state.message}", color = MaterialTheme.colorScheme.error) }

                is DetailUiState.Success -> PlanDetailContent(
                    detail = state.detail,
                    contentPadding = padding,
                    onStartWorkout = { onStartWorkout(state.detail.plan.id) },
                    onEditExercises = { onEditExercises(state.detail.plan.id) },
                    onOpenExerciseInfo = onOpenExerciseInfo
                )
            }
        }
    }
}

@Composable
private fun PlanDetailContent(
    detail: WorkoutPlanDetail,
    contentPadding: PaddingValues,
    onStartWorkout: () -> Unit,
    onEditExercises: () -> Unit,
    onOpenExerciseInfo: (Long) -> Unit
) {
    val allExercises = detail.days.flatMap { it.exercises }
    val estimatedMinutes = (allExercises.sumOf { (it.targetDurationSec ?: 30) } / 60).coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, bottom = 24.dp,
            top = contentPadding.calculateTopPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ExerciseThumbnail(size = 96.dp, modifier = Modifier.fillMaxWidth().height(140.dp))
        }
        item {
            Text(detail.plan.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Text(
                "$estimatedMinutes mins · ${allExercises.size} Exercises",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            AppButton(text = "Start", onClick = onStartWorkout, modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Exercises", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Edit",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onEditExercises)
                )
            }
        }
        items(allExercises, key = { it.planExerciseId }) { exercise ->
            ExerciseRow(
                title = exercise.title,
                subtitle = exercise.subtitleText(),
                onClick = { onOpenExerciseInfo(exercise.exerciseId) }
            )
        }
    }
}

private fun PlanExerciseSummary.subtitleText(): String = when {
    targetReps != null -> "x$targetReps"
    targetDurationSec != null -> "${targetDurationSec}s"
    else -> ""
}
