package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.CameraManager
import com.example.data.model.CameraLens
import com.example.ui.components.BatteryStatusChip
import com.example.ui.components.LiveRecBadge
import com.example.ui.components.MotionAlertBanner
import com.example.ui.strings.AppLanguage
import com.example.ui.strings.AppStrings
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
import com.example.ui.theme.CctvSecondaryBlue
import com.example.ui.theme.CctvSuccessGreen
import com.example.ui.theme.CctvTextMuted
import com.example.ui.theme.CctvTextPrimary
import com.example.ui.theme.CctvTextSecondary
import com.example.ui.theme.CctvWarningAmber
import com.example.ui.viewmodel.CctvViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraModeScreen(
    viewModel: CctvViewModel,
    language: AppLanguage,
    onStopCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraId by viewModel.cameraId.collectAsState()
    val cameraRoomPin by viewModel.cameraRoomPin.collectAsState()
    val cameraIp by viewModel.cameraIp.collectAsState()
    val cameraPort by viewModel.cameraPort.collectAsState()
    val connectedViewers by viewModel.connectedViewersCount.collectAsState()
    val isMotionDetected by viewModel.isMotionDetected.collectAsState()
    val isPowerSaverActive by viewModel.isPowerSaverActive.collectAsState()
    val telemetry by viewModel.cameraTelemetry.collectAsState()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopCameraMode()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Camera View Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    previewViewRef = this
                    viewModel.startCameraMode(lifecycleOwner, this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_preview_view")
        )

        // 2. Camera HUD Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Bar with Sleek styling
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xE61A1C1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CctvCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CctvNavyPrimary)
                                        .border(1.dp, CctvGlassBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = CctvIceBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = AppStrings.cameraActiveTitle(language),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = CctvTextPrimary
                                        )
                                    )
                                    Text(
                                        text = "BROADCASTING FEED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = CctvIceBlue,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                            LiveRecBadge()
                        }

                        // WebRTC Mobile Data Room PIN Banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CctvNavyPrimary.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CctvPrimaryCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = AppStrings.webrtcRoomCode(language),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CctvPrimaryCyan
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = cameraRoomPin,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 2.sp,
                                            modifier = Modifier.testTag("camera_pin_display")
                                        )
                                        Text(
                                            text = "(4G/5G P2P)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CctvSuccessGreen
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("CCTV Room PIN", cameraRoomPin)
                                        clipboard.setPrimaryClip(clip)
                                        viewModel.showToast("Room PIN $cameraRoomPin Copied!")
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CctvNavyHover)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy PIN",
                                        tint = CctvIceBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Camera ID & Local IP Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "LAN IP:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CctvTextSecondary
                                )
                                Text(
                                    text = "$cameraIp:$cameraPort",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CctvIceBlue,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("camera_id_display")
                                )
                            }

                            BatteryStatusChip(
                                level = telemetry.batteryLevel,
                                isCharging = telemetry.isCharging
                            )
                        }

                        // Status Badge (Viewer connection status)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (connectedViewers > 0) CctvSuccessGreen.copy(alpha = 0.15f) else CctvCardBgSecondary,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (connectedViewers > 0) CctvSuccessGreen.copy(alpha = 0.6f) else CctvCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (connectedViewers > 0) CctvSuccessGreen else CctvTextMuted)
                                    )
                                    Text(
                                        text = if (connectedViewers > 0) AppStrings.viewerConnected(language) else AppStrings.waitingViewer(language),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (connectedViewers > 0) CctvSuccessGreen else CctvTextSecondary,
                                        modifier = Modifier.testTag("cam_status_text")
                                    )
                                }
                                if (connectedViewers > 0) {
                                    Text(
                                        text = "$connectedViewers Client(s)",
                                        fontSize = 11.sp,
                                        color = CctvSuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Motion Alert Banner
                MotionAlertBanner(
                    visible = isMotionDetected,
                    text = AppStrings.motionAlertDetected(language)
                )
            }

            // Bottom Controls Deck
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary Quick Bar (Flip, Torch, Motion, Siren, Snapshot)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Switch Camera
                    IconButton(
                        onClick = { viewModel.switchCameraLens(lifecycleOwner, previewViewRef) },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CctvCardBg)
                            .border(1.dp, CctvCardBorder, CircleShape)
                            .testTag("cam_switch_lens_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = CctvIceBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Torch Toggle
                    IconButton(
                        onClick = { viewModel.toggleCameraTorch() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.cameraManager.isTorchOn) CctvNavyPrimary else CctvCardBg)
                            .border(1.dp, if (viewModel.cameraManager.isTorchOn) CctvIceBlue else CctvCardBorder, CircleShape)
                            .testTag("cam_toggle_torch_btn")
                    ) {
                        Icon(
                            imageVector = if (viewModel.cameraManager.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (viewModel.cameraManager.isTorchOn) CctvIceBlue else CctvTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Motion Detection Toggle
                    IconButton(
                        onClick = {
                            val enabled = viewModel.toggleMotionDetection()
                            viewModel.showToast(if (enabled) "Motion Detection ON" else "Motion Detection OFF")
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.cameraManager.motionDetectionEnabled) CctvSuccessGreen.copy(alpha = 0.25f) else CctvCardBg)
                            .border(1.dp, if (viewModel.cameraManager.motionDetectionEnabled) CctvSuccessGreen else CctvCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MotionPhotosOn,
                            contentDescription = "Motion Detection",
                            tint = if (viewModel.cameraManager.motionDetectionEnabled) CctvSuccessGreen else CctvTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Test Siren
                    IconButton(
                        onClick = { viewModel.toggleCameraSiren() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.audioStreamManager.isSirenActive()) CctvAlertRed else CctvCardBg)
                            .border(1.dp, if (viewModel.audioStreamManager.isSirenActive()) CctvAlertRed else CctvCardBorder, CircleShape)
                            .testTag("cam_test_siren_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Siren",
                            tint = if (viewModel.audioStreamManager.isSirenActive()) Color.White else CctvAlertRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Local Snapshot
                    IconButton(
                        onClick = {
                            scope.launch { viewModel.takeCameraLocalSnapshot() }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(CctvCardBg)
                            .border(1.dp, CctvCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Snapshot",
                            tint = CctvIceBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Primary Bottom Buttons (Power Saver & Stop Camera)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.togglePowerSaver() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("power_saver_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CctvCardBg,
                            contentColor = CctvIceBlue
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CctvCardBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = CctvIceBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.powerSaverBtn(language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CctvIceBlue
                        )
                    }

                    Button(
                        onClick = onStopCamera,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("stop_camera_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CctvAlertRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.stopCamera(language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Black Screen / Power Saver Overlay
        if (isPowerSaverActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.togglePowerSaver()
                    }
                    .testTag("black_screen_power_saver_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentTime,
                        color = Color(0x33FFFFFF),
                        fontSize = 48.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "● CCTV STREAMING ACTIVE",
                        color = Color(0x3310B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = AppStrings.powerSaverHint(language),
                        color = Color(0x44FFFFFF),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
