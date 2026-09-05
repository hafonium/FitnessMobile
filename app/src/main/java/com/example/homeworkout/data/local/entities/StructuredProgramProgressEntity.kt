package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "structured_program_progress")
data class StructuredProgramProgressEntity(
    @PrimaryKey val programId: String,
    val status: String,
    val currentWeekNumber: Int,
    val activeSessionId: String?,
    val enrolledAt: Long,
    val completedAt: Long?
)
