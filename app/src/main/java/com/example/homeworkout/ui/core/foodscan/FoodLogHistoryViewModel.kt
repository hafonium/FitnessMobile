package com.example.homeworkout.ui.core.foodscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.FoodLogEntry
import com.example.homeworkout.domain.usecases.food.GetFoodLogHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FoodLogHistoryViewModel(
    getFoodLogHistoryUseCase: GetFoodLogHistoryUseCase
) : ViewModel() {
    val logs: StateFlow<List<FoodLogEntry>> = getFoodLogHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
