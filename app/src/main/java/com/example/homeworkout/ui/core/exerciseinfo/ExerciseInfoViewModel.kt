package com.example.homeworkout.ui.core.exerciseinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.ExerciseDetail
import com.example.homeworkout.domain.usecases.exerciseinfo.GetExerciseDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExerciseInfoUiState {
    object Loading : ExerciseInfoUiState()
    data class Success(val detail: ExerciseDetail) : ExerciseInfoUiState()
    data class Error(val message: String) : ExerciseInfoUiState()
}

class ExerciseInfoViewModel(
    private val exerciseId: Long,
    private val getExerciseDetailUseCase: GetExerciseDetailUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<ExerciseInfoUiState>(ExerciseInfoUiState.Loading)
    val uiState: StateFlow<ExerciseInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getExerciseDetailUseCase(exerciseId)
                .onSuccess { _uiState.value = ExerciseInfoUiState.Success(it) }
                .onFailure { _uiState.value = ExerciseInfoUiState.Error(it.message ?: "Exercise not found") }
        }
    }
}
