package com.example.homeworkout.domain.models

/** Consecutive-calendar-day workout streaks, computed from completed session days.
 * See [com.example.homeworkout.domain.usecases.report.GetStreakUseCase]. */
data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int
)
