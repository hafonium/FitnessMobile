package com.example.homeworkout.ui.core.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val workouts: List<WorkoutModel>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val getWorkoutsUseCase: GetWorkoutsUseCase
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<WorkoutCategory?>(null)
    val selectedCategory: StateFlow<WorkoutCategory?> = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = _selectedCategory
        .flatMapLatest { category ->
            getWorkoutsUseCase(category)
                .map<List<WorkoutModel>, HomeUiState> { HomeUiState.Success(it) }
                .catch { e -> emit(HomeUiState.Error(e.message ?: "Something went wrong")) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun selectCategory(category: WorkoutCategory?) {
        _selectedCategory.value = category
    }
}
