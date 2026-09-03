package com.example.homeworkout.domain.usecases.customworkout

import com.example.homeworkout.domain.repositories.WorkoutRepository

/** Deletes a plan the user created — guarded to CUSTOM plans only; a no-op for system plans. */
class DeleteCustomWorkoutPlanUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(planId: Long) {
        workoutRepository.deleteCustomPlan(planId)
    }
}
