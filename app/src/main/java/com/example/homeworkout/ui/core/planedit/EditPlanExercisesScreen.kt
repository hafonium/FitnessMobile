package com.example.homeworkout.ui.core.planedit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.ui.components.BackTopBar
import com.example.homeworkout.ui.components.ExerciseRow
import com.example.homeworkout.ui.components.ConfirmDialog
import com.example.homeworkout.ui.core.details.DetailUiState
import com.example.homeworkout.ui.core.details.DetailViewModel
import com.example.homeworkout.ui.theme.CloudGray
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * "Edit plan": lets you add/remove exercises and tweak reps/duration for the exercise list
 * loaded from [DetailViewModel]. Changes are persisted for user-created plans only.
 */
@Composable
fun EditPlanExercisesScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onAlterExercise: (planExerciseId: Long) -> Unit,
    onAddExercises: (planDayId: Long) -> Unit,
    onUpdateReps: (planExerciseId: Long, reps: Int) -> Unit,
    onDeleteExercise: (planExerciseId: Long) -> Unit,
    onReorder: (planDayId: Long, newOrderIds: List<Long>) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // planExerciseId -> locally edited reps, purely for on-screen stepper feedback.
    val repsOverrides = remember { mutableStateMapOf<Long, Int>() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var pendingDelete by remember { mutableStateOf<PlanExerciseSummary?>(null) }

        when (val state = uiState) {
            is DetailUiState.Success -> {
                val days = state.detail.days
                if (selectedTabIndex >= days.size && days.isNotEmpty()) {
                    selectedTabIndex = days.size - 1
                }
                val currentDay = days.getOrNull(selectedTabIndex)
                val initialExercises = currentDay?.exercises ?: emptyList()
                val currentDayId = currentDay?.planDayId ?: 0L

                val exercises = remember { mutableStateListOf<PlanExerciseSummary>() }
                LaunchedEffect(initialExercises) {
                    exercises.clear()
                    exercises.addAll(initialExercises)
                }

                val reorderState = rememberReorderableLazyListState(
                    onMove = { from, to ->
                        exercises.add(to.index, exercises.removeAt(from.index))
                    },
                    onDragEnd = { startIndex, endIndex ->
                        if (startIndex != endIndex) {
                            onReorder(currentDayId, exercises.map { it.planExerciseId })
                        }
                    }
                )

                Scaffold(
                    topBar = { BackTopBar(title = "Edit plan", onNavigateBack = onNavigateBack) },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { onAddExercises(currentDayId) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) { Icon(Icons.Default.Add, contentDescription = "Add exercise") }
                    }
                ) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (days.size > 1) {
                            ScrollableTabRow(
                                selectedTabIndex = selectedTabIndex,
                                edgePadding = 16.dp
                            ) {
                                days.forEachIndexed { index, day ->
                                    Tab(
                                        selected = selectedTabIndex == index,
                                        onClick = { selectedTabIndex = index },
                                        text = { Text(day.title ?: "Day ${day.dayNumber}") }
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            state = reorderState.listState,
                            modifier = Modifier.weight(1f).reorderable(reorderState),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, bottom = 100.dp,
                                top = 8.dp
                            )
                        ) {
                            items(exercises, key = { it.planExerciseId }) { exercise ->
                                val reps = repsOverrides[exercise.planExerciseId] ?: exercise.targetReps
                                val isDragging = reorderState.draggingItemKey == exercise.planExerciseId
                                ExerciseRow(
                                    title = exercise.title,
                                    subtitle = exercise.subtitleText(reps),
                                    imageUrl = exercise.gifUrl,
                                    modifier = Modifier
                                        .zIndex(if (isDragging) 1f else 0f)
                                        .graphicsLayer {
                                            if (isDragging) {
                                                translationY = reorderState.draggingItemTop
                                                translationX = reorderState.draggingItemLeft
                                            } else {
                                                val cancel = exercise.planExerciseId == reorderState.dragCancelledAnimation.position?.key
                                                if (cancel) {
                                                    translationY = reorderState.dragCancelledAnimation.offset.y
                                                    translationX = reorderState.dragCancelledAnimation.offset.x
                                                }
                                            }
                                        }
                                        .animateItem()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (exercise.targetReps != null) {
                                            FilledTonalIconButton(
                                                onClick = {
                                                    val current = reps ?: 0
                                                    if (current > 1) {
                                                        repsOverrides[exercise.planExerciseId] = current - 1
                                                        onUpdateReps(exercise.planExerciseId, current - 1)
                                                    }
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
                                                onClick = {
                                                    val newReps = (reps ?: 0) + 1
                                                    repsOverrides[exercise.planExerciseId] = newReps
                                                    onUpdateReps(exercise.planExerciseId, newReps)
                                                },
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
                                        IconButton(onClick = { pendingDelete = exercise }) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete exercise",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Icon(
                                            Icons.Default.DragHandle,
                                            contentDescription = "Reorder",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.detectReorderAfterLongPress(reorderState)
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }

        is DetailUiState.Loading -> Scaffold(topBar = { BackTopBar(title = "Edit plan", onNavigateBack = onNavigateBack) }) { inner -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            else -> Scaffold(topBar = { BackTopBar(title = "Edit plan", onNavigateBack = onNavigateBack) }) { inner -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) { Text("This workout could not be found.") } }
        }

    pendingDelete?.let { exercise ->
        ConfirmDialog(
            title = "Delete exercise?",
            message = "\"${exercise.title}\" will be removed from this workout day.",
            confirmLabel = "Delete",
            onConfirm = {
                repsOverrides.remove(exercise.planExerciseId)
                onDeleteExercise(exercise.planExerciseId)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

private fun PlanExerciseSummary.subtitleText(overrideReps: Int?): String = when {
    overrideReps != null -> "x$overrideReps"
    targetDurationSec != null -> "${targetDurationSec}s"
    else -> ""
}
