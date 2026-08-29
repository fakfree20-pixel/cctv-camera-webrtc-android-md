package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppRole
import com.example.ui.strings.AppLanguage
import com.example.ui.strings.AppStrings
import com.example.ui.theme.CctvAccentTeal
import com.example.ui.theme.CctvAlertRed
import com.example.ui.theme.CctvCardBg
import com.example.ui.theme.CctvCardBgSecondary
import com.example.ui.theme.CctvCardBorder
import com.example.ui.theme.CctvDarkBg
import com.example.ui.theme.CctvGlassBorder
import com.example.ui.theme.CctvIceBlue
import com.example.ui.theme.CctvNavyDark
import com.example.ui.theme.CctvNavyPrimary
import com.example.ui.theme.CctvPrimaryCyan
import com.example.ui.theme.CctvSecondaryBlue
import com.example.ui.theme.CctvSuccessGreen
import com.example.ui.theme.CctvTextMuted
import com.example.ui.theme.CctvTextPrimary
import com.example.ui.theme.CctvTextSecondary

@Composable
fun ModeSelectionScreen(
    language: AppLanguage,
    onSelectRole: (AppRole) -> Unit,
    onOpenGallery: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CctvDarkBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Hero Header Badge (Sleek Theme)
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(CctvNavyPrimary)
                .border(1.dp, CctvGlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "CCTV Camera",
                tint = CctvIceBlue,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = AppStrings.appTitle(language),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CctvTextPrimary,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CctvSuccessGreen)
            )
            Text(
                text = "READY • LOCAL WI-FI STREAMING",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CctvSuccessGreen,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mode Selection Card (Sleek rounded-3xl with 24.dp corner)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CctvCardBorder, RoundedCornerShape(24.dp))
                .testTag("mode_selection_box"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CctvCardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT DEVICE ROLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = CctvTextSecondary,
                        letterSpacing = 1.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = AppStrings.modeQuestion(language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = CctvTextPrimary,
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                )

                // 1. OLD PHONE (Camera Mode) - Sleek Navy Theme
                Button(
                    onClick = { onSelectRole(AppRole.CAMERA_DEVICE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_camera_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CctvNavyPrimary,
                        contentColor = CctvIceBlue
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Camera",
                            tint = CctvIceBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = AppStrings.oldPhoneBtn(language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CctvIceBlue
                        )
                    }
                }

                Text(
                    text = AppStrings.oldPhoneDesc(language),
                    fontSize = 12.sp,
                    color = CctvTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                )

                // 2. NEW PHONE (Viewer Mode) - Sleek Slate/Blue Theme
                Button(
                    onClick = { onSelectRole(AppRole.VIEWER_DEVICE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("show_viewer_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CctvCardBgSecondary,
                        contentColor = CctvTextPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Viewer",
                            tint = CctvIceBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = AppStrings.newPhoneBtn(language),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CctvTextPrimary
                        )
                    }
                }

                Text(
                    text = AppStrings.newPhoneDesc(language),
                    fontSize = 12.sp,
                    color = CctvTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Snapshots Gallery Button (Sleek Ice Button)
        OutlinedButton(
            onClick = onOpenGallery,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("snapshots_gallery_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CctvIceBlue
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, CctvCardBorder)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Snapshots",
                tint = CctvIceBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppStrings.snapshotsGalleryBtn(language),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CctvIceBlue
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Features Grid Overview
        Text(
            text = (if (language == AppLanguage.HINDI) "मुख्य विशेषताएं (FEATURES)" else "KEY REMOTE CAPABILITIES").uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = CctvTextSecondary,
                letterSpacing = 1.5.sp
            ),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SleekFeaturePill(
                category = "ROTATION",
                title = if (language == AppLanguage.HINDI) "रिमोट फ्लिप" else "Flip Cam",
                icon = Icons.Default.Videocam,
                modifier = Modifier.weight(1f)
            )
            SleekFeaturePill(
                category = "AUDIO SINK",
                title = if (language == AppLanguage.HINDI) "सुनें माइक" else "Listen Mic",
                icon = Icons.Default.Mic,
                modifier = Modifier.weight(1f),
                isHighlight = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SleekFeaturePill(
                category = "LIGHTING",
                title = if (language == AppLanguage.HINDI) "टॉर्च कंट्रोल" else "Remote Torch",
                icon = Icons.Default.FlashOn,
                modifier = Modifier.weight(1f)
            )
            SleekFeaturePill(
                category = "SECURITY",
                title = if (language == AppLanguage.HINDI) "सिक्योरिटी सायरन" else "Siren Alarm",
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SleekFeaturePill(
    category: String,
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isHighlight) CctvNavyPrimary else CctvCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isHighlight) CctvNavyPrimary else CctvCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp)
        ) {
            Text(
                text = category,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) CctvIceBlue.copy(alpha = 0.8f) else CctvTextSecondary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = if (isHighlight) Color.White else CctvTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlight) CctvIceBlue else CctvIceBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
