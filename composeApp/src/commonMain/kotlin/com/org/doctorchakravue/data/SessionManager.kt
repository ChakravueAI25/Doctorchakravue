package com.org.doctorchakravue.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * SessionManager - Handles user session storage.
 * Manages login state, doctor credentials, and session data.
 */
class SessionManager {
    private val settings = Settings()

    fun saveSession(id: String, name: String, email: String = "") {
        settings["doctorId"] = id
        settings["doctorName"] = name
        if (email.isNotEmpty()) {
            settings["doctorEmail"] = email
        }
    }

    fun isLoggedIn(): Boolean = settings.getStringOrNull("doctorId") != null

    fun getDoctorId(): String = settings.getString("doctorId", "")

    fun getDoctorName(): String = settings.getString("doctorName", "Doctor")

    fun getDoctorEmail(): String = settings.getString("doctorEmail", "")

    fun logout() {
        settings.remove("doctorId")
        settings.remove("doctorName")
        settings.remove("doctorEmail")
        // Note: FCM token is NOT removed on logout - it's device-specific
    }

    // --- FCM Token Management ---
    fun saveFcmToken(token: String) {
        settings["fcmToken"] = token
    }

    fun getFcmToken(): String = settings.getString("fcmToken", "")

    fun hasFcmToken(): Boolean = settings.getStringOrNull("fcmToken") != null
}
