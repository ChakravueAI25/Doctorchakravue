package com.org.doctorchakravue.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Authentication ---
@Serializable
data class LoginResponse(
    @SerialName("_id") val id: String,
    val name: String,
    val email: String
)

@Serializable
data class ApiError(val detail: String)

// --- Video Call ---
@Serializable
data class CallTokenResponse(
    val token: String,
    val app_id: String
)

// --- Patient Records ---
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

// --- Submissions ---
@Serializable
data class Submission(
    @SerialName("_id") val id: String? = null,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("pain_scale") val painScale: Int? = 0,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("image_file_id") val imageId: String? = null,
    val is_viewed: Boolean? = false,
    @SerialName("form_name") val formName: String? = null,
    @SerialName("submission_type") val submissionType: String? = null
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

// --- Patients ---
@Serializable
data class PatientSimple(
    @SerialName("_id") val id: String? = null,
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class DoctorDto(
    val id: String,
    val name: String
)

// --- Adherence ---
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

// --- Notifications ---
@Serializable
data class NotificationItem(
    @SerialName("_id") val id: String? = null,
    val title: String? = null,
    val message: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val sent: Boolean? = false
)

// --- Video Call Requests ---
@Serializable
data class VideoCallRequest(
    @SerialName("_id") val id: String? = null,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("created_at") val timestamp: String? = null,
    val status: String? = null,
    val reason: String? = null
)

@Serializable
data class VideoCallRequestsResponse(
    @SerialName("videocallrequests")
    val requests: List<VideoCallRequest> = emptyList()
)

// --- Slit Lamp Images ---
@Serializable
data class SlitLampImage(
    @SerialName("_id") val id: String? = null,
    val patientId: String? = null,
    val patientName: String? = null,
    val doctorName: String? = null,
    val image: String? = null,  // Base64 encoded image string
    val notes: String? = null,
    val timestamp: String? = null,
    val eyeSide: String? = null  // "Left", "Right", or "Both"
)

@Serializable
data class SlitLampImagesResponse(
    val images: List<SlitLampImage> = emptyList()
)

