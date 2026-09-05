package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.running.RunPoint
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunSessionMetadata
import com.example.homeworkout.domain.models.running.RunStatus
import kotlinx.coroutines.flow.Flow

interface RunningRepository {
    fun observeLatestSession(): Flow<RunSession?>
    fun observeFinishedSessions(): Flow<List<RunSession>>
    suspend fun getSession(id: Long): RunSession?
    suspend fun getRecoverableSession(): RunSession?
    suspend fun createSession(
        startedAt: Long,
        elapsedRealtimeMillis: Long,
        metadata: RunSessionMetadata = RunSessionMetadata()
    ): RunSession
    suspend fun appendPoint(
        point: RunPoint,
        distanceMeters: Double,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long,
        calories: Double?
    )
    suspend fun updateState(
        id: Long,
        status: RunStatus,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long?,
        segmentIndex: Int,
        finishedAt: Long? = null,
        errorMessage: String? = null,
        encodedPolyline: String? = null
    )
    suspend fun deleteSession(id: Long)
}
