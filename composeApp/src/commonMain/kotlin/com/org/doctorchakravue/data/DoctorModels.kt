package com.org.doctorchakravue.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoctorDto(
    val id: String,
    val name: String
)

@Serializable
data class Submission(
    @SerialName("_id") val id: String? = null,
    @SerialName("patient_name") val patientName: String? = null,
    @SerialName("pain_scale") val painScale: Int? = 0,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("image_file_id") val imageId: String? = null,
    val is_viewed: Boolean? = false
)

