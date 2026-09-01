package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel
import com.example.homeworkout.domain.models.enums.WorkoutPlanSource

/**
 * Room table row for `workout_plans` — shown on the Training (home) screen and as the
 * plan-detail "Workout Screen". [ownerUserId] is null for a system plan and required for a
 * custom, user-authored plan. `totalDays`/exercise counts are derived from
 * [WorkoutPlanDayEntity]/[WorkoutPlanExerciseEntity] rather than stored redundantly here.
 */
@Entity(
    tableName = "workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["ownerUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ownerUserId"])]
)
data class WorkoutPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val planId: Long = 0,
    val ownerUserId: Long? = null,
    val title: String,
    val description: String? = null,
    val category: WorkoutCategory,
    val level: WorkoutLevel,
    val source: WorkoutPlanSource = WorkoutPlanSource.SYSTEM,
    val coverImageUrl: String? = null,
    val requiresPremium: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
