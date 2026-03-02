package com.org.doctorchakravue.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific video call screen.
 * On Android: uses Agora RtcEngine.
 * On iOS:     shows a placeholder (Agora iOS SDK not wired here).
 */
@Composable
expect fun PlatformVideoCallScreen(
    appId: String,
    token: String,
    channelName: String,
    onEndCall: () -> Unit
)
