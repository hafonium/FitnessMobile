package com.example.homeworkout.domain.models

/** The single most-recent in-progress/paused workout, across every plan — backs the Home screen's "Continue" card. */
data class ActiveWorkoutSummary(
    val sessionId: Long,
    val planId: Long,
    val planTitle: String,
    val coverImageUrl: String?,
    val dayNumber: Int,
    val totalDays: Int,
    val totalExercises: Int,
    val completedExercises: Int
)
