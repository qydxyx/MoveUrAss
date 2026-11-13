package com.kegeltrainer.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@Singleton
class SoundCoach @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Volatile
    private var audioFocusRequest: AudioFocusRequest? = null

    // Pre-synthesized completion sound buffer (E5 659Hz -> A5 880Hz chime with harmonics and decay)
    private val completionSoundPcm: ShortArray by lazy {
        synthesizeChimePcm()
    }

    fun playCompletionSound() {
        scope.launch {
            requestAudioFocus()
            val played = runCatching { playPcm(completionSoundPcm) }.getOrDefault(false)
            if (!played) {
                // Fallback to system notification sound if AudioTrack playback fails
                playSystemNotificationSound()
            }
            delay(750)
            abandonAudioFocus()
        }
    }

    private fun requestAudioFocus() {
        runCatching {
            val am = audioManager ?: return
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { }
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

    private fun playPcm(pcm: ShortArray): Boolean {
        val sampleRate = 44100
        val bufferSize = pcm.size * 2 // 2 bytes per short

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        var track: AudioTrack? = null
        return try {
            track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STATIC,
                )
            }

            track.write(pcm, 0, pcm.size)
            track.play()
            true
        } catch (e: Exception) {
            false
        } finally {
            // Clean up track after playback finishes
            track?.let { trk ->
                scope.launch {
                    delay(1000)
                    runCatching {
                        trk.stop()
                        trk.release()
                    }
                }
            }
        }
    }

    private fun playSystemNotificationSound() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        }
    }

    private fun synthesizeChimePcm(): ShortArray {
        val sampleRate = 44100
        val t1 = 0.14 // First note duration (s)
        val t2 = 0.46 // Second note duration (s)
        val totalSamples = (sampleRate * (t1 + t2)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal: Double = if (t < t1) {
                val freq = 659.25 // E5
                val decay = exp(-t * 8.0)
                val wave = 0.75 * sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t)
                wave * decay
            } else {
                val tNote = t - t1
                val freq = 880.00 // A5
                val decay = exp(-tNote * 4.5)
                val wave = 0.75 * sin(2.0 * PI * freq * tNote) + 0.25 * sin(4.0 * PI * freq * tNote)
                wave * decay
            }
            buffer[i] = (sampleVal.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
        }
        return buffer
    }
}
