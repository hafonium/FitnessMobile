package com.example.homeworkout.domain.usecases.report

import com.example.homeworkout.domain.models.StreakInfo
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Current and best workout streaks: consecutive calendar days containing at least one COMPLETED
 * workout session. The current streak counts backward from today, but if today has no completed
 * session yet it starts counting from yesterday instead — so the streak doesn't drop to zero
 * first thing in the morning before today's workout is done, only once a full day is actually
 * missed.
 */
class GetStreakUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    operator fun invoke(): Flow<StreakInfo> =
        workoutSessionRepository.observeAllCompletedSessionTimestamps().map { timestamps ->
            val dayStarts = timestamps.map { startOfDay(it) }.distinct().sorted()
            StreakInfo(
                currentStreak = currentStreak(dayStarts.toHashSet()),
                bestStreak = bestStreak(dayStarts)
            )
        }

    private fun currentStreak(dayStartSet: Set<Long>): Int {
        if (dayStartSet.isEmpty()) return 0
        val today = startOfDay(System.currentTimeMillis())
        var cursor = if (today in dayStartSet) today else today - DAY_MILLIS
        var streak = 0
        while (cursor in dayStartSet) {
            streak++
            cursor -= DAY_MILLIS
        }
        return streak
    }

    /** [sortedDayStarts] must already be sorted ascending with no duplicates. */
    private fun bestStreak(sortedDayStarts: List<Long>): Int {
        if (sortedDayStarts.isEmpty()) return 0
        var best = 1
        var run = 1
        for (i in 1 until sortedDayStarts.size) {
            run = if (sortedDayStarts[i] - sortedDayStarts[i - 1] == DAY_MILLIS) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
