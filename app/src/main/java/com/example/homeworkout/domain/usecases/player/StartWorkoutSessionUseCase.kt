package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.models.WorkoutPlanDayDetail
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository

/** What "Start" resolved to: which day of the plan to play, out of how many, and the session row now tracking it. */
data class WorkoutSessionStart(
    val sessionId: Long,
    val day: WorkoutPlanDayDetail,
    val totalDays: Int
)

/**
 * Resolves which day of a plan "Start" should play next (via [ResolveNextPlanDayUseCase]) and
 * opens a fresh session for it. A plan is always played one day at a time this way — never every
 * day's exercises flattened into a single session, which is what the player used to do.
 */
class StartWorkoutSessionUseCase(
    private val resolveNextPlanDayUseCase: ResolveNextPlanDayUseCase,
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(planId: Long): WorkoutSessionStart? {
        val resolved = resolveNextPlanDayUseCase(planId) ?: return null
        if (resolved.day.exercises.isEmpty()) return null

        val sessionId = workoutSessionRepository.createSession(planId, resolved.day.planDayId)
        return WorkoutSessionStart(sessionId = sessionId, day = resolved.day, totalDays = resolved.totalDays)
    }
}
