package com.example.homeworkout.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.homeworkout.data.local.entities.WorkoutSessionEntity
import com.example.homeworkout.data.local.entities.WorkoutSessionExerciseEntity
import com.example.homeworkout.domain.models.enums.WorkoutPhase
import com.example.homeworkout.domain.models.enums.WorkoutSessionStatus
import kotlinx.coroutines.flow.Flow

data class AchievementTotalsRow(
    val completedSessions: Long,
    val totalDurationSeconds: Long,
    val completedPlans: Long
)

data class ExerciseHistoryRow(
    val exerciseId: Long,
    val actualReps: Int?,
    val actualDurationSec: Int?,
    val completedAt: Long?
)

data class WorkoutHistoryRow(
    val sessionId: Long,
    val planId: Long,
    val endedAt: Long,
    val durationSeconds: Int?,
    val caloriesBurned: Double?,
    val planTitle: String,
    val dayNumber: Int,
    val dayTitle: String?,
    val coverImageUrl: String?
)

@Dao
interface WorkoutSessionDao {
    @Query(
        """
        SELECT
            s.sessionId,
            s.planId,
            s.endedAt,
            s.durationSeconds,
            s.caloriesBurned,
            p.title AS planTitle,
            d.dayNumber,
            d.title AS dayTitle,
            p.coverImageUrl
        FROM workout_sessions s
        INNER JOIN workout_plans p ON p.planId = s.planId
        INNER JOIN workout_plan_days d ON d.planDayId = s.planDayId
        WHERE s.userId = :userId
          AND s.status = :status
          AND s.endedAt IS NOT NULL
        ORDER BY s.endedAt DESC
        """
    )
    fun observeCompletedSessions(
        userId: Long,
        status: WorkoutSessionStatus
    ): Flow<List<WorkoutHistoryRow>>

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

    /**
     * Reactive most-recent in-progress/paused session across every plan — backs the Home screen's
     * "Continue" card. A `Flow` (not a one-shot suspend read) so Room's invalidation tracker
     * re-emits automatically on every [updateProgress]/abandon/complete write, keeping the card's
     * exercise count live without depending on the screen being re-entered.
     */
    @Query(
        "SELECT * FROM workout_sessions WHERE userId = :userId AND (status = :inProgress OR status = :paused) " +
            "ORDER BY startedAt DESC LIMIT 1"
    )
    fun observeLatestActiveSession(
        userId: Long,
        inProgress: WorkoutSessionStatus,
        paused: WorkoutSessionStatus
    ): Flow<WorkoutSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionExercises(exercises: List<WorkoutSessionExerciseEntity>)

    @Update
    suspend fun updateSessionExercise(exercise: WorkoutSessionExerciseEntity)

    @Query("SELECT * FROM workout_session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex")
    fun observeSessionExercises(sessionId: Long): Flow<List<WorkoutSessionExerciseEntity>>

    /** Backs "Restart progress" — cascades to workout_session_exercises via FK. */
    @Query("DELETE FROM workout_sessions WHERE userId = :userId")
    suspend fun deleteAllSessionsForUser(userId: Long)

    /** Completed-session history for a set of exercises — backs progression/skill-tree mastery detection. */
    @Query(
        """
        SELECT wse.exerciseId, wse.actualReps, wse.actualDurationSec, wse.completedAt
        FROM workout_session_exercises wse
        INNER JOIN workout_sessions ws ON ws.sessionId = wse.sessionId
        WHERE ws.userId = :userId AND ws.status = :status AND wse.exerciseId IN (:exerciseIds)
        """
    )
    fun observeExerciseHistory(
        userId: Long,
        status: WorkoutSessionStatus,
        exerciseIds: List<Long>
    ): Flow<List<ExerciseHistoryRow>>

    /**
     * `AND status IN (IN_PROGRESS, PAUSED)` guards against a stray/late auto-save reviving a
     * session that already finished: [com.example.homeworkout.data.repositories.WorkoutSessionRepositoryImpl.completeSession]
     * / `abandonSession` write a terminal status, and if a still-in-flight `updateProgress` from
     * just before that lands afterward, it must not silently flip the row back to "active".
     */
    @Query(
        """
        UPDATE workout_sessions
        SET currentPhase = :phase, currentOrderIndex = :orderIndex, phaseRemainingSec = :remainingSec, status = :status
        WHERE sessionId = :sessionId AND status IN ('IN_PROGRESS', 'PAUSED')
        """
    )
    suspend fun updateProgress(
        sessionId: Long,
        phase: WorkoutPhase,
        orderIndex: Int,
        remainingSec: Int?,
        status: WorkoutSessionStatus
    )

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
