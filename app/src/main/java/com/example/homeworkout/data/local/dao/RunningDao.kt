package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.homeworkout.data.local.entities.RunPointEntity
import com.example.homeworkout.data.local.entities.RunSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunningDao {
    @Query("SELECT * FROM run_sessions ORDER BY id DESC LIMIT 1")
    fun observeLatestSession(): Flow<RunSessionEntity?>

    @Query("SELECT * FROM run_sessions WHERE status IN ('RUNNING', 'PAUSED') ORDER BY id DESC LIMIT 1")
    suspend fun getRecoverableSession(): RunSessionEntity?

    @Query("SELECT * FROM run_points WHERE sessionId = :sessionId ORDER BY sequence")
    fun observePoints(sessionId: Long): Flow<List<RunPointEntity>>

    @Query("SELECT * FROM run_points WHERE sessionId = :sessionId ORDER BY sequence")
    suspend fun getPoints(sessionId: Long): List<RunPointEntity>

    @Insert suspend fun insertSession(session: RunSessionEntity): Long
    @Insert suspend fun insertPoint(point: RunPointEntity): Long
    @Update suspend fun updateSession(session: RunSessionEntity)

    @Query("SELECT * FROM run_sessions WHERE id = :id")
    suspend fun getSession(id: Long): RunSessionEntity?

    @Transaction
    suspend fun appendPointAndProgress(
        point: RunPointEntity,
        distanceMeters: Double,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long,
        calories: Double?
    ) {
        insertPoint(point)
        val session = getSession(point.sessionId) ?: return
        updateSession(session.copy(
            distanceMeters = distanceMeters,
            activeDurationMillis = activeDurationMillis,
            runningStartedElapsedRealtimeMillis = runningStartedElapsedRealtimeMillis,
            calories = calories
        ))
    }
}
