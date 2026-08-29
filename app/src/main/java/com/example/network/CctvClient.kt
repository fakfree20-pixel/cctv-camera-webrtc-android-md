package com.example.network

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.example.data.model.CameraLens
import com.example.data.model.CameraTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class CctvClient {
    private val TAG = "CctvClient"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for MJPEG stream
        .build()

    private val controlClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private var streamJob: Job? = null
    private var audioJob: Job? = null
    private var telemetryJob: Job? = null
    private var talkJob: Job? = null

    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame

    private val _telemetry = MutableStateFlow(CameraTelemetry())
    val telemetry: StateFlow<CameraTelemetry> = _telemetry

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isRemoteMicListening = MutableStateFlow(true)
    val isRemoteMicListening: StateFlow<Boolean> = _isRemoteMicListening

    private val _isTwoWayTalkActive = MutableStateFlow(false)
    val isTwoWayTalkActive: StateFlow<Boolean> = _isTwoWayTalkActive

    private var audioTrack: AudioTrack? = null
    private var talkAudioRecord: AudioRecord? = null

    var currentHost: String = ""
        private set
    var currentPort: Int = 8080
        private set

    fun connect(scope: CoroutineScope, host: String, port: Int = 8080) {
        disconnect()
        currentHost = host
        currentPort = port

        _isConnecting.value = true
        _errorMessage.value = null

        val baseUrl = "http://$host:$port"

        // 1. Start MJPEG Stream Reader
        streamJob = scope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/stream")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    _errorMessage.value = "Failed to connect: HTTP ${response.code}"
                    _isConnecting.value = false
                    _isConnected.value = false
                    return@launch
                }

                val inputStream = response.body?.byteStream()
                if (inputStream == null) {
                    _errorMessage.value = "Empty stream response"
                    _isConnecting.value = false
                    _isConnected.value = false
                    return@launch
                }

                _isConnected.value = true
                _isConnecting.value = false
                _errorMessage.value = null

                readMjpegStream(inputStream)
            } catch (e: Exception) {
                Log.e(TAG, "Stream connection failed", e)
                _errorMessage.value = "Connection error: ${e.localizedMessage ?: "Unknown error"}"
                _isConnected.value = false
                _isConnecting.value = false
            }
        }

        // 2. Start Audio Listener
        startAudioListener(scope, baseUrl)

        // 3. Start Telemetry Poller
        startTelemetryPoller(scope, baseUrl)
    }

    private fun readMjpegStream(inputStream: InputStream) {
        val boundary = "--cctv_frame_boundary"
        val buffer = ByteArray(4096)
        val streamBuffer = ByteArrayOutputStream()

        var prevByte = -1

        while (true) {
            val curByte = inputStream.read()
            if (curByte == -1) break

            // Detect JPEG SOI (0xFF, 0xD8) and EOI (0xFF, 0xD9)
            if (prevByte == 0xFF && curByte == 0xD8) {
                streamBuffer.reset()
                streamBuffer.write(0xFF)
                streamBuffer.write(0xD8)
            } else if (prevByte == 0xFF && curByte == 0xD9) {
                streamBuffer.write(0xD9)
                val jpegBytes = streamBuffer.toByteArray()
                if (jpegBytes.size > 100) {
                    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                    if (bitmap != null) {
                        _latestFrame.value = bitmap
                    }
                }
                streamBuffer.reset()
            } else {
                if (streamBuffer.size() < 1024 * 1024) { // Max 1MB frame buffer
                    streamBuffer.write(curByte)
                }
            }
            prevByte = curByte
        }
    }

    private fun startAudioListener(scope: CoroutineScope, baseUrl: String) {
        audioJob = scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 16000
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize.coerceAtLeast(2048))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val request = Request.Builder().url("$baseUrl/audio").build()
                val response = httpClient.newCall(request).execute()
                val input = response.body?.byteStream() ?: return@launch

                val buffer = ByteArray(1024)
                while (isActive && input.read(buffer).also { } != -1) {
                    val read = input.read(buffer)
                    if (read > 0 && _isRemoteMicListening.value) {
                        audioTrack?.write(buffer, 0, read)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Audio stream ended or failed", e)
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Exception) {}
                audioTrack = null
            }
        }
    }

    private fun startTelemetryPoller(scope: CoroutineScope, baseUrl: String) {
        telemetryJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val request = Request.Builder().url("$baseUrl/telemetry").build()
                    val response = controlClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            _telemetry.value = CameraTelemetry(
                                cameraId = json.optString("cameraId", ""),
                                ipAddress = json.optString("ipAddress", ""),
                                port = json.optInt("port", 8080),
                                lens = if (json.optString("lens") == "FRONT") CameraLens.FRONT else CameraLens.BACK,
                                isTorchOn = json.optBoolean("isTorchOn", false),
                                isMicEnabled = json.optBoolean("isMicEnabled", true),
                                isSirenPlaying = json.optBoolean("isSirenPlaying", false),
                                batteryLevel = json.optInt("batteryLevel", 100),
                                isCharging = json.optBoolean("isCharging", false),
                                motionDetected = json.optBoolean("motionDetected", false),
                                motionCount = json.optInt("motionCount", 0),
                                connectedClients = json.optInt("connectedClients", 1),
                                fps = json.optInt("fps", 0),
                                timestamp = json.optLong("timestamp", System.currentTimeMillis())
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Telemetry poll error", e)
                }
                delay(2000)
            }
        }
    }

    suspend fun sendCommand(action: String): Boolean = withContext(Dispatchers.IO) {
        if (currentHost.isBlank()) return@withContext false
        try {
            val url = "http://$currentHost:$currentPort/control?action=$action"
            val request = Request.Builder().url(url).build()
            val response = controlClient.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command $action", e)
            return@withContext false
        }
    }

    suspend fun fetchHighResSnapshot(): ByteArray? = withContext(Dispatchers.IO) {
        if (currentHost.isBlank()) return@withContext null
        try {
            val url = "http://$currentHost:$currentPort/snapshot"
            val request = Request.Builder().url(url).build()
            val response = controlClient.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext response.body?.bytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch snapshot", e)
        }
        return@withContext null
    }

    fun toggleRemoteMic(): Boolean {
        val newState = !_isRemoteMicListening.value
        _isRemoteMicListening.value = newState
        return newState
    }

    @SuppressLint("MissingPermission")
    fun startTwoWayTalk(scope: CoroutineScope) {
        if (_isTwoWayTalkActive.value) return
        _isTwoWayTalkActive.value = true

        talkJob = scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 16000
                val minBuffer = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = minBuffer.coerceAtLeast(2048)

                talkAudioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                talkAudioRecord?.startRecording()

                val buffer = ByteArray(1024)
                while (isActive && _isTwoWayTalkActive.value) {
                    val read = talkAudioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val pcmChunk = buffer.copyOf(read)
                        val body = pcmChunk.toRequestBody("application/octet-stream".toMediaType())
                        val request = Request.Builder()
                            .url("http://$currentHost:$currentPort/talk")
                            .post(body)
                            .build()
                        try {
                            controlClient.newCall(request).execute().close()
                        } catch (_: Exception) {}
                    }
                    delay(30)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in two-way talk recorder", e)
            } finally {
                try {
                    talkAudioRecord?.stop()
                    talkAudioRecord?.release()
                } catch (_: Exception) {}
                talkAudioRecord = null
                _isTwoWayTalkActive.value = false
            }
        }
    }

    fun stopTwoWayTalk() {
        _isTwoWayTalkActive.value = false
        talkJob?.cancel()
        talkJob = null
        try {
            talkAudioRecord?.stop()
            talkAudioRecord?.release()
        } catch (_: Exception) {}
        talkAudioRecord = null
    }

    fun toggleTwoWayTalk(scope: CoroutineScope) {
        if (_isTwoWayTalkActive.value) {
            stopTwoWayTalk()
        } else {
            startTwoWayTalk(scope)
        }
    }

    fun disconnect() {
        _isConnected.value = false
        _isConnecting.value = false
        streamJob?.cancel()
        streamJob = null
        audioJob?.cancel()
        audioJob = null
        telemetryJob?.cancel()
        telemetryJob = null
        stopTwoWayTalk()
        _latestFrame.value = null
    }
}
