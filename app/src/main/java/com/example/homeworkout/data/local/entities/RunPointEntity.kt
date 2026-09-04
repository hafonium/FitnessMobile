package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "run_points",
    foreignKeys = [ForeignKey(
        entity = RunSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId"), Index(value = ["sessionId", "sequence"], unique = true)]
)
data class RunPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val elapsedRealtimeNanos: Long,
    val sequence: Int,
    val segmentIndex: Int
)
