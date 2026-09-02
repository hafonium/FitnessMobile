package com.example.homeworkout.ui.core.editgoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.SettingsPreferences
import com.example.homeworkout.domain.models.enums.WeekDay
import com.example.homeworkout.domain.usecases.settings.GetSettingsUseCase
import com.example.homeworkout.domain.usecases.settings.UpdateSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs "Set your weekly goal" (Screen.EditGoal) — reads/writes the same `user_settings` row as the Settings tab. */
class EditGoalViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<SettingsPreferences> = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPreferences())

    fun save(goalDays: Int, firstDay: WeekDay) {
        viewModelScope.launch {
            updateSettingsUseCase(settings.value.copy(weeklyGoalDays = goalDays, firstDayOfWeek = firstDay))
        }
    }
}
