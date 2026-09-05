package com.example.homeworkout.ui.core.trainingplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.SettingsPreferences
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunStatus
import com.example.homeworkout.domain.models.training.StructuredTrainingProgram
import com.example.homeworkout.domain.models.training.StructuredTrainingSession
import com.example.homeworkout.domain.models.training.StructuredTrainingStep
import com.example.homeworkout.domain.usecases.running.ObserveRunningSessionUseCase
import com.example.homeworkout.domain.usecases.settings.GetSettingsUseCase
import com.example.homeworkout.domain.usecases.training.CompleteStructuredSessionUseCase
import com.example.homeworkout.domain.usecases.training.GetTrainingProgramUseCase
import com.example.homeworkout.domain.usecases.training.StartStructuredSessionUseCase
import com.example.homeworkout.ui.services.TickSoundPlayer
import com.example.homeworkout.ui.services.TtsService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class IntervalPlayerStatus { READY, ACTIVE, PAUSED, FINISHED }

data class StructuredTrainingPlayerUiState(
    val isLoading: Boolean = true,
    val program: StructuredTrainingProgram? = null,
    val session: StructuredTrainingSession? = null,
    val steps: List<StructuredTrainingStep> = emptyList(),
    val stepIndex: Int = 0,
    val secondsRemaining: Int = 0,
    val elapsedSeconds: Int = 0,
    val status: IntervalPlayerStatus = IntervalPlayerStatus.READY,
    val tracking: RunSession? = null,
    val errorMessage: String? = null,
    /** True while the FINISHED write is in flight — gates the screen's exit controls so leaving
     * doesn't clear this ViewModel (cancelling viewModelScope) before the write lands. */
    val savingCompletion: Boolean = false
) {
    val currentStep: StructuredTrainingStep? get() = steps.getOrNull(stepIndex)
}

class StructuredTrainingPlayerViewModel(
    private val programId: String,
    private val sessionId: String,
    private val getProgram: GetTrainingProgramUseCase,
    private val startSession: StartStructuredSessionUseCase,
    private val completeSession: CompleteStructuredSessionUseCase,
    observeRunningSession: ObserveRunningSessionUseCase,
    getSettings: GetSettingsUseCase,
    private val ttsService: TtsService,
    private val tickSoundPlayer: TickSoundPlayer
) : ViewModel() {
    private val _uiState = MutableStateFlow(StructuredTrainingPlayerUiState())
    val uiState: StateFlow<StructuredTrainingPlayerUiState> = _uiState.asStateFlow()
    private var timer: Job? = null
    private var completionPersisted = false
    private val settings: StateFlow<SettingsPreferences> = getSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPreferences())

    init {
        viewModelScope.launch {
            val program = getProgram(programId)
            val session = program?.weeks?.asSequence()?.flatMap { it.sessions.asSequence() }?.firstOrNull { it.id == sessionId }
            if (program == null || session == null) {
                _uiState.value = StructuredTrainingPlayerUiState(isLoading = false, errorMessage = "Workout session not found")
                return@launch
            }
            val steps = session.steps.ifEmpty {
                listOf(StructuredTrainingStep(session.title, session.durationMinMinutes * 60, intensityFor(session.type)))
            }
            _uiState.value = StructuredTrainingPlayerUiState(
                isLoading = false,
                program = program,
                session = session,
                steps = steps,
                secondsRemaining = steps.first().durationSeconds
            )
        }
        viewModelScope.launch {
            observeRunningSession().collect { tracking ->
                _uiState.value = _uiState.value.copy(
                    tracking = tracking?.takeIf { it.status == RunStatus.RUNNING || it.status == RunStatus.PAUSED }
                )
            }
        }
    }

    fun begin() {
        val current = _uiState.value
        val program = current.program ?: return
        val session = current.session ?: return
        val week = program.weeks.first { week -> week.sessions.any { it.id == session.id } }
        viewModelScope.launch { startSession(programId, sessionId, week.weekNumber) }
        _uiState.value = current.copy(status = IntervalPlayerStatus.ACTIVE)
        signalStep(current.currentStep)
        launchTimer()
    }

    fun pause() {
        timer?.cancel()
        _uiState.value = _uiState.value.copy(status = IntervalPlayerStatus.PAUSED)
    }

    fun resume() {
        _uiState.value = _uiState.value.copy(status = IntervalPlayerStatus.ACTIVE)
        launchTimer()
    }

    fun finish() {
        timer?.cancel()
        val current = _uiState.value
        _uiState.value = current.copy(status = IntervalPlayerStatus.FINISHED)
        persistCompletion(current)
    }

    private fun launchTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            while (isActive && _uiState.value.status == IntervalPlayerStatus.ACTIVE) {
                delay(1_000)
                val current = _uiState.value
                if (current.secondsRemaining > 1) {
                    val nextRemaining = current.secondsRemaining - 1
                    _uiState.value = current.copy(secondsRemaining = nextRemaining, elapsedSeconds = current.elapsedSeconds + 1)
                    if (nextRemaining <= 5) tick()
                } else {
                    val nextIndex = current.stepIndex + 1
                    if (nextIndex >= current.steps.size) {
                        _uiState.value = current.copy(
                            secondsRemaining = 0,
                            elapsedSeconds = current.elapsedSeconds + 1,
                            status = IntervalPlayerStatus.FINISHED
                        )
                        persistCompletion(_uiState.value)
                        return@launch
                    }
                    val nextStep = current.steps[nextIndex]
                    _uiState.value = current.copy(
                        stepIndex = nextIndex,
                        secondsRemaining = nextStep.durationSeconds,
                        elapsedSeconds = current.elapsedSeconds + 1
                    )
                    signalStep(nextStep)
                }
            }
        }
    }

    private fun persistCompletion(current: StructuredTrainingPlayerUiState) {
        if (completionPersisted) return
        completionPersisted = true
        _uiState.update { it.copy(savingCompletion = true) }
        viewModelScope.launch {
            completeSession(
                programId,
                sessionId,
                durationSeconds = current.elapsedSeconds,
                distanceMeters = current.tracking?.distanceMeters
            )
            speak("Workout complete")
            _uiState.update { it.copy(savingCompletion = false) }
        }
    }

    private fun signalStep(step: StructuredTrainingStep?) {
        step ?: return
        val current = settings.value
        if (current.soundEnabled) tickSoundPlayer.exerciseStartSignal(current.soundVolume)
        if (current.voiceEnabled) ttsService.speak(step.label, current.ttsVoiceType, current.customVoiceName)
    }

    private fun tick() {
        val current = settings.value
        if (current.soundEnabled) tickSoundPlayer.tick(current.soundVolume)
    }

    private fun speak(text: String) {
        val current = settings.value
        if (current.voiceEnabled) ttsService.speak(text, current.ttsVoiceType, current.customVoiceName)
    }

    private fun intensityFor(type: String): String = when (type) {
        "LONG_WALK" -> "EASY"
        else -> type
    }

    override fun onCleared() {
        timer?.cancel()
        ttsService.stop()
        super.onCleared()
    }
}
