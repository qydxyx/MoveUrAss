package com.kegeltrainer.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.kegeltrainer.app.domain.model.PhaseType
import com.kegeltrainer.app.domain.model.spokenCue
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsCoach @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val main = Handler(Looper.getMainLooper())
    private val pending = AtomicReference<String?>(null)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val utteranceSeq = AtomicLong(0L)
    private val activeUtteranceId = AtomicReference<String?>(null)

    @Volatile
    private var audioFocusRequest: AudioFocusRequest? = null

    @Volatile
    private var ready = false

    @Volatile
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
        val engine = tts
        if (ready && engine != null) {
            speakNow(engine, text)
        } else {
            pending.set(text)
            main.post {
                val current = tts
                if (current == null) {
                    bindEngine()
                } else if (ready) {
                    flushPending()
                }
            }
        }
    }

    fun stop() {
        pending.set(null)
        activeUtteranceId.set(null)
        tts?.stop()
        abandonAudioFocus()
    }

    fun shutdown() {
        main.removeCallbacksAndMessages(null)
        ready = false
        pending.set(null)
        activeUtteranceId.set(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        abandonAudioFocus()
    }

    private fun speakNow(engine: TextToSpeech, text: String) {
        val uttId = "utt_${utteranceSeq.incrementAndGet()}"
        activeUtteranceId.set(uttId)
        requestAudioFocus()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, speakParams(), uttId)
    }

    private fun requestAudioFocus() {
        runCatching {
            val am = audioManager ?: return
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { /* maintain focus */ }
                    .build()
                    .also { audioFocusRequest = it }
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        runCatching {
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
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
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        engine.setAudioAttributes(audioAttrs)
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    releaseFocusIfCurrent(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    releaseFocusIfCurrent(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    releaseFocusIfCurrent(utteranceId)
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (!interrupted) {
                        releaseFocusIfCurrent(utteranceId)
                    }
                }
            },
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

    private fun releaseFocusIfCurrent(utteranceId: String?) {
        if (utteranceId != null && activeUtteranceId.compareAndSet(utteranceId, null)) {
            abandonAudioFocus()
        }
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
        speakNow(engine, text)
    }

    private fun speakParams(): Bundle = Bundle().apply {
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
    }
}
