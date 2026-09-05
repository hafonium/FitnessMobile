package com.example.homeworkout.domain.models

import com.example.homeworkout.domain.models.enums.WorkoutPhase

/** The exact spot an in-progress/paused `workout_sessions` row was left at, for Resume. */
data class ResumableSession(
    val sessionId: Long,
    val planId: Long,
    val planDayId: Long,
    val phase: WorkoutPhase,
    val orderIndex: Int,
    val remainingSec: Int?
)
