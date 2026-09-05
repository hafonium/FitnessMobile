package com.example.homeworkout.data.local.entities

import androidx.room.Entity

@Entity(tableName = "structured_session_progress", primaryKeys = ["programId", "sessionId"])
data class StructuredSessionProgressEntity(
    val programId: String,
    val sessionId: String,
    val weekNumber: Int,
    val status: String,
    val startedAt: Long?,
    val completedAt: Long?,
    val durationSeconds: Int?,
    val distanceMeters: Double?
)
