package com.example.homeworkout.domain.usecases.home

import com.example.homeworkout.domain.models.ActiveWorkoutSummary
import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.repositories.WorkoutRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Backs the Home screen's "Continue" card, shown above Body Focus so an in-progress or paused
 * workout is never buried. Built entirely on `Flow`s — not a one-shot suspend snapshot — so the
 * card's exercise count stays live as the user plays: Home is a persistent bottom-nav destination
 * whose `ViewModel` (and this Flow's collection) survives navigating into the player and back, so a
 * single fetch on first load would otherwise go stale after the very first save.
 */
class GetActiveWorkoutUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutRepository: WorkoutRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ActiveWorkoutSummary?> =
        workoutSessionRepository.observeActiveSession().flatMapLatest { active ->
            if (active == null) {
                flowOf(null)
            } else {
                workoutRepository.getWorkoutPlanDetail(active.planId).map { detail ->
                    val day = detail?.days?.firstOrNull { it.planDayId == active.planDayId } ?: return@map null

                    // The saved orderIndex points at the exercise the player is ON, not a completed
                    // count. In every other phase that's exactly the number of exercises already
                    // finished (0..index-1 are done, index itself is up next/in progress) — but
                    // during REST it hasn't advanced yet: it still names the exercise that *just*
                    // finished, so one more is done than the raw index says.
                    val completedExercises = when (active.phase) {
                        WorkoutPhase.REST, WorkoutPhase.COMPLETED -> active.orderIndex + 1
                        WorkoutPhase.PREP, WorkoutPhase.EXERCISE -> active.orderIndex
                    }

                    ActiveWorkoutSummary(
                        sessionId = active.sessionId,
                        planId = active.planId,
                        planTitle = detail.plan.title,
                        coverImageUrl = detail.plan.coverImageUrl,
                        dayNumber = day.dayNumber,
                        totalDays = detail.days.size,
                        totalExercises = day.exercises.size,
                        completedExercises = completedExercises.coerceIn(0, day.exercises.size)
                    )
                }
            }
        }
}
