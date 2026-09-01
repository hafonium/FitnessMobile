package com.example.homeworkout.ui.core.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.usecases.details.GetWorkoutDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val workout: WorkoutModel) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class DetailViewModel(
    private val workoutId: Int,
    private val getWorkoutDetailsUseCase: GetWorkoutDetailsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadWorkoutDetails() {
        viewModelScope.launch {
            TODO("Call getWorkoutDetailsUseCase(workoutId) and update _uiState with Success/Error")
        }
    }
}
