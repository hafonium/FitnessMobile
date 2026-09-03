package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import com.example.homeworkout.domain.usecases.badges.EvaluateBadgesUseCase

/** The player reached the end of every exercise for the day. */
class CompleteWorkoutSessionUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val evaluateBadgesUseCase: EvaluateBadgesUseCase
) {
    suspend operator fun invoke(sessionId: Long): List<BadgeProgress> {
        workoutSessionRepository.completeSession(sessionId)
        return evaluateBadgesUseCase(triggerSessionId = sessionId)
    }
}
