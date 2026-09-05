package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.training.StructuredProgramProgress
import com.example.homeworkout.domain.models.training.StructuredTrainingProgram
import kotlinx.coroutines.flow.Flow

interface StructuredTrainingCatalogRepository {
    suspend fun getProgram(programId: String): StructuredTrainingProgram?
}

interface StructuredTrainingProgressRepository {
    fun observeProgress(programId: String): Flow<StructuredProgramProgress>
    suspend fun enroll(programId: String)
    suspend fun setActiveSession(programId: String, sessionId: String, weekNumber: Int)
    suspend fun completeSession(
        programId: String,
        sessionId: String,
        weekNumber: Int,
        requiredSessionIds: Set<String>,
        nextWeekNumber: Int?,
        isLastWeek: Boolean,
        durationSeconds: Int?,
        distanceMeters: Double?
    )
    suspend fun resetWeek(programId: String, weekNumber: Int)
}
