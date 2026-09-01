package com.example.homeworkout.domain.usecases.home

import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.flow.Flow

/** Used by both the Training (home) screen and the category Workout List screen. */
class GetWorkoutsUseCase(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(category: WorkoutCategory? = null): Flow<List<WorkoutModel>> {
        return workoutRepository.getWorkouts(category)
    }
}
