package com.example.homeworkout.data.repositories

import androidx.room.withTransaction
import com.example.homeworkout.data.local.AppDatabase
import com.example.homeworkout.data.local.dao.UserDao
import com.example.homeworkout.data.local.dao.WorkoutPlanDao
import com.example.homeworkout.data.local.dao.WorkoutSessionDao
import com.example.homeworkout.data.local.entities.UserEntity
import com.example.homeworkout.data.local.entities.UserSettingsEntity
import com.example.homeworkout.data.local.entities.WorkoutSessionEntity
import com.example.homeworkout.data.local.entities.WorkoutSessionExerciseEntity
import com.example.homeworkout.data.local.seed.AppDatabaseSeeder
import com.example.homeworkout.domain.models.WorkoutHistoryEntry
import com.example.homeworkout.domain.models.WorkoutSessionSummary
import com.example.homeworkout.domain.models.AchievementTotals
import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class WorkoutSessionRepositoryImpl(
    private val database: AppDatabase,
    private val userDao: UserDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val workoutSessionDao: WorkoutSessionDao
) : WorkoutSessionRepository {

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCompletedHistory(
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<WorkoutHistoryEntry>> =
        flow { emit(currentUserId()) }.flatMapLatest { userId ->
            workoutSessionDao.observeCompletedHistory(
                userId = userId,
                status = WorkoutSessionStatus.COMPLETED,
                fromMillis = fromMillis,
                toMillis = toMillis
            ).map { rows ->
                rows.map { row ->
                    val planTitle = row.planTitleSnapshot?.takeIf { it.isNotBlank() }
                    val title = row.planDayTitleSnapshot?.takeIf { it.isNotBlank() }
                        ?: if (row.planDayNumberSnapshot != null && planTitle != null) {
                            "Day ${row.planDayNumberSnapshot} – $planTitle"
                        } else {
                            planTitle ?: "Workout"
                        }
                    WorkoutHistoryEntry(
                        sessionId = row.sessionId,
                        title = title,
                        imageUrl = row.planCoverImageSnapshot,
                        startedAt = row.startedAt,
                        completedAt = row.endedAt,
                        durationSeconds = row.durationSeconds,
                        caloriesBurned = row.caloriesBurned
                    )
                }
            }
        }

    override suspend fun getLatestSessionForPlan(planId: Long): WorkoutSessionSummary? {
        val session = workoutSessionDao.getLatestSessionForPlan(currentUserId(), planId) ?: return null
        return WorkoutSessionSummary(sessionId = session.sessionId, planDayId = session.planDayId, status = session.status)
    }

    override suspend fun createSession(planId: Long, planDayId: Long): Long {
        val userId = currentUserId()
        return database.withTransaction {
            val settings = userDao.getUserSettings(userId) ?: UserSettingsEntity(userId = userId)
            val plan = requireNotNull(
                workoutPlanDao.getSessionPlanSnapshotSource(planId, planDayId)
            ) { "Plan day $planDayId does not belong to plan $planId" }
            val exercises = workoutPlanDao.getSessionExerciseSnapshotSources(planDayId)
            require(exercises.isNotEmpty()) { "Cannot start an empty workout day" }

            val sessionId = workoutSessionDao.insertSession(
                WorkoutSessionEntity(
                    userId = userId,
                    planId = planId,
                    planDayId = planDayId,
                    planTitleSnapshot = plan.planTitle,
                    planCoverImageSnapshot = plan.planCoverImageUrl,
                    planDayNumberSnapshot = plan.dayNumber,
                    planDayTitleSnapshot = plan.dayTitle,
                    restTimerSec = settings.restTimerSec,
                    prepTimerSec = settings.prepTimerSec,
                    musicEnabled = settings.musicEnabled,
                    soundEnabled = settings.soundEnabled,
                    coachVideoEnabled = settings.coachVideoEnabled,
                    ttsVoiceType = settings.ttsVoiceType
                )
            )
            workoutSessionDao.insertSessionExercises(
                exercises.map { exercise ->
                    WorkoutSessionExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = exercise.exerciseId,
                        orderIndex = exercise.orderIndex,
                        exerciseTitleSnapshot = exercise.exerciseTitle,
                        plannedReps = exercise.targetReps,
                        plannedDurationSec = exercise.targetDurationSec
                    )
                }
            )
            sessionId
        }
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
}
