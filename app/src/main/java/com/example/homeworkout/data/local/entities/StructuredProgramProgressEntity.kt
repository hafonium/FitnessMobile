package com.example.homeworkout.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.homeworkout.domain.models.training.TrainingEnrollmentStatus

@Entity(tableName = "structured_program_progress")
data class StructuredProgramProgressEntity(
    @PrimaryKey val programId: String,
    val status: TrainingEnrollmentStatus,
    val currentWeekNumber: Int,
    val activeSessionId: String?,
    val enrolledAt: Long,
    val completedAt: Long?
)
