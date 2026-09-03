package com.example.homeworkout.domain.usecases.planedit

import com.example.homeworkout.domain.repositories.WorkoutRepository

/** Swaps one plan-exercise slot's underlying exercise, keeping its reps/duration/order. */
class ReplacePlanExerciseUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(planExerciseId: Long, newExerciseId: Long) {
        workoutRepository.replacePlanExercise(planExerciseId, newExerciseId)
    }
}
