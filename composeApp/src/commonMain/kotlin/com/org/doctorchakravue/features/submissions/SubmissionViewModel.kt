package com.org.doctorchakravue.features.submissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.org.doctorchakravue.data.DoctorRepository
import com.org.doctorchakravue.data.HistoryEntry
import com.org.doctorchakravue.data.Submission
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubmissionDetailState(
    val isLoading: Boolean = false,
    // Submission data
    val patientName: String = "",
    val patientId: String = "",
    val painScale: Int = 0,
    val swelling: Int = 0,
    val redness: Int = 0,
    val discharge: Int = 0,
    val comments: String = "",
    val imageId: String = "",
    val timestamp: String = "",
    // Patient record data
    val patientPrescriptions: List<String> = emptyList(),
    val currentMeds: List<String> = emptyList(),
    val complaints: List<String> = emptyList(),
    val previousVisits: List<HistoryEntry> = emptyList(),
    // Actions
    val isSending: Boolean = false,
    val sendSuccess: Boolean = false,
    // Video call
    val isInitiatingCall: Boolean = false,
    val callToken: String? = null,
    val callAppId: String? = null,
    val callChannelName: String? = null,
    val callReady: Boolean = false
)

class SubmissionViewModel(
    private val repository: DoctorRepository,
    private val submission: Submission
) : ViewModel() {

    private val _state = MutableStateFlow(SubmissionDetailState())
    val state = _state.asStateFlow()
    private val settings = Settings()

    init {
        loadSubmissionDetails()
    }

    private fun loadSubmissionDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // First, set data from the passed submission
            _state.update {
                it.copy(
                    patientName = submission.patientName ?: "Unknown",
                    painScale = submission.painScale ?: 0,
                    imageId = submission.imageId ?: "",
                    timestamp = submission.timestamp ?: ""
                )
            }

            // Fetch full submission details from API
            val submissionId = submission.id
            if (!submissionId.isNullOrEmpty()) {
                val details = repository.getSubmissionDetails(submissionId)
                if (details != null) {
                    _state.update {
                        it.copy(
                            patientId = details.patientId ?: "",
                            swelling = details.swelling ?: 0,
                            redness = details.redness ?: 0,
                            discharge = details.discharge ?: 0,
                            comments = details.comments ?: ""
                        )
                    }
                }
            }

            // Fetch patient record for prescriptions and history
            val patientQuery = submission.patientName
            if (!patientQuery.isNullOrEmpty()) {
                val patientRecord = repository.getFullPatientProfile(patientQuery)
                if (patientRecord != null) {
                    // Extract prescriptions
                    val prescriptions = patientRecord.doctor?.prescription?.map { "${it.key}: ${it.value}" } ?: emptyList()

                    // Extract current medications
                    val meds = patientRecord.drugHistory?.currentMeds?.mapNotNull { med ->
                        val name = med.name ?: med.drug ?: return@mapNotNull null
                        val dosage = med.dosage ?: ""
                        if (dosage.isNotEmpty()) "$name • $dosage" else name
                    } ?: emptyList()

                    // Extract complaints
                    val complaints = patientRecord.presentingComplaints?.complaints?.mapNotNull { c ->
                        val complaint = c.complaint ?: return@mapNotNull null
                        val duration = c.duration ?: ""
                        if (duration.isNotEmpty()) "$complaint • $duration" else complaint
                    } ?: emptyList()

                    // Extract history
                    val history = patientRecord.history ?: emptyList()

                    _state.update {
                        it.copy(
                            patientPrescriptions = prescriptions,
                            currentMeds = meds,
                            complaints = complaints,
                            previousVisits = history
                        )
                    }
                }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun sendNote(note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            val doctorId = settings.getString("doctorId", "")
            val success = repository.sendSubmissionNote(submission.id ?: "", note, doctorId)
            _state.update { it.copy(isSending = false, sendSuccess = success) }
        }
    }

    fun startCall() {
        viewModelScope.launch {
            _state.update { it.copy(isInitiatingCall = true) }

            val doctorId = settings.getString("doctorId", "")
            val patientId = _state.value.patientId.ifEmpty { submission.patientName ?: "" }
            val channelName = "call_$patientId"

            // Get token from backend
            val tokenResponse = repository.getCallToken(channelName)
            if (tokenResponse != null) {
                // Notify patient
                repository.initiateCall(doctorId, patientId, channelName)

                _state.update {
                    it.copy(
                        isInitiatingCall = false,
                        callToken = tokenResponse.token,
                        callAppId = tokenResponse.app_id,
                        callChannelName = channelName,
                        callReady = true
                    )
                }
            } else {
                _state.update { it.copy(isInitiatingCall = false) }
            }
        }
    }

    fun clearCallState() {
        _state.update {
            it.copy(
                callToken = null,
                callAppId = null,
                callChannelName = null,
                callReady = false
            )
        }
    }
}
