package com.example.homeworkout.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "run_sessions", indices = [Index(value = ["status", "startedAt"])])
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
    val errorMessage: String?,
    val encodedPolyline: String?,
    @ColumnInfo(defaultValue = "'RUNNING'") val activityType: String,
    val title: String?,
    val programId: String?,
    val trainingSessionId: String?
)
