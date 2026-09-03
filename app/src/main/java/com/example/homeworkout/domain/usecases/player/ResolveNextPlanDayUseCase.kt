package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.models.WorkoutPlanDayDetail
import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus
import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.flow.first

/** Which day of a plan "Start" would play right now, out of how many days the plan has. */
data class ResolvedPlanDay(
    val day: WorkoutPlanDayDetail,
    val totalDays: Int
)

/**
 * Pure preview of the day-by-day resolution — no session is opened. Shared by
 * [StartWorkoutSessionUseCase] (which resolves, then opens a session for the result) and by the
 * Workout Screen (which shows it as "Start · Day N" and highlights that day in the exercise list,
 * without starting anything).
 *
 * Resolution: no session yet for this plan -> day 1. Last session wasn't COMPLETED (still open, or
 * abandoned) -> that same day again. Last session was COMPLETED -> the next day after it, wrapping
 * back to day 1 once the last day is done.
 */
class ResolveNextPlanDayUseCase(
    private val workoutRepository: WorkoutRepository,
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(planId: Long): ResolvedPlanDay? {
        val detail = workoutRepository.getWorkoutPlanDetail(planId).first() ?: return null
        val days = detail.days.sortedBy { it.dayNumber }
        if (days.isEmpty()) return null

        val latest = workoutSessionRepository.getLatestSessionForPlan(planId)
        val nextDay = when {
            latest == null -> days.first()
            latest.status != WorkoutSessionStatus.COMPLETED ->
                days.firstOrNull { it.planDayId == latest.planDayId } ?: days.first()
            else -> {
                val completedDayNumber = days.firstOrNull { it.planDayId == latest.planDayId }?.dayNumber ?: 0
                days.firstOrNull { it.dayNumber > completedDayNumber } ?: days.first()
            }
        }
        return ResolvedPlanDay(day = nextDay, totalDays = days.size)
    }
}
