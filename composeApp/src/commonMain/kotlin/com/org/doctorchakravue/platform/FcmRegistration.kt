package com.org.doctorchakravue.platform

import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.data.SessionManager

/**
 * Platform-specific FCM token registration.
 * Implemented differently on Android (Firebase) and iOS (APNs).
 */
expect suspend fun registerFcmTokenAfterLogin(
    repository: ApiRepository,
    sessionManager: SessionManager
): Boolean
