package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.domain.models.WorkoutPlanDayDetail
import com.example.homeworkout.ui.components.AppCard
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseRow
import com.example.homeworkout.ui.core.details.DetailUiState
import com.example.homeworkout.ui.core.details.DetailViewModel
import com.example.homeworkout.ui.theme.BrandBlue
import com.example.homeworkout.ui.theme.CardWhite
import com.example.homeworkout.ui.theme.CloudGray
import com.example.homeworkout.ui.theme.InkBlack

/**
 * "Edit plan": lets you add/remove exercises and tweak reps/duration for the exercise list
 * loaded from [DetailViewModel]. A multi-day plan is grouped into per-day panels — mirroring the
 * Workout Screen's day grouping — each with its own "Add" action so new exercises land on that
 * day instead of always the plan's first day; a single-day plan keeps the simpler flat list + FAB.
 */
@Composable
fun EditPlanExercisesScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onAlterExercise: (planExerciseId: Long) -> Unit,
    onAddExercises: (planDayId: Long) -> Unit,
    onUpdateReps: (planExerciseId: Long, reps: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // planExerciseId -> locally edited reps, purely for on-screen stepper feedback.
    val repsOverrides = remember { mutableStateMapOf<Long, Int>() }

    fun updateReps(planExerciseId: Long, reps: Int) {
        repsOverrides[planExerciseId] = reps
        onUpdateReps(planExerciseId, reps)
    }

    when (val state = uiState) {
        is DetailUiState.Success -> {
            val days = state.detail.days
            val isMultiDay = days.size > 1
            val firstDayId = days.firstOrNull()?.planDayId ?: 0L
            Scaffold(
                topBar = { BackTopBar(title = "Edit plan", onNavigateBack = onNavigateBack) },
                floatingActionButton = {
                    if (!isMultiDay) {
                        FloatingActionButton(
                            onClick = { onAddExercises(firstDayId) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) { Icon(Icons.Default.Add, contentDescription = "Add exercise") }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = if (isMultiDay) 24.dp else 100.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp
                    ),
                    verticalArrangement = if (isMultiDay) Arrangement.spacedBy(12.dp) else Arrangement.Top
                ) {
                    if (isMultiDay) {
                        days.forEachIndexed { dayIndex, day ->
                            item(key = "day-${day.planDayId}") {
                                val panelColor = if (dayIndex % 2 == 1) CloudGray else CardWhite
                                DayEditPanel(
                                    day = day,
                                    panelColor = panelColor,
                                    repsOverrides = repsOverrides,
                                    onUpdateReps = ::updateReps,
                                    onAlterExercise = onAlterExercise,
                                    onAddExercises = onAddExercises
                                )
                            }
                        }
                    } else {
                        items(days.flatMap { it.exercises }, key = { it.planExerciseId }) { exercise ->
                            ExerciseEditRow(
                                exercise = exercise,
                                reps = repsOverrides[exercise.planExerciseId] ?: exercise.targetReps,
                                onUpdateReps = ::updateReps,
                                onAlterExercise = onAlterExercise
                            )
                        }
                    }
                }
            }
        }

        is DetailUiState.Loading -> Scaffold(topBar = { BackTopBar(title = "Edit plan", onNavigateBack = onNavigateBack) }) { inner -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        else -> Scaffold(topBar = { BackTopBar(title = "Edit plan", onNavigateBack = onNavigateBack) }) { inner -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { Text("This workout could not be found.") } }
    }
}

/** One day's exercises grouped into a tinted panel with its own "Add" action, so exercises added
 * from this day's section land on the correct [WorkoutPlanDayDetail.planDayId] instead of always
 * the plan's first day. */
@Composable
private fun DayEditPanel(
    day: WorkoutPlanDayDetail,
    panelColor: Color,
    repsOverrides: Map<Long, Int>,
    onUpdateReps: (planExerciseId: Long, reps: Int) -> Unit,
    onAlterExercise: (planExerciseId: Long) -> Unit,
    onAddExercises: (planDayId: Long) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth(), containerColor = panelColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                buildString {
                    append("DAY ${day.dayNumber}")
                    if (!day.title.isNullOrBlank()) append(" · ${day.title}")
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = InkBlack
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAddExercises(day.planDayId) }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                Text(
                    "Add",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandBlue,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
        if (day.exercises.isEmpty()) {
            Text(
                "No exercises yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
            )
        } else {
            day.exercises.forEachIndexed { index, exercise ->
                ExerciseEditRow(
                    exercise = exercise,
                    reps = repsOverrides[exercise.planExerciseId] ?: exercise.targetReps,
                    onUpdateReps = onUpdateReps,
                    onAlterExercise = onAlterExercise,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    showDivider = index != day.exercises.lastIndex
                )
            }
        }
    }
}

/** One exercise's row in Edit Plan: reps stepper (hidden for timed exercises, same rule as
 * before), swap, and a drag-handle affordance. Shared by both the flat single-day list and each
 * [DayEditPanel] so the row itself stays identical either way. */
@Composable
private fun ExerciseEditRow(
    exercise: PlanExerciseSummary,
    reps: Int?,
    onUpdateReps: (planExerciseId: Long, reps: Int) -> Unit,
    onAlterExercise: (planExerciseId: Long) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    ExerciseRow(
        title = exercise.title,
        subtitle = exercise.subtitleText(reps),
        imageUrl = exercise.gifUrl,
        modifier = modifier,
        showDivider = showDivider
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (exercise.targetReps != null) {
                FilledTonalIconButton(
                    onClick = {
                        val current = reps ?: 0
                        if (current > 1) onUpdateReps(exercise.planExerciseId, current - 1)
                    },
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = CloudGray,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) { Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp)) }
                Text(
                    "${reps ?: 0}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                FilledTonalIconButton(
                    onClick = { onUpdateReps(exercise.planExerciseId, (reps ?: 0) + 1) },
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = CloudGray,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) { Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp)) }
                Spacer(modifier = Modifier.width(4.dp))
            }
            IconButton(onClick = { onAlterExercise(exercise.planExerciseId) }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Replace exercise", tint = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Default.DragHandle, contentDescription = "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun PlanExerciseSummary.subtitleText(overrideReps: Int?): String = when {
    overrideReps != null -> "x$overrideReps"
    targetDurationSec != null -> "${targetDurationSec}s"
    else -> ""
}
