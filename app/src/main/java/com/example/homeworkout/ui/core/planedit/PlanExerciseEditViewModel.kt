package com.example.homeworkout.ui.core.planedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.usecases.planedit.AddExercisesToPlanDayUseCase
import com.example.homeworkout.domain.usecases.planedit.DeletePlanExerciseUseCase
import com.example.homeworkout.domain.usecases.planedit.ReplacePlanExerciseUseCase
import com.example.homeworkout.domain.usecases.planedit.ReorderPlanExercisesUseCase
import com.example.homeworkout.domain.usecases.planedit.UpdatePlanExerciseRepsUseCase
import kotlinx.coroutines.launch

/**
 * Fire-and-forget writes for the plan-editing flow: Edit Plan's reps stepper, Add Exercises, and
 * Alter Exercise. This is the ViewModel those screens' `ui/navigation` call sites go through, so
 * Domain Use Cases are only ever invoked from a ViewModel, never straight from navigation code.
 * [onDone] runs after the write completes — used to pop the back stack once the exercise picker's
 * result has actually been saved, without this ViewModel knowing anything about navigation.
 */
class PlanExerciseEditViewModel(
    private val addExercisesToPlanDayUseCase: AddExercisesToPlanDayUseCase,
    private val replacePlanExerciseUseCase: ReplacePlanExerciseUseCase,
    private val updatePlanExerciseRepsUseCase: UpdatePlanExerciseRepsUseCase,
    private val deletePlanExerciseUseCase: DeletePlanExerciseUseCase,
    private val reorderPlanExercisesUseCase: ReorderPlanExercisesUseCase
) : ViewModel() {

    fun addExercises(planDayId: Long, exerciseIds: List<Long>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            addExercisesToPlanDayUseCase(planDayId, exerciseIds)
            onDone()
        }
    }

    fun replaceExercise(planExerciseId: Long, newExerciseId: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            replacePlanExerciseUseCase(planExerciseId, newExerciseId)
            onDone()
        }
    }

    fun updateReps(planExerciseId: Long, reps: Int) {
        viewModelScope.launch { updatePlanExerciseRepsUseCase(planExerciseId, reps) }
    }

    fun deleteExercise(planExerciseId: Long) {
        viewModelScope.launch { deletePlanExerciseUseCase(planExerciseId) }
    }

    fun reorderExercises(planDayId: Long, orderedPlanExerciseIds: List<Long>) {
        viewModelScope.launch { reorderPlanExercisesUseCase(planDayId, orderedPlanExerciseIds) }
    }
}
