package com.example.homeworkout.domain.usecases.planedit

import com.example.homeworkout.domain.repositories.WorkoutRepository

/** Updates one plan-exercise slot's target rep count. */
class UpdatePlanExerciseRepsUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(planExerciseId: Long, targetReps: Int) {
        workoutRepository.updatePlanExerciseReps(planExerciseId, targetReps)
    }
}
