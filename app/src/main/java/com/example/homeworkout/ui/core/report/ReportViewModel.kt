package com.example.homeworkout.ui.core.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.StreakInfo
import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.models.WeightDashboard
import com.example.homeworkout.domain.usecases.badges.EvaluateBadgesUseCase
import com.example.homeworkout.domain.usecases.badges.GetBadgesUseCase
import com.example.homeworkout.domain.usecases.badges.MarkBadgesSeenUseCase
import com.example.homeworkout.domain.usecases.report.GetStreakUseCase
import com.example.homeworkout.domain.usecases.report.GetWeightDashboardUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReportViewModel(
    getStreakUseCase: GetStreakUseCase,
    getWeightDashboardUseCase: GetWeightDashboardUseCase,
    getBadgesUseCase: GetBadgesUseCase,
    private val evaluateBadgesUseCase: EvaluateBadgesUseCase,
    private val markBadgesSeenUseCase: MarkBadgesSeenUseCase
) : ViewModel() {
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
