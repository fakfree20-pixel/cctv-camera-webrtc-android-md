package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sin

class AudioStreamManager(private val context: Context) {
    private val TAG = "AudioStreamManager"

    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
    private val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null

    private var audioTrack: AudioTrack? = null
    private var sirenTrack: AudioTrack? = null
    private var isSirenRunning = false
    private var sirenJob: Job? = null

    // Listeners for outgoing microphone packets
    private val audioListeners = CopyOnWriteArrayList<(ByteArray) -> Unit>()

    fun addAudioListener(listener: (ByteArray) -> Unit) {
        audioListeners.add(listener)
    }

    fun removeAudioListener(listener: (ByteArray) -> Unit) {
        audioListeners.remove(listener)
    }

    @SuppressLint("MissingPermission")
    fun startMicrophoneStreaming(scope: CoroutineScope) {
        if (isRecording) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(1024)
                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val packet = buffer.copyOf(read)
                        for (listener in audioListeners) {
                            try {
                                listener(packet)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error sending audio to listener", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start microphone recording", e)
        }
    }

    fun stopMicrophoneStreaming() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null
    }

    // Play incoming audio chunks on phone speaker (Two-way audio)
    fun playSpeakerAudio(pcmData: ByteArray) {
        try {
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT)
                val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val format = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AUDIO_FORMAT)
                    .setChannelMask(CHANNEL_CONFIG_OUT)
                    .build()

                audioTrack = AudioTrack(
                    attributes,
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                audioTrack?.play()
            }
            audioTrack?.write(pcmData, 0, pcmData.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio on speaker", e)
        }
    }

    fun stopSpeakerAudio() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio track", e)
        }
        audioTrack = null
    }

    // Siren alarm generator
    fun startSiren(scope: CoroutineScope) {
        if (isSirenRunning) return
        isSirenRunning = true

        sirenJob = scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 22050
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf.coerceAtLeast(4096))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                sirenTrack = audioTrack
                audioTrack.play()

                var phase = 0.0
                val buffer = ShortArray(1024)
                var time = 0.0

                while (isActive && isSirenRunning) {
                    // Modulate frequency between 600Hz and 1400Hz (classic emergency siren)
                    val freq = 1000.0 + 400.0 * sin(2.0 * Math.PI * 1.5 * time)
                    for (i in buffer.indices) {
                        buffer[i] = (sin(phase) * 32000).toInt().toShort()
                        phase += 2.0 * Math.PI * freq / sampleRate
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }
                    }
                    time += buffer.size.toDouble() / sampleRate
                    audioTrack.write(buffer, 0, buffer.size)
                }

                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error generating siren sound", e)
            } finally {
                sirenTrack = null
                isSirenRunning = false
            }
        }
    }

    fun stopSiren() {
        isSirenRunning = false
        sirenJob?.cancel()
        sirenJob = null
        try {
            sirenTrack?.stop()
            sirenTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping siren", e)
        }
        sirenTrack = null
    }

    fun isSirenActive(): Boolean = isSirenRunning
}
