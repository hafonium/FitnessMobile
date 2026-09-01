package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room table row for `workout_plan_exercises` — one ordered exercise slot within a
 * [WorkoutPlanDayEntity]. At least one of [targetReps]/[targetDurationSec] must be provided.
 */
@Entity(
    tableName = "workout_plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanDayEntity::class,
            parentColumns = ["planDayId"],
            childColumns = ["planDayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["planDayId", "orderIndex"], unique = true),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutPlanExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val planExerciseId: Long = 0,
    val planDayId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetReps: Int? = null,
    val targetDurationSec: Int? = null,
    val restAfterSec: Int? = null
)
