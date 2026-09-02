package com.example.homeworkout.ui.core.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homeworkout.domain.models.SettingsPreferences
import com.example.homeworkout.domain.models.enums.UnitSystemType
import com.example.homeworkout.domain.models.enums.UserGender
import com.example.homeworkout.domain.models.enums.VoiceType
import com.example.homeworkout.domain.usecases.settings.GetSettingsUseCase
import com.example.homeworkout.domain.usecases.settings.ResetWorkoutProgressUseCase
import com.example.homeworkout.domain.usecases.settings.UpdateSettingsUseCase
import com.example.homeworkout.ui.services.TtsService
import com.example.homeworkout.ui.services.TtsVoiceOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shared by every screen under the Settings tab (Workout, General, Voice). */
class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val resetWorkoutProgressUseCase: ResetWorkoutProgressUseCase,
    private val ttsService: TtsService
) : ViewModel() {

    val settings: StateFlow<SettingsPreferences> = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPreferences())

    private fun update(transform: (SettingsPreferences) -> SettingsPreferences) {
        viewModelScope.launch { updateSettingsUseCase(transform(settings.value)) }
    }

    fun setGender(gender: UserGender) = update { it.copy(gender = gender) }
    fun setMusicEnabled(enabled: Boolean) = update { it.copy(musicEnabled = enabled) }
    fun setMusicVolume(volume: Float) = update { it.copy(musicVolume = volume) }
    fun setSoundEnabled(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setSoundVolume(volume: Float) = update { it.copy(soundVolume = volume) }
    fun setRestTimerSec(seconds: Int) = update { it.copy(restTimerSec = seconds) }
    fun setPrepTimerSec(seconds: Int) = update { it.copy(prepTimerSec = seconds) }
    fun setUnitSystem(unitSystem: UnitSystemType) = update { it.copy(unitSystem = unitSystem) }
    fun setKeepScreenOn(enabled: Boolean) = update { it.copy(keepScreenOn = enabled) }
    fun setDailyReminder(enabled: Boolean, time: String?) = update { it.copy(dailyReminderEnabled = enabled, dailyReminderTime = time) }
    fun setVoiceType(voiceType: VoiceType) = update { it.copy(ttsVoiceType = voiceType) }

    /** Persists a specific engine voice (picked from [listVoices]) as the active coach voice. */
    fun setCustomVoice(voiceName: String) = update { it.copy(ttsVoiceType = VoiceType.CUSTOM, customVoiceName = voiceName) }

    fun resetWorkoutProgress() {
        viewModelScope.launch { resetWorkoutProgressUseCase() }
    }

    /** Plays the sample coaching line using the currently selected voice, without persisting anything new. */
    fun previewVoice() {
        val current = settings.value
        ttsService.speak(TtsService.SAMPLE_SENTENCE, current.ttsVoiceType, current.customVoiceName)
    }

    /** Every offline voice the installed engine reports, for the "Browse all voices" picker. */
    fun listVoices(): List<TtsVoiceOption> = ttsService.listVoices()

    /** Auditions a specific engine voice by name without selecting or persisting it. */
    fun previewVoiceByName(voiceName: String) {
        ttsService.previewVoiceByName(voiceName)
    }

    /**
     * True once the engine reports it has no real distinct male/female voices for the current
     * locale — the UI uses this to nudge the user toward installing a fuller TTS engine (e.g.
     * Google Text-to-Speech) instead of relying on a forced pitch shift.
     */
    fun hasLimitedVoiceEngine(): Boolean = !ttsService.hasGenderedVoices()

    /** Which TTS engine package is active and how many voices it reports — shown for troubleshooting. */
    fun engineDiagnostics(): String = ttsService.engineDiagnostics()
}
