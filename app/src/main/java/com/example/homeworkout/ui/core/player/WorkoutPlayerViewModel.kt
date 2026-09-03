package com.example.homeworkout.ui.core.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.domain.usecases.player.AbandonWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.player.CompleteWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.player.RestartWorkoutDayUseCase
import com.example.homeworkout.domain.usecases.player.StartSpecificWorkoutDayUseCase
import com.example.homeworkout.domain.usecases.player.StartWorkoutSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Ready(
        val sessionId: Long,
        val planDayId: Long,
        val dayNumber: Int,
        val totalDays: Int,
        val exercises: List<PlanExerciseSummary>
    ) : PlayerUiState()
    /** The plan couldn't be found, or the resolved day has no exercises. */
    object Empty : PlayerUiState()
}

/**
 * Backs the During Workout player: resolves which day of [planId] to play — [requestedPlanDayId]
 * when the user picked a specific day from the Detail screen, otherwise the next day in sequence
 * (via [StartWorkoutSessionUseCase] — never all days flattened together) — and opens a
 * `workout_sessions` row for it, then reports completion/abandonment back so day-by-day
 * progression and streaks stay accurate.
 */
class WorkoutPlayerViewModel(
    private val planId: Long,
    private val requestedPlanDayId: Long?,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
    private val startSpecificWorkoutDayUseCase: StartSpecificWorkoutDayUseCase,
    private val restartWorkoutDayUseCase: RestartWorkoutDayUseCase,
    private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase,
    private val abandonWorkoutSessionUseCase: AbandonWorkoutSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { beginDay() }
    }

    private suspend fun beginDay() {
        val dayId = requestedPlanDayId
        val start = if (dayId != null) {
            startSpecificWorkoutDayUseCase(planId = planId, planDayId = dayId)
        } else {
            startWorkoutSessionUseCase(planId)
        }
        _uiState.value = if (start == null) {
            PlayerUiState.Empty
        } else {
            PlayerUiState.Ready(
                sessionId = start.sessionId,
                planDayId = start.day.planDayId,
                dayNumber = start.day.dayNumber,
                totalDays = start.totalDays,
                exercises = start.day.exercises
            )
        }
    }

    /** "Restart this workout" from the Completed screen — same day, fresh session row (not [beginDay] again, which would advance to the next day since this one just completed). */
    fun restartDay() {
        val current = _uiState.value as? PlayerUiState.Ready ?: return
        viewModelScope.launch {
            val newSessionId = restartWorkoutDayUseCase(planId = planId, planDayId = current.planDayId)
            _uiState.value = current.copy(sessionId = newSessionId)
        }
    }

    fun completeSession(sessionId: Long) {
        viewModelScope.launch { completeWorkoutSessionUseCase(sessionId) }
    }

    fun abandonSession(sessionId: Long) {
        viewModelScope.launch { abandonWorkoutSessionUseCase(sessionId) }
    }
}
