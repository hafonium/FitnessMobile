package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus

/** Minimal view of the most recent `workout_sessions` row for a plan — just enough for
 * [com.example.homeworkout.domain.usecases.player.StartWorkoutSessionUseCase] to decide which day
 * to play next. */
data class WorkoutSessionSummary(
    val sessionId: Long,
    val planDayId: Long,
    val status: WorkoutSessionStatus
)
