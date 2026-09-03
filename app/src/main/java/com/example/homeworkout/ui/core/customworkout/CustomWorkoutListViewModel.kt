package com.example.homeworkout.ui.core.customworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import com.example.homeworkout.domain.usecases.customworkout.DeleteCustomWorkoutPlanUseCase
import com.example.homeworkout.domain.usecases.home.GetWorkoutsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Custom Workout list — the user's own plans (source = CUSTOM). */
class CustomWorkoutListViewModel(
    getWorkoutsUseCase: GetWorkoutsUseCase,
    private val deleteCustomWorkoutPlanUseCase: DeleteCustomWorkoutPlanUseCase
) : ViewModel() {

    val customPlans: StateFlow<List<WorkoutModel>> = getWorkoutsUseCase(source = WorkoutPlanSource.CUSTOM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deletePlan(planId: Long) {
        viewModelScope.launch { deleteCustomWorkoutPlanUseCase(planId) }
    }
}
