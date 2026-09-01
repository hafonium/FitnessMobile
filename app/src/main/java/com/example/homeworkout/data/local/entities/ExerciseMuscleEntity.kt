package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.homeworkout.domain.models.enums.MuscleRole

/**
 * Room table row for `exercise_muscles` — the primaryMuscles/secondaryMuscles join table.
 * data/data.json has exactly one primary muscle and zero to ten secondary muscles per exercise.
 */
@Entity(
    tableName = "exercise_muscles",
    primaryKeys = ["exerciseId", "muscleId", "role"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MuscleEntity::class,
            parentColumns = ["muscleId"],
            childColumns = ["muscleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["muscleId"])]
)
data class ExerciseMuscleEntity(
    val exerciseId: Long,
    val muscleId: Long,
    val role: MuscleRole
)
