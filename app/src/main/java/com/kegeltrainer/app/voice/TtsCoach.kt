package com.kegeltrainer.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.spokenCue
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsCoach @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val main = Handler(Looper.getMainLooper())
    private val pending = AtomicReference<String?>(null)

    @Volatile
    private var ready = false

    private var tts: TextToSpeech? = null

    init {
        main.post { bindEngine() }
    }

    fun speakPhase(type: PhaseType, durationMs: Long = Long.MAX_VALUE) {
        speak(type.spokenCue(durationMs))
    }

    fun speakCountdown(sec: Int) {
        if (sec in 1..3) speak(sec.toString())
    }

    fun speak(text: String) {
        pending.set(text)
        main.post {
            if (ready) flushPending() else if (tts == null) bindEngine()
        }
    }

    fun shutdown() {
        main.post {
            ready = false
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    private fun bindEngine() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            main.post { onInit(status) }
        }
    }

    private fun onInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            ready = false
            tts = null
            return
        }
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        val locale = preferredLocale(engine)
        if (locale != null) engine.setLanguage(locale)
        engine.setSpeechRate(0.95f)
        engine.setPitch(1.0f)
        ready = true
        // Prime the engine; some OEM TTS drops the first utterance.
        engine.speak(" ", TextToSpeech.QUEUE_FLUSH, speakParams(), "warmup")
        main.postDelayed({ flushPending() }, 80)
    }

    private fun preferredLocale(engine: TextToSpeech): Locale? {
        val candidates = listOf(
            Locale.CHINA,
            Locale.SIMPLIFIED_CHINESE,
            Locale.CHINESE,
            Locale.TAIWAN,
            Locale.getDefault(),
        )
        return candidates.firstOrNull { locale ->
            engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
        }
    }

    private fun flushPending() {
        val text = pending.getAndSet(null) ?: return
        val engine = tts
        if (!ready || engine == null) {
            pending.compareAndSet(null, text)
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, speakParams(), text)
    }

    private fun speakParams(): Bundle = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
    }
}
