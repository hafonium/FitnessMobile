package com.example.homeworkout.domain.usecases.home

import com.example.homeworkout.domain.models.WeeklyGoalDay
import com.example.homeworkout.domain.models.WeeklyGoalProgress
import com.example.homeworkout.domain.models.enums.WeekDay
import com.example.homeworkout.domain.repositories.SettingsRepository
import com.example.homeworkout.domain.repositories.WorkoutSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.Calendar

/** Backs the Home screen's "Weekly Goal" card: the 7-day window starting at the user's chosen first-day-of-week, plus which days already have a completed session. */
class GetWeeklyGoalProgressUseCase(
    private val settingsRepository: SettingsRepository,
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<WeeklyGoalProgress> =
        settingsRepository.observeSettings().flatMapLatest { settings ->
            val dayStarts = weekDayStarts(settings.firstDayOfWeek)
            val weekStart = dayStarts.first()
            val weekEnd = dayStarts.last() + DAY_MILLIS

            workoutSessionRepository.observeCompletedSessionTimestamps(weekStart, weekEnd).map { completedTimestamps ->
                val todayStart = startOfDay(System.currentTimeMillis())
                val days = dayStarts.map { dayStart ->
                    val dayEnd = dayStart + DAY_MILLIS
                    WeeklyGoalDay(
                        dayStartMillis = dayStart,
                        dayOfMonth = dayOfMonthOf(dayStart),
                        isToday = dayStart == todayStart,
                        isCompleted = completedTimestamps.any { it in dayStart until dayEnd }
                    )
                }
                WeeklyGoalProgress(
                    goalDays = settings.weeklyGoalDays,
                    completedDays = days.count { it.isCompleted },
                    days = days
                )
            }
        }

    /** The 7 day-start timestamps of the week containing today, starting on [firstDayOfWeek]. */
    private fun weekDayStarts(firstDayOfWeek: WeekDay): List<Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startOfDay(System.currentTimeMillis())
        val targetDayOfWeek = firstDayOfWeek.toCalendarDayOfWeek()
        while (cal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return (0 until 7).map { offset ->
            (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }.timeInMillis
        }
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

    private fun dayOfMonthOf(millis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return cal.get(Calendar.DAY_OF_MONTH)
    }

    private fun WeekDay.toCalendarDayOfWeek(): Int = when (this) {
        WeekDay.SUNDAY -> Calendar.SUNDAY
        WeekDay.MONDAY -> Calendar.MONDAY
        WeekDay.TUESDAY -> Calendar.TUESDAY
        WeekDay.WEDNESDAY -> Calendar.WEDNESDAY
        WeekDay.THURSDAY -> Calendar.THURSDAY
        WeekDay.FRIDAY -> Calendar.FRIDAY
        WeekDay.SATURDAY -> Calendar.SATURDAY
    }

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
