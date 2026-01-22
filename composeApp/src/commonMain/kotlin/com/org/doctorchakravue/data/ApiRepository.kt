package com.org.doctorchakravue.data

import com.org.doctorchakravue.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * ApiRepository - Handles all API communication with the backend.
 * Uses SessionManager for credential storage.
 */
class ApiRepository(
    private val sessionManager: SessionManager = SessionManager()
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            url("https://doctor.chakravue.co.in")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    // --- Session Delegation ---
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun getDoctorId(): String = sessionManager.getDoctorId()
    fun getDoctorName(): String = sessionManager.getDoctorName()
    fun logout() = sessionManager.logout()

    // --- Authentication ---
    suspend fun login(email: String, pass: String): LoginResponse {
        try {
            val response = client.post("/login/doctor") {
                setBody(mapOf("email" to email, "password" to pass))
            }

            if (response.status.isSuccess()) {
                val data = response.body<LoginResponse>()
                sessionManager.saveSession(data.id, data.name, data.email)
                return data
            } else {
                val err = response.body<ApiError>()
                throw Exception(err.detail)
            }
        } catch (e: Exception) {
            throw Exception(e.message ?: "Connection failed")
        }
    }

    // --- Submissions ---
    suspend fun getUrgentSubmissions(doctorId: String): List<Submission> {
        return try {
            client.get("/submissions/doctor/$doctorId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHistory(doctorId: String): List<Submission> {
        return try {
            client.get("/submissions/doctor/$doctorId/history").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVisionSubmissions(doctorId: String): List<Submission> {
        return try {
            client.get("/submissions/doctor/$doctorId/vision-tests").body()
        } catch (e: Exception) {
            // Fallback: try to filter from history if specific endpoint doesn't exist
            emptyList()
        }
    }

    suspend fun getSubmissionDetails(submissionId: String): SubmissionDetail? {
        return try {
            client.get("/submissions/$submissionId").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendSubmissionNote(submissionId: String, note: String, doctorId: String): Boolean {
        return try {
            val response = client.post("/submissions/$submissionId/notes") {
                setBody(mapOf(
                    "note" to note,
                    "doctorId" to doctorId
                ))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    // --- Patients ---
    suspend fun getPatients(): List<PatientSimple> {
        return try {
            client.get("/patients").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFullPatientProfile(query: String?): PatientRecord? {
        return try {
            if (!query.isNullOrEmpty()) {
                val resp = client.get("/patients/case/search/?query=$query")
                if (resp.status.isSuccess()) return resp.body()
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // --- Video Calls ---
    suspend fun getCallToken(channelName: String): CallTokenResponse? {
        return try {
            val response = client.post("/call/token?channel_name=$channelName")
            if (response.status.isSuccess()) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun initiateCall(doctorId: String, patientId: String, channelName: String): Boolean {
        return try {
            client.post("/call/initiate") {
                setBody(mapOf(
                    "doctor_id" to doctorId,
                    "patient_id" to patientId,
                    "channel_name" to channelName
                ))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Notifications ---
    suspend fun sendNotification(
        doctorId: String,
        title: String,
        message: String,
        sendToAll: Boolean,
        selectedEmails: List<String>
    ): Boolean {
        return try {
            val recipients = if (sendToAll) {
                mapOf("all" to true)
            } else {
                mapOf("all" to false, "emails" to selectedEmails)
            }

            val response = client.post("/notifications") {
                setBody(MultiPartFormDataContent(
                    formData {
                        append("doctor_id", doctorId)
                        append("title", title)
                        append("message", message)
                        append("recipients", Json.encodeToString(
                            kotlinx.serialization.serializer<Map<String, Any>>(),
                            recipients
                        ))
                    }
                ))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getNotifications(doctorId: String): List<NotificationItem> {
        return try {
            val response: Map<String, List<NotificationItem>> = client.get("/notifications?doctor_id=$doctorId").body()
            response["notifications"] ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Adherence ---
    suspend fun getAdherenceList(doctorId: String): List<AdherencePatient> {
        return try {
            client.get("/doctors/$doctorId/adherence-list").body()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
