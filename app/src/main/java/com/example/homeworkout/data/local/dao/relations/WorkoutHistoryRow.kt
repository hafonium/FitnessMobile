package com.example.homeworkout.data.local.dao.relations

/** Immutable session projection consumed by the history repository mapper. */
data class WorkoutHistoryRow(
    val sessionId: Long,
    val planTitleSnapshot: String?,
    val planCoverImageSnapshot: String?,
    val planDayNumberSnapshot: Int?,
    val planDayTitleSnapshot: String?,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Int?,
    val caloriesBurned: Double?
)
