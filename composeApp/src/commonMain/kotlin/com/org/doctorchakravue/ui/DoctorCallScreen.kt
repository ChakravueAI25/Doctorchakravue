package com.org.doctorchakravue.ui

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen
import kotlinx.coroutines.delay

/**
 * DoctorCallScreen - Video Call UI.
 * Actual Agora SDK integration is platform-specific.
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

    LaunchedEffect(Unit) {
        delay(2000)
        isConnecting = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isConnecting) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DoctorGreen)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connecting to patient...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                }
            } else if (!remoteUserJoined) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for patient to join...", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Channel: $channelName", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                    Text("Remote Video", color = Color.White)
                }
            }
        }

        // Local Video Preview
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
                Icon(Icons.Default.VideocamOff, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, null, tint = DoctorBlue, modifier = Modifier.size(40.dp))
                    Text("You", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Call info header
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Video Call", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!isConnecting) {
                Text(if (remoteUserJoined) "Connected" else "Ringing...", color = DoctorGreen, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Control buttons
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                backgroundColor = if (isMuted) Color(0xFF3D3D5C) else Color(0xFF2D2D44),
                onClick = { isMuted = !isMuted }
            )

            FloatingActionButton(onClick = onEndCall, containerColor = Color.Red, modifier = Modifier.size(72.dp)) {
                Icon(Icons.Default.CallEnd, "End Call", tint = Color.White, modifier = Modifier.size(32.dp))
            }

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
private fun CallControlButton(icon: ImageVector, label: String, backgroundColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(onClick = onClick, containerColor = backgroundColor, modifier = Modifier.size(56.dp)) {
            Icon(icon, label, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}
