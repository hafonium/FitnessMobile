package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.repositories.WorkoutSessionRepository

/** The user quit the player before finishing the day's exercises. */
class AbandonWorkoutSessionUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(sessionId: Long) = workoutSessionRepository.abandonSession(sessionId)
}
