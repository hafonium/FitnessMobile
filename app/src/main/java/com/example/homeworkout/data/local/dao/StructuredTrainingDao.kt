package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}
