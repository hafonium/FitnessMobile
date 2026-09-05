package com.example.homeworkout.domain.usecases.home

import com.example.homeworkout.domain.models.RecoveryScore
import com.example.homeworkout.domain.models.WorkoutHistoryRecord
import com.example.homeworkout.domain.models.enums.RecoveryTier
import java.util.Calendar

/**
 * Deterministic, fully offline readiness heuristic - no network calls, no ML model, instant to
 * compute from already-loaded local history. Base score of 100, deducted for consecutive-day
 * training strain and a high-volume session the day before, then clamped to [10, 100].
 *
 * The nutritional modifier from the feature spec is intentionally never applied: this build has
 * no persisted daily nutrition log (the food scanner result in docs/food-calorie-scanner.md is a
 * one-shot lookup, not stored history), so every user is always in the spec's
 * "no nutrition logged - skip the modifier" branch.
 */
class RecoveryCalculator {

    fun calculate(
        completedSessions: List<WorkoutHistoryRecord>,
        nowMillis: Long = System.currentTimeMillis()
    ): RecoveryScore {
        if (completedSessions.isEmpty()) {
            return RecoveryScore(score = 100, tier = RecoveryTier.OPTIMAL, badges = emptyList())
        }

        val dayStarts = completedSessions.map { startOfDay(it.endedAt) }.toHashSet()
        val today = startOfDay(nowMillis)
        val yesterday = today - DAY_MILLIS

        var score = 100
        val badges = mutableListOf<String>()

        val consecutiveDays = consecutiveTrainedDays(dayStarts, yesterday)
        score -= when {
            consecutiveDays >= 4 -> 45
            consecutiveDays == 3 -> 30
            consecutiveDays == 2 -> 15
            else -> 0
        }
        if (consecutiveDays >= 2) badges.add("$consecutiveDays-Day Streak")

        val longestSessionYesterdaySec = completedSessions
            .filter { startOfDay(it.endedAt) == yesterday }
            .maxOfOrNull { it.durationSeconds } ?: 0
        if (longestSessionYesterdaySec > HIGH_INTENSITY_DURATION_SEC) {
            score -= 10
            badges.add("High Intensity Yesterday")
        }

        val clamped = score.coerceIn(10, 100)
        return RecoveryScore(score = clamped, tier = RecoveryTier.fromScore(clamped), badges = badges)
    }

    /** Length of the run of trained days ending at (and including) [fromDay], counting backward. */
    private fun consecutiveTrainedDays(dayStarts: Set<Long>, fromDay: Long): Int {
        var cursor = fromDay
        var count = 0
        while (cursor in dayStarts) {
            count++
            cursor -= DAY_MILLIS
        }
        return count
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
        private const val HIGH_INTENSITY_DURATION_SEC = 30 * 60
    }
}
