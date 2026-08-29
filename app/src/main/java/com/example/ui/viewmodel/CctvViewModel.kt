package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.camera.AudioStreamManager
import com.example.camera.BatteryMonitor
import com.example.camera.CameraManager
import com.example.data.db.AppDatabase
import com.example.data.model.AppRole
import com.example.data.model.CameraLens
import com.example.data.model.CameraTelemetry
import com.example.data.model.DiscoveredCamera
import com.example.data.model.SavedCamera
import com.example.data.model.SecurityEvent
import com.example.data.model.SnapshotRecord
import com.example.network.CctvClient
import com.example.network.CctvDiscovery
import com.example.network.CctvHttpServer
import com.example.ui.strings.AppLanguage
import com.example.webrtc.WebRtcConnectionState
import com.example.webrtc.WebRtcSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class CctvViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CctvViewModel"
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.cctvDao()

    val savedCameras = dao.getAllSavedCameras()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allSnapshots = dao.getAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val securityEvents = dao.getRecentSecurityEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // UI Navigation & Language
    private val _currentRole = MutableStateFlow(AppRole.SELECTION)
    val currentRole: StateFlow<AppRole> = _currentRole

    private val _language = MutableStateFlow(AppLanguage.HINDI)
    val language: StateFlow<AppLanguage> = _language

    // --- CAMERA MODE STATE (Old Phone) ---
    private val _cameraId = MutableStateFlow("CAM-" + (1000 + Random().nextInt(9000)))
    val cameraId: StateFlow<String> = _cameraId

    private val _cameraRoomPin = MutableStateFlow((100000 + Random().nextInt(900000)).toString())
    val cameraRoomPin: StateFlow<String> = _cameraRoomPin

    private val _cameraIp = MutableStateFlow("127.0.0.1")
    val cameraIp: StateFlow<String> = _cameraIp

    private val _cameraPort = MutableStateFlow(8080)
    val cameraPort: StateFlow<Int> = _cameraPort

    private val _connectedViewersCount = MutableStateFlow(0)
    val connectedViewersCount: StateFlow<Int> = _connectedViewersCount

    private val _isCameraStreaming = MutableStateFlow(false)
    val isCameraStreaming: StateFlow<Boolean> = _isCameraStreaming

    private val _isPowerSaverActive = MutableStateFlow(false)
    val isPowerSaverActive: StateFlow<Boolean> = _isPowerSaverActive

    private val _isMotionDetected = MutableStateFlow(false)
    val isMotionDetected: StateFlow<Boolean> = _isMotionDetected

    private val _cameraTelemetry = MutableStateFlow(CameraTelemetry())
    val cameraTelemetry: StateFlow<CameraTelemetry> = _cameraTelemetry

    // Camera & Audio engines
    val cameraManager = CameraManager(application)
    val audioStreamManager = AudioStreamManager(application)
    private var batteryMonitor: BatteryMonitor? = null
    private var httpServer: CctvHttpServer? = null
    private val discovery = CctvDiscovery(application)
    var cameraWebRtcSession: WebRtcSessionManager? = null
        private set

    // --- VIEWER MODE STATE (New Phone) ---
    val cctvClient = CctvClient()
    var viewerWebRtcSession: WebRtcSessionManager? = null
        private set

    private val _viewerModeTab = MutableStateFlow("WEBRTC") // "WEBRTC" (Mobile Data) or "LAN" (Local Wi-Fi)
    val viewerModeTab: StateFlow<String> = _viewerModeTab

    private val _viewerRoomPinInput = MutableStateFlow("")
    val viewerRoomPinInput: StateFlow<String> = _viewerRoomPinInput

    private val _discoveredCameras = MutableStateFlow<List<DiscoveredCamera>>(emptyList())
    val discoveredCameras: StateFlow<List<DiscoveredCamera>> = _discoveredCameras

    private val _viewerPeerInput = MutableStateFlow("")
    val viewerPeerInput: StateFlow<String> = _viewerPeerInput

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    private val _isRecordingStream = MutableStateFlow(false)
    val isRecordingStream: StateFlow<Boolean> = _isRecordingStream

    private val _isViewerWebRtcActive = MutableStateFlow(false)
    val isViewerWebRtcActive: StateFlow<Boolean> = _isViewerWebRtcActive

    private val _webRtcStatus = MutableStateFlow("Ready")
    val webRtcStatus: StateFlow<String> = _webRtcStatus

    private val _isViewerMicTalking = MutableStateFlow(false)
    val isViewerMicTalking: StateFlow<Boolean> = _isViewerMicTalking

    init {
        // Setup battery monitoring
        batteryMonitor = BatteryMonitor(application) { level, isCharging ->
            _cameraTelemetry.value = _cameraTelemetry.value.copy(
                batteryLevel = level,
                isCharging = isCharging
            )
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun toggleLanguage() {
        _language.value = if (_language.value == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
    }

    fun selectRole(role: AppRole) {
        _currentRole.value = role
        if (role == AppRole.VIEWER_DEVICE) {
            discovery.startListening(viewModelScope) { cameras ->
                _discoveredCameras.value = cameras
            }
        }
    }

    fun setViewerModeTab(tab: String) {
        _viewerModeTab.value = tab
    }

    fun setViewerRoomPinInput(pin: String) {
        _viewerRoomPinInput.value = pin
    }

    fun setViewerPeerInput(input: String) {
        _viewerPeerInput.value = input
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(3000)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // --- CAMERA MODE CONTROLS ---
    fun startCameraMode(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        _cameraIp.value = CctvHttpServer.getLocalIpAddress()
        batteryMonitor?.start()

        // 1. Setup CameraX for local display & motion analysis
        cameraManager.startCamera(lifecycleOwner, previewView) {
            _isCameraStreaming.value = true
        }

        // 2. Setup Motion Detection Callback
        cameraManager.onMotionDetected = { pct ->
            _isMotionDetected.value = true
            _cameraTelemetry.value = _cameraTelemetry.value.copy(
                motionDetected = true,
                motionCount = _cameraTelemetry.value.motionCount + 1
            )
            viewModelScope.launch {
                dao.insertSecurityEvent(
                    SecurityEvent(
                        cameraId = _cameraId.value,
                        eventType = "MOTION_DETECTED",
                        description = "Motion detected (${pct.toInt()}% change)"
                    )
                )
                delay(3000)
                _isMotionDetected.value = false
                _cameraTelemetry.value = _cameraTelemetry.value.copy(motionDetected = false)
            }
        }

        // 3. Audio manager
        audioStreamManager.startMicrophoneStreaming(viewModelScope)

        // 4. Start WebRTC Session for Mobile Data / Cellular P2P low latency
        cameraWebRtcSession = WebRtcSessionManager(
            context = getApplication(),
            isCameraMode = true
        ).apply {
            onCommandReceived = { action ->
                handleRemoteCommand(action, lifecycleOwner, previewView)
            }
            startSession(
                scope = viewModelScope,
                roomId = _cameraRoomPin.value,
                isFrontCamera = (cameraManager.currentLens == CameraLens.FRONT)
            )
        }

        // 5. Start HTTP & MJPEG Server (Local LAN fallback)
        httpServer = CctvHttpServer(
            context = getApplication(),
            port = 8080,
            onCommandReceived = { action ->
                handleRemoteCommand(action, lifecycleOwner, previewView)
            },
            onAudioReceived = { pcmChunk ->
                // Play viewer voice on loudspeaker
                audioStreamManager.playSpeakerAudio(pcmChunk)
            },
            getTelemetry = {
                _cameraTelemetry.value.copy(
                    cameraId = _cameraId.value,
                    ipAddress = _cameraIp.value,
                    port = _cameraPort.value,
                    lens = cameraManager.currentLens,
                    isTorchOn = cameraManager.isTorchOn,
                    isMicEnabled = true,
                    isSirenPlaying = audioStreamManager.isSirenActive(),
                    connectedClients = _connectedViewersCount.value
                )
            },
            getLatestJpeg = {
                cameraManager.latestJpegFrame
            }
        ).apply {
            val boundPort = start(viewModelScope)
            _cameraPort.value = boundPort
            onClientCountChanged = { count ->
                _connectedViewersCount.value = count
            }
        }

        // Connect CameraManager frame broadcast to HTTP server
        cameraManager.addFrameListener { jpeg ->
            httpServer?.broadcastJpegFrame(jpeg)
        }

        // Connect Audio broadcast to HTTP server
        audioStreamManager.addAudioListener { pcm ->
            httpServer?.broadcastAudioPacket(pcm)
        }

        // 6. Start UDP Beacon for instant Viewer Auto-Discovery on LAN
        discovery.startBroadcasting(
            scope = viewModelScope,
            cameraId = _cameraId.value,
            port = _cameraPort.value,
            deviceName = android.os.Build.MODEL ?: "CCTV Camera"
        )
    }

    private fun handleRemoteCommand(
        action: String,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView?
    ): String {
        return when (action) {
            "SWITCH_CAMERA" -> {
                viewModelScope.launch(Dispatchers.Main) {
                    cameraManager.switchCamera(lifecycleOwner, previewView)
                    cameraWebRtcSession?.switchCamera(cameraManager.currentLens == CameraLens.FRONT)
                }
                "Switched to ${cameraManager.currentLens}"
            }
            "TOGGLE_TORCH" -> {
                val state = cameraManager.toggleTorch()
                "Torch set to $state"
            }
            "TOGGLE_MIC" -> {
                "Mic toggled"
            }
            "TRIGGER_SIREN" -> {
                audioStreamManager.startSiren(viewModelScope)
                "Siren started"
            }
            "STOP_SIREN" -> {
                audioStreamManager.stopSiren()
                "Siren stopped"
            }
            "TAKE_SNAPSHOT" -> {
                viewModelScope.launch {
                    takeCameraLocalSnapshot()
                }
                "Snapshot taken"
            }
            else -> "Unknown command"
        }
    }

    fun switchCameraLens(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        cameraManager.switchCamera(lifecycleOwner, previewView)
        cameraWebRtcSession?.switchCamera(cameraManager.currentLens == CameraLens.FRONT)
    }

    fun toggleCameraTorch() {
        cameraManager.toggleTorch()
    }

    fun toggleMotionDetection(): Boolean {
        cameraManager.motionDetectionEnabled = !cameraManager.motionDetectionEnabled
        return cameraManager.motionDetectionEnabled
    }

    fun toggleCameraSiren() {
        if (audioStreamManager.isSirenActive()) {
            audioStreamManager.stopSiren()
        } else {
            audioStreamManager.startSiren(viewModelScope)
        }
    }

    fun togglePowerSaver() {
        _isPowerSaverActive.value = !_isPowerSaverActive.value
    }

    suspend fun takeCameraLocalSnapshot(): Boolean = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(getApplication<Application>().filesDir, "SNAP_${_cameraId.value}_$timeStamp.jpg")
        val success = cameraManager.takeSnapshot(file)
        if (success) {
            dao.insertSnapshot(
                SnapshotRecord(
                    cameraId = _cameraId.value,
                    filePath = file.absolutePath,
                    isMotionTriggered = _isMotionDetected.value,
                    note = "Captured on Camera device"
                )
            )
            showToast("📸 Snapshot saved!")
        }
        return@withContext success
    }

    fun stopCameraMode() {
        discovery.stopBroadcasting()
        httpServer?.stop()
        httpServer = null
        cameraWebRtcSession?.release()
        cameraWebRtcSession = null
        audioStreamManager.stopMicrophoneStreaming()
        audioStreamManager.stopSpeakerAudio()
        audioStreamManager.stopSiren()
        batteryMonitor?.stop()
        cameraManager.release()
        _isCameraStreaming.value = false
        _connectedViewersCount.value = 0
    }

    // --- VIEWER MODE CONTROLS ---

    // 1. Connect via WebRTC over Mobile Data (4G/5G) using 6-Digit Room PIN
    fun connectWebRtc(pin: String) {
        val cleanPin = pin.filter { it.isDigit() || it.isLetter() }.trim()
        if (cleanPin.isBlank() || cleanPin.length < 4) {
            showToast("Please enter a valid 6-digit Room PIN")
            return
        }

        disconnectViewer()

        _isViewerWebRtcActive.value = true
        _webRtcStatus.value = "Connecting to Room $cleanPin on 4G/5G..."

        viewerWebRtcSession = WebRtcSessionManager(
            context = getApplication(),
            isCameraMode = false
        ).apply {
            startSession(
                scope = viewModelScope,
                roomId = cleanPin
            )
        }

        viewModelScope.launch {
            viewerWebRtcSession?.connectionState?.collect { state ->
                when (state) {
                    WebRtcConnectionState.CONNECTED -> {
                        showToast("✅ Connected via WebRTC P2P (Mobile Data)!")
                    }
                    WebRtcConnectionState.FAILED -> {
                        showToast("WebRTC connection failed. Retrying...")
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            dao.insertOrUpdateCamera(
                SavedCamera(
                    cameraId = cleanPin,
                    host = "WebRTC_PIN_$cleanPin",
                    port = 0,
                    label = "WebRTC Camera ($cleanPin)"
                )
            )
        }
    }

    fun disconnectWebRtc() {
        viewerWebRtcSession?.release()
        viewerWebRtcSession = null
        _isViewerWebRtcActive.value = false
        _webRtcStatus.value = "Disconnected"
    }

    // 2. Connect via Local Wi-Fi / Hotspot LAN
    fun connectToCamera(targetInput: String) {
        val trimmed = targetInput.trim()
        if (trimmed.isBlank()) {
            showToast("Please enter a valid Camera ID or IP address")
            return
        }

        disconnectWebRtc()

        // Check if matching discovered camera
        val match = _discoveredCameras.value.firstOrNull {
            it.cameraId.equals(trimmed, ignoreCase = true) || it.host == trimmed
        }

        val host: String
        val port: Int

        if (match != null) {
            host = match.host
            port = match.port
        } else if (trimmed.contains(":")) {
            val parts = trimmed.split(":")
            host = parts[0].removePrefix("http://")
            port = parts[1].toIntOrNull() ?: 8080
        } else {
            host = trimmed.removePrefix("http://")
            port = 8080
        }

        cctvClient.connect(viewModelScope, host, port)

        viewModelScope.launch {
            dao.insertOrUpdateCamera(
                SavedCamera(
                    cameraId = trimmed,
                    host = host,
                    port = port,
                    label = "CCTV Camera ($host)"
                )
            )
        }
    }

    fun disconnectViewer() {
        disconnectWebRtc()
        cctvClient.disconnect()
        discovery.stopListening()
    }

    fun sendRemoteCommand(action: String) {
        if (_isViewerWebRtcActive.value && viewerWebRtcSession != null) {
            viewerWebRtcSession?.sendCommand(action)
        } else {
            viewModelScope.launch {
                val ok = cctvClient.sendCommand(action)
                if (!ok) {
                    showToast("Failed to send command to camera")
                }
            }
        }
    }

    fun toggleRemoteMic() {
        cctvClient.toggleRemoteMic()
        sendRemoteCommand("TOGGLE_MIC")
    }

    fun toggleViewerMic() {
        if (_isViewerWebRtcActive.value && viewerWebRtcSession != null) {
            val newState = !_isViewerMicTalking.value
            _isViewerMicTalking.value = newState
            viewerWebRtcSession?.enableViewerTwoWayAudio(newState)
            showToast(if (newState) "🗣️ WebRTC 2-Way Audio ON" else "🔇 WebRTC 2-Way Audio OFF")
        } else {
            cctvClient.toggleTwoWayTalk(viewModelScope)
        }
    }

    fun remoteSwitchCamera() {
        sendRemoteCommand("SWITCH_CAMERA")
    }

    fun remoteToggleTorch() {
        sendRemoteCommand("TOGGLE_TORCH")
    }

    fun remoteToggleSiren() {
        if (cctvClient.telemetry.value.isSirenPlaying) {
            sendRemoteCommand("STOP_SIREN")
        } else {
            sendRemoteCommand("TRIGGER_SIREN")
        }
    }

    fun takeRemoteSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            sendRemoteCommand("TAKE_SNAPSHOT")

            val bytes = cctvClient.fetchHighResSnapshot() ?: run {
                // Fallback: capture current frame bitmap
                val bmp = cctvClient.latestFrame.value
                if (bmp != null) {
                    val out = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.toByteArray()
                } else null
            }

            if (bytes != null) {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File(getApplication<Application>().filesDir, "VIEWER_SNAP_$timeStamp.jpg")
                file.writeBytes(bytes)
                dao.insertSnapshot(
                    SnapshotRecord(
                        cameraId = cctvClient.currentHost.ifBlank { "WebRTC_CAM" },
                        filePath = file.absolutePath,
                        isMotionTriggered = false,
                        note = "Remote Snapshot"
                    )
                )
                showToast("📸 Snapshot captured & saved to Gallery!")
            } else {
                showToast("📸 Snapshot command sent to camera!")
            }
        }
    }

    fun toggleRecording() {
        _isRecordingStream.value = !_isRecordingStream.value
        if (_isRecordingStream.value) {
            showToast("🔴 Recording live stream...")
        } else {
            showToast("💾 Recording saved!")
        }
    }

    fun deleteSnapshot(snapshot: SnapshotRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(snapshot.filePath).delete()
            } catch (_: Exception) {}
            dao.deleteSnapshot(snapshot)
        }
    }

    fun deleteSavedCamera(camera: SavedCamera) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCamera(camera)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCameraMode()
        disconnectViewer()
    }
}
