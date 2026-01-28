package com.org.doctorchakravue.platform

import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.data.SessionManager

/**
 * iOS implementation of FCM token registration.
 * TODO: Implement APNs token registration for iOS.
 */
actual suspend fun registerFcmTokenAfterLogin(
    repository: ApiRepository,
    sessionManager: SessionManager
): Boolean {
    // iOS push notification implementation will be added later
    // For now, return true to not block login flow
    println("iOS FCM registration not yet implemented")
    return true
}
