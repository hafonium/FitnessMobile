package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.BadgeDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.entities.UserBadgeEntity
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.UnlockedBadge
import com.example.homeworkout.domain.repositories.BadgeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class BadgeRepositoryImpl(
    private val userDao: UserDao,
    private val badgeDao: BadgeDao
) : BadgeRepository {

    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(
            UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only")
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnlockedBadges(): Flow<List<UnlockedBadge>> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            badgeDao.observeBadgesForUser(userId).map { badges -> badges.map { it.toDomain() } }
        }

    override suspend fun unlockBadges(badges: List<UnlockedBadge>): Set<String> {
        if (badges.isEmpty()) return emptySet()
        val userId = currentUserId()
        val insertedRows = badgeDao.insertBadges(badges.map { it.toEntity(userId) })
        return badges.zip(insertedRows)
            .filter { (_, rowId) -> rowId != -1L }
            .mapTo(linkedSetOf()) { (badge, _) -> badge.badgeId }
    }

    override suspend fun markBadgesSeen(badgeIds: List<String>) {
        badgeDao.markBadgesSeen(currentUserId(), badgeIds)
    }
}

private fun UserBadgeEntity.toDomain() = UnlockedBadge(
    badgeId = badgeId,
    unlockedAt = unlockedAt,
    triggerSessionId = triggerSessionId,
    isSeen = isSeen
)

private fun UnlockedBadge.toEntity(userId: Long) = UserBadgeEntity(
    userId = userId,
    badgeId = badgeId,
    unlockedAt = unlockedAt,
    triggerSessionId = triggerSessionId,
    isSeen = isSeen
)
