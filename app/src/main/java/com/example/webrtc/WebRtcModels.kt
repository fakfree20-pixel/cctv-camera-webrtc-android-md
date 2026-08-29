package com.example.webrtc

enum class WebRtcConnectionState {
    IDLE,
    CONNECTING_SIGNALING,
    WAITING_PEER,
    EXCHANGING_SDP,
    CONNECTING_P2P,
    CONNECTED,
    DISCONNECTED,
    FAILED
}

enum class SignalingMessageType {
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    HEARTBEAT,
    COMMAND,
    ROOM_JOINED,
    LEAVE
}

data class SignalingMessage(
    val type: String, // "OFFER", "ANSWER", "ICE_CANDIDATE", "COMMAND", "HEARTBEAT"
    val senderId: String,
    val targetRoom: String,
    val sdp: String? = null,
    val sdpType: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val candidate: String? = null,
    val command: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
