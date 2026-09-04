package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.running.RunPoint
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunStatus
import kotlinx.coroutines.flow.Flow

interface RunningRepository {
    fun observeLatestSession(): Flow<RunSession?>
    suspend fun getRecoverableSession(): RunSession?
    suspend fun createSession(startedAt: Long, elapsedRealtimeMillis: Long): RunSession
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
        errorMessage: String? = null
    )
}
