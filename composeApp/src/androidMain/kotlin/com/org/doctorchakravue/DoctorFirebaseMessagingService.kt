package com.org.doctorchakravue

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.org.doctorchakravue.data.SessionManager

/**
 * Firebase Cloud Messaging Service for Doctor App.
 * Handles incoming push notifications and token refresh.
 */
class DoctorFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "DoctorFCM"
    }

    /**
     * Called when a new FCM token is generated.
     * This happens on first app start and when token is refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Store token locally - will be sent to server when doctor logs in
        val sessionManager = SessionManager()
        sessionManager.saveFcmToken(token)
    }

    /**
     * Called when a message is received while app is in foreground.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification - Title: ${notification.title}, Body: ${notification.body}")
            // TODO: Show local notification if needed
        }

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        when (type) {
            "video_call" -> {
                // Handle incoming video call notification
                val patientName = data["patient_name"]
                val callId = data["call_id"]
                Log.d(TAG, "Video call from: $patientName, callId: $callId")
            }
            "new_submission" -> {
                // Handle new patient submission notification
                val patientName = data["patient_name"]
                val submissionType = data["submission_type"]
                Log.d(TAG, "New submission from: $patientName, type: $submissionType")
            }
            else -> {
                Log.d(TAG, "Unknown message type: $type")
            }
        }
    }
}
