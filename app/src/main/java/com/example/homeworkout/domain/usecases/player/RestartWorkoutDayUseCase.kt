package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.repositories.WorkoutSessionRepository

/** "Restart this workout" on the Completed screen: a fresh session row for the same day just played (not day-resolution again — that would skip to the next day since this one just completed). */
class RestartWorkoutDayUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(planId: Long, planDayId: Long): Long =
        workoutSessionRepository.createSession(planId, planDayId)
}
