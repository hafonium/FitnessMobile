package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.WorkoutSessionSummary
import com.example.homeworkout.domain.models.AchievementTotals
import com.example.homeworkout.domain.models.ResumableSession
import com.example.homeworkout.domain.models.WorkoutHistoryRecord
import com.example.homeworkout.domain.models.enums.WorkoutPhase
import kotlinx.coroutines.flow.Flow

interface WorkoutSessionRepository {
    /** Completed sessions, newest first, with their plan/day display metadata. */
    fun observeCompletedSessions(): Flow<List<WorkoutHistoryRecord>>

    /** End timestamps (epoch millis) of completed workout sessions in [fromMillis, toMillis) for the single local user. */
    fun observeCompletedSessionTimestamps(fromMillis: Long, toMillis: Long): Flow<List<Long>>

    /** End timestamps (epoch millis) of every completed workout session ever, for the single local user. */
    fun observeAllCompletedSessionTimestamps(): Flow<List<Long>>

    /** Aggregate values used to display badge progress and evaluate achievement rules. */
    fun observeAchievementTotals(): Flow<AchievementTotals>

    /** The most recently started session (any status) for [planId], or null if it's never been played. */
    suspend fun getLatestSessionForPlan(planId: Long): WorkoutSessionSummary?

    /** Opens a new IN_PROGRESS session for one day of a plan, snapshotting the user's current settings. Returns the new session's id. */
    suspend fun createSession(planId: Long, planDayId: Long): Long

    /** Marks a session COMPLETED (every exercise finished) and stamps its end time/duration. */
    suspend fun completeSession(sessionId: Long)

    /** Marks a session ABANDONED (the user quit before finishing) and stamps its end time. */
    suspend fun abandonSession(sessionId: Long)

    /**
     * The latest IN_PROGRESS/PAUSED session for [planId], or null if there's nothing resumable —
     * either none exists, the latest one is COMPLETED/ABANDONED, or it's older than 12 hours (in
     * which case it's auto-abandoned as a side effect before returning null).
     */
    suspend fun getResumableSession(planId: Long): ResumableSession?

    /**
     * Reactive single most-recent resumable session across every plan, or null if none — same
     * staleness rules as [getResumableSession]. A `Flow` (not a one-shot suspend read) so the Home
     * screen's "Continue" card stays live across saves/resumes without needing to be re-entered.
     */
    fun observeActiveSession(): Flow<ResumableSession?>

    /** Auto-save point during active play — keeps the session IN_PROGRESS. */
    suspend fun saveProgress(sessionId: Long, phase: WorkoutPhase, orderIndex: Int, remainingSec: Int?)

    /** "Save & Exit" from the mid-workout guard dialog — saves progress and marks the session PAUSED. */
    suspend fun saveAndExit(sessionId: Long, phase: WorkoutPhase, orderIndex: Int, remainingSec: Int?)
}
