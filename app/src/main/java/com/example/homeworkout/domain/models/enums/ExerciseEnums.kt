package com.example.homeworkout.domain.models.enums

/** Mirrors the `exercise_category` enum in docs/db_diagram.dbml. */
enum class ExerciseCategory {
    ABS_CORE, ARMS_SHOULDERS, BACK_PULL, CARDIO_HIIT, CHEST_PUSH, GENERAL_FITNESS, LEGS_GLUTES, STRETCHING
}

/** Mirrors the `exercise_level` enum in docs/db_diagram.dbml. */
enum class ExerciseLevel {
    BEGINNER, INTERMEDIATE, EXPERT
}

/** Mirrors the `exercise_force` enum in docs/db_diagram.dbml. */
enum class ExerciseForce {
    PULL, PUSH, STATIC
}

/** Mirrors the `muscle_role` enum in docs/db_diagram.dbml. */
enum class MuscleRole {
    PRIMARY, SECONDARY
}
