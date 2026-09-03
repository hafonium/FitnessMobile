package com.example.homeworkout.domain.usecases.badges

import com.example.homeworkout.domain.repositories.BadgeRepository

class MarkBadgesSeenUseCase(private val badgeRepository: BadgeRepository) {
    suspend operator fun invoke(badgeIds: List<String>) {
        if (badgeIds.isNotEmpty()) badgeRepository.markBadgesSeen(badgeIds)
    }
}
