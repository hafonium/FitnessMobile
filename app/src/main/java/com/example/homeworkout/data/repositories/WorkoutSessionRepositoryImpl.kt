package com.example.homeworkout.data.repositories

import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WorkoutSessionDao
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.UserSettingsEntity
import com.example.homeworkout.data.local.entities.WorkoutSessionEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.WorkoutSessionSummary
import com.example.homeworkout.domain.models.WorkoutHistoryRecord
import com.example.homeworkout.domain.models.AchievementTotals
import com.example.homeworkout.domain.models.ResumableSession
import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class WorkoutSessionRepositoryImpl(
    private val userDao: UserDao,
    private val workoutSessionDao: WorkoutSessionDao
) : WorkoutSessionRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryRecord>> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            workoutSessionDao.observeCompletedSessions(userId, WorkoutSessionStatus.COMPLETED)
                .map { rows ->
                    rows.map { row ->
                        WorkoutHistoryRecord(
                            sessionId = row.sessionId,
                            planId = row.planId,
                            endedAt = row.endedAt,
                            durationSeconds = row.durationSeconds ?: 0,
                            caloriesBurned = row.caloriesBurned,
                            planTitle = row.planTitle,
                            dayNumber = row.dayNumber,
                            dayTitle = row.dayTitle,
                            coverImageUrl = row.coverImageUrl
                        )
                    }
                }
        }

    /** This app has a single local user; resolve (or lazily create) it by its seeded email. */
    private suspend fun currentUserId(): Long {
        userDao.getUserByEmail(AppDatabaseSeeder.DEFAULT_USER_EMAIL)?.let { return it.userId }
        return userDao.insertUser(UserEntity(email = AppDatabaseSeeder.DEFAULT_USER_EMAIL, passwordHash = "local-only"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCompletedSessionTimestamps(fromMillis: Long, toMillis: Long): Flow<List<Long>> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            workoutSessionDao.observeCompletedSessionEndTimes(userId, WorkoutSessionStatus.COMPLETED, fromMillis, toMillis)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAllCompletedSessionTimestamps(): Flow<List<Long>> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            workoutSessionDao.observeAllCompletedSessionEndTimes(userId, WorkoutSessionStatus.COMPLETED)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAchievementTotals(): Flow<AchievementTotals> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            workoutSessionDao.observeAchievementTotals(userId, WorkoutSessionStatus.COMPLETED).map { row ->
                AchievementTotals(
                    completedSessions = row.completedSessions,
                    totalDurationSeconds = row.totalDurationSeconds,
                    completedPlans = row.completedPlans
                )
            }
        }

    override suspend fun getLatestSessionForPlan(planId: Long): WorkoutSessionSummary? {
        val session = workoutSessionDao.getLatestSessionForPlan(currentUserId(), planId) ?: return null
        return WorkoutSessionSummary(sessionId = session.sessionId, planDayId = session.planDayId, status = session.status)
    }

    override suspend fun createSession(planId: Long, planDayId: Long): Long {
        val userId = currentUserId()
        // Snapshot the settings in effect right now, so a later settings change never rewrites this session's history.
        val settings = userDao.getUserSettings(userId) ?: UserSettingsEntity(userId = userId)
        val session = WorkoutSessionEntity(
            userId = userId,
            planId = planId,
            planDayId = planDayId,
            restTimerSec = settings.restTimerSec,
            prepTimerSec = settings.prepTimerSec,
            soundEnabled = settings.soundEnabled,
            coachVideoEnabled = settings.coachVideoEnabled,
            ttsVoiceType = settings.ttsVoiceType
        )
        return workoutSessionDao.insertSession(session)
    }

    override suspend fun completeSession(sessionId: Long) {
        val session = workoutSessionDao.getSessionById(sessionId) ?: return
        val now = System.currentTimeMillis()
        workoutSessionDao.updateSession(
            session.copy(
                status = WorkoutSessionStatus.COMPLETED,
                endedAt = now,
                durationSeconds = ((now - session.startedAt) / 1000).toInt().coerceAtLeast(0)
            )
        )
    }

    override suspend fun abandonSession(sessionId: Long) {
        val session = workoutSessionDao.getSessionById(sessionId) ?: return
        workoutSessionDao.updateSession(
            session.copy(status = WorkoutSessionStatus.ABANDONED, endedAt = System.currentTimeMillis())
        )
    }

    override suspend fun getResumableSession(planId: Long): ResumableSession? {
        val session = workoutSessionDao.getLatestSessionForPlan(currentUserId(), planId) ?: return null
        return resolveResumable(session)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeActiveSession(): Flow<ResumableSession?> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            workoutSessionDao.observeLatestActiveSession(userId, WorkoutSessionStatus.IN_PROGRESS, WorkoutSessionStatus.PAUSED)
        }.map { session -> session?.let { resolveResumable(it) } }

    /** Shared by [getResumableSession] and [observeActiveSession]: not-actually-active/stale sessions map to null, auto-abandoning stale ones as a side effect. */
    private suspend fun resolveResumable(session: WorkoutSessionEntity): ResumableSession? {
        if (session.status != WorkoutSessionStatus.IN_PROGRESS && session.status != WorkoutSessionStatus.PAUSED) {
            return null
        }
        if (System.currentTimeMillis() - session.startedAt > STALE_SESSION_MS) {
            abandonSession(session.sessionId)
            return null
        }
        return ResumableSession(
            sessionId = session.sessionId,
            planId = session.planId,
            planDayId = session.planDayId,
            phase = session.currentPhase,
            orderIndex = session.currentOrderIndex,
            remainingSec = session.phaseRemainingSec
        )
    }

    override suspend fun saveProgress(sessionId: Long, phase: WorkoutPhase, orderIndex: Int, remainingSec: Int?) {
        workoutSessionDao.updateProgress(sessionId, phase, orderIndex, remainingSec, WorkoutSessionStatus.IN_PROGRESS)
    }

    override suspend fun saveAndExit(sessionId: Long, phase: WorkoutPhase, orderIndex: Int, remainingSec: Int?) {
        workoutSessionDao.updateProgress(sessionId, phase, orderIndex, remainingSec, WorkoutSessionStatus.PAUSED)
    }

    private companion object {
        const val STALE_SESSION_MS = 12L * 60 * 60 * 1000
    }
}
