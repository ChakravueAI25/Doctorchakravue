package com.org.doctorchakravue.app

import androidx.compose.runtime.Composable

/**
 * AppBackHandler - Platform-specific back button handler.
 * Android: Handles hardware back button
 * iOS: No-op (iOS uses gesture navigation)
 */
@Composable
expect fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit)
