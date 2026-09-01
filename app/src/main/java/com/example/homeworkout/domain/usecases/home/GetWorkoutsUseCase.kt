package com.example.homeworkout.domain.usecases.home

import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.repositories.WorkoutRepository
import kotlinx.coroutines.flow.Flow

class GetWorkoutsUseCase(
    private val workoutRepository: WorkoutRepository
) {
    operator fun invoke(): Flow<List<WorkoutModel>> {
        TODO("Return workoutRepository.getWorkouts(), applying any home-screen-only filtering/sorting")
    }
}
