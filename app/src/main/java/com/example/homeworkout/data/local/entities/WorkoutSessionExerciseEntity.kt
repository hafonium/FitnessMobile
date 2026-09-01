package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.enums.SessionExerciseStatus

/**
 * Room table row for `workout_session_exercises` — one per exercise performed in a
 * [WorkoutSessionEntity]. [exerciseTitleSnapshot] (and the other snapshot fields) preserve
 * history when the underlying plan or exercise is edited later.
 */
@Entity(
    tableName = "workout_session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = com.example.homeworkout.data.local.entities.ExerciseEntity::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sessionId", "orderIndex"], unique = true),
        Index(value = ["exerciseId"])
    ]
)
data class WorkoutSessionExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionExerciseId: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val exerciseTitleSnapshot: String,
    val plannedReps: Int? = null,
    val plannedDurationSec: Int? = null,
    val actualReps: Int? = null,
    val actualDurationSec: Int? = null,
    val status: SessionExerciseStatus = SessionExerciseStatus.PENDING,
    val startedAt: Long? = null,
    val completedAt: Long? = null
)
