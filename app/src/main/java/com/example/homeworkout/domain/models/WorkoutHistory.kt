package com.example.homeworkout.domain.models

data class WorkoutHistoryEntry(
    val sessionId: Long,
    val title: String,
    val imageUrl: String?,
    val startedAt: Long,
    val completedAt: Long,
    val durationSeconds: Int?,
    val caloriesBurned: Double?
)

data class WorkoutHistorySummary(
    val workoutCount: Int = 0,
    val totalDurationSeconds: Long = 0,
    val totalCaloriesBurned: Double? = null
)

data class WorkoutHistoryPeriod(
    val sessions: List<WorkoutHistoryEntry>,
    val workoutDayStarts: Set<Long>,
    val weeklySessions: List<WorkoutHistoryEntry>,
    val weeklySummary: WorkoutHistorySummary
)
