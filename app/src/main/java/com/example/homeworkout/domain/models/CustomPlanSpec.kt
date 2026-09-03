package com.example.homeworkout.domain.models

/**
 * One exercise slot in a plan being authored via
 * [com.example.homeworkout.domain.repositories.WorkoutRepository.createCustomPlan], before it has
 * a real planDayId/orderIndex — those are assigned once the day itself is written.
 */
data class CustomExerciseSpec(
    val exerciseId: Long,
    val targetReps: Int?,
    val targetDurationSec: Int?,
    val restAfterSec: Int? = 15
)

/**
 * One day of a plan being authored via
 * [com.example.homeworkout.domain.repositories.WorkoutRepository.createCustomPlan].
 */
data class CustomDaySpec(
    val title: String?,
    val exercises: List<CustomExerciseSpec>
)
