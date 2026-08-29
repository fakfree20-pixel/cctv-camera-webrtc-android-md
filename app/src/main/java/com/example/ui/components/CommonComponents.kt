package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.strings.AppLanguage
import com.example.ui.theme.CctvAlertRed
import com.example.ui.theme.CctvCardBg
import com.example.ui.theme.CctvCardBorder
import com.example.ui.theme.CctvDarkBg
import com.example.ui.theme.CctvGlassBorder
import com.example.ui.theme.CctvIceBlue
import com.example.ui.theme.CctvNavyDark
import com.example.ui.theme.CctvNavyPrimary
import com.example.ui.theme.CctvPrimaryCyan
import com.example.ui.theme.CctvSuccessGreen
import com.example.ui.theme.CctvSurfaceOverlay
import com.example.ui.theme.CctvTextMuted
import com.example.ui.theme.CctvTextPrimary
import com.example.ui.theme.CctvTextSecondary
import com.example.ui.theme.CctvWarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CctvTopBar(
    title: String,
    language: AppLanguage,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onLanguageToggle: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    showGalleryIcon: Boolean = true
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sleek circular icon badge (w-10 h-10 rounded-full bg-[#004A77])
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
                        contentDescription = "CCTV Logo",
                        tint = CctvIceBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = CctvTextPrimary,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI) "स्मार्ट रिमोट कैमरा" else "Sleek Remote Control",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CctvTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CctvTextPrimary
                    )
                }
            }
        },
        actions = {
            // Language switch badge (Sleek pill)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CctvCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, CctvCardBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onLanguageToggle() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("language_toggle_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = CctvIceBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (language == AppLanguage.HINDI) "हिन्दी" else "EN",
                        color = CctvIceBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showGalleryIcon) {
                IconButton(
                    onClick = onGalleryClick,
                    modifier = Modifier.testTag("gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = CctvTextSecondary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CctvDarkBg,
            titleContentColor = CctvTextPrimary
        )
    )
}

@Composable
fun LiveRecBadge(modifier: Modifier = Modifier, isRecording: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0x99000000),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            CctvGlassBorder
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) CctvAlertRed.copy(alpha = alpha) else CctvSuccessGreen.copy(alpha = alpha))
            )
            Text(
                text = if (isRecording) "REC • LIVE" else "LIVE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) CctvAlertRed else Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BatteryStatusChip(level: Int, isCharging: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x99000000),
        border = androidx.compose.foundation.BorderStroke(1.dp, CctvGlassBorder),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                contentDescription = "Battery",
                tint = if (level > 20) Color.White else CctvAlertRed,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "$level%",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MotionAlertBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
    text: String = "⚠️ Motion Detected!"
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CctvAlertRed.copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.dp, CctvAlertRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

