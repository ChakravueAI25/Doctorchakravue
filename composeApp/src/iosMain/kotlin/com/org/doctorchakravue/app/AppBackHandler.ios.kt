package com.org.doctorchakravue.app

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses gesture-based navigation, no back handler needed
}
