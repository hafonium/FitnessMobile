package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.enums.UnitSystemType
import com.example.homeworkout.domain.models.enums.VoiceType
import com.example.homeworkout.domain.models.enums.WeekDay

/**
 * Room table row for `user_settings` — a 1:1 extension of [UserEntity] holding global defaults.
 * Each [com.example.homeworkout.data.local.entities.WorkoutSessionEntity] snapshots the values
 * it was started with, so changing a setting here never rewrites history.
 */
@Entity(
    tableName = "user_settings",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserSettingsEntity(
    @PrimaryKey
    val userId: Long,
    val weeklyGoalDays: Int = 6,
    val firstDayOfWeek: WeekDay = WeekDay.SUNDAY,
    val restTimerSec: Int = 30,
    val prepTimerSec: Int = 15,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 1f,
    val coachVideoEnabled: Boolean = true,
    val unitSystem: UnitSystemType = UnitSystemType.METRIC,
    val ttsVoiceType: VoiceType = VoiceType.MALE_COACH,
    val ttsVoiceName: String? = null,
    val voiceEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String? = null,
    val healthConnectEnabled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
