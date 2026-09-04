package com.example.homeworkout.ui.services

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Short countdown "tick" played on each of the final seconds of a prep/exercise/rest timer, plus
 * the longer tick + vibration that marks the start of a new exercise. Uses the platform
 * [ToneGenerator] instead of a bundled audio asset — cheap, no file to ship, and loud/short
 * enough to read as a clock tick.
 */
class TickSoundPlayer(context: Context) {

    private val appContext = context.applicationContext

    private var toneGenerator: ToneGenerator? = null
    private var toneGeneratorVolume = -1

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    /** Plays one tick at [volume] (0f..1f, from [com.example.homeworkout.domain.models.SettingsPreferences.soundVolume]). */
    fun tick(volume: Float) {
        val volumePercent = (volume.coerceIn(0f, 1f) * 100).toInt().coerceIn(1, 100)
        val generator = obtainGenerator(volumePercent) ?: return
        runCatching { generator.startTone(ToneGenerator.TONE_PROP_BEEP2, TICK_DURATION_MS) }
    }

    /** Longer tick + a short vibration, played once when a new exercise starts. */
    fun exerciseStartSignal(volume: Float) {
        val volumePercent = (volume.coerceIn(0f, 1f) * 100).toInt().coerceIn(1, 100)
        val generator = obtainGenerator(volumePercent) ?: return
        runCatching { generator.startTone(ToneGenerator.TONE_PROP_BEEP2, LONG_TICK_DURATION_MS) }
        vibrate()
    }

    private fun vibrate() {
        val vib = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(VIBRATE_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(VIBRATE_DURATION_MS)
            }
        }
    }

    /** [ToneGenerator]'s volume is fixed at construction, so rebuild it if the setting changed since last tick. */
    private fun obtainGenerator(volumePercent: Int): ToneGenerator? {
        if (toneGenerator == null || toneGeneratorVolume != volumePercent) {
            toneGenerator?.release()
            toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, volumePercent) }.getOrNull()
            toneGeneratorVolume = volumePercent
        }
        return toneGenerator
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        private const val TICK_DURATION_MS = 120
        private const val LONG_TICK_DURATION_MS = 400
        private const val VIBRATE_DURATION_MS = 300L
    }
}
