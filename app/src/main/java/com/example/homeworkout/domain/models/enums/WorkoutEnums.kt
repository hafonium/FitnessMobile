package com.example.homeworkout.domain.models.enums

/** Mirrors the `workout_category` enum in docs/db_diagram.dbml. */
enum class WorkoutCategory {
    BUILD_MUSCLE, BURN_FAT, KEEP_FIT, STRETCH_AND_WARM_UP
}

/** Mirrors the `workout_level` enum in docs/db_diagram.dbml. */
enum class WorkoutLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

/** Mirrors the `workout_plan_source` enum in docs/db_diagram.dbml. */
enum class WorkoutPlanSource {
    SYSTEM, CUSTOM
}

/** Mirrors the `workout_session_status` enum in docs/db_diagram.dbml. */
enum class WorkoutSessionStatus {
    IN_PROGRESS, PAUSED, COMPLETED, ABANDONED
}

/** Mirrors the `workout_phase` enum in docs/db_diagram.dbml. */
enum class WorkoutPhase {
    PREP, EXERCISE, REST, COMPLETED
}

/** Mirrors the `session_exercise_status` enum in docs/db_diagram.dbml. */
enum class SessionExerciseStatus {
    PENDING, ACTIVE, COMPLETED, SKIPPED
}
