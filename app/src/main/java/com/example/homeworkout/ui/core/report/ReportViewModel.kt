package com.example.homeworkout.ui.core.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.StreakInfo
import com.example.homeworkout.domain.usecases.report.GetStreakUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReportViewModel(
    getStreakUseCase: GetStreakUseCase
) : ViewModel() {
    val streak: StateFlow<StreakInfo> = getStreakUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StreakInfo(currentStreak = 0, bestStreak = 0)
        )
}
