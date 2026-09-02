package com.example.homeworkout.ui.services

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.homeworkout.domain.models.enums.VoiceType
import java.io.File
import java.util.Locale
import java.util.UUID

/** One voice the installed TTS engine can browse/preview, for the Voice Options picker. */
data class TtsVoiceOption(
    /** Engine-internal id, e.g. "en-us-x-iom-local" — persisted as [com.example.homeworkout.domain.models.SettingsPreferences.customVoiceName]. */
    val name: String,
    val label: String,
    val localeLabel: String
)

/**
 * Thin wrapper around the platform [TextToSpeech] engine. Owns engine init/lifecycle and picks
 * the voice for the selected coach persona.
 *
 * Primary strategy: ask the installed TTS engine for an actual distinct [Voice] matching the
 * requested gender (e.g. Android's Google Text-to-Speech engine ships real per-locale voices
 * named like "...#male_1-local" / "...#female_2" — a genuinely different voice model, not just a
 * pitch tweak on one voice). [listVoices] exposes the engine's full offline voice list so the
 * user can browse and pick a specific one instead of just Male/Female/Device. Only when the
 * installed engine has no gendered voices at all (e.g. a bare-bones emulator engine) do we fall
 * back to forcing a pitch shift at *playback* time (via [MediaPlayer]/[PlaybackParams]).
 */
class TtsService(context: Context) {

    private val appContext = context.applicationContext
    private var engine: TextToSpeech? = null
    private var isReady = false
    private var player: MediaPlayer? = null

    init {
        engine = TextToSpeech(appContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                engine?.language = Locale.getDefault()
            }
        }
    }

    /** Speaks [text] as [voiceType]; [customVoiceName] is required (and used) only for [VoiceType.CUSTOM]. */
    fun speak(text: String, voiceType: VoiceType, customVoiceName: String? = null) {
        val tts = engine ?: return
        if (!isReady) return

        val voice = when (voiceType) {
            VoiceType.CUSTOM -> customVoiceName?.let { name ->
                runCatching { tts.voices?.firstOrNull { it.name == name } }.getOrNull()
            }
            else -> genderVoiceFor(voiceType, tts)
        }

        if (voice != null) {
            speakWithVoice(tts, text, voice)
        } else {
            tts.voice = tts.defaultVoice
            speakWithForcedPitch(tts, text, pitchFor(voiceType))
        }
    }

    /** Immediately previews a specific engine voice by name, e.g. from the voice browser list. */
    fun previewVoiceByName(name: String) {
        val tts = engine ?: return
        if (!isReady) return
        val voice = runCatching { tts.voices?.firstOrNull { it.name == name } }.getOrNull() ?: return
        runCatching { speakWithVoice(tts, SAMPLE_SENTENCE, voice) }
    }

    private fun speakWithVoice(tts: TextToSpeech, text: String, voice: Voice) {
        tts.voice = voice
        tts.setPitch(1.0f)
        tts.setSpeechRate(SPEECH_RATE)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hw_tts_${System.currentTimeMillis()}")
    }

    /**
     * Every offline voice the engine reports, for the current locale, browsable/previewable one
     * by one. Some engines throw instead of returning null/empty from [TextToSpeech.getVoices],
     * or hand back [Voice] entries with unexpected fields — never let that crash the UI.
     */
    fun listVoices(): List<TtsVoiceOption> {
        val tts = engine ?: return emptyList()
        if (!isReady) return emptyList()
        return runCatching {
            tts.voices.orEmpty()
                .filter { runCatching { !it.isNetworkConnectionRequired }.getOrDefault(false) }
                .mapNotNull { voice -> runCatching { voice.toOption() }.getOrNull() }
                // Some engines report the same voice name more than once — collapse those since
                // the name is both the Compose list key and what we persist as the selection.
                .distinctBy { it.name }
                .sortedBy { it.localeLabel }
        }.getOrDefault(emptyList())
    }

    private fun Voice.toOption(): TtsVoiceOption = TtsVoiceOption(
        name = name,
        label = labelFor(this),
        localeLabel = runCatching { locale.displayName }.getOrDefault(name)
    )

    private fun labelFor(voice: Voice): String {
        val genderTag = when {
            voice.name.contains("female", ignoreCase = true) -> "Female"
            voice.name.contains("male", ignoreCase = true) -> "Male"
            else -> "Voice"
        }
        return "$genderTag — ${voice.name}"
    }

    /** Which engine package is actually active and how many voices it reports — for on-screen diagnostics. */
    fun engineDiagnostics(): String {
        val tts = engine ?: return "TTS engine not initialized"
        if (!isReady) return "TTS engine still initializing…"
        return runCatching {
            val enginePackage = tts.defaultEngine ?: "unknown"
            val total = tts.voices?.size ?: 0
            val offline = tts.voices.orEmpty().count { runCatching { !it.isNetworkConnectionRequired }.getOrDefault(false) }
            "Engine: $enginePackage — $total voice(s) reported ($offline offline)"
        }.getOrDefault("Unable to read TTS engine info")
    }

    /** True once the engine is ready and its current locale exposes at least one male AND one female voice. */
    fun hasGenderedVoices(): Boolean {
        val tts = engine ?: return false
        if (!isReady) return false
        return genderVoiceFor(VoiceType.MALE_COACH, tts) != null && genderVoiceFor(VoiceType.FEMALE_COACH, tts) != null
    }

    /** An engine voice matching [voiceType]'s gender in the current locale, if the engine exposes one. */
    private fun genderVoiceFor(voiceType: VoiceType, tts: TextToSpeech): Voice? {
        if (voiceType != VoiceType.MALE_COACH && voiceType != VoiceType.FEMALE_COACH) return null
        val genderTag = if (voiceType == VoiceType.MALE_COACH) "male" else "female"
        val language = Locale.getDefault().language
        return runCatching {
            tts.voices
                ?.filter { voice -> voice.locale.language == language && voice.name.contains(genderTag, ignoreCase = true) }
                // Prefer voices that don't need a network round trip, but still fall back to a
                // network voice (often higher quality) if that's all the engine offers.
                ?.sortedWith(compareBy({ it.isNetworkConnectionRequired }, { -it.quality }))
                ?.firstOrNull()
        }.getOrNull()
    }

    /** Synthesizes to a temp file and plays it back with a forced pitch shift — see class doc. */
    private fun speakWithForcedPitch(tts: TextToSpeech, text: String, pitch: Float) {
        val outFile = File(appContext.cacheDir, "hw_tts_${UUID.randomUUID()}.wav")
        val utteranceId = outFile.name

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(id: String?) {
                if (id == utteranceId) playWithPitch(outFile, pitch)
            }

            @Deprecated("Deprecated in Java", ReplaceWith(""))
            override fun onError(utteranceId: String?) {
                outFile.delete()
            }

            override fun onError(id: String?, errorCode: Int) {
                outFile.delete()
            }
        })

        val result = tts.synthesizeToFile(text, null, outFile, utteranceId)
        if (result != TextToSpeech.SUCCESS) outFile.delete()
    }

    private fun playWithPitch(file: File, pitch: Float) {
        releasePlayer()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { mp ->
                mp.playbackParams = PlaybackParams().setPitch(pitch).setSpeed(1.0f)
                mp.start()
            }
            setOnCompletionListener {
                releasePlayer()
                file.delete()
            }
            setOnErrorListener { _, _, _ ->
                releasePlayer()
                file.delete()
                true
            }
            prepareAsync()
        }
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    fun stop() {
        engine?.stop()
        player?.stop()
        releasePlayer()
    }

    fun shutdown() {
        stop()
        engine?.shutdown()
        engine = null
    }

    /** Fallback shaping for engines with no distinct male/female voices to select from. */
    private fun pitchFor(voiceType: VoiceType): Float = when (voiceType) {
        VoiceType.MALE_COACH -> 0.75f
        VoiceType.FEMALE_COACH -> 1.35f
        VoiceType.DEVICE_TTS, VoiceType.CUSTOM -> 1.0f
    }

    companion object {
        const val SAMPLE_SENTENCE = "Ready to go! Next exercise: Jumping Jacks"
        private const val SPEECH_RATE = 1.0f
    }
}
