package com.example.ui.strings

enum class AppLanguage {
    HINDI,
    ENGLISH
}

object AppStrings {
    // Mode Selection Screen
    fun appTitle(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "📱 Remote CCTV Control" else "📱 Remote CCTV Control"
    fun modeQuestion(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "यह डिवाइस क्या बनेगा?" else "What will this device be?"
    fun oldPhoneBtn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "पुराना फोन (Camera)" else "Old Phone (Camera)"
    fun oldPhoneDesc(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "इस फोन को CCTV सुरक्षा कैमरा बनाएं" else "Turn this device into a live CCTV camera"
    fun newPhoneBtn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "नया फोन (Viewer)" else "New Phone (Viewer)"
    fun newPhoneDesc(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "दूसरे फोन के कैमरे को लाइव देखें और कंट्रोल करें" else "Watch live feed & remotely control camera"
    fun snapshotsGalleryBtn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🖼️ स्नैपशॉट गैलरी" else "🖼️ Snapshots Gallery"

    // Camera Mode Screen
    fun cameraActiveTitle(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "📷 CCTV कैमरा सक्रिय है" else "📷 CCTV Camera Active"
    fun cameraIdLabel(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "Camera ID:" else "Camera ID:"
    fun waitingViewer(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "नए फोन के कनेक्ट होने का इंतज़ार..." else "Waiting for viewer phone to connect..."
    fun viewerConnected(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "✅ नया फोन कनेक्ट हो गया है!" else "✅ Viewer connected!"
    fun localUrl(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "स्थानीय URL:" else "Local Network URL:"
    fun powerSaverBtn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "ब्लैक स्क्रीन / बैटरी सेवर" else "Black Screen / Power Saver"
    fun powerSaverHint(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "स्क्रीन को जगाने के लिए कहीं भी टैप करें" else "Tap anywhere to wake up screen"
    fun motionDetectionToggle(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "गति पहचान (Motion Detection)" else "Motion Detection"
    fun testSiren(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🚨 सायरन टेस्ट" else "🚨 Test Siren"
    fun stopCamera(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "कैमरा बंद करें" else "Stop Camera"

    // Viewer Mode Screen
    fun remoteControlPanel(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🖥️ रिमोट कंट्रोल पैनल" else "🖥️ Remote Control Panel"
    fun enterCameraIdPlaceholder(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "Camera ID / IP दर्ज करें (e.g. 192.168.1.5)" else "Enter Camera ID or IP (e.g. 192.168.1.5)"
    fun connectBtn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "कनेक्ट करें" else "Connect"
    fun disconnectBtn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "डिस्कनेक्ट" else "Disconnect"
    fun discoveredCamerasTitle(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "आस-पास मिले कैमरे (Auto-Detected):" else "Nearby Cameras (Auto-Detected):"
    fun savedCamerasTitle(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "हाल के कैमरे:" else "Recent Cameras:"

    // Remote Controls
    fun remoteSwitchCamera(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🔄 रिमोट कैमरा बदलें (Front/Back)" else "🔄 Switch Remote Camera (Front/Back)"
    fun remoteMicOn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🎙️ पुराना फोन का माइक (ON)" else "🎙️ Camera Mic (ON)"
    fun remoteMicOff(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🔇 पुराना फोन का माइक (OFF)" else "🔇 Camera Mic (OFF)"
    fun viewerMicOn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🗣️ खुद बोलें (Two-Way Audio: ON)" else "🗣️ Talk Back (Two-Way Audio: ON)"
    fun viewerMicOff(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🗣️ खुद बोलें (Two-Way Audio: OFF)" else "🗣️ Talk Back (Two-Way Audio: OFF)"
    fun remoteTorchOn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🔦 रिमोट टॉर्च (ON)" else "🔦 Remote Torch (ON)"
    fun remoteTorchOff(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🔦 रिमोट टॉर्च (OFF)" else "🔦 Remote Torch (OFF)"
    fun remoteSirenOn(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🚨 रिमोट सायरन बजाएं" else "🚨 Sound Siren Alarm"
    fun remoteSirenOff(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🔇 सायरन बंद करें" else "🔇 Stop Siren"
    fun takeSnapshot(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "📸 स्नैपशॉट लें" else "📸 Take Snapshot"
    fun recordStream(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🔴 रिकॉर्डिंग" else "🔴 Record"

    // Telemetry & Status
    fun battery(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "बैटरी" else "Battery"
    fun liveBadge(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "● लाइव" else "● LIVE"
    fun liveWebRtcBadge(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "● WebRTC P2P (मोबाइल डेटा 4G/5G)" else "● WebRTC P2P (Mobile Data)"
    fun motionAlertDetected(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "⚠️ गति पकड़ी गई!" else "⚠️ Motion Detected!"
    fun noSnapshots(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "अभी कोई स्नैपशॉट नहीं है" else "No snapshots yet"

    // WebRTC & Mobile Data Connection
    fun webrtcRoomCode(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "मोबाइल डेटा रूम कोड (WebRTC PIN):" else "Mobile Data Room PIN (WebRTC):"
    fun webrtcRoomCodeHint(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "दूसरे मोबाइल में यह 6-अंकों का कोड डालकर 4G/5G पर कहीं से भी लाइव देखें" else "Enter this 6-digit PIN on the viewer device to stream over 4G/5G from anywhere"
    fun webrtcTab(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "🌐 मोबाइल डेटा (WebRTC 4G/5G)" else "🌐 Mobile Data (WebRTC)"
    fun lanTab(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "📶 लोकल वाई-फ़ाई / हॉटस्पॉट" else "📶 Local Wi-Fi / Hotspot"
    fun webrtcPinPlaceholder(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "6-अंकों का रूम कोड दर्ज करें (e.g. 786 123)" else "Enter 6-digit Room PIN (e.g. 786 123)"
    fun webrtcConnecting(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "WebRTC P2P से कनेक्ट हो रहा है..." else "Connecting via WebRTC P2P..."
    fun webrtcLowLatency(lang: AppLanguage) = if (lang == AppLanguage.HINDI) "अल्ट्रा-लो लेटेंसी (<150ms) एचडी ऑडियो-वीडियो" else "Ultra-low latency (<150ms) HD Video & Audio"
}
