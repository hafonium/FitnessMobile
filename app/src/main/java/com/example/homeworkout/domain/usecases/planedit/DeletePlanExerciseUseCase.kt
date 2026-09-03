package com.example.homeworkout.domain.usecases.planedit

import com.example.homeworkout.domain.repositories.WorkoutRepository

/** Removes an exercise from a user-created plan day and compacts its order. */
class DeletePlanExerciseUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(planExerciseId: Long) {
        workoutRepository.deletePlanExercise(planExerciseId)
    }
}
