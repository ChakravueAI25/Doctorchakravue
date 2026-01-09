package com.org.doctorchakravue.features.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.org.doctorchakravue.core.ui.theme.DoctorBlue
import com.org.doctorchakravue.core.ui.theme.DoctorGreen

/**
 * Video Call Screen - Common UI
 *
 * Note: Actual Agora SDK integration is platform-specific (Android only).
 * This screen provides the UI shell. For full video functionality,
 * you need to implement expect/actual for AgoraVideoView on Android.
 */
@Composable
fun DoctorCallScreen(
    appId: String,
    token: String,
    channelName: String,
    onEndCall: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(true) }
    var remoteUserJoined by remember { mutableStateOf(false) }

    // Simulate connection after 2 seconds (in real app, this would be from Agora events)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        isConnecting = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // Remote Video Area (Large)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isConnecting) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DoctorGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Connecting to patient...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (!remoteUserJoined) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Waiting for patient to join...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Channel: $channelName",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                // Placeholder for remote video
                // In real implementation, this would be AgoraVideoView
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Remote Video", color = Color.White)
                }
            }
        }

        // Local Video Preview (Small, top-left)
        Box(
            modifier = Modifier
                .padding(16.dp)
                .size(120.dp, 160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2D2D44))
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            if (isCameraOff) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            } else {
                // Placeholder for local video preview
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = DoctorBlue,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("You", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Call info header
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Video Call",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (!isConnecting) {
                Text(
                    if (remoteUserJoined) "Connected" else "Ringing...",
                    color = DoctorGreen,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Control buttons at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute button
            CallControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                backgroundColor = if (isMuted) Color(0xFF3D3D5C) else Color(0xFF2D2D44),
                onClick = { isMuted = !isMuted }
            )

            // End call button (large, red)
            FloatingActionButton(
                onClick = onEndCall,
                containerColor = Color.Red,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Camera toggle
            CallControlButton(
                icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                label = if (isCameraOff) "Camera On" else "Camera Off",
                backgroundColor = if (isCameraOff) Color(0xFF3D3D5C) else Color(0xFF2D2D44),
                onClick = { isCameraOff = !isCameraOff }
            )
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = backgroundColor,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}
