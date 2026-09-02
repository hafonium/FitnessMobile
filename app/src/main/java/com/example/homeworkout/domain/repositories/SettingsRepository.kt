package com.example.homeworkout.domain.repositories

import com.example.homeworkout.domain.models.SettingsPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    /** Settings for the single local user this app runs as. */
    fun observeSettings(): Flow<SettingsPreferences>

    suspend fun updateSettings(preferences: SettingsPreferences)

    /** Clears all workout session history/progress, keeping preferences untouched. */
    suspend fun resetWorkoutProgress()
}
