package com.org.doctorchakravue.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun CameraPreviewView(
    modifier: Modifier,
    isCameraOff: Boolean
) {
    // iOS camera preview placeholder
    // Actual iOS camera integration would require native Swift/Objective-C code
    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFF2D2D44)),
        contentAlignment = Alignment.Center
    ) {
        if (isCameraOff) {
            Icon(
                Icons.Default.VideocamOff,
                contentDescription = "Camera Off",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = "Camera Preview",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

