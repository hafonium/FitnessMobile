package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import com.example.homeworkout.domain.models.training.TrainingSessionStatus

@Entity(tableName = "structured_session_progress", primaryKeys = ["programId", "sessionId"])
data class StructuredSessionProgressEntity(
    val programId: String,
    val sessionId: String,
    val weekNumber: Int,
    val status: TrainingSessionStatus,
    val startedAt: Long?,
    val completedAt: Long?,
    val durationSeconds: Int?,
    val distanceMeters: Double?
)
