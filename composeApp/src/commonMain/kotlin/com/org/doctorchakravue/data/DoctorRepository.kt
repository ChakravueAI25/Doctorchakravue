package com.org.doctorchakravue.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
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
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- Models ---
@Serializable
data class LoginResponse(
    @SerialName("_id") val id: String,
    val name: String,
    val email: String
)

@Serializable
data class ApiError(val detail: String)

@Serializable
data class CallTokenResponse(
    val token: String,
    val app_id: String
)

@Serializable
data class PatientRecord(
    @SerialName("_id") val id: String? = null,
    val registrationId: String? = null,
    val patientDetails: PatientDetails? = null,
    val presentingComplaints: PresentingComplaints? = null,
    val drugHistory: DrugHistory? = null,
    val history: List<HistoryEntry>? = null,
    val doctor: DoctorInfo? = null
)

@Serializable
data class PatientDetails(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null
)

@Serializable
data class PresentingComplaints(
    val complaints: List<ComplaintItem>? = null
)

@Serializable
data class ComplaintItem(
    val complaint: String? = null,
    val duration: String? = null
)

@Serializable
data class DrugHistory(
    val currentMeds: List<MedicationItem>? = null
)

@Serializable
data class MedicationItem(
    val name: String? = null,
    val drug: String? = null,
    val dosage: String? = null,
    val indication: String? = null
)

@Serializable
data class HistoryEntry(
    val at: String? = null,
    val problem: String? = null,
    @SerialName("doctor_notes") val doctorNotes: String? = null,
    val medicines: List<String>? = null,
    @SerialName("procedure_type") val procedureType: String? = null,
    @SerialName("procedure_done") val procedureDone: Boolean? = null
)

@Serializable
data class DoctorInfo(
    val prescription: Map<String, String>? = null
)

@Serializable
data class SubmissionDetail(
    @SerialName("_id") val id: String? = null,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("patient_email") val patientEmail: String? = null,
    @SerialName("pain_scale") val painScale: Int? = 0,
    val swelling: Int? = 0,
    val redness: Int? = 0,
    val discharge: Int? = 0,
    val comments: String? = null,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("image_file_id") val imageId: String? = null,
    @SerialName("is_viewed") val isViewed: Boolean? = false,
    @SerialName("is_archived") val isArchived: Boolean? = false
)

@Serializable
data class PatientSimple(
    @SerialName("_id") val id: String? = null,
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class AdherencePatient(
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("last_medication_at") val lastMedicationAt: String? = null,
    @SerialName("medication_history") val medicationHistory: List<MedicationEntry>? = null
)

@Serializable
data class MedicationEntry(
    val medicine: String? = null,
    val taken: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class NotificationItem(
    @SerialName("_id") val id: String? = null,
    val title: String? = null,
    val message: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val sent: Boolean? = false
)

class DoctorRepository {
    // Ktor Client Setup
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            url("https://doctor.chakravue.co.in")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    private val settings = Settings()

    suspend fun login(email: String, pass: String): LoginResponse {
        try {
            val response = client.post("/login/doctor") {
                setBody(mapOf("email" to email, "password" to pass))
            }

            if (response.status.isSuccess()) {
                val data = response.body<LoginResponse>()
                // Save session
                settings["doctorId"] = data.id
                settings["doctorName"] = data.name
                return data
            } else {
                val err = response.body<ApiError>()
                throw Exception(err.detail)
            }
        } catch (e: Exception) {
            throw Exception(e.message ?: "Connection failed")
        }
    }

    fun isLoggedIn(): Boolean = settings.getStringOrNull("doctorId") != null

    fun getDoctorId(): String = settings.getString("doctorId", "")

    fun getDoctorName(): String = settings.getString("doctorName", "Doctor")

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

    fun logout() {
        settings.remove("doctorId")
        settings.remove("doctorName")
    }

    // Fetch full submission details with all fields
    suspend fun getSubmissionDetails(submissionId: String): SubmissionDetail? {
        return try {
            client.get("/submissions/$submissionId").body()
        } catch (e: Exception) {
            null
        }
    }

    // Fetch full patient record to show prescriptions
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

    // Send the reply note and archive the submission
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

    // Get video call token
    suspend fun getCallToken(channelName: String): CallTokenResponse? {
        return try {
            val response = client.post("/call/token?channel_name=$channelName")
            if (response.status.isSuccess()) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    // Trigger the video call signal
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

    // Get all patients for notification selection
    suspend fun getPatients(): List<PatientSimple> {
        return try {
            client.get("/patients").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Send notification to patients
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

    // Get sent notifications
    suspend fun getNotifications(doctorId: String): List<NotificationItem> {
        return try {
            val response: Map<String, List<NotificationItem>> = client.get("/notifications?doctor_id=$doctorId").body()
            response["notifications"] ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get adherence list
    suspend fun getAdherenceList(doctorId: String): List<AdherencePatient> {
        return try {
            client.get("/doctors/$doctorId/adherence-list").body()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
