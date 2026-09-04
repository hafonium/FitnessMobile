package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long?,
    val activeDurationMillis: Long,
    val runningStartedElapsedRealtimeMillis: Long?,
    val distanceMeters: Double,
    val calories: Double?,
    val weightKg: Double?,
    val status: String,
    val currentSegmentIndex: Int,
    val errorMessage: String?
)
