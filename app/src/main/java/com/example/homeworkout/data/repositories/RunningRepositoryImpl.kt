package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.RunningDao
import com.example.homeworkout.data.local.dao.WeightLogDao
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.entities.RunPointEntity
import com.example.homeworkout.data.local.entities.RunSessionEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.running.RunPoint
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.models.running.RunStatus
import com.example.homeworkout.domain.repositories.RunningRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RunningRepositoryImpl(
    private val runningDao: RunningDao,
    private val weightLogDao: WeightLogDao,
    private val userDao: UserDao
) : RunningRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeLatestSession(): Flow<RunSession?> = runningDao.observeLatestSession().flatMapLatest { session ->
        if (session == null) flowOf(null)
        else runningDao.observePoints(session.id).map { points -> session.toDomain(points) }
    }

    override suspend fun getRecoverableSession(): RunSession? = runningDao.getRecoverableSession()?.let { session ->
        session.toDomain(runningDao.getPoints(session.id))
    }

    override suspend fun createSession(startedAt: Long, elapsedRealtimeMillis: Long): RunSession {
        getRecoverableSession()?.let { return it }
        val entity = RunSessionEntity(
            startedAt = startedAt,
            finishedAt = null,
            activeDurationMillis = 0,
            runningStartedElapsedRealtimeMillis = elapsedRealtimeMillis,
            distanceMeters = 0.0,
            calories = null,
            weightKg = userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)
                ?.let { weightLogDao.getLatestWeightLog(it.userId) }
                ?.weightKg,
            status = RunStatus.RUNNING.name,
            currentSegmentIndex = 0,
            errorMessage = null
        )
        val id = runningDao.insertSession(entity)
        return entity.copy(id = id).toDomain(emptyList())
    }

    override suspend fun appendPoint(
        point: RunPoint,
        distanceMeters: Double,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long,
        calories: Double?
    ) {
        runningDao.appendPointAndProgress(
            point.toEntity(), distanceMeters, activeDurationMillis, runningStartedElapsedRealtimeMillis, calories
        )
    }

    override suspend fun updateState(
        id: Long,
        status: RunStatus,
        activeDurationMillis: Long,
        runningStartedElapsedRealtimeMillis: Long?,
        segmentIndex: Int,
        finishedAt: Long?,
        errorMessage: String?
    ) {
        val current = runningDao.getSession(id) ?: return
        runningDao.updateSession(current.copy(
            status = status.name,
            activeDurationMillis = activeDurationMillis,
            runningStartedElapsedRealtimeMillis = runningStartedElapsedRealtimeMillis,
            currentSegmentIndex = segmentIndex,
            finishedAt = finishedAt,
            errorMessage = errorMessage
        ))
    }

    private fun RunSessionEntity.toDomain(points: List<RunPointEntity>) = RunSession(
        id, startedAt, finishedAt, activeDurationMillis, runningStartedElapsedRealtimeMillis,
        distanceMeters, calories, weightKg, RunStatus.valueOf(status), currentSegmentIndex,
        errorMessage, points.map { it.toDomain() }
    )

    private fun RunPointEntity.toDomain() = RunPoint(
        id, sessionId, latitude, longitude, altitudeMeters, accuracyMeters, speedMps,
        elapsedRealtimeNanos, sequence, segmentIndex
    )

    private fun RunPoint.toEntity() = RunPointEntity(
        id, sessionId, latitude, longitude, altitudeMeters, accuracyMeters, speedMps,
        elapsedRealtimeNanos, sequence, segmentIndex
    )
}
