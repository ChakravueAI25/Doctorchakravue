package com.org.doctorchakravue

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.data.SessionManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Helper for Doctor App.
 * Handles FCM token retrieval and registration with backend.
 */
object FirebaseHelper {
    private const val TAG = "FirebaseHelper"

    /**
     * Get the current FCM token.
     * Returns the token or null if unavailable.
     */
    suspend fun getFcmToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM Token retrieved: $token")
                continuation.resume(token)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get FCM token", exception)
                continuation.resume(null)
            }
    }

    /**
     * Register FCM token with backend for the logged-in doctor.
     * Should be called after successful login.
     */
    suspend fun registerTokenWithBackend(
        repository: ApiRepository,
        sessionManager: SessionManager
    ): Boolean {
        val doctorId = sessionManager.getDoctorId()
        if (doctorId.isEmpty()) {
            Log.w(TAG, "Cannot register FCM token: Doctor not logged in")
            return false
        }

        // Get token from Firebase
        val token = getFcmToken()
        if (token == null) {
            Log.w(TAG, "Cannot register FCM token: Token unavailable")
            return false
        }

        // Save token locally
        sessionManager.saveFcmToken(token)

        // Register with backend
        val success = repository.registerFcmToken(doctorId, token)
        if (success) {
            Log.d(TAG, "FCM token registered with backend successfully")
        } else {
            Log.e(TAG, "Failed to register FCM token with backend")
        }

        return success
    }

    /**
     * Re-register token if doctor is already logged in.
     * Call this on app start to ensure token is up-to-date.
     */
    suspend fun ensureTokenRegistered(
        repository: ApiRepository,
        sessionManager: SessionManager
    ) {
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "Skipping token registration: Not logged in")
            return
        }

        // Get fresh token
        val newToken = getFcmToken() ?: return
        val savedToken = sessionManager.getFcmToken()

        // Only re-register if token changed
        if (newToken != savedToken) {
            Log.d(TAG, "FCM token changed, re-registering...")
            registerTokenWithBackend(repository, sessionManager)
        } else {
            Log.d(TAG, "FCM token unchanged, skipping re-registration")
        }
    }
}
