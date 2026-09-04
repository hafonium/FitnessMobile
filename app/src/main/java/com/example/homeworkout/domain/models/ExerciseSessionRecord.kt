package com.example.homeworkout.domain.models

/** One completed workout-session log entry for a single exercise — backs progression mastery detection. */
data class ExerciseSessionRecord(
    val exerciseId: Long,
    val actualReps: Int?,
    val actualDurationSec: Int?,
    val completedAt: Long?
)
