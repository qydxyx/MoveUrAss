package com.kegeltrainer.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.spokenCue
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsCoach @Inject constructor(
    @ApplicationContext context: Context,
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.CHINESE)
                }
                tts?.setSpeechRate(0.95f)
            }
        }
    }

    fun speakPhase(type: PhaseType) {
        speak(type.spokenCue())
    }

    fun speakCountdown(sec: Int) {
        if (sec in 1..3) speak(sec.toString())
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
