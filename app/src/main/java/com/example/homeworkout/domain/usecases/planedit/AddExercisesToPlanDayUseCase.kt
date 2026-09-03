package com.example.homeworkout.domain.usecases.planedit

import com.example.homeworkout.domain.repositories.WorkoutRepository

/** Appends exercises to one day of a plan, after its existing exercises. */
class AddExercisesToPlanDayUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(planDayId: Long, exerciseIds: List<Long>) {
        workoutRepository.addExercisesToDay(planDayId, exerciseIds)
    }
}
