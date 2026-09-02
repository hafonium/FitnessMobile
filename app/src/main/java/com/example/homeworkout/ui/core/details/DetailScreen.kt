package com.example.homeworkout.ui.core.details

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseRow
import com.example.homeworkout.ui.components.SectionHeader
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.theme.AppGradients
import com.example.homeworkout.ui.theme.CardShape
import com.example.homeworkout.ui.theme.SlateGray
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
    val topBarTitle = (uiState as? DetailUiState.Success)?.detail?.plan?.title ?: "Workout"

    ScreenWrapper {
        Scaffold(
            topBar = {
                BackTopBar(title = topBarTitle, onNavigateBack = onNavigateBack) {
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
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

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
            top = contentPadding.calculateTopPadding() + 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroBanner(coverImageUrl = detail.plan.coverImageUrl) }
        item {
            Text(detail.plan.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatChip(Icons.Default.Schedule, "$estimatedMinutes mins")
                StatChip(Icons.Default.FitnessCenter, "${allExercises.size} Exercises")
            }
        }
        item {
            AppButton(text = "Start", onClick = onStartWorkout, modifier = Modifier.fillMaxWidth())
        }
        item {
            SectionHeader(title = "Exercises", actionText = "Edit", onActionClick = onEditExercises)
        }
        items(allExercises, key = { it.planExerciseId }) { exercise ->
            ExerciseRow(
                title = exercise.title,
                subtitle = exercise.subtitleText(),
                imageUrl = exercise.gifUrl,
                onClick = { onOpenExerciseInfo(exercise.exerciseId) }
            )
        }
    }
}

@Composable
private fun HeroBanner(coverImageUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(CardShape)
    ) {
        if (coverImageUrl.isNullOrBlank()) {
            HeroBannerFallback()
        } else {
            SubcomposeAsyncImage(
                model = coverImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { HeroBannerFallback() },
                error = { HeroBannerFallback() }
            )
        }
    }
}

@Composable
private fun HeroBannerFallback() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGradients.DarkButton),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = SlateGray, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = SlateGray, fontWeight = FontWeight.SemiBold)
    }
}

private fun PlanExerciseSummary.subtitleText(): String = when {
    targetReps != null -> "x$targetReps"
    targetDurationSec != null -> "${targetDurationSec}s"
    else -> ""
}
