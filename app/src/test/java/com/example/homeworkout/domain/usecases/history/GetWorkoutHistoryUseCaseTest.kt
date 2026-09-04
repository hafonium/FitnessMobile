package com.example.homeworkout.domain.usecases.history

import com.example.homeworkout.domain.models.AchievementTotals
import com.example.homeworkout.domain.models.WorkoutHistoryEntry
import com.example.homeworkout.domain.models.WorkoutSessionSummary
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetWorkoutHistoryUseCaseTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun groupsDaysAndCalculatesOnlyTheSelectedWeek() = runBlocking {
        val monday = timestamp(2026, Calendar.SEPTEMBER, 7, 10)
        val tuesday = timestamp(2026, Calendar.SEPTEMBER, 8, 11)
        val nextMonday = timestamp(2026, Calendar.SEPTEMBER, 14, 9)
        val repository = FakeWorkoutSessionRepository(
            listOf(
                entry(1, monday, 60, 2.5),
                entry(2, tuesday, 90, null),
                entry(3, nextMonday, 120, 4.0)
            )
        )

        val result = GetWorkoutHistoryUseCase(repository)(
            fromMillis = timestamp(2026, Calendar.SEPTEMBER, 1),
            toMillis = timestamp(2026, Calendar.OCTOBER, 1),
            weekFromMillis = timestamp(2026, Calendar.SEPTEMBER, 7),
            weekToMillis = timestamp(2026, Calendar.SEPTEMBER, 14)
        ).first()

        assertEquals(listOf(1L, 2L), result.weeklySessions.map { it.sessionId })
        assertEquals(2, result.weeklySummary.workoutCount)
        assertEquals(150L, result.weeklySummary.totalDurationSeconds)
        assertEquals(2.5, result.weeklySummary.totalCaloriesBurned ?: 0.0, 0.0)
        assertEquals(3, result.workoutDayStarts.size)
    }

    @Test
    fun leavesCaloriesUnknownWhenEverySessionIsMissingCalories() = runBlocking {
        val completedAt = timestamp(2026, Calendar.SEPTEMBER, 7, 10)
        val repository = FakeWorkoutSessionRepository(
            listOf(entry(1, completedAt, null, null))
        )

        val result = GetWorkoutHistoryUseCase(repository)(
            fromMillis = timestamp(2026, Calendar.SEPTEMBER, 1),
            toMillis = timestamp(2026, Calendar.OCTOBER, 1),
            weekFromMillis = timestamp(2026, Calendar.SEPTEMBER, 7),
            weekToMillis = timestamp(2026, Calendar.SEPTEMBER, 14)
        ).first()

        assertEquals(0L, result.weeklySummary.totalDurationSeconds)
        assertNull(result.weeklySummary.totalCaloriesBurned)
    }

    private fun entry(
        id: Long,
        completedAt: Long,
        durationSeconds: Int?,
        calories: Double?
    ) = WorkoutHistoryEntry(
        sessionId = id,
        title = "Workout $id",
        imageUrl = null,
        startedAt = completedAt - 1_000,
        completedAt = completedAt,
        durationSeconds = durationSeconds,
        caloriesBurned = calories
    )

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        Calendar.getInstance().run {
            clear()
            set(year, month, day, hour, 0, 0)
            timeInMillis
        }
}

private class FakeWorkoutSessionRepository(
    private val history: List<WorkoutHistoryEntry>
) : WorkoutSessionRepository {
    override fun observeCompletedHistory(
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<WorkoutHistoryEntry>> = flowOf(
        history.filter { it.completedAt in fromMillis until toMillis }
    )

    override fun observeCompletedSessionTimestamps(
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<Long>> = flowOf(emptyList())

    override fun observeAllCompletedSessionTimestamps(): Flow<List<Long>> = flowOf(emptyList())

    override fun observeAchievementTotals(): Flow<AchievementTotals> =
        flowOf(AchievementTotals(0, 0, 0))

    override suspend fun getLatestSessionForPlan(planId: Long): WorkoutSessionSummary? = null

    override suspend fun createSession(planId: Long, planDayId: Long): Long = 0

    override suspend fun completeSession(sessionId: Long) = Unit

    override suspend fun abandonSession(sessionId: Long) = Unit
}
