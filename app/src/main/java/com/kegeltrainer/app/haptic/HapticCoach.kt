package com.kegeltrainer.app.haptic

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.kegeltrainer.app.domain.model.PhaseType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticCoach @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun onPhase(type: PhaseType) {
        val effect = when (type) {
            PhaseType.PREPARE -> oneShot(80, 120)
            PhaseType.CONTRACT -> waveform(longArrayOf(0, 40, 50, 70), intArrayOf(0, 220, 0, 255), -1)
            PhaseType.HOLD -> oneShot(35, 90)
            PhaseType.RELAX -> waveform(longArrayOf(0, 160), intArrayOf(0, 80), -1)
            PhaseType.REST -> return
        }
        vibrateEffect(effect)
    }

    fun onComplete() {
        val effect = waveform(
            longArrayOf(0, 100, 80, 100, 80, 220),
            intArrayOf(0, 180, 0, 220, 0, 255),
            -1,
        )
        vibrateEffect(effect)
    }

    private fun vibrateEffect(effect: VibrationEffect) {
        val vib = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_MEDIA)
                    .build()
                vib.vibrate(effect, attrs)
            } else {
                @Suppress("DEPRECATION")
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vib.vibrate(effect, attrs)
            }
        }
    }

    private fun oneShot(ms: Long, amplitude: Int): VibrationEffect =
        VibrationEffect.createOneShot(ms, amplitude.coerceIn(1, 255))

    private fun waveform(timings: LongArray, amps: IntArray, repeat: Int): VibrationEffect =
        VibrationEffect.createWaveform(timings, amps, repeat)
}
