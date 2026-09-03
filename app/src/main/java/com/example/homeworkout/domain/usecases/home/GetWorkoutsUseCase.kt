package com.example.homeworkout.domain.usecases.home

import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.flow.Flow

/** Used by the Training (home) screen, the category Workout List screen, and (source = CUSTOM)
 *  the Custom Workout list screen. */
class GetWorkoutsUseCase(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(category: WorkoutCategory? = null, source: WorkoutPlanSource? = null): Flow<List<WorkoutModel>> {
        return workoutRepository.getWorkouts(category, source)
    }
}
