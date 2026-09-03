package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.UnlockedBadge
import kotlinx.coroutines.flow.Flow

interface BadgeRepository {
    fun observeUnlockedBadges(): Flow<List<UnlockedBadge>>

    suspend fun unlockBadges(badges: List<UnlockedBadge>): Set<String>

    suspend fun markBadgesSeen(badgeIds: List<String>)
}
