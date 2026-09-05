package com.example.homeworkout.ui.core.running.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.usecases.running.DeleteRunUseCase
import com.example.homeworkout.domain.usecases.running.GetRunHistoryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RunHistoryUiState(
    val sessions: List<RunSession> = emptyList(),
    val totalDistanceKilometers: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val averagePaceMinutesPerKilometer: Double = 0.0,
    val totalCalories: Double = 0.0,
    val isLoading: Boolean = true
)

class RunHistoryViewModel(
    getRunHistory: GetRunHistoryUseCase,
    private val deleteRun: DeleteRunUseCase
) : ViewModel() {
    val uiState: StateFlow<RunHistoryUiState> = getRunHistory()
        .map { sessions ->
            val distanceMeters = sessions.sumOf { it.distanceMeters }
            val durationSeconds = sessions.sumOf { it.durationSeconds }
            val distanceKilometers = distanceMeters / 1_000.0
            RunHistoryUiState(
                sessions = sessions,
                totalDistanceKilometers = distanceKilometers,
                totalDurationSeconds = durationSeconds,
                averagePaceMinutesPerKilometer = if (distanceKilometers > 0.0 && durationSeconds > 0L) {
                    durationSeconds / 60.0 / distanceKilometers
                } else 0.0,
                totalCalories = sessions.sumOf { it.calories ?: 0.0 },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunHistoryUiState())

    fun delete(sessionId: Long) {
        viewModelScope.launch { deleteRun(sessionId) }
    }
}
