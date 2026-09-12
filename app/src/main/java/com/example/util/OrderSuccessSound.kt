package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

object OrderSuccessSound {
    private const val TAG = "OrderSuccessSound"

    /**
     * Plays a pleasant, professional, short 3-note ascending success chime
     * (C5 -> E5 -> G5, ~350ms duration) with smooth decay.
     * Guaranteed to execute cleanly on background thread, without blocking UI.
     */
    suspend fun playSuccessChime(context: Context? = null) = withContext(Dispatchers.Default) {
        var audioTrack: AudioTrack? = null
        try {
            val sampleRate = 44100
            val totalDurationMs = 380
            val numSamples = (sampleRate * (totalDurationMs / 1000.0)).toInt()
            val samples = ShortArray(numSamples)

            // 3 notes: C5 (523.25 Hz), E5 (659.25 Hz), G5 (783.99 Hz)
            val note1End = (numSamples * 0.28).toInt()
            val note2End = (numSamples * 0.58).toInt()

            for (i in 0 until numSamples) {
                val freq = when {
                    i < note1End -> 523.25
                    i < note2End -> 659.25
                    else -> 783.99
                }
                val localProgress = when {
                    i < note1End -> i.toDouble() / note1End
                    i < note2End -> (i - note1End).toDouble() / (note2End - note1End)
                    else -> (i - note2End).toDouble() / (numSamples - note2End)
                }

                // Envelope: quick attack, smooth decay
                val envelope = when {
                    localProgress < 0.1 -> localProgress / 0.1
                    else -> (1.0 - localProgress) * 0.95 + 0.05
                }

                // Sine wave
                val angle = 2.0 * PI * i / (sampleRate / freq)
                val sampleValue = (sin(angle) * envelope * 14000.0).toInt().coerceIn(-32767, 32767)
                samples[i] = sampleValue.toShort()
            }

            val bufferSize = samples.size * 2
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()

            // Wait for duration before releasing
            kotlinx.coroutines.delay(totalDurationMs.toLong() + 50L)
        } catch (e: Exception) {
            Log.w(TAG, "Order placed sound playback: ${e.message}")
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (ignored: Exception) { }
        }
    }
}
