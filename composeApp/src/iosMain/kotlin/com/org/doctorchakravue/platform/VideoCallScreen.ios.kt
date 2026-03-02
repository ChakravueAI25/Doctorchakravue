package com.org.doctorchakravue.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * iOS stub – full Agora iOS SDK integration requires Swift/Objective-C native bridge.
 * Shows a placeholder screen so the app compiles.
 */
@Composable
actual fun PlatformVideoCallScreen(
    appId: String,
    token: String,
    channelName: String,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Video call (iOS)", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Channel: $channelName", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(48.dp))
            FloatingActionButton(
                onClick = onEndCall,
                containerColor = Color.Red,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.CallEnd, "End Call", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}
