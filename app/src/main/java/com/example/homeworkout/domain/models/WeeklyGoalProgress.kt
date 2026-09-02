package com.example.homeworkout.domain.models

/** Backs the Home screen's "Weekly Goal" card — the current goal window plus which days are done. */
data class WeeklyGoalProgress(
    val goalDays: Int,
    val completedDays: Int,
    val days: List<WeeklyGoalDay>
)

data class WeeklyGoalDay(
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isCompleted: Boolean
)
