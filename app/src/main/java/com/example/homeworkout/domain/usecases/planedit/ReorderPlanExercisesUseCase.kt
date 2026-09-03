package com.example.homeworkout.domain.usecases.planedit

import com.example.homeworkout.domain.repositories.WorkoutRepository

/** Saves the user-selected order of all exercises in one custom plan day. */
class ReorderPlanExercisesUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(planDayId: Long, orderedPlanExerciseIds: List<Long>) {
        workoutRepository.reorderPlanExercises(planDayId, orderedPlanExerciseIds)
    }
}
