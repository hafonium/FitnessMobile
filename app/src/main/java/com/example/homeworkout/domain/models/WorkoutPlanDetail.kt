package com.example.homeworkout.domain.models

/** Full plan-detail tree for the Workout Screen: a plan, its days, and each day's exercises. */
data class WorkoutPlanDetail(
    val plan: WorkoutModel,
    val days: List<WorkoutPlanDayDetail>
) {
    val totalExercises: Int get() = days.sumOf { it.exercises.size }
}

data class WorkoutPlanDayDetail(
    val planDayId: Long,
    val dayNumber: Int,
    val title: String?,
    val exercises: List<PlanExerciseSummary>
)

/** One row in a plan's exercise list (Workout Screen, Edit Workout Exercise). */
data class PlanExerciseSummary(
    val planExerciseId: Long,
    val exerciseId: Long,
    val title: String,
    val gifUrl: String?,
    val targetReps: Int?,
    val targetDurationSec: Int?
)
