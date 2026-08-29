package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.data.model.AppRole
import com.example.ui.components.CctvTopBar
import com.example.ui.screens.CameraModeScreen
import com.example.ui.screens.ModeSelectionScreen
import com.example.ui.screens.SnapshotsScreen
import com.example.ui.screens.ViewerModeScreen
import com.example.ui.strings.AppStrings
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CctvViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: CctvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CctvApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CctvApp(viewModel: CctvViewModel) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val language by viewModel.language.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Permission launcher for Camera & Mic
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted) {
            viewModel.selectRole(AppRole.CAMERA_DEVICE)
        } else {
            Toast.makeText(context, "Camera permission is required for CCTV mode", Toast.LENGTH_SHORT).show()
        }
    }

    val requestPermissionsAndStartCamera = {
        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (hasCam && hasAudio) {
            viewModel.selectRole(AppRole.CAMERA_DEVICE)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val title = when (currentRole) {
        AppRole.SELECTION -> AppStrings.appTitle(language)
        AppRole.CAMERA_DEVICE -> AppStrings.cameraActiveTitle(language)
        AppRole.VIEWER_DEVICE -> AppStrings.remoteControlPanel(language)
        AppRole.SNAPSHOTS_GALLERY -> AppStrings.snapshotsGalleryBtn(language)
    }

    val showBack = currentRole != AppRole.SELECTION

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (currentRole != AppRole.CAMERA_DEVICE || !viewModel.isPowerSaverActive.collectAsState().value) {
                CctvTopBar(
                    title = title,
                    language = language,
                    showBack = showBack,
                    onBackClick = {
                        if (currentRole == AppRole.CAMERA_DEVICE) {
                            viewModel.stopCameraMode()
                        } else if (currentRole == AppRole.VIEWER_DEVICE) {
                            viewModel.disconnectViewer()
                        }
                        viewModel.selectRole(AppRole.SELECTION)
                    },
                    onLanguageToggle = { viewModel.toggleLanguage() },
                    onGalleryClick = { viewModel.selectRole(AppRole.SNAPSHOTS_GALLERY) },
                    showGalleryIcon = currentRole != AppRole.SNAPSHOTS_GALLERY
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentRole == AppRole.CAMERA_DEVICE && viewModel.isPowerSaverActive.collectAsState().value) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
        ) {
            when (currentRole) {
                AppRole.SELECTION -> {
                    ModeSelectionScreen(
                        language = language,
                        onSelectRole = { role ->
                            if (role == AppRole.CAMERA_DEVICE) {
                                requestPermissionsAndStartCamera()
                            } else {
                                viewModel.selectRole(role)
                            }
                        },
                        onOpenGallery = {
                            viewModel.selectRole(AppRole.SNAPSHOTS_GALLERY)
                        }
                    )
                }

                AppRole.CAMERA_DEVICE -> {
                    CameraModeScreen(
                        viewModel = viewModel,
                        language = language,
                        onStopCamera = {
                            viewModel.stopCameraMode()
                            viewModel.selectRole(AppRole.SELECTION)
                        }
                    )
                }

                AppRole.VIEWER_DEVICE -> {
                    ViewerModeScreen(
                        viewModel = viewModel,
                        language = language,
                        onBackToSelection = {
                            viewModel.disconnectViewer()
                            viewModel.selectRole(AppRole.SELECTION)
                        }
                    )
                }

                AppRole.SNAPSHOTS_GALLERY -> {
                    SnapshotsScreen(
                        viewModel = viewModel,
                        language = language,
                        onBack = {
                            viewModel.selectRole(AppRole.SELECTION)
                        }
                    )
                }
            }
        }
    }
}

