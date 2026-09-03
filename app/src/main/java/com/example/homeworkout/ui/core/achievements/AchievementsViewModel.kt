package com.example.homeworkout.ui.core.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.BadgeProgress
import com.example.homeworkout.domain.usecases.badges.EvaluateBadgesUseCase
import com.example.homeworkout.domain.usecases.badges.GetBadgesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AchievementsViewModel(
    getBadgesUseCase: GetBadgesUseCase,
    private val evaluateBadgesUseCase: EvaluateBadgesUseCase
) : ViewModel() {
    val badges: StateFlow<List<BadgeProgress>> = getBadgesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch { evaluateBadgesUseCase() }
    }
}
