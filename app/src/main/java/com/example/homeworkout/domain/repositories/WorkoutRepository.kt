package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.WorkoutModel
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getWorkouts(): Flow<List<WorkoutModel>>
    suspend fun getWorkoutById(id: Int): WorkoutModel?
    suspend fun addWorkout(workout: WorkoutModel)
}
