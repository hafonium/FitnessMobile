package com.example.homeworkout.data.repositories

import androidx.room.withTransaction
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.local.dao.StructuredTrainingDao
import com.example.homeworkout.data.local.entities.StructuredProgramProgressEntity
import com.example.homeworkout.data.local.entities.StructuredSessionProgressEntity
import com.example.homeworkout.domain.models.training.StructuredProgramProgress
import com.example.homeworkout.domain.models.training.TrainingEnrollmentStatus
import com.example.homeworkout.domain.models.training.TrainingSessionStatus
import com.example.homeworkout.domain.repositories.StructuredTrainingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class StructuredTrainingProgressRepositoryImpl(
    private val database: AppDatabase,
    private val dao: StructuredTrainingDao
) : StructuredTrainingProgressRepository {
    override fun observeProgress(programId: String): Flow<StructuredProgramProgress> = combine(
        dao.observeProgram(programId),
        dao.observeSessions(programId)
    ) { program, sessions ->
        StructuredProgramProgress(
            programId = programId,
            status = program?.status ?: TrainingEnrollmentStatus.NOT_ENROLLED,
            currentWeekNumber = program?.currentWeekNumber ?: 1,
            completedSessions = sessions.filter { it.status == TrainingSessionStatus.COMPLETED }.mapTo(mutableSetOf()) { it.sessionId },
            activeSessionId = program?.activeSessionId
        )
    }

    override suspend fun enroll(programId: String) {
        if (dao.getProgram(programId) != null) return
        dao.upsertProgram(
            StructuredProgramProgressEntity(programId, TrainingEnrollmentStatus.ACTIVE, 1, null, System.currentTimeMillis(), null)
        )
    }

    override suspend fun setActiveSession(programId: String, sessionId: String, weekNumber: Int) {
        if (dao.getProgram(programId) == null) enroll(programId)
        val now = System.currentTimeMillis()
        val program = dao.getProgram(programId) ?: return
        dao.upsertSession(
            StructuredSessionProgressEntity(programId, sessionId, weekNumber, TrainingSessionStatus.IN_PROGRESS, now, null, null, null)
        )
        dao.upsertProgram(program.copy(status = TrainingEnrollmentStatus.ACTIVE, activeSessionId = sessionId))
    }

    override suspend fun completeSession(
        programId: String,
        sessionId: String,
        weekNumber: Int,
        requiredSessionIds: Set<String>,
        nextWeekNumber: Int?,
        isLastWeek: Boolean,
        durationSeconds: Int?,
        distanceMeters: Double?
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = dao.getSessions(programId).firstOrNull { it.sessionId == sessionId }
            dao.upsertSession(
                StructuredSessionProgressEntity(
                    programId, sessionId, weekNumber, TrainingSessionStatus.COMPLETED, existing?.startedAt ?: now,
                    now, durationSeconds, distanceMeters
                )
            )
            val program = dao.getProgram(programId) ?: return@withTransaction
            val completed = dao.getSessions(programId)
                .filter { it.status == TrainingSessionStatus.COMPLETED }
                .mapTo(mutableSetOf()) { it.sessionId }
            val weekComplete = completed.containsAll(requiredSessionIds)
            dao.upsertProgram(
                program.copy(
                    status = if (weekComplete && isLastWeek) TrainingEnrollmentStatus.COMPLETED else TrainingEnrollmentStatus.ACTIVE,
                    currentWeekNumber = if (weekComplete) nextWeekNumber ?: program.currentWeekNumber else program.currentWeekNumber,
                    activeSessionId = null,
                    completedAt = if (weekComplete && isLastWeek) now else null
                )
            )
        }
    }

    override suspend fun resetWeek(programId: String, weekNumber: Int) {
        database.withTransaction {
            dao.deleteWeekSessions(programId, weekNumber)
            val program = dao.getProgram(programId) ?: return@withTransaction
            dao.upsertProgram(
                program.copy(status = TrainingEnrollmentStatus.ACTIVE, currentWeekNumber = weekNumber, activeSessionId = null, completedAt = null)
            )
        }
    }
}
