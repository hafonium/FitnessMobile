package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room table row for `workout_plan_days` — e.g. "Day 1" of a multi-day [WorkoutPlanEntity]. */
@Entity(
    tableName = "workout_plan_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["planId", "dayNumber"], unique = true)]
)
data class WorkoutPlanDayEntity(
    @PrimaryKey(autoGenerate = true)
    val planDayId: Long = 0,
    val planId: Long,
    val dayNumber: Int,
    val title: String? = null
)
