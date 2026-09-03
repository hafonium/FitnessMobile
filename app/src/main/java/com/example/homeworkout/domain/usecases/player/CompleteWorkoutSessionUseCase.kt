package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.repositories.WorkoutSessionRepository

/** The player reached the end of every exercise for the day. */
class CompleteWorkoutSessionUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(sessionId: Long) = workoutSessionRepository.completeSession(sessionId)
}
