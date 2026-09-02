package com.example.homeworkout.domain.usecases.settings

import com.example.homeworkout.domain.models.SettingsPreferences
import com.example.homeworkout.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow

/** Used by every screen under the Settings tab. */
class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<SettingsPreferences> = settingsRepository.observeSettings()
}
