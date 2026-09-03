package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.homeworkout.data.local.entities.UserBadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Query("SELECT * FROM user_badges WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun observeBadgesForUser(userId: Long): Flow<List<UserBadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadges(badges: List<UserBadgeEntity>): List<Long>

    @Query("UPDATE user_badges SET isSeen = 1 WHERE userId = :userId AND badgeId IN (:badgeIds)")
    suspend fun markBadgesSeen(userId: Long, badgeIds: List<String>)

    @Query("DELETE FROM user_badges WHERE userId = :userId")
    suspend fun deleteAllBadgesForUser(userId: Long)
}
