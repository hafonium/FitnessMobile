package com.example.homeworkout.domain.usecases.badges

import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.models.UnlockedBadge
import com.example.homeworkout.domain.repositories.BadgeRepository
import kotlinx.coroutines.flow.first

class EvaluateBadgesUseCase(
    private val getBadgesUseCase: GetBadgesUseCase,
    private val badgeRepository: BadgeRepository
) {
    suspend operator fun invoke(triggerSessionId: Long? = null): List<BadgeProgress> {
        val eligible = getBadgesUseCase().first().filter {
            !it.isUnlocked && it.currentValue >= it.definition.targetValue
        }
        if (eligible.isEmpty()) return emptyList()

        val unlockedAt = System.currentTimeMillis()
        val insertedIds = badgeRepository.unlockBadges(
            eligible.map {
                UnlockedBadge(
                    badgeId = it.definition.id,
                    unlockedAt = unlockedAt,
                    triggerSessionId = triggerSessionId,
                    isSeen = false
                )
            }
        )
        return eligible.filter { it.definition.id in insertedIds }.map {
            it.copy(unlockedAt = unlockedAt, isSeen = false)
        }
    }
}
