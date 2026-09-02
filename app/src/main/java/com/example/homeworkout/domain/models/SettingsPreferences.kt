package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.UnitSystemType
import com.example.homeworkout.domain.models.enums.UserGender
import com.example.homeworkout.domain.models.enums.VoiceType
import com.example.homeworkout.domain.models.enums.WeekDay

/** Unified state for every screen under the Settings tab (main, Workout, General, Voice) plus Edit Goal. */
data class SettingsPreferences(
    val gender: UserGender? = null,
    val weeklyGoalDays: Int = 6,
    val firstDayOfWeek: WeekDay = WeekDay.SUNDAY,
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 1f,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 1f,
    val restTimerSec: Int = 30,
    val prepTimerSec: Int = 15,
    val unitSystem: UnitSystemType = UnitSystemType.METRIC,
    val keepScreenOn: Boolean = true,
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String? = null,
    val ttsVoiceType: VoiceType = VoiceType.MALE_COACH,
    /** The engine voice name when [ttsVoiceType] is [VoiceType.CUSTOM], e.g. "en-us-x-iom-local". */
    val customVoiceName: String? = null
)
