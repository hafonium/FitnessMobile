package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.StructuredTrainingDao
import com.example.homeworkout.data.local.entities.StructuredProgramProgressEntity
import com.example.homeworkout.data.local.entities.StructuredSessionProgressEntity
import com.example.homeworkout.domain.models.training.StructuredProgramProgress
import com.example.homeworkout.domain.models.training.TrainingEnrollmentStatus
import com.example.homeworkout.domain.repositories.StructuredTrainingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class StructuredTrainingProgressRepositoryImpl(
    private val dao: StructuredTrainingDao
) : StructuredTrainingProgressRepository {
    override fun observeProgress(programId: String): Flow<StructuredProgramProgress> = combine(
        dao.observeProgram(programId),
        dao.observeSessions(programId)
    ) { program, sessions ->
        StructuredProgramProgress(
            programId = programId,
            status = program?.status?.let(TrainingEnrollmentStatus::valueOf) ?: TrainingEnrollmentStatus.NOT_ENROLLED,
            currentWeekNumber = program?.currentWeekNumber ?: 1,
            completedSessions = sessions.filter { it.status == "COMPLETED" }.mapTo(mutableSetOf()) { it.sessionId },
            activeSessionId = program?.activeSessionId
        )
    }

    override suspend fun enroll(programId: String) {
        if (dao.getProgram(programId) != null) return
        dao.upsertProgram(
            StructuredProgramProgressEntity(programId, "ACTIVE", 1, null, System.currentTimeMillis(), null)
        )
    }

    override suspend fun setActiveSession(programId: String, sessionId: String, weekNumber: Int) {
        if (dao.getProgram(programId) == null) enroll(programId)
        val now = System.currentTimeMillis()
        val program = dao.getProgram(programId) ?: return
        dao.upsertSession(
            StructuredSessionProgressEntity(programId, sessionId, weekNumber, "IN_PROGRESS", now, null, null, null)
        )
        dao.upsertProgram(program.copy(status = "ACTIVE", activeSessionId = sessionId))
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
        val existing = dao.getSessions(programId).firstOrNull { it.sessionId == sessionId }
        dao.completeSession(
            session = StructuredSessionProgressEntity(
                programId, sessionId, weekNumber, "COMPLETED", existing?.startedAt ?: now,
                now, durationSeconds, distanceMeters
            ),
            requiredSessionIds = requiredSessionIds,
            nextWeekNumber = nextWeekNumber,
            isLastWeek = isLastWeek,
            now = now
        )
    }

    override suspend fun resetWeek(programId: String, weekNumber: Int) = dao.resetWeek(programId, weekNumber)
}
