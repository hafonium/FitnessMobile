package com.example.homeworkout.ui.core.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WeightDashboard
import com.example.homeworkout.domain.models.WeightForecast
import com.example.homeworkout.domain.usecases.report.GetWeightDashboardUseCase
import com.example.homeworkout.domain.usecases.report.GetWeightForecastUseCase
import com.example.homeworkout.domain.usecases.report.RecordWeightUseCase
import com.example.homeworkout.domain.usecases.report.UpdateAgeUseCase
import com.example.homeworkout.domain.usecases.report.UpdateHeightUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeightViewModel(
    getWeightDashboardUseCase: GetWeightDashboardUseCase,
    private val recordWeightUseCase: RecordWeightUseCase,
    private val updateHeightUseCase: UpdateHeightUseCase,
    private val updateAgeUseCase: UpdateAgeUseCase,
    getWeightForecastUseCase: GetWeightForecastUseCase
) : ViewModel() {

    val dashboard: StateFlow<WeightDashboard?> = getWeightDashboardUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The TDEE forecast (docs/weight-forecast-feature.md), overlaid on this screen's own trend chart. */
    val forecast: StateFlow<WeightForecast?> = getWeightForecastUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun recordWeight(weightKg: Double, heightCm: Double) {
        viewModelScope.launch { recordWeightUseCase(weightKg, heightCm) }
    }

    fun updateHeight(heightCm: Double) {
        viewModelScope.launch { updateHeightUseCase(heightCm) }
    }

    fun updateAge(ageYears: Int) {
        viewModelScope.launch { updateAgeUseCase(ageYears) }
    }
}
