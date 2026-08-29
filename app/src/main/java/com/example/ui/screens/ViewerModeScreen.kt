package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CameraLens
import com.example.ui.components.BatteryStatusChip
import com.example.ui.components.WebRtcVideoPlayer
import com.example.ui.strings.AppLanguage
import com.example.ui.strings.AppStrings
import com.example.webrtc.WebRtcConnectionState
import com.example.ui.theme.CctvAlertRed
import com.example.ui.theme.CctvCardBg
import com.example.ui.theme.CctvCardBgSecondary
import com.example.ui.theme.CctvCardBorder
import com.example.ui.theme.CctvDarkBg
import com.example.ui.theme.CctvGlassBorder
import com.example.ui.theme.CctvIceBlue
import com.example.ui.theme.CctvNavyDark
import com.example.ui.theme.CctvNavyHover
import com.example.ui.theme.CctvNavyPrimary
import com.example.ui.theme.CctvPrimaryCyan
import com.example.ui.theme.CctvSuccessGreen
import com.example.ui.theme.CctvTextMuted
import com.example.ui.theme.CctvTextPrimary
import com.example.ui.theme.CctvTextSecondary
import com.example.ui.viewmodel.CctvViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewerModeScreen(
    viewModel: CctvViewModel,
    language: AppLanguage,
    onBackToSelection: () -> Unit
) {
    val scrollState = rememberScrollState()

    val isConnected by viewModel.cctvClient.isConnected.collectAsState()
    val isConnecting by viewModel.cctvClient.isConnecting.collectAsState()
    val errorMessage by viewModel.cctvClient.errorMessage.collectAsState()
    val latestFrame by viewModel.cctvClient.latestFrame.collectAsState()
    val telemetry by viewModel.cctvClient.telemetry.collectAsState()

    val isRemoteMicOn by viewModel.cctvClient.isRemoteMicListening.collectAsState()
    val isViewerMicOn by viewModel.cctvClient.isTwoWayTalkActive.collectAsState()
    val isRecording by viewModel.isRecordingStream.collectAsState()

    val discoveredCameras by viewModel.discoveredCameras.collectAsState()
    val savedCameras by viewModel.savedCameras.collectAsState()
    val peerInput by viewModel.viewerPeerInput.collectAsState()

    val viewerModeTab by viewModel.viewerModeTab.collectAsState()
    val roomPinInput by viewModel.viewerRoomPinInput.collectAsState()
    val isViewerWebRtcActive by viewModel.isViewerWebRtcActive.collectAsState()
    val isViewerMicTalking by viewModel.isViewerMicTalking.collectAsState()

    val webRtcSession = viewModel.viewerWebRtcSession
    val webRtcVideoTrack by (webRtcSession?.remoteVideoTrack?.collectAsState() ?: remember { mutableStateOf(null) })
    val webRtcConnState by (webRtcSession?.connectionState?.collectAsState() ?: remember { mutableStateOf(WebRtcConnectionState.IDLE) })

    val isAnyConnected = isConnected || (isViewerWebRtcActive && webRtcConnState == WebRtcConnectionState.CONNECTED)
    val isAnyConnecting = isConnecting || (isViewerWebRtcActive && (
        webRtcConnState == WebRtcConnectionState.CONNECTING_SIGNALING ||
        webRtcConnState == WebRtcConnectionState.WAITING_PEER ||
        webRtcConnState == WebRtcConnectionState.EXCHANGING_SDP ||
        webRtcConnState == WebRtcConnectionState.CONNECTING_P2P
    ))

    // Zoom & pan state for video player
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CctvDarkBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CctvNavyPrimary)
                        .border(1.dp, CctvGlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "CCTV Remote",
                        tint = CctvIceBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = AppStrings.remoteControlPanel(language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CctvTextPrimary
                        )
                    )
                    Text(
                        text = if (isAnyConnected) {
                            if (isViewerWebRtcActive) "WEBRTC P2P • MOBILE DATA 4G/5G" else "CONNECTED TO ${telemetry.ipAddress.ifBlank { "CAM" }}"
                        } else "READY TO CONNECT",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isAnyConnected) CctvSuccessGreen else CctvTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            if (isAnyConnected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CctvSuccessGreen)
                        )
                        Text(
                            text = if (isViewerWebRtcActive) "4G/5G" else "LAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CctvSuccessGreen
                        )
                    }

                    Button(
                        onClick = {
                            if (isViewerWebRtcActive) {
                                viewModel.disconnectWebRtc()
                            } else {
                                viewModel.disconnectViewer()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CctvAlertRed.copy(alpha = 0.2f),
                            contentColor = CctvAlertRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("disconnect_button")
                    ) {
                        Text(
                            text = AppStrings.disconnectBtn(language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 1. CONNECT FORM (When not connected) ---
        if (!isAnyConnected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CctvCardBorder, RoundedCornerShape(24.dp))
                    .testTag("connectForm"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CctvCardBg)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Mode Selection Tabs: WebRTC Mobile Data (Default) vs Local Wi-Fi
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CctvCardBgSecondary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TabRow(
                            selectedTabIndex = if (viewerModeTab == "WEBRTC") 0 else 1,
                            containerColor = Color.Transparent,
                            contentColor = CctvIceBlue,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (viewerModeTab == "WEBRTC") 0 else 1]),
                                    color = CctvIceBlue
                                )
                            }
                        ) {
                            Tab(
                                selected = viewerModeTab == "WEBRTC",
                                onClick = { viewModel.setViewerModeTab("WEBRTC") },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SignalCellularAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (viewerModeTab == "WEBRTC") CctvIceBlue else CctvTextSecondary
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "मोबाइल डेटा (4G/5G)" else "Mobile Data (4G/5G)",
                                            fontSize = 12.sp,
                                            fontWeight = if (viewerModeTab == "WEBRTC") FontWeight.Bold else FontWeight.Normal,
                                            color = if (viewerModeTab == "WEBRTC") CctvIceBlue else CctvTextSecondary
                                        )
                                    }
                                }
                            )

                            Tab(
                                selected = viewerModeTab == "LAN",
                                onClick = { viewModel.setViewerModeTab("LAN") },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (viewerModeTab == "LAN") CctvIceBlue else CctvTextSecondary
                                        )
                                        Text(
                                            text = if (language == AppLanguage.HINDI) "लोकल वाई-फ़ाई" else "Local Wi-Fi",
                                            fontSize = 12.sp,
                                            fontWeight = if (viewerModeTab == "LAN") FontWeight.Bold else FontWeight.Normal,
                                            color = if (viewerModeTab == "LAN") CctvIceBlue else CctvTextSecondary
                                        )
                                    }
                                }
                            )
                        }
                    }

                    if (viewerModeTab == "WEBRTC") {
                        // --- WEBRTC MOBILE DATA SECTION ---
                        Text(
                            text = if (language == AppLanguage.HINDI) "📱 दोनों मोबाइल अपने-अपने मोबाइल डेटा (4G/5G) पर चलेंगे" else "📱 Stream between any two phones over mobile data",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CctvPrimaryCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Text(
                            text = AppStrings.webrtcRoomCodeHint(language),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CctvTextSecondary,
                                fontSize = 11.sp
                            )
                        )

                        // 6-Digit PIN input
                        OutlinedTextField(
                            value = roomPinInput,
                            onValueChange = { viewModel.setViewerRoomPinInput(it) },
                            placeholder = {
                                Text(
                                    text = AppStrings.webrtcPinPlaceholder(language),
                                    color = CctvTextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("webrtc-pin-input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CctvIceBlue,
                                unfocusedBorderColor = CctvCardBorder,
                                focusedTextColor = CctvTextPrimary,
                                unfocusedTextColor = CctvTextPrimary,
                                focusedContainerColor = CctvCardBgSecondary,
                                unfocusedContainerColor = CctvCardBgSecondary
                            )
                        )

                        // WebRTC Connect Button
                        Button(
                            onClick = { viewModel.connectWebRtc(roomPinInput) },
                            enabled = !isAnyConnecting && roomPinInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("connect_webrtc_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CctvNavyPrimary,
                                contentColor = CctvIceBlue
                            )
                        ) {
                            if (isAnyConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = CctvIceBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = AppStrings.webrtcConnecting(language),
                                    color = CctvIceBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SignalCellularAlt,
                                    contentDescription = null,
                                    tint = CctvIceBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.HINDI) "🚀 4G/5G पर लाइव देखें (Connect WebRTC)" else "🚀 Watch Live on Mobile Data",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CctvIceBlue
                                )
                            }
                        }
                    } else {
                        // --- LOCAL LAN / WI-FI SECTION ---
                        Text(
                            text = if (language == AppLanguage.HINDI) "कैमरे से कनेक्ट करें (लोकल वाई-फ़ाई / हॉटस्पॉट)" else "Connect to Remote Camera (Local Wi-Fi)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CctvTextPrimary,
                                fontSize = 16.sp
                            )
                        )

                        // Text Input
                        OutlinedTextField(
                            value = peerInput,
                            onValueChange = { viewModel.setViewerPeerInput(it) },
                            placeholder = {
                                Text(
                                    text = AppStrings.enterCameraIdPlaceholder(language),
                                    color = CctvTextMuted,
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("peer-id-input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CctvIceBlue,
                                unfocusedBorderColor = CctvCardBorder,
                                focusedTextColor = CctvTextPrimary,
                                unfocusedTextColor = CctvTextPrimary,
                                focusedContainerColor = CctvCardBgSecondary,
                                unfocusedContainerColor = CctvCardBgSecondary
                            )
                        )

                        // Connect Button
                        Button(
                            onClick = { viewModel.connectToCamera(peerInput) },
                            enabled = !isConnecting && peerInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("connect_to_camera_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CctvNavyPrimary,
                                contentColor = CctvIceBlue
                            )
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = CctvIceBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "Connecting...", color = CctvIceBlue, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CastConnected,
                                    contentDescription = null,
                                    tint = CctvIceBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = AppStrings.connectBtn(language),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CctvIceBlue
                                )
                            }
                        }
                    }

                    errorMessage?.let { err ->
                        Surface(
                            color = CctvAlertRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CctvAlertRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = CctvAlertRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Auto-Discovered Cameras on LAN / Wi-Fi
                    if (discoveredCameras.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LAN BEACON DETECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CctvIceBlue,
                            letterSpacing = 1.2.sp
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            discoveredCameras.forEach { cam ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CctvNavyPrimary,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CctvNavyHover),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.setViewerPeerInput(cam.host)
                                            viewModel.connectToCamera(cam.host)
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = CctvSuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${cam.cameraId} (${cam.host})",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recent Saved Cameras History
                    if (savedCameras.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SAVED RECENTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CctvTextSecondary,
                            letterSpacing = 1.2.sp
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            savedCameras.take(4).forEach { saved ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CctvCardBgSecondary,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CctvCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (saved.host.startsWith("WebRTC_PIN_")) {
                                                val pin = saved.cameraId
                                                viewModel.setViewerModeTab("WEBRTC")
                                                viewModel.setViewerRoomPinInput(pin)
                                                viewModel.connectWebRtc(pin)
                                            } else {
                                                viewModel.setViewerModeTab("LAN")
                                                viewModel.setViewerPeerInput(saved.host)
                                                viewModel.connectToCamera(saved.host)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = CctvTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "${saved.cameraId} • ${saved.label}",
                                                fontSize = 12.sp,
                                                color = CctvTextPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteSavedCamera(saved) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = CctvTextMuted,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. VIDEO CONTAINER & REMOTE CONTROLS (When connected) ---
        if (isAnyConnected) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("videoContainer"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sleek Video Player Card with HUD (rounded-3xl border border-[#44474E] shadow-2xl)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CctvCardBorder, RoundedCornerShape(24.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .background(Color.Black)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 4f)
                                    if (scale == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isViewerWebRtcActive && webRtcSession != null) {
                            // WebRTC Live Player
                            WebRtcVideoPlayer(
                                videoTrack = webRtcVideoTrack,
                                eglBase = webRtcSession.eglBase,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("remoteVideo")
                            )
                        } else if (latestFrame != null) {
                            // MJPEG Frame Fallback
                            Image(
                                bitmap = latestFrame!!.asImageBitmap(),
                                contentDescription = "Remote CCTV Live Stream",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                                    .testTag("remoteVideo"),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = CctvIceBlue)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isViewerWebRtcActive) "Connecting WebRTC video stream..." else "Video stream active...",
                                    color = CctvTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // HUD Overlay on Video
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Top left: LIVE badge & Lens
                            Row(
                                modifier = Modifier.align(Alignment.TopStart),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0x99000000),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CctvGlassBorder)
                                ) {
                                    Text(
                                        text = if (isViewerWebRtcActive) "LIVE • WebRTC 4G/5G" else if (isRecording) "REC • ${if (telemetry.lens == CameraLens.FRONT) "FRONT CAM" else "BACK CAM"}" else "LIVE • ${if (telemetry.lens == CameraLens.FRONT) "FRONT CAM" else "BACK CAM"}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Top right: Remote Battery
                            Row(
                                modifier = Modifier.align(Alignment.TopEnd),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BatteryStatusChip(
                                    level = telemetry.batteryLevel,
                                    isCharging = telemetry.isCharging
                                )
                            }

                            // Bottom bar: Telemetry info and Snapshot trigger
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x99000000),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CctvGlassBorder)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                        Text(
                                            text = if (isViewerWebRtcActive) "LATENCY: <120ms (P2P)" else "LATENCY: 32ms",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CctvIceBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "FPS: 30.0 (HD)",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = CctvIceBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Sleek Snapshot Button (bg-[#D1E4FF] text-[#003258] p-4 rounded-2xl)
                                Button(
                                    onClick = { viewModel.takeRemoteSnapshot() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CctvIceBlue,
                                        contentColor = CctvNavyDark
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("remote_snapshot_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Snapshot",
                                        tint = CctvNavyDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SNAPSHOT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = CctvNavyDark
                                    )
                                }
                            }

                            // Motion alert flag
                            if (telemetry.motionDetected) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CctvAlertRed.copy(alpha = 0.95f),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                ) {
                                    Text(
                                        text = "⚠️ MOTION DETECTED",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2-Column Sleek Control Grid (Matching Design HTML: Flip Cam & Audio Sink)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Flip Cam Card (bg-[#2A2D31] rounded-3xl border border-[#44474E])
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = CctvCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CctvCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { viewModel.remoteSwitchCamera() }
                            .testTag("remote_switch_camera_btn")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "ROTATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CctvTextSecondary,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == AppLanguage.HINDI) "कैमरा बदलें" else "Flip Cam",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CctvTextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Flip",
                                    tint = CctvIceBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Audio Sink Listen Card (bg-[#004A77] rounded-3xl)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isRemoteMicOn || isViewerWebRtcActive) CctvNavyPrimary else CctvCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isRemoteMicOn || isViewerWebRtcActive) CctvNavyPrimary else CctvCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { viewModel.toggleRemoteMic() }
                            .testTag("remoteMicBtn")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "AUDIO SINK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRemoteMicOn || isViewerWebRtcActive) CctvIceBlue else CctvTextSecondary,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRemoteMicOn || isViewerWebRtcActive) (if (language == AppLanguage.HINDI) "माइक चालू" else "Listen ON") else (if (language == AppLanguage.HINDI) "माइक बंद" else "Listen OFF"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isRemoteMicOn || isViewerWebRtcActive) Color.White else CctvTextPrimary
                                )
                                Icon(
                                    imageVector = if (isRemoteMicOn || isViewerWebRtcActive) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Mic",
                                    tint = if (isRemoteMicOn || isViewerWebRtcActive) CctvIceBlue else CctvTextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Two-way Talk Banner Card (Matching Design HTML: bg-[#3D4146] rounded-3xl with big mic trigger)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = CctvCardBgSecondary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CctvCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.HINDI) "टू-वे टॉक (Two-way Talk)" else "Two-way Talk",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = CctvTextPrimary
                            )
                            Text(
                                text = if (isViewerMicOn || isViewerMicTalking) (if (language == AppLanguage.HINDI) "आवाज़ भेजी जा रही है..." else "Speaking to camera...") else (if (language == AppLanguage.HINDI) "कैमरे पर बोलने के लिए दबाएं" else "Push to speak to camera"),
                                fontSize = 12.sp,
                                color = if (isViewerMicOn || isViewerMicTalking) CctvSuccessGreen else CctvTextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Circular mic action button (h-14 w-14 rounded-full bg-[#BA1A1A])
                        IconButton(
                            onClick = { viewModel.toggleViewerMic() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isViewerMicOn || isViewerMicTalking) CctvSuccessGreen else CctvAlertRed)
                                .testTag("viewerMicBtn")
                        ) {
                            Icon(
                                imageVector = if (isViewerMicOn || isViewerMicTalking) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                                contentDescription = "Speak",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Extra controls row: Torch, Siren & Stream Record
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Remote Torch
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (telemetry.isTorchOn) CctvNavyPrimary else CctvCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (telemetry.isTorchOn) CctvNavyPrimary else CctvCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { viewModel.remoteToggleTorch() }
                            .testTag("remote_torch_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (telemetry.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = null,
                                tint = if (telemetry.isTorchOn) CctvIceBlue else CctvTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (telemetry.isTorchOn) "Torch ON" else "Torch",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.isTorchOn) Color.White else CctvTextPrimary
                            )
                        }
                    }

                    // Remote Siren
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (telemetry.isSirenPlaying) CctvAlertRed else CctvCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (telemetry.isSirenPlaying) CctvAlertRed else CctvCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { viewModel.remoteToggleSiren() }
                            .testTag("remote_siren_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (telemetry.isSirenPlaying) Color.White else CctvAlertRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (telemetry.isSirenPlaying) "Stop Siren" else "Siren",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (telemetry.isSirenPlaying) Color.White else CctvAlertRed
                            )
                        }
                    }

                    // Record Stream
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isRecording) CctvAlertRed else CctvCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecording) CctvAlertRed else CctvCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { viewModel.toggleRecording() }
                            .testTag("record_stream_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (isRecording) Color.White else CctvAlertRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecording) "Recording" else "Record",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) Color.White else CctvTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
