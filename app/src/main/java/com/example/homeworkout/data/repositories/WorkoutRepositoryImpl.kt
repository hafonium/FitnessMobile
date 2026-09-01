package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.WorkoutDao
import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.flow.Flow

class WorkoutRepositoryImpl(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {

    override fun getWorkouts(): Flow<List<WorkoutModel>> {
        TODO("Collect workoutDao.getAllWorkouts() and map each WorkoutEntity to a WorkoutModel")
    }

    override suspend fun getWorkoutById(id: Int): WorkoutModel? {
        TODO("Call workoutDao.getWorkoutById(id) and map the WorkoutEntity? to a WorkoutModel?")
    }

    override suspend fun addWorkout(workout: WorkoutModel) {
        TODO("Map the WorkoutModel to a WorkoutEntity and call workoutDao.insertWorkout(...)")
    }
}
