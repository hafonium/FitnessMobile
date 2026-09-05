package com.example.homeworkout.ui.core.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.PlanExerciseSummary
import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.models.ExerciseDetail
import com.example.homeworkout.domain.models.SettingsPreferences
import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.usecases.badges.MarkBadgesSeenUseCase
import com.example.homeworkout.domain.usecases.exerciseinfo.GetExerciseDetailUseCase
import com.example.homeworkout.domain.usecases.player.AbandonWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.player.CompleteWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.player.GetResumableWorkoutUseCase
import com.example.homeworkout.domain.usecases.player.RestartWorkoutDayUseCase
import com.example.homeworkout.domain.usecases.player.SaveAndExitWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.player.SaveWorkoutProgressUseCase
import com.example.homeworkout.domain.usecases.player.StartSpecificWorkoutDayUseCase
import com.example.homeworkout.domain.usecases.player.StartWorkoutSessionUseCase
import com.example.homeworkout.domain.usecases.settings.GetSettingsUseCase
import com.example.homeworkout.ui.services.TickSoundPlayer
import com.example.homeworkout.ui.services.TtsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Ready(
        val sessionId: Long,
        val planDayId: Long,
        val dayNumber: Int,
        val totalDays: Int,
        val exercises: List<PlanExerciseSummary>,
        val initialPhase: WorkoutPhase = WorkoutPhase.PREP,
        val initialOrderIndex: Int = 0,
        val initialRemainingSec: Int? = null
    ) : PlayerUiState()
    /** The plan couldn't be found, or the resolved day has no exercises. */
    object Empty : PlayerUiState()
}

/**
 * The exercise-info bottom sheet's state, shown inline over the player (see
 * [com.example.homeworkout.ui.core.exerciseinfo.ExerciseInfoContent]) instead of navigating to a
 * separate screen, so the running timer, ticking sounds and coach voice never get torn down just
 * to check an exercise's form mid-set.
 */
sealed class ExerciseInfoSheetState {
    object Hidden : ExerciseInfoSheetState()
    object Loading : ExerciseInfoSheetState()
    data class Loaded(val detail: ExerciseDetail) : ExerciseInfoSheetState()
    data class Error(val message: String) : ExerciseInfoSheetState()
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
    private val resume: Boolean,
    private val startWorkoutSessionUseCase: StartWorkoutSessionUseCase,
    private val startSpecificWorkoutDayUseCase: StartSpecificWorkoutDayUseCase,
    private val restartWorkoutDayUseCase: RestartWorkoutDayUseCase,
    private val completeWorkoutSessionUseCase: CompleteWorkoutSessionUseCase,
    private val abandonWorkoutSessionUseCase: AbandonWorkoutSessionUseCase,
    private val getResumableWorkoutUseCase: GetResumableWorkoutUseCase,
    private val saveWorkoutProgressUseCase: SaveWorkoutProgressUseCase,
    private val saveAndExitWorkoutSessionUseCase: SaveAndExitWorkoutSessionUseCase,
    private val getExerciseDetailUseCase: GetExerciseDetailUseCase,
    private val markBadgesSeenUseCase: MarkBadgesSeenUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val ttsService: TtsService,
    private val tickSoundPlayer: TickSoundPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _newlyUnlockedBadges = MutableStateFlow<List<BadgeProgress>>(emptyList())
    val newlyUnlockedBadges: StateFlow<List<BadgeProgress>> = _newlyUnlockedBadges.asStateFlow()

    private val _exerciseInfoSheet = MutableStateFlow<ExerciseInfoSheetState>(ExerciseInfoSheetState.Hidden)
    val exerciseInfoSheet: StateFlow<ExerciseInfoSheetState> = _exerciseInfoSheet.asStateFlow()

    private val settings: StateFlow<SettingsPreferences> = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPreferences())

    init {
        viewModelScope.launch { beginDay() }
    }

    /** Speaks a coaching cue (get ready / rest time / next exercise / finished) in the user's chosen voice. */
    fun speak(text: String) {
        val current = settings.value
        if (!current.soundEnabled) return
        ttsService.speak(text, current.ttsVoiceType, current.customVoiceName)
    }

    /** Plays one countdown tick — called for each of a timer's last 5 seconds. */
    fun tick() {
        val current = settings.value
        if (!current.soundEnabled) return
        tickSoundPlayer.tick(current.soundVolume)
    }

    /** Long tick + vibration — called once when a new exercise starts. */
    fun signalExerciseStart() {
        val current = settings.value
        if (!current.soundEnabled) return
        tickSoundPlayer.exerciseStartSignal(current.soundVolume)
    }

    override fun onCleared() {
        ttsService.stop()
        tickSoundPlayer.release()
        super.onCleared()
    }

    private suspend fun beginDay() {
        if (resume) {
            val resumed = getResumableWorkoutUseCase(planId)
            if (resumed != null) {
                _uiState.value = PlayerUiState.Ready(
                    sessionId = resumed.sessionId,
                    planDayId = resumed.day.planDayId,
                    dayNumber = resumed.day.dayNumber,
                    totalDays = resumed.totalDays,
                    exercises = resumed.day.exercises,
                    initialPhase = resumed.phase,
                    initialOrderIndex = resumed.orderIndex,
                    initialRemainingSec = resumed.remainingSec
                )
                return
            }
        }

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
        viewModelScope.launch {
            _newlyUnlockedBadges.value = completeWorkoutSessionUseCase(sessionId)
        }
    }

    fun dismissUnlockedBadge(badgeId: String) {
        _newlyUnlockedBadges.value = _newlyUnlockedBadges.value.filterNot {
            it.definition.id == badgeId
        }
        viewModelScope.launch { markBadgesSeenUseCase(listOf(badgeId)) }
    }

    /**
     * "Discard" from the mid-workout guard dialog. Takes [onDone] rather than being pure
     * fire-and-forget: the caller navigates away right after this, which would otherwise clear
     * this ViewModel (cancelling [viewModelScope]) before the write ever ran.
     */
    fun abandonSession(sessionId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            abandonWorkoutSessionUseCase(sessionId)
            onDone()
        }
    }

    /** Auto-save point: exercise completed, phase changed, or the user paused. */
    fun saveProgress(sessionId: Long, phase: WorkoutPhase, orderIndex: Int, remainingSec: Int?) {
        viewModelScope.launch { saveWorkoutProgressUseCase(sessionId, phase, orderIndex, remainingSec) }
    }

    /**
     * "Save & Exit" from the mid-workout guard dialog. Takes [onDone] for the same reason as
     * [abandonSession] — the caller navigates away right after, so we must finish writing first.
     */
    fun saveAndExit(sessionId: Long, phase: WorkoutPhase, orderIndex: Int, remainingSec: Int?, onDone: () -> Unit) {
        viewModelScope.launch {
            saveAndExitWorkoutSessionUseCase(sessionId, phase, orderIndex, remainingSec)
            onDone()
        }
    }

    /** Opens the exercise-info sheet in place — never navigates, so the workout keeps running underneath it. */
    fun showExerciseInfo(exerciseId: Long) {
        _exerciseInfoSheet.value = ExerciseInfoSheetState.Loading
        viewModelScope.launch {
            getExerciseDetailUseCase(exerciseId)
                .onSuccess { _exerciseInfoSheet.value = ExerciseInfoSheetState.Loaded(it) }
                .onFailure { _exerciseInfoSheet.value = ExerciseInfoSheetState.Error(it.message ?: "Exercise not found") }
        }
    }

    fun dismissExerciseInfo() {
        _exerciseInfoSheet.value = ExerciseInfoSheetState.Hidden
    }
}
