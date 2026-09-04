package com.example.homeworkout.domain.usecases.history

import com.example.homeworkout.domain.models.WorkoutHistoryPeriod
import com.example.homeworkout.domain.models.WorkoutHistorySummary
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetWorkoutHistoryUseCase(
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    operator fun invoke(
        fromMillis: Long,
        toMillis: Long,
        weekFromMillis: Long,
        weekToMillis: Long
    ): Flow<WorkoutHistoryPeriod> =
        workoutSessionRepository.observeCompletedHistory(fromMillis, toMillis).map { sessions ->
            val weeklySessions = sessions.filter { it.completedAt in weekFromMillis until weekToMillis }
            val calorieValues = weeklySessions.mapNotNull { it.caloriesBurned }
            WorkoutHistoryPeriod(
                sessions = sessions,
                workoutDayStarts = sessions.mapTo(linkedSetOf()) { startOfDay(it.completedAt) },
                weeklySessions = weeklySessions,
                weeklySummary = WorkoutHistorySummary(
                    workoutCount = weeklySessions.size,
                    totalDurationSeconds = weeklySessions.sumOf { it.durationSeconds?.toLong() ?: 0L },
                    totalCaloriesBurned = calorieValues.takeIf { it.isNotEmpty() }?.sum()
                )
            )
        }

    private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().run {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}
