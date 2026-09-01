package com.example.homeworkout.ui.core.workoutlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WorkoutListViewModel(
    val category: WorkoutCategory,
    getWorkoutsUseCase: GetWorkoutsUseCase
) : ViewModel() {
    val workouts: StateFlow<List<WorkoutModel>> = getWorkoutsUseCase(category)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
