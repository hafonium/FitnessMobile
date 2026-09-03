package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "user_badges",
    primaryKeys = ["userId", "badgeId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserBadgeEntity(
    val userId: Long,
    val badgeId: String,
    val unlockedAt: Long,
    val triggerSessionId: Long? = null,
    val isSeen: Boolean = false
)
