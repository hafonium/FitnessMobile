package com.example.homeworkout.ui.core.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.ui.components.ExerciseThumbnail
import com.example.homeworkout.ui.components.buttons.AppButton
import com.example.homeworkout.ui.components.buttons.AppButtonVariant
import com.example.homeworkout.ui.theme.BrandBlueTint
import com.example.homeworkout.ui.theme.InkBlack
import com.example.homeworkout.ui.theme.PillShape
import com.example.homeworkout.ui.theme.SlateGray
import com.example.homeworkout.utils.ScreenWrapper
import kotlinx.coroutines.delay

private const val PREP_SECONDS = 6
private const val REST_SECONDS = 20

private enum class Phase { PREP, EXERCISE, REST, COMPLETED }

/**
 * The "During Workout" player: prep countdown -> exercise -> rest -> ... -> completed, for one day
 * of a plan at a time (never every day flattened together — see [WorkoutPlayerViewModel] /
 * [StartWorkoutSessionUseCase][com.example.homeworkout.domain.usecases.player.StartWorkoutSessionUseCase]).
 * Reaching the end or quitting reports back to the view model so the `workout_sessions` row is
 * closed out as COMPLETED/ABANDONED, which is what drives day-by-day progression and streaks.
 * Fine-grained progress (which exercise you were on) is still in-memory only, so quitting always
 * restarts the day from the top next time.
 */
@Composable
fun WorkoutPlayerScreen(
    viewModel: WorkoutPlayerViewModel,
    onClose: () -> Unit,
    onExerciseInfo: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenWrapper {
        when (val state = uiState) {
            is PlayerUiState.Ready -> PlayerContent(
                exercises = state.exercises,
                dayNumber = state.dayNumber,
                totalDays = state.totalDays,
                onComplete = { viewModel.completeSession(state.sessionId) },
                onAbandon = { viewModel.abandonSession(state.sessionId) },
                onRestartSession = { viewModel.restartDay() },
                onClose = onClose,
                onExerciseInfo = onExerciseInfo
            )

            is PlayerUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is PlayerUiState.Empty -> CenteredMessage("This workout has no exercises yet.")
        }
    }
}

@Composable
private fun PlayerContent(
    exercises: List<PlanExerciseSummary>,
    dayNumber: Int,
    totalDays: Int,
    onComplete: () -> Unit,
    onAbandon: () -> Unit,
    onRestartSession: () -> Unit,
    onClose: () -> Unit,
    onExerciseInfo: (Long) -> Unit
) {
    var phase by remember { mutableStateOf(Phase.PREP) }
    var index by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(PREP_SECONDS) }
    var paused by remember { mutableStateOf(false) }
    var completedCount by remember { mutableIntStateOf(0) }
    var showQuitDialog by remember { mutableStateOf(false) }

    val current = exercises.getOrNull(index) ?: exercises.first()
    val isTimed = current.targetDurationSec != null

    fun finishCurrentExercise() {
        completedCount = (index + 1).coerceAtMost(exercises.size)
        if (index >= exercises.lastIndex) {
            phase = Phase.COMPLETED
            onComplete()
        } else {
            phase = Phase.REST
            remaining = REST_SECONDS
        }
    }

    LaunchedEffect(phase, index, paused, showQuitDialog) {
        if (paused || showQuitDialog || phase == Phase.COMPLETED) return@LaunchedEffect
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

    Box(modifier = Modifier.fillMaxSize()) {
    when (phase) {
        Phase.PREP -> PrepView(nextExerciseTitle = current.title, gifUrl = current.gifUrl, count = remaining, onSkip = {
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
                    onComplete()
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
                onRestartSession()
                index = 0
                completedCount = 0
                remaining = PREP_SECONDS
                phase = Phase.PREP
            },
            onDoItLater = onClose
        )
    }

        if (totalDays > 1 && phase != Phase.COMPLETED) {
            Text(
                "DAY $dayNumber/$totalDays",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (phase == Phase.REST) Color.White else SlateGray,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            )
        }

        if (phase != Phase.COMPLETED) {
            IconButton(
                onClick = { showQuitDialog = true },
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quit workout",
                    tint = if (phase == Phase.REST) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Quit workout?") },
            text = { Text("You'll leave this session and go back to the plan. Progress isn't saved.") },
            confirmButton = {
                TextButton(onClick = { showQuitDialog = false; onAbandon(); onClose() }) { Text("Quit") }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) { Text("Keep going") }
            }
        )
    }
}

@Composable
private fun PrepView(nextExerciseTitle: String, gifUrl: String?, count: Int, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ExerciseThumbnail(size = 180.dp, imageUrl = gifUrl)
        Spacer(Modifier.height(28.dp))
        Text(
            "READY TO GO!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            nextExerciseTitle.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = SlateGray,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(96.dp).border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$count", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            }
            IconButton(onClick = onSkip) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Skip",
                    tint = SlateGray,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
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
        Text("${index + 1} / $total", style = MaterialTheme.typography.labelLarge, color = SlateGray)
        ExerciseThumbnail(size = 180.dp, imageUrl = exercise.gifUrl)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                exercise.title.uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = InkBlack
            )
            IconButton(onClick = onInfo, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Info, contentDescription = "Exercise information", tint = SlateGray)
            }
        }
        Text(
            text = if (exercise.targetDurationSec != null) formatTime(remaining) else "x${exercise.targetReps ?: 0}",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            color = InkBlack
        )
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            TransportButton(icon = Icons.Default.SkipPrevious, contentDescription = "Previous exercise", onClick = onPrevious)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onTogglePause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (paused) "Resume" else "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            TransportButton(icon = Icons.Default.SkipNext, contentDescription = "Next exercise", onClick = onNext)
        }
        AppButton(
            text = if (exercise.targetDurationSec != null) "Done" else "Done · next",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TransportButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = SlateGray, modifier = Modifier.size(30.dp))
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (next != null) {
                Text("NEXT $nextNumber/$total", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge)
                Text(
                    next.title.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 36.dp)
                )
            }
            Text("REST", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(formatTime(remaining), color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(36.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PillShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onAddTime)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+20s", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PillShape)
                        .background(Color.White)
                        .clickable(onClick = onSkip)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
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
        modifier = Modifier.fillMaxSize().background(BrandBlueTint).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("💪", fontSize = 36.sp)
        }
        Spacer(Modifier.height(20.dp))
        Row {
            Text(
                "$completedCount exercises",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(" completed.", color = InkBlack, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Sweat more, shine later!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = InkBlack,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        AppButton(text = "Keep exercising", onClick = onKeepExercising, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        AppButton(
            text = "Restart this workout",
            onClick = onRestart,
            variant = AppButtonVariant.Tonal,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Do it later",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(8.dp).clickable(onClick = onDoItLater)
        )
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
