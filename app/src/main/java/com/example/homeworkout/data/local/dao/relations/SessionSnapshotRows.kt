package com.example.homeworkout.data.local.dao.relations

/** Plan/day metadata copied into a session when playback starts. */
data class SessionPlanSnapshotRow(
    val planId: Long,
    val planTitle: String,
    val planCoverImageUrl: String?,
    val planDayId: Long,
    val dayNumber: Int,
    val dayTitle: String?
)

/** Exercise data copied into workout_session_exercises when playback starts. */
data class SessionExerciseSnapshotSourceRow(
    val exerciseId: Long,
    val orderIndex: Int,
    val exerciseTitle: String,
    val targetReps: Int?,
    val targetDurationSec: Int?
)
