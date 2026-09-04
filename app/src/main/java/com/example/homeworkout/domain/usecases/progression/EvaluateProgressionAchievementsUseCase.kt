package com.example.homeworkout.domain.usecases.progression

import com.example.homeworkout.domain.models.UnlockedBadge
import com.example.homeworkout.domain.models.enums.ProgressionBranch
import com.example.homeworkout.domain.models.enums.ProgressionNodeStatus
import com.example.homeworkout.domain.repositories.BadgeRepository
import kotlinx.coroutines.flow.first

/**
 * Achievement hook for the Bodyweight Progression Tree: whenever a branch's tree is loaded, any
 * node that has reached MASTERED and carries a [com.example.homeworkout.domain.models.ProgressionNode.badgeId]
 * gets unlocked (idempotent - `unlockBadges` is insert-or-ignore). The Discovery ViewModel calls
 * this once per branch after collecting a fresh tree so a badge notification can fire the moment
 * a node's real workout history clears its mastery threshold.
 */
class EvaluateProgressionAchievementsUseCase(
    private val getProgressionTreeUseCase: GetProgressionTreeUseCase,
    private val badgeRepository: BadgeRepository
) {
    suspend operator fun invoke(branch: ProgressionBranch): List<String> {
        val tree = getProgressionTreeUseCase(branch).first()
        val newlyEligible = tree.filter { it.status == ProgressionNodeStatus.MASTERED && it.badgeId != null }
        if (newlyEligible.isEmpty()) return emptyList()

        val unlockedAt = System.currentTimeMillis()
        return badgeRepository.unlockBadges(
            newlyEligible.map { node ->
                UnlockedBadge(
                    badgeId = requireNotNull(node.badgeId),
                    unlockedAt = unlockedAt,
                    triggerSessionId = null,
                    isSeen = false
                )
            }
        ).toList()
    }
}
