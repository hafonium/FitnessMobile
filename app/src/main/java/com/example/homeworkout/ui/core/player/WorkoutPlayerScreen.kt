package com.example.homeworkout.ui.core.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.core.details.DetailUiState
import com.example.homeworkout.ui.core.details.DetailViewModel
import com.example.homeworkout.utils.ScreenWrapper
import kotlinx.coroutines.delay

private const val PREP_SECONDS = 6
private const val REST_SECONDS = 20

private enum class Phase { PREP, EXERCISE, REST, COMPLETED }

/**
 * The "During Workout" player: prep countdown -> exercise -> rest -> ... -> completed. Drives an
 * in-memory countdown only; nothing is written to `workout_sessions` (that is out of scope here).
 */
@Composable
fun WorkoutPlayerScreen(
    viewModel: DetailViewModel,
    onClose: () -> Unit,
    onExerciseInfo: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenWrapper {
        when (val state = uiState) {
            is DetailUiState.Success -> {
                val exercises = state.detail.days.flatMap { it.exercises }
                if (exercises.isEmpty()) {
                    CenteredMessage("This workout has no exercises yet.")
                } else {
                    PlayerContent(exercises = exercises, onClose = onClose, onExerciseInfo = onExerciseInfo)
                }
            }

            is DetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else -> CenteredMessage("This workout could not be found.")
        }
    }
}

@Composable
private fun PlayerContent(
    exercises: List<PlanExerciseSummary>,
    onClose: () -> Unit,
    onExerciseInfo: (Long) -> Unit
) {
    var phase by remember { mutableStateOf(Phase.PREP) }
    var index by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(PREP_SECONDS) }
    var paused by remember { mutableStateOf(false) }
    var completedCount by remember { mutableIntStateOf(0) }

    val current = exercises.getOrNull(index) ?: exercises.first()
    val isTimed = current.targetDurationSec != null

    fun finishCurrentExercise() {
        completedCount = (index + 1).coerceAtMost(exercises.size)
        if (index >= exercises.lastIndex) {
            phase = Phase.COMPLETED
        } else {
            phase = Phase.REST
            remaining = REST_SECONDS
        }
    }

    LaunchedEffect(phase, index, paused) {
        if (paused || phase == Phase.COMPLETED) return@LaunchedEffect
        if (phase == Phase.EXERCISE && !isTimed) return@LaunchedEffect
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        when (phase) {
            Phase.PREP -> {
                phase = Phase.EXERCISE
                remaining = current.targetDurationSec ?: 30
            }
            Phase.EXERCISE -> finishCurrentExercise()
            Phase.REST -> {
                index += 1
                phase = Phase.EXERCISE
                remaining = exercises[index].targetDurationSec ?: 30
            }
            Phase.COMPLETED -> Unit
        }
    }

    when (phase) {
        Phase.PREP -> PrepView(nextExerciseTitle = current.title, count = remaining, onSkip = {
            phase = Phase.EXERCISE
            remaining = current.targetDurationSec ?: 30
        })

        Phase.EXERCISE -> ExerciseView(
            exercise = current,
            index = index,
            total = exercises.size,
            remaining = remaining,
            paused = paused,
            onInfo = { onExerciseInfo(current.exerciseId) },
            onTogglePause = { paused = !paused },
            onPrevious = {
                if (index > 0) {
                    index -= 1
                    phase = Phase.EXERCISE
                    remaining = exercises[index].targetDurationSec ?: 30
                }
            },
            onNext = { finishCurrentExercise() }
        )

        Phase.REST -> RestView(
            remaining = remaining,
            next = exercises.getOrNull(index + 1),
            nextNumber = (index + 2).coerceAtMost(exercises.size),
            total = exercises.size,
            onAddTime = { remaining += 20 },
            onSkip = {
                if (index >= exercises.lastIndex) {
                    phase = Phase.COMPLETED
                } else {
                    index += 1
                    phase = Phase.EXERCISE
                    remaining = exercises[index].targetDurationSec ?: 30
                }
            }
        )

        Phase.COMPLETED -> CompletedView(
            completedCount = completedCount,
            onKeepExercising = onClose,
            onRestart = {
                index = 0
                completedCount = 0
                remaining = PREP_SECONDS
                phase = Phase.PREP
            },
            onDoItLater = onClose
        )
    }
}

@Composable
private fun PrepView(nextExerciseTitle: String, count: Int, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ExerciseThumbnail(size = 160.dp)
        Text("READY TO GO!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(nextExerciseTitle.uppercase(), style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier.size(96.dp).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$count", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        AppButton(text = "Skip", onClick = onSkip)
    }
}

@Composable
private fun ExerciseView(
    exercise: PlanExerciseSummary,
    index: Int,
    total: Int,
    remaining: Int,
    paused: Boolean,
    onInfo: () -> Unit,
    onTogglePause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("${index + 1} / $total", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExerciseThumbnail(size = 180.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(exercise.title.uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onInfo) { Icon(Icons.Default.Info, contentDescription = "Exercise information") }
        }
        Text(
            text = if (exercise.targetDurationSec != null) formatTime(remaining) else "x${exercise.targetReps ?: 0}",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous exercise") }
            IconButton(onClick = onTogglePause) {
                Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = if (paused) "Resume" else "Pause")
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, contentDescription = "Next exercise") }
        }
        AppButton(text = if (exercise.targetDurationSec != null) "Done" else "Done · next", onClick = onNext)
    }
}

@Composable
private fun RestView(
    remaining: Int,
    next: PlanExerciseSummary?,
    nextNumber: Int,
    total: Int,
    onAddTime: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("REST", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(formatTime(remaining), color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            if (next != null) {
                Text(
                    "NEXT $nextNumber/$total  ${next.title.uppercase()}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton(text = "+20s", onClick = onAddTime)
                AppButton(text = "Skip", onClick = onSkip)
            }
        }
    }
}

@Composable
private fun CompletedView(
    completedCount: Int,
    onKeepExercising: () -> Unit,
    onRestart: () -> Unit,
    onDoItLater: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💪", style = MaterialTheme.typography.displaySmall)
        Text("$completedCount exercises completed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Sweat more, shine later!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        AppButton(text = "Keep exercising", onClick = onKeepExercising)
        AppButton(text = "Restart this workout", onClick = onRestart)
        Text("Do it later", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message) }
}

private fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
