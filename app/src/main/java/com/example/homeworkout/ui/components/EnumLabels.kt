package com.example.homeworkout.ui.components

import com.example.homeworkout.domain.models.enums.ExerciseCategory
import com.example.homeworkout.domain.models.enums.ExerciseLevel
import com.example.homeworkout.domain.models.enums.WorkoutCategory
import com.example.homeworkout.domain.models.enums.WorkoutLevel

/** Human-readable labels for the domain enums, e.g. BUILD_MUSCLE -> "Build Muscle". */
fun WorkoutCategory.label(): String = when (this) {
    WorkoutCategory.BUILD_MUSCLE -> "Build Muscle"
    WorkoutCategory.BURN_FAT -> "Burn Fat"
    WorkoutCategory.KEEP_FIT -> "Keep Fit"
    WorkoutCategory.STRETCH_AND_WARM_UP -> "Stretch & Warm Up"
}

fun WorkoutLevel.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

fun ExerciseCategory.label(): String = when (this) {
    ExerciseCategory.ABS_CORE -> "Abs & Core"
    ExerciseCategory.ARMS_SHOULDERS -> "Arms & Shoulders"
    ExerciseCategory.BACK_PULL -> "Back"
    ExerciseCategory.CARDIO_HIIT -> "Cardio / HIIT"
    ExerciseCategory.CHEST_PUSH -> "Chest"
    ExerciseCategory.GENERAL_FITNESS -> "General Fitness"
    ExerciseCategory.LEGS_GLUTES -> "Legs & Glutes"
    ExerciseCategory.STRETCHING -> "Stretching"
}

fun ExerciseLevel.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }
