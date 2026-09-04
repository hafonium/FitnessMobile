package com.example.homeworkout.ui.core.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.StreakInfo
import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.models.WeightDashboard
import com.example.homeworkout.domain.models.WeeklyGoalProgress
import com.example.homeworkout.domain.usecases.badges.EvaluateBadgesUseCase
import com.example.homeworkout.domain.usecases.badges.GetBadgesUseCase
import com.example.homeworkout.domain.usecases.badges.MarkBadgesSeenUseCase
import com.example.homeworkout.domain.usecases.history.GetWorkoutHistoryUseCase
import com.example.homeworkout.domain.usecases.home.GetWeeklyGoalProgressUseCase
import com.example.homeworkout.domain.usecases.report.GetStreakUseCase
import com.example.homeworkout.domain.usecases.report.GetWeightDashboardUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class WorkoutReportSummary(
    val completedWorkouts: Int = 0,
    val totalDurationSeconds: Long = 0,
    val totalCalories: Double? = null
)

class ReportViewModel(
    getStreakUseCase: GetStreakUseCase,
    getWeightDashboardUseCase: GetWeightDashboardUseCase,
    getBadgesUseCase: GetBadgesUseCase,
    getWeeklyGoalProgressUseCase: GetWeeklyGoalProgressUseCase,
    getWorkoutHistoryUseCase: GetWorkoutHistoryUseCase,
    private val evaluateBadgesUseCase: EvaluateBadgesUseCase,
    private val markBadgesSeenUseCase: MarkBadgesSeenUseCase
) : ViewModel() {
    val workoutSummary: StateFlow<WorkoutReportSummary> = getWorkoutHistoryUseCase()
        .map { records ->
            val recordedCalories = records.mapNotNull { it.caloriesBurned }
            WorkoutReportSummary(
                completedWorkouts = records.size,
                totalDurationSeconds = records.sumOf { it.durationSeconds.toLong() },
                totalCalories = recordedCalories.takeIf { it.isNotEmpty() }?.sum()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WorkoutReportSummary()
        )

    val weeklyProgress: StateFlow<WeeklyGoalProgress> = getWeeklyGoalProgressUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeeklyGoalProgress(goalDays = 0, completedDays = 0, days = emptyList())
        )

    val streak: StateFlow<StreakInfo> = getStreakUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StreakInfo(currentStreak = 0, bestStreak = 0)
        )

    val badges: StateFlow<List<BadgeProgress>> = getBadgesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val weightDashboard: StateFlow<WeightDashboard?> = getWeightDashboardUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        viewModelScope.launch { evaluateBadgesUseCase() }
    }

    fun markBadgeSeen(badgeId: String) {
        viewModelScope.launch { markBadgesSeenUseCase(listOf(badgeId)) }
    }
}
