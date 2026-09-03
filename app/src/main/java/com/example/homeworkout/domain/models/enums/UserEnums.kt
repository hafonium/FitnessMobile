package com.example.homeworkout.domain.models.enums

/** Mirrors the `user_gender` enum in docs/db_diagram.dbml. */
enum class UserGender {
    MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
}

/** Mirrors the `week_day` enum in docs/db_diagram.dbml. */
enum class WeekDay {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
}

/** Mirrors the `unit_system_type` enum in docs/db_diagram.dbml. */
enum class UnitSystemType {
    METRIC, IMPERIAL
}

/** Mirrors the `voice_type` enum in docs/db_diagram.dbml. */
enum class VoiceType {
    MALE_COACH, FEMALE_COACH, DEVICE_TTS,

    /** A specific engine voice picked by name from the Voice Options browser; see [com.example.homeworkout.domain.models.SettingsPreferences.customVoiceName]. */
    CUSTOM
}
