package com.org.doctorchakravue.platform

actual class BitmapCapture {
    actual fun captureScreen(): ByteArray? {
        // Android-specific implementation would go here
        // For now, return null as placeholder
        return null
    }
}
