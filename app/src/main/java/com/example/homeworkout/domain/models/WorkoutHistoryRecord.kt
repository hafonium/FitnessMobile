package com.example.homeworkout.domain.models

/** A completed workout session enriched with the plan/day labels needed by History. */
data class WorkoutHistoryRecord(
    val sessionId: Long,
    val endedAt: Long,
    val durationSeconds: Int,
    val caloriesBurned: Double?,
    val planTitle: String,
    val dayNumber: Int,
    val dayTitle: String?,
    val coverImageUrl: String?
)
