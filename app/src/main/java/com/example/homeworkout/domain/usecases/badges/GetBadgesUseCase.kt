package com.example.homeworkout.domain.usecases.badges

import com.example.homeworkout.domain.models.BadgeCatalog
import com.example.homeworkout.domain.models.BadgeMetric
import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.repositories.BadgeRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import com.example.homeworkout.domain.usecases.report.GetStreakUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetBadgesUseCase(
    private val badgeRepository: BadgeRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val getStreakUseCase: GetStreakUseCase
) {
    operator fun invoke(): Flow<List<BadgeProgress>> = combine(
        badgeRepository.observeUnlockedBadges(),
        workoutSessionRepository.observeAchievementTotals(),
        getStreakUseCase()
    ) { unlockedBadges, totals, streak ->
        val unlockedById = unlockedBadges.associateBy { it.badgeId }
        BadgeCatalog.all.map { definition ->
            val unlocked = unlockedById[definition.id]
            val currentValue = when (definition.metric) {
                BadgeMetric.COMPLETED_SESSIONS -> totals.completedSessions
                BadgeMetric.BEST_STREAK_DAYS -> streak.bestStreak.toLong()
                BadgeMetric.TOTAL_DURATION_SECONDS -> totals.totalDurationSeconds
                BadgeMetric.COMPLETED_PLANS -> totals.completedPlans
            }
            BadgeProgress(
                definition = definition,
                currentValue = currentValue.coerceAtMost(definition.targetValue),
                unlockedAt = unlocked?.unlockedAt,
                isSeen = unlocked?.isSeen ?: true
            )
        }
    }
}
