package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.flow.first

/**
 * Opens a session for a specific day of a plan, chosen explicitly by the user — e.g. tapping
 * "Start" on one day's panel in the Workout Screen's grouped exercise list — bypassing the
 * automatic day-by-day resolution in [StartWorkoutSessionUseCase].
 */
class StartSpecificWorkoutDayUseCase(
    private val workoutRepository: WorkoutRepository,
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(planId: Long, planDayId: Long): WorkoutSessionStart? {
        val detail = workoutRepository.getWorkoutPlanDetail(planId).first() ?: return null
        val day = detail.days.firstOrNull { it.planDayId == planDayId } ?: return null
        if (day.exercises.isEmpty()) return null

        val sessionId = workoutSessionRepository.createSession(planId, day.planDayId)
        return WorkoutSessionStart(sessionId = sessionId, day = day, totalDays = detail.days.size)
    }
}
