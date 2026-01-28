package com.org.doctorchakravue.platform

import com.org.doctorchakravue.FirebaseHelper
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.data.SessionManager

/**
 * Android implementation of FCM token registration.
 * Uses Firebase Cloud Messaging.
 */
actual suspend fun registerFcmTokenAfterLogin(
    repository: ApiRepository,
    sessionManager: SessionManager
): Boolean {
    return FirebaseHelper.registerTokenWithBackend(repository, sessionManager)
}
