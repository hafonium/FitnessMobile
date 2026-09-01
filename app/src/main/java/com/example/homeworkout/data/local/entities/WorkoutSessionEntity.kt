package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.enums.VoiceType
import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus

/**
 * Room table row for `workout_sessions` — backs Resume/Restart, the During Workout player, and
 * completed-workout history. The `restTimerSec`..`ttsVoiceType` fields snapshot the
 * [com.example.homeworkout.data.local.entities.UserSettingsEntity] values in effect when the
 * session started, so a later settings change never rewrites past sessions.
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = WorkoutPlanDayEntity::class,
            parentColumns = ["planDayId"],
            childColumns = ["planDayId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["userId", "startedAt"]),
        Index(value = ["userId", "status"]),
        Index(value = ["planId"]),
        Index(value = ["planDayId"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    val userId: Long,
    val planId: Long,
    val planDayId: Long,
    val status: WorkoutSessionStatus = WorkoutSessionStatus.IN_PROGRESS,
    val currentPhase: WorkoutPhase = WorkoutPhase.PREP,
    val currentOrderIndex: Int = 1,
    val phaseRemainingSec: Int? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val durationSeconds: Int? = null,
    val caloriesBurned: Double? = null,

    // Snapshot of the settings used by this particular session.
    val restTimerSec: Int,
    val prepTimerSec: Int,
    val musicEnabled: Boolean,
    val soundEnabled: Boolean,
    val coachVideoEnabled: Boolean,
    val ttsVoiceType: VoiceType
)
