package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.models.WorkoutPlanDetail
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    /** All active plans, optionally narrowed to one Training-tab category. */
    fun getWorkouts(category: WorkoutCategory? = null): Flow<List<WorkoutModel>>

    suspend fun getWorkoutById(id: Long): WorkoutModel?

    /** The plan plus its days and each day's exercises, for the Workout Screen. */
    fun getWorkoutPlanDetail(id: Long): Flow<WorkoutPlanDetail?>
}
