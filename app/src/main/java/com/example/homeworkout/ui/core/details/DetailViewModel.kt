package com.example.homeworkout.ui.core.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.domain.usecases.details.GetWorkoutDetailsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    getWorkoutDetailsUseCase: GetWorkoutDetailsUseCase
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
}
