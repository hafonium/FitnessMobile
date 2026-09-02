package com.example.homeworkout.ui.core.editgoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.SettingsPreferences
import com.example.homeworkout.domain.models.enums.WeekDay
import com.example.homeworkout.domain.usecases.settings.GetSettingsUseCase
import com.example.homeworkout.domain.usecases.settings.UpdateSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs "Set your weekly goal" (Screen.EditGoal) — reads/writes the same `user_settings` row as the Settings tab. */
class EditGoalViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)

    /** False until the first real Room value has loaded — see [SettingsViewModel.isReady] for why this exists. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val settings: StateFlow<SettingsPreferences> = getSettingsUseCase()
        .onEach { _isReady.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPreferences())

    fun save(goalDays: Int, firstDay: WeekDay) {
        viewModelScope.launch {
            updateSettingsUseCase(settings.value.copy(weeklyGoalDays = goalDays, firstDayOfWeek = firstDay))
        }
    }
}
