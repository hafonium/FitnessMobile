package com.example.homeworkout.ui.core.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.domain.usecases.details.GetWorkoutDetailsUseCase
import com.example.homeworkout.domain.usecases.player.ResolveNextPlanDayUseCase
import com.example.homeworkout.domain.usecases.player.ResolvedPlanDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val detail: WorkoutPlanDetail) : DetailUiState()
    object NotFound : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class DetailViewModel(
    private val workoutId: Long,
    getWorkoutDetailsUseCase: GetWorkoutDetailsUseCase,
    resolveNextPlanDayUseCase: ResolveNextPlanDayUseCase
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = getWorkoutDetailsUseCase(workoutId)
        .map<WorkoutPlanDetail?, DetailUiState> { detail ->
            if (detail != null) DetailUiState.Success(detail) else DetailUiState.NotFound
        }
        .catch { e -> emit(DetailUiState.Error(e.message ?: "Something went wrong")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState.Loading
        )

    /** Which day "Start" would play right now — a pure preview, no session opened. Wrapped in a
     * lazily-started flow so it's only computed while actually collected: the Edit Workout
     * Exercises screen reuses this same view model but never shows it, so it never triggers there. */
    val nextDay: StateFlow<ResolvedPlanDay?> = flow { emit(resolveNextPlanDayUseCase(workoutId)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}
