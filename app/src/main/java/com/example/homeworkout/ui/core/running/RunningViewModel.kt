package com.example.homeworkout.ui.core.running

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.running.RunSession
import com.example.homeworkout.domain.running.RunningTelemetry
import com.example.homeworkout.domain.running.RunningTelemetryCalculator
import com.example.homeworkout.domain.usecases.running.ObserveRunningSessionUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RunningUiState(
    val session: RunSession? = null,
    val activeDurationMillis: Long = 0,
    val telemetry: RunningTelemetry = RunningTelemetry(0.0, null, null)
)

class RunningViewModel(observeRunningSession: ObserveRunningSessionUseCase) : ViewModel() {
    private val clock = MutableStateFlow(SystemClock.elapsedRealtime())
    private val calculator = RunningTelemetryCalculator()

    val uiState: StateFlow<RunningUiState> = combine(observeRunningSession(), clock) { session, now ->
        val duration = session?.activeDurationAt(now) ?: 0L
        RunningUiState(
            session = session,
            activeDurationMillis = duration,
            telemetry = calculator.calculate(session?.distanceMeters ?: 0.0, duration, session?.weightKg)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RunningUiState())

    init {
        viewModelScope.launch {
            while (isActive) {
                clock.value = SystemClock.elapsedRealtime()
                delay(1_000)
            }
        }
    }
}
