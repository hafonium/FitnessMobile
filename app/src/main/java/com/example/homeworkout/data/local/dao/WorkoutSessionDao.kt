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

data class AchievementTotalsRow(
    val completedSessions: Long,
    val totalDurationSeconds: Long,
    val completedPlans: Long
)

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

    /** Most recent session (any status) started for this plan — backs day-by-day "Start" resolution. */
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND planId = :planId ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestSessionForPlan(userId: Long, planId: Long): WorkoutSessionEntity?

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

    /** End timestamps of every completed session, unbounded — backs streak calculation (a streak can span more than one week). */
    @Query(
        "SELECT endedAt FROM workout_sessions WHERE userId = :userId AND status = :status AND endedAt IS NOT NULL ORDER BY endedAt DESC"
    )
    fun observeAllCompletedSessionEndTimes(userId: Long, status: WorkoutSessionStatus): Flow<List<Long>>

    @Query(
        """
        SELECT
            COUNT(*) AS completedSessions,
            COALESCE(SUM(durationSeconds), 0) AS totalDurationSeconds,
            (
                SELECT COUNT(*)
                FROM workout_plans p
                WHERE EXISTS (
                    SELECT 1 FROM workout_plan_days d WHERE d.planId = p.planId
                )
                AND NOT EXISTS (
                    SELECT 1
                    FROM workout_plan_days d
                    WHERE d.planId = p.planId
                    AND NOT EXISTS (
                        SELECT 1
                        FROM workout_sessions completed
                        WHERE completed.userId = :userId
                        AND completed.planDayId = d.planDayId
                        AND completed.status = :status
                    )
                )
            ) AS completedPlans
        FROM workout_sessions
        WHERE userId = :userId AND status = :status
        """
    )
    fun observeAchievementTotals(
        userId: Long,
        status: WorkoutSessionStatus
    ): Flow<AchievementTotalsRow>
}
