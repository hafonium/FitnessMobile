package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.homeworkout.data.local.entities.StructuredProgramProgressEntity
import com.example.homeworkout.data.local.entities.StructuredSessionProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StructuredTrainingDao {
    @Query("SELECT * FROM structured_program_progress WHERE programId = :programId")
    fun observeProgram(programId: String): Flow<StructuredProgramProgressEntity?>

    @Query("SELECT * FROM structured_session_progress WHERE programId = :programId")
    fun observeSessions(programId: String): Flow<List<StructuredSessionProgressEntity>>

    @Query("SELECT * FROM structured_program_progress WHERE programId = :programId")
    suspend fun getProgram(programId: String): StructuredProgramProgressEntity?

    @Query("SELECT * FROM structured_session_progress WHERE programId = :programId")
    suspend fun getSessions(programId: String): List<StructuredSessionProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgram(progress: StructuredProgramProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(progress: StructuredSessionProgressEntity)

    @Query("DELETE FROM structured_session_progress WHERE programId = :programId AND weekNumber = :weekNumber")
    suspend fun deleteWeekSessions(programId: String, weekNumber: Int)

    @Transaction
    suspend fun completeSession(
        session: StructuredSessionProgressEntity,
        requiredSessionIds: Set<String>,
        nextWeekNumber: Int?,
        isLastWeek: Boolean,
        now: Long
    ) {
        upsertSession(session)
        val program = getProgram(session.programId) ?: return
        val completed = getSessions(session.programId).filter { it.status == "COMPLETED" }.mapTo(mutableSetOf()) { it.sessionId }
        val weekComplete = completed.containsAll(requiredSessionIds)
        upsertProgram(
            program.copy(
                status = if (weekComplete && isLastWeek) "COMPLETED" else "ACTIVE",
                currentWeekNumber = if (weekComplete) nextWeekNumber ?: program.currentWeekNumber else program.currentWeekNumber,
                activeSessionId = null,
                completedAt = if (weekComplete && isLastWeek) now else null
            )
        )
    }

    @Transaction
    suspend fun resetWeek(programId: String, weekNumber: Int) {
        deleteWeekSessions(programId, weekNumber)
        val program = getProgram(programId) ?: return
        upsertProgram(program.copy(status = "ACTIVE", currentWeekNumber = weekNumber, activeSessionId = null, completedAt = null))
    }
}
