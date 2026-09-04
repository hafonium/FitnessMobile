package com.example.homeworkout.domain.usecases.player

import com.example.homeworkout.domain.models.WorkoutPlanDayDetail
import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.flow.first

/** What a resumable session should reopen to: which day, out of how many, at which exact spot. */
data class ResumedWorkoutSession(
    val sessionId: Long,
    val day: WorkoutPlanDayDetail,
    val totalDays: Int,
    val phase: WorkoutPhase,
    val orderIndex: Int,
    val remainingSec: Int?
)

/**
 * Backs the Detail screen's "Resume Workout" button and the player's resume path: is there an
 * IN_PROGRESS/PAUSED, non-stale session for [planId] (see
 * [WorkoutSessionRepository.getResumableSession]), and if so, what day/exercise/phase does it
 * resolve to?
 */
class GetResumableWorkoutUseCase(
    private val workoutRepository: WorkoutRepository,
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(planId: Long): ResumedWorkoutSession? {
        val resumable = workoutSessionRepository.getResumableSession(planId) ?: return null
        val detail = workoutRepository.getWorkoutPlanDetail(planId).first() ?: return null
        val day = detail.days.firstOrNull { it.planDayId == resumable.planDayId } ?: return null

        return ResumedWorkoutSession(
            sessionId = resumable.sessionId,
            day = day,
            totalDays = detail.days.size,
            phase = resumable.phase,
            orderIndex = resumable.orderIndex,
            remainingSec = resumable.remainingSec
        )
    }
}
