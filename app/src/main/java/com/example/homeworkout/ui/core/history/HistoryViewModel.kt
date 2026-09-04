package com.example.homeworkout.ui.core.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.WorkoutHistoryEntry
import com.example.homeworkout.domain.models.WorkoutHistorySummary
import com.example.homeworkout.domain.usecases.history.GetWorkoutHistoryUseCase
import com.example.homeworkout.ui.components.WorkoutCalendarDay
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HistoryUiState(
    val isLoading: Boolean = true,
    val visibleMonthStartMillis: Long,
    val selectedDayMillis: Long,
    val weekFromMillis: Long,
    val weekToMillis: Long,
    val calendarDays: List<WorkoutCalendarDay> = emptyList(),
    val weeklySessions: List<WorkoutHistoryEntry> = emptyList(),
    val weeklySummary: WorkoutHistorySummary = WorkoutHistorySummary(),
    val errorMessage: String? = null
)

private data class HistorySelection(
    val visibleYear: Int,
    val visibleMonth: Int,
    val selectedDayMillis: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val getWorkoutHistoryUseCase: GetWorkoutHistoryUseCase
) : ViewModel() {
    private val initialSelection = selectionFor(System.currentTimeMillis())
    private val selection = MutableStateFlow(initialSelection)
    private val retryNonce = MutableStateFlow(0)

    val uiState: StateFlow<HistoryUiState> = selection
        .combine(retryNonce) { selected, _ -> selected }
        .flatMapLatest { selected ->
            val calendarDays = buildCalendarDays(selected)
            val queryFrom = calendarDays.first().dayStartMillis
            val queryTo = nextDayStart(calendarDays.last().dayStartMillis)
            val (weekFrom, weekTo) = weekBounds(selected.selectedDayMillis)
            val baseState = HistoryUiState(
                visibleMonthStartMillis = monthStart(selected.visibleYear, selected.visibleMonth),
                selectedDayMillis = selected.selectedDayMillis,
                weekFromMillis = weekFrom,
                weekToMillis = weekTo,
                calendarDays = calendarDays
            )

            getWorkoutHistoryUseCase(queryFrom, queryTo, weekFrom, weekTo)
                .map { period ->
                    baseState.copy(
                        isLoading = false,
                        calendarDays = calendarDays.map { day ->
                            day.copy(hasWorkout = day.dayStartMillis in period.workoutDayStarts)
                        },
                        weeklySessions = period.weeklySessions,
                        weeklySummary = period.weeklySummary
                    )
                }
                .onStart { emit(baseState) }
                .catch {
                    emit(
                        baseState.copy(
                            isLoading = false,
                            errorMessage = "Could not load workout history."
                        )
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialState(initialSelection)
        )

    fun selectDay(dayStartMillis: Long) {
        selection.value = selectionFor(dayStartMillis)
    }

    fun showPreviousMonth() = moveMonth(-1)

    fun showNextMonth() = moveMonth(1)

    fun retry() {
        retryNonce.update { it + 1 }
    }

    private fun moveMonth(amount: Int) {
        selection.update { current ->
            val selected = Calendar.getInstance().apply { timeInMillis = current.selectedDayMillis }
            val desiredDay = selected.get(Calendar.DAY_OF_MONTH)
            selected.set(Calendar.DAY_OF_MONTH, 1)
            selected.add(Calendar.MONTH, amount)
            val targetDay = desiredDay.coerceAtMost(selected.getActualMaximum(Calendar.DAY_OF_MONTH))
            selected.set(Calendar.DAY_OF_MONTH, targetDay)
            selectionFor(selected.timeInMillis)
        }
    }

    private fun initialState(selected: HistorySelection): HistoryUiState {
        val (weekFrom, weekTo) = weekBounds(selected.selectedDayMillis)
        return HistoryUiState(
            visibleMonthStartMillis = monthStart(selected.visibleYear, selected.visibleMonth),
            selectedDayMillis = selected.selectedDayMillis,
            weekFromMillis = weekFrom,
            weekToMillis = weekTo,
            calendarDays = buildCalendarDays(selected)
        )
    }
}

private fun selectionFor(timestamp: Long): HistorySelection {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timestamp
        normalizeToDayStart()
    }
    return HistorySelection(
        visibleYear = calendar.get(Calendar.YEAR),
        visibleMonth = calendar.get(Calendar.MONTH),
        selectedDayMillis = calendar.timeInMillis
    )
}

private fun buildCalendarDays(selection: HistorySelection): List<WorkoutCalendarDay> {
    val firstOfMonth = Calendar.getInstance().apply {
        clear()
        set(selection.visibleYear, selection.visibleMonth, 1)
        normalizeToDayStart()
    }
    val offset = (firstOfMonth.get(Calendar.DAY_OF_WEEK) - firstOfMonth.firstDayOfWeek + 7) % 7
    val cursor = (firstOfMonth.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -offset) }
    val lastOfMonth = (firstOfMonth.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }
    val trailing = (firstOfMonth.firstDayOfWeek + 6 - lastOfMonth.get(Calendar.DAY_OF_WEEK) + 7) % 7
    val cellCount = offset + lastOfMonth.get(Calendar.DAY_OF_MONTH) + trailing
    val today = Calendar.getInstance().apply { normalizeToDayStart() }.timeInMillis

    return List(cellCount) {
        val millis = cursor.timeInMillis
        WorkoutCalendarDay(
            dayStartMillis = millis,
            dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH),
            belongsToVisibleMonth = cursor.get(Calendar.MONTH) == selection.visibleMonth &&
                cursor.get(Calendar.YEAR) == selection.visibleYear,
            isToday = millis == today,
            isSelected = millis == selection.selectedDayMillis,
            hasWorkout = false
        ).also { cursor.add(Calendar.DAY_OF_MONTH, 1) }
    }
}

private fun weekBounds(dayStartMillis: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = dayStartMillis
        normalizeToDayStart()
        val offset = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
        add(Calendar.DAY_OF_MONTH, -offset)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
    return start.timeInMillis to end.timeInMillis
}

private fun monthStart(year: Int, month: Int): Long = Calendar.getInstance().run {
    clear()
    set(year, month, 1)
    normalizeToDayStart()
    timeInMillis
}

private fun nextDayStart(timestamp: Long): Long = Calendar.getInstance().run {
    timeInMillis = timestamp
    normalizeToDayStart()
    add(Calendar.DAY_OF_MONTH, 1)
    timeInMillis
}

private fun Calendar.normalizeToDayStart() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
