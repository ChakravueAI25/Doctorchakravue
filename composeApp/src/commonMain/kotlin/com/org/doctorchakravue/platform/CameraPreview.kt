package com.org.doctorchakravue.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Expect declaration for camera preview composable.
 * Platform-specific implementations will provide the actual camera preview.
 */
@Composable
expect fun CameraPreviewView(
    modifier: Modifier = Modifier,
    isCameraOff: Boolean = false
)

