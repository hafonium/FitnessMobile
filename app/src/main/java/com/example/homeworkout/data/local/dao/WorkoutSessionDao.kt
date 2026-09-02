package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.homeworkout.data.local.entities.WorkoutSessionEntity
import com.example.homeworkout.data.local.entities.WorkoutSessionExerciseEntity
import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startedAt DESC")
    fun observeSessionsForUser(userId: Long): Flow<List<WorkoutSessionEntity>>

    @Query(
        "SELECT * FROM workout_sessions WHERE userId = :userId AND status = :status ORDER BY startedAt DESC LIMIT 1"
    )
    suspend fun getLatestSessionByStatus(userId: Long, status: WorkoutSessionStatus): WorkoutSessionEntity?

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE userId = :userId AND status = :status")
    suspend fun countSessionsByStatus(userId: Long, status: WorkoutSessionStatus): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionExercises(exercises: List<WorkoutSessionExerciseEntity>)

    @Update
    suspend fun updateSessionExercise(exercise: WorkoutSessionExerciseEntity)

    @Query("SELECT * FROM workout_session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex")
    fun observeSessionExercises(sessionId: Long): Flow<List<WorkoutSessionExerciseEntity>>

    /** Backs "Restart progress" — cascades to workout_session_exercises via FK. */
    @Query("DELETE FROM workout_sessions WHERE userId = :userId")
    suspend fun deleteAllSessionsForUser(userId: Long)

    /** End timestamps of completed sessions in [fromMillis, toMillis) — backs the Home weekly-goal day tracker. */
    @Query(
        "SELECT endedAt FROM workout_sessions WHERE userId = :userId AND status = :status " +
            "AND endedAt IS NOT NULL AND endedAt >= :fromMillis AND endedAt < :toMillis"
    )
    fun observeCompletedSessionEndTimes(
        userId: Long,
        status: WorkoutSessionStatus,
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<Long>>
}
