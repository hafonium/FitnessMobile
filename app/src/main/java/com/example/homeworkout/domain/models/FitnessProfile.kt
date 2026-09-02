package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.ExerciseCategory

/** The onboarding answers used to pick a workout plan — see docs/workout_plan_selection_guide.md §2. */
data class FitnessProfile(
    val primaryGoal: PrimaryGoal,
    val experienceLevel: ExperienceLevel,
    val daysPerWeek: Int,          // 2..6
    val sessionMinutes: Int,       // 15, 20, 30, 45, 60
    val availableEquipment: Set<String>,   // dataset equipment values; "bodyweight" is always implied
    val focusCategories: Set<ExerciseCategory> = emptySet(),
    val focusMuscles: Set<String> = emptySet(),
    val injuriesOrLimitations: String = ""
)

enum class PrimaryGoal(val key: String, val label: String) {
    GENERAL_FITNESS("general_fitness", "General fitness"),
    BUILD_MUSCLE("build_muscle", "Build muscle"),
    FAT_LOSS("fat_loss", "Fat loss"),
    MOBILITY("mobility", "Mobility"),
    FOCUS_AREA("focus_area", "Focus area");

    companion object {
        fun fromKey(key: String?): PrimaryGoal? = entries.firstOrNull { it.key == key }
    }
}

enum class ExperienceLevel(val key: String, val label: String) {
    BEGINNER("beginner", "Beginner"),
    INTERMEDIATE("intermediate", "Intermediate"),
    EXPERT("expert", "Expert");

    companion object {
        fun fromKey(key: String?): ExperienceLevel? = entries.firstOrNull { it.key == key }
    }
}
