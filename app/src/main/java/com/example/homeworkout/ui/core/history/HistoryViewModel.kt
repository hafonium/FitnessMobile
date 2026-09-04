package com.example.homeworkout.ui.core.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WorkoutHistoryRecord
import com.example.homeworkout.domain.usecases.history.GetWorkoutHistoryUseCase
import java.util.Calendar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(
    val records: List<WorkoutHistoryRecord> = emptyList(),
    val weekRecords: List<WorkoutHistoryRecord> = emptyList(),
    val weekStartMillis: Long = currentWeekBounds().first,
    val weekEndMillis: Long = currentWeekBounds().second,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HistoryViewModel(
    getWorkoutHistoryUseCase: GetWorkoutHistoryUseCase
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = getWorkoutHistoryUseCase()
        .map { records ->
            val (weekStart, weekEnd) = currentWeekBounds()
            HistoryUiState(
                records = records,
                weekRecords = records.filter { it.endedAt in weekStart until weekEnd },
                weekStartMillis = weekStart,
                weekEndMillis = weekEnd,
                isLoading = false
            )
        }
        .catch { error ->
            emit(HistoryUiState(isLoading = false, errorMessage = error.message ?: "Unable to load workout history"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )
}

/** Sunday 00:00 (inclusive) to the following Sunday 00:00 (exclusive). */
private fun currentWeekBounds(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_MONTH, -(get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
    return start.timeInMillis to end.timeInMillis
}
