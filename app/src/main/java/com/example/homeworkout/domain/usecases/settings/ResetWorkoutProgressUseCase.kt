package com.example.homeworkout.domain.usecases.settings

import com.example.homeworkout.domain.repositories.SettingsRepository

/** Backs the destructive "Restart progress" action on the Workout Settings screen. */
class ResetWorkoutProgressUseCase(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        settingsRepository.resetWorkoutProgress()
    }
}
