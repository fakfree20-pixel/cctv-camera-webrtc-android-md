package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppRole {
    SELECTION,
    CAMERA_DEVICE, // Old Phone (Camera)
    VIEWER_DEVICE, // New Phone (Viewer / Remote)
    SNAPSHOTS_GALLERY
}

enum class CameraLens {
    BACK,
    FRONT
}

data class CameraTelemetry(
    val cameraId: String = "",
    val ipAddress: String = "",
    val port: Int = 8080,
    val lens: CameraLens = CameraLens.BACK,
    val isTorchOn: Boolean = false,
    val isMicEnabled: Boolean = true,
    val isSirenPlaying: Boolean = false,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val motionDetected: Boolean = false,
    val motionCount: Int = 0,
    val connectedClients: Int = 0,
    val fps: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiscoveredCamera(
    val cameraId: String,
    val host: String,
    val port: Int,
    val deviceName: String = "CCTV Camera",
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_cameras")
data class SavedCamera(
    @PrimaryKey
    val cameraId: String,
    val host: String,
    val port: Int = 8080,
    val label: String = "CCTV Camera",
    val lastConnected: Long = System.currentTimeMillis()
)

@Entity(tableName = "snapshots")
data class SnapshotRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cameraId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String,
    val isMotionTriggered: Boolean = false,
    val note: String = ""
)

@Entity(tableName = "security_events")
data class SecurityEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cameraId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "MOTION_DETECTED", "VIEWER_CONNECTED", "SIREN_TRIGGERED"
    val description: String
)
