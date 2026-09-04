package com.example.homeworkout.domain.usecases.history

import com.example.homeworkout.domain.models.WorkoutHistoryRecord
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow

class GetWorkoutHistoryUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    operator fun invoke(): Flow<List<WorkoutHistoryRecord>> =
        workoutSessionRepository.observeCompletedSessions()
}
