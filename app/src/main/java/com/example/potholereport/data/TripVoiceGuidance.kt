package com.example.potholereport.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Spoken turn-by-turn and pothole prompts during an active trip.
 * Mute is persisted so the user does not have to silence guidance every session.
 */
class TripVoiceGuidance(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        runCatching {
            tts?.language = Locale("en", "IN").takeIf { tts?.isLanguageAvailable(it) == TextToSpeech.LANG_AVAILABLE }
                ?: Locale.ENGLISH
        }
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || !ready || isMuted(appContext)) return
        runCatching {
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "citygrid-trip-${clean.hashCode()}")
        }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        ready = false
    }

    companion object {
        private const val PREFS = "trip_voice_guidance"
        private const val KEY_MUTED = "muted"

        fun isMuted(context: Context): Boolean =
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_MUTED, false)

        fun setMuted(context: Context, muted: Boolean) {
            context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MUTED, muted)
                .apply()
            if (muted) {
                // Best-effort stop if an instance is speaking via a fresh TTS is heavy;
                // callers should also stop their held instance.
            }
        }
    }
}
