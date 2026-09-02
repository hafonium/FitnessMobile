package com.example.homeworkout.domain.usecases.settings

import com.example.homeworkout.domain.models.SettingsPreferences
import com.example.homeworkout.domain.repositories.SettingsRepository

class UpdateSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(preferences: SettingsPreferences) {
        settingsRepository.updateSettings(preferences)
    }
}
