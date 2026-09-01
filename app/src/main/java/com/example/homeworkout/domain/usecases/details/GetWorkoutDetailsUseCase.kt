package com.example.homeworkout.domain.usecases.details

import com.example.homeworkout.domain.models.WorkoutModel
import com.example.homeworkout.domain.repositories.WorkoutRepository

class GetWorkoutDetailsUseCase(
    private val workoutRepository: WorkoutRepository
) {
    suspend operator fun invoke(workoutId: Int): Result<WorkoutModel> {
        TODO("Call workoutRepository.getWorkoutById(workoutId) and wrap it in a Result, failing if null")
    }
}
