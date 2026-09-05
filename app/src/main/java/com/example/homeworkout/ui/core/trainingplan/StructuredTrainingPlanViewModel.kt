package com.example.homeworkout.ui.core.trainingplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.training.StructuredProgramProgress
import com.example.homeworkout.domain.models.training.StructuredTrainingProgram
import com.example.homeworkout.domain.usecases.training.EnrollTrainingProgramUseCase
import com.example.homeworkout.domain.usecases.training.GetTrainingProgramUseCase
import com.example.homeworkout.domain.usecases.training.GetTrainingProgressUseCase
import com.example.homeworkout.domain.usecases.training.RepeatStructuredWeekUseCase
import com.example.homeworkout.domain.usecases.training.StartStructuredSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class StructuredTrainingPlanUiState(
    val isLoading: Boolean = true,
    val program: StructuredTrainingProgram? = null,
    val progress: StructuredProgramProgress? = null,
    val expandedWeeks: Set<Int> = setOf(1),
    val errorMessage: String? = null
)

class StructuredTrainingPlanViewModel(
    private val programId: String,
    private val getProgram: GetTrainingProgramUseCase,
    private val getProgress: GetTrainingProgressUseCase,
    private val enrollProgram: EnrollTrainingProgramUseCase,
    private val startSession: StartStructuredSessionUseCase,
    private val repeatWeek: RepeatStructuredWeekUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(StructuredTrainingPlanUiState())
    val uiState: StateFlow<StructuredTrainingPlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val program = runCatching { getProgram(programId) }.getOrElse {
                _uiState.value = StructuredTrainingPlanUiState(isLoading = false, errorMessage = it.message)
                return@launch
            }
            if (program == null) {
                _uiState.value = StructuredTrainingPlanUiState(isLoading = false, errorMessage = "Program not found")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = false, program = program)
            getProgress(programId).collectLatest { progress ->
                val previous = _uiState.value
                _uiState.value = previous.copy(
                    progress = progress,
                    expandedWeeks = if (previous.progress?.currentWeekNumber != progress.currentWeekNumber) {
                        previous.expandedWeeks + progress.currentWeekNumber
                    } else previous.expandedWeeks
                )
            }
        }
    }

    fun toggleWeek(weekNumber: Int) {
        _uiState.value = _uiState.value.let { state ->
            state.copy(expandedWeeks = if (weekNumber in state.expandedWeeks) state.expandedWeeks - weekNumber else state.expandedWeeks + weekNumber)
        }
    }

    fun enroll(onReady: (String, String) -> Unit) {
        val program = _uiState.value.program ?: return
        val first = program.weeks.first().sessions.first()
        viewModelScope.launch {
            enrollProgram(program.id)
            startSession(program.id, first.id, 1)
            onReady(program.id, first.id)
        }
    }

    fun start(sessionId: String, weekNumber: Int, onReady: (String, String) -> Unit) {
        viewModelScope.launch {
            startSession(programId, sessionId, weekNumber)
            onReady(programId, sessionId)
        }
    }

    fun repeatCurrentWeek() {
        val progress = _uiState.value.progress ?: return
        viewModelScope.launch { repeatWeek(programId, progress.currentWeekNumber) }
    }
}
