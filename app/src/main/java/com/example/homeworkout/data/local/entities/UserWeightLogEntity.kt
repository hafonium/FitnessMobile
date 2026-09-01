package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room table row for `user_weight_logs`. BMI is calculated from [weightKg] and
 * [heightCmSnapshot] on read instead of being stored redundantly.
 */
@Entity(
    tableName = "user_weight_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "loggedAt"], unique = true)]
)
data class UserWeightLogEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,
    val userId: Long,
    val weightKg: Double,
    val heightCmSnapshot: Double,
    val loggedAt: Long = System.currentTimeMillis()
)
