package com.example.homeworkout.domain.usecases.details

import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.flow.Flow

/** Backs the Workout Screen (plan detail): the plan plus its days and each day's exercises. */
class GetWorkoutDetailsUseCase(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(workoutId: Long): Flow<WorkoutPlanDetail?> {
        return workoutRepository.getWorkoutPlanDetail(workoutId)
    }
}
