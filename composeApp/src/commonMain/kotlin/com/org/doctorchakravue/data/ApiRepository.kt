package com.org.doctorchakravue.data

import com.org.doctorchakravue.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * ApiRepository - Handles all API communication with the backend.
 * Uses SessionManager for credential storage.
 */
class ApiRepository(
    private val sessionManager: SessionManager = SessionManager()
) {
    companion object {

        // admin.chakravue.co.in is the EMR backend served directly (no nginx path rewrite),
        // so the mobile router's own /api/mobile prefix must be part of the base URL.
        //   curl https://admin.chakravue.co.in/api/mobile/  ->  {"status":"ok"}
        const val BASE_URL = "https://admin.chakravue.co.in/api/mobile"
        fun fileImageUrl(fileId: String?): String = if (fileId.isNullOrEmpty()) "" else "$BASE_URL/files/$fileId"
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // ponytail: without these two, a dead backend looks identical to "nothing happened" —
        // no logcat line and a ~20s silent hang.
        install(Logging) {
            level = LogLevel.INFO
            // Logger.DEFAULT routes through SLF4J, which has no binding on Android and
            // silently drops everything. println() lands in logcat under "System.out".
            logger = object : Logger {
                override fun log(message: String) = println("[Ktor] $message")
            }
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        defaultRequest {
            url("$BASE_URL/")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        // Forward every failed call (non-2xx and network/timeout) to the backend
        // "mobile_app" logger so errors are visible in the server logs.
        HttpResponseValidator {
            validateResponse { response ->
                if (!response.status.isSuccess()) {
                    RemoteLogger.report(
                        "error",
                        "HTTP ${response.status.value} ${response.request.method.value} ${response.request.url.encodedPath}"
                    )
                }
            }
            handleResponseExceptionWithRequest { cause, request ->
                RemoteLogger.report(
                    "error",
                    "NET ${request.method.value} ${request.url.encodedPath} :: ${cause.message}"
                )
                throw cause
            }
        }
    }

    // --- Session Delegation ---
    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun getDoctorId(): String = sessionManager.getDoctorId()
    fun getDoctorName(): String = sessionManager.getDoctorName()
    fun logout() = sessionManager.logout()

    // --- Authentication ---
    suspend fun login(email: String, pass: String): LoginResponse {
        val response = try {
            client.post("login/doctor") {
                setBody(mapOf("email" to email, "password" to pass))
            }
        } catch (e: Exception) {
            throw Exception("Cannot reach server: ${e.message ?: "connection failed"}")
        }

        if (!response.status.isSuccess()) {
            // Error bodies are not always JSON — a proxy 5xx (e.g. Cloudflare 522) is
            // text/plain, and body<ApiError>() then throws a Ktor internal message that
            // hides the real status. Read text, try for "detail", fall back to the code.
            val raw = runCatching { response.bodyAsText() }.getOrDefault("")
            val detail = runCatching {
                Json.parseToJsonElement(raw).jsonObject["detail"]?.jsonPrimitive?.content
            }.getOrNull()
            throw Exception(detail ?: "Login failed (HTTP ${response.status.value}) ${raw.take(120)}")
        }

        val data = response.body<LoginResponse>()
        sessionManager.saveSession(data.id, data.name ?: data.email, data.email)
        return data
    }

    // --- Submissions ---
    suspend fun getUrgentSubmissions(doctorId: String): List<Submission> {
        return try {
            client.get("submissions/doctor/$doctorId").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHistory(doctorId: String): List<Submission> {
        return try {
            client.get("submissions/doctor/$doctorId/history").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getVisionSubmissions(doctorId: String): List<Submission> {
        return try {
            client.get("submissions/doctor/$doctorId/vision-tests").body()
        } catch (e: Exception) {
            // Fallback: try to filter from history if specific endpoint doesn't exist
            emptyList()
        }
    }

    suspend fun getSubmissionDetails(submissionId: String): SubmissionDetail? {
        return try {
            client.get("submissions/$submissionId").body()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sendSubmissionNote(submissionId: String, note: String, doctorId: String): Boolean {
        return try {
            val response = client.post("submissions/$submissionId/notes") {
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
            val response = client.get("patients")
            println("DEBUG: getPatients response status: ${response.status}")
            val responseText = response.bodyAsText()
            println("DEBUG: getPatients response body (first 500 chars): ${responseText.take(500)}")

            // Parse the response with ignoreUnknownKeys
            val jsonParser = Json { ignoreUnknownKeys = true }
            val patients: List<PatientSimple> = jsonParser.decodeFromString(responseText)
            println("DEBUG: Parsed ${patients.size} patients")
            patients.forEach { p ->
                println("DEBUG: Patient - id: ${p.id}, name: ${p.name}, email: ${p.email}, displayName: ${p.displayName}, displayEmail: ${p.displayEmail}")
            }
            patients
        } catch (e: Exception) {
            println("DEBUG: getPatients error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getFullPatientProfile(query: String?): PatientRecord? {
        return try {
            if (!query.isNullOrEmpty()) {
                val resp = client.get("patients/case/search/?query=$query")
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
            val response = client.post("call/token?channel_name=$channelName")
            val rawJson = response.bodyAsText()
            println("[VideoCall] /call/token raw response: $rawJson")

            if (!response.status.isSuccess()) {
                println("[VideoCall] /call/token failed with status: ${response.status}")
                return null
            }

            // Try flexible parsing — patient backend may return different field names
            val json = Json { ignoreUnknownKeys = true }
            try {
                val parsed = json.decodeFromString<CallTokenResponse>(rawJson)
                val resolvedId = parsed.resolvedAppId
                println("[VideoCall] Parsed → app_id='${parsed.app_id}' appId='${parsed.appId}' resolvedAppId='$resolvedId' token.len=${parsed.token.length}")

                // If resolvedAppId is still blank, try extracting from raw JSON keys
                if (resolvedId.isBlank()) {
                    println("[VideoCall] WARNING: resolvedAppId is BLANK. Attempting fallback extraction from raw JSON...")
                    try {
                        val jsonObj = json.parseToJsonElement(rawJson).jsonObject
                        println("[VideoCall] Raw JSON keys: ${jsonObj.keys}")
                        // Search for any key containing "app" and "id" (case-insensitive)
                        val appIdKey = jsonObj.keys.firstOrNull { key ->
                            key.lowercase().contains("app") && key.lowercase().contains("id")
                        }
                        if (appIdKey != null) {
                            val extractedAppId = jsonObj[appIdKey]?.jsonPrimitive?.content ?: ""
                            println("[VideoCall] Fallback: found key='$appIdKey' value='$extractedAppId'")
                            if (extractedAppId.isNotBlank()) {
                                return CallTokenResponse(
                                    token = parsed.token,
                                    app_id = extractedAppId
                                )
                            }
                        } else {
                            println("[VideoCall] No key matching '*app*id*' found in response keys: ${jsonObj.keys}")
                        }
                    } catch (e: Exception) {
                        println("[VideoCall] Fallback JSON extraction failed: ${e.message}")
                    }
                }

                parsed
            } catch (e: Exception) {
                println("[VideoCall] Parse error: ${e.message} — raw: $rawJson")
                null
            }
        } catch (e: Exception) {
            println("[VideoCall] getCallToken exception: ${e.message}")
            null
        }
    }

    // Fetch video call requests for all doctors (doctor_id is always null in DB, so no filtering)
    suspend fun getVideoCallRequests(status: String? = null): List<com.org.doctorchakravue.model.VideoCallRequest> {
        return try {
            val url = if (!status.isNullOrEmpty()) "videocallrequests?status=$status" else "videocallrequests"
            val response = client.get(url)

            // Try to parse as VideoCallRequestsResponse first
            return try {
                val parsed: com.org.doctorchakravue.model.VideoCallRequestsResponse = response.body()
                parsed.requests
            } catch (e: Exception) {
                // If that fails, try parsing as a direct list
                try {
                    val parsed: List<com.org.doctorchakravue.model.VideoCallRequest> = response.body()
                    parsed
                } catch (e2: Exception) {
                    println("Failed to parse video call requests: ${e2.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            println("Failed to fetch video call requests: ${e.message}")
            emptyList()
        }
    }

    suspend fun initiateCall(doctorId: String, patientId: String, channelName: String): Boolean {
        return try {
            client.post("call/initiate") {
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

            val response = client.post("notifications") {
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
            val response: Map<String, List<NotificationItem>> = client.get("notifications?doctor_id=$doctorId").body()
            response["notifications"] ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Adherence ---
    suspend fun getAdherenceList(doctorId: String): List<AdherencePatient> {
        return try {
            client.get("doctors/$doctorId/adherence-list").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Refetch one patient's adherence by id and rebuild the AdherencePatient the detail screen needs.
    suspend fun getPatientAdherence(patientId: String): AdherencePatient? {
        return try {
            val resp: com.org.doctorchakravue.model.AdherenceLogsResponse =
                client.get("adherence/patient/$patientId").body()
            val logs = resp.adherence
            AdherencePatient(
                patientId = patientId,
                patientName = logs.firstOrNull { !it.patientName.isNullOrBlank() }?.patientName,
                lastMedicationAt = logs.firstOrNull()?.createdAt,
                medicationHistory = logs.map { MedicationEntry(it.medicine, it.taken, it.createdAt) }
            )
        } catch (e: Exception) {
            println("getPatientAdherence failed: ${e.message}")
            null
        }
    }

    // --- Slit Lamp Images ---
    suspend fun getAllSlitLampImages(): List<SlitLampImage> {
        return try {
            val response = client.get("slit-lamp/all")
            if (response.status.isSuccess()) {
                val parsed: SlitLampImagesResponse = response.body()
                parsed.images
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Failed to fetch slit lamp images: ${e.message}")
            emptyList()
        }
    }

    // --- FCM Token Registration ---
    suspend fun registerFcmToken(doctorId: String, fcmToken: String): Boolean {
        return try {
            val response = client.post("doctors/$doctorId/fcm-token") {
                setBody(mapOf(
                    "fcm_token" to fcmToken,
                    "platform" to "android",
                    "app_type" to "doctor_app"
                ))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Failed to register FCM token: ${e.message}")
            false
        }
    }

    // --- Legal/Consent ---
    suspend fun recordConsent(doctorId: String, version: Int): Boolean {
        return try {
            // EMR mobile module exposes POST /consent {user_id, role, terms_version} —
            // there is no /doctors/{id}/consent route, so this silently no-opped before.
            val response = client.post("consent") {
                setBody(ConsentBody(user_id = doctorId, role = "doctor", terms_version = version))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Failed to record consent: ${e.message}")
            false
        }
    }
}

/** Fire-and-forget client error reporter -> backend "mobile_app" logger. */
object RemoteLogger {
    private const val APP = "doctor"
    private val scope = CoroutineScope(Dispatchers.Default)
    // Own client with NO validator, so a failing report never recurses.
    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    fun report(level: String, message: String, context: String? = null) {
        scope.launch {
            runCatching {
                client.post("${ApiRepository.BASE_URL}/client-logs") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "app" to APP, "level" to level,
                            "message" to message, "context" to (context ?: "")
                        )
                    )
                }
            }
        }
    }
}
