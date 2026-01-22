package com.org.doctorchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.HistoryEntry
import com.org.doctorchakravue.model.Submission
import com.org.doctorchakravue.ui.theme.AppBackground
import com.org.doctorchakravue.ui.theme.AppTheme
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen
import com.org.doctorchakravue.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- State ---
data class PainScaleDetailState(
    val isLoading: Boolean = false,
    val patientName: String = "",
    val patientId: String = "",
    val painScale: Int = 0,
    val swelling: Int = 0,
    val redness: Int = 0,
    val discharge: Int = 0,
    val comments: String = "",
    val imageId: String = "",
    val timestamp: String = "",
    val patientPrescriptions: List<String> = emptyList(),
    val currentMeds: List<String> = emptyList(),
    val previousVisits: List<HistoryEntry> = emptyList(),
    val isSending: Boolean = false,
    val sendSuccess: Boolean = false,
    val isInitiatingCall: Boolean = false,
    val callToken: String? = null,
    val callAppId: String? = null,
    val callChannelName: String? = null,
    val callReady: Boolean = false
)

// --- ViewModel ---
class PainScaleDetailViewModel(
    private val repository: ApiRepository,
    private val submission: Submission
) {
    private val _state = MutableStateFlow(PainScaleDetailState())
    val state = _state.asStateFlow()
    private val sessionManager = SessionManager()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        loadDetails()
    }

    private fun loadDetails() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }

            _state.update {
                it.copy(
                    patientName = submission.patientName ?: "Unknown",
                    painScale = submission.painScale ?: 0,
                    imageId = submission.imageId ?: "",
                    timestamp = submission.timestamp ?: ""
                )
            }

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

            val patientQuery = submission.patientName
            if (!patientQuery.isNullOrEmpty()) {
                val patientRecord = repository.getFullPatientProfile(patientQuery)
                if (patientRecord != null) {
                    val prescriptions = patientRecord.doctor?.prescription?.map { "${it.key}: ${it.value}" } ?: emptyList()
                    val meds = patientRecord.drugHistory?.currentMeds?.mapNotNull { med ->
                        val name = med.name ?: med.drug ?: return@mapNotNull null
                        val dosage = med.dosage ?: ""
                        if (dosage.isNotEmpty()) "$name • $dosage" else name
                    } ?: emptyList()
                    val history = patientRecord.history ?: emptyList()

                    _state.update {
                        it.copy(
                            patientPrescriptions = prescriptions,
                            currentMeds = meds,
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
        scope.launch {
            _state.update { it.copy(isSending = true) }
            val doctorId = sessionManager.getDoctorId()
            val success = repository.sendSubmissionNote(submission.id ?: "", note, doctorId)
            _state.update { it.copy(isSending = false, sendSuccess = success) }
        }
    }

    fun startCall() {
        scope.launch {
            _state.update { it.copy(isInitiatingCall = true) }
            val doctorId = sessionManager.getDoctorId()
            val patientId = _state.value.patientId.ifEmpty { submission.patientName ?: "" }
            val channelName = "call_$patientId"

            val tokenResponse = repository.getCallToken(channelName)
            if (tokenResponse != null) {
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
        _state.update { it.copy(callToken = null, callAppId = null, callChannelName = null, callReady = false) }
    }
}

// --- Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainScaleDetailScreen(
    submission: Submission,
    onBack: () -> Unit,
    onNavigateToCall: (appId: String, token: String, channelName: String) -> Unit = { _, _, _ -> }
) {
    val repository = remember { ApiRepository() }
    val viewModel = remember { PainScaleDetailViewModel(repository, submission) }
    val state by viewModel.state.collectAsState()

    var noteText by remember { mutableStateOf("") }
    var commentsExpanded by remember { mutableStateOf(false) }
    var showPrescriptions by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    LaunchedEffect(state.sendSuccess) {
        if (state.sendSuccess) onBack()
    }

    LaunchedEffect(state.callReady) {
        if (state.callReady && state.callAppId != null && state.callToken != null && state.callChannelName != null) {
            onNavigateToCall(state.callAppId!!, state.callToken!!, state.callChannelName!!)
            viewModel.clearCallState()
        }
    }

    AppTheme {
        AppBackground {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Submission Detail", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                containerColor = Color.White
            ) { padding ->
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DoctorGreen)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black)) {
                            AsyncImage(
                                model = "https://doctor.chakravue.co.in/files/${submission.imageId}",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = state.patientName.ifEmpty { submission.patientName ?: "Unknown" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DoctorBlue
                            )
                            if (state.timestamp.isNotEmpty()) {
                                Text("Submitted: ${state.timestamp}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Pain Scale", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = (state.painScale.takeIf { it > 0 } ?: submission.painScale ?: 0).toFloat(),
                                    onValueChange = {},
                                    valueRange = 0f..10f,
                                    enabled = false,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        disabledThumbColor = DoctorBlue,
                                        disabledActiveTrackColor = DoctorBlue,
                                        disabledInactiveTrackColor = Color(0xFFE0E0E0)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "${state.painScale.takeIf { it > 0 } ?: submission.painScale ?: 0}/10",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DoctorBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SymptomBadge("Redness", state.redness, Color(0xFFFFEBEE), Color(0xFFD32F2F))
                                SymptomBadge("Swelling", state.swelling, Color(0xFFFFF3E0), Color(0xFFF57C00))
                                SymptomBadge("Discharge", state.discharge, Color(0xFFE3F2FD), Color(0xFF1976D2))
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Card(
                                onClick = { commentsExpanded = !commentsExpanded },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Patient Comments", fontWeight = FontWeight.Bold)
                                        Icon(
                                            if (commentsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                    if (commentsExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(state.comments.ifEmpty { "No comments provided." }, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { showPrescriptions = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(60.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Medication, null, tint = DoctorBlue)
                                        Text("Prescriptions", color = DoctorBlue, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = { showHistory = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(60.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.History, null, tint = DoctorBlue)
                                        Text("Previous Visits", color = DoctorBlue, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Doctor's Action", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                placeholder = { Text("Write your advice here...") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DoctorGreen,
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row {
                                Button(
                                    onClick = { viewModel.startCall() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DoctorBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    enabled = !state.isInitiatingCall
                                ) {
                                    if (state.isInitiatingCall) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Icon(Icons.Default.VideoCall, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Video Call")
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = { viewModel.sendNote(noteText) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DoctorGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    enabled = !state.isSending && noteText.isNotBlank()
                                ) {
                                    if (state.isSending) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text("Send & Archive")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                if (showPrescriptions) {
                    AlertDialog(
                        onDismissRequest = { showPrescriptions = false },
                        title = { Text("Medications & Prescriptions") },
                        text = {
                            Column {
                                if (state.currentMeds.isNotEmpty()) {
                                    Text("Current Medications:", fontWeight = FontWeight.Bold, color = DoctorBlue)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    state.currentMeds.forEach { Text("• $it"); Spacer(modifier = Modifier.height(4.dp)) }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                if (state.patientPrescriptions.isNotEmpty()) {
                                    Text("Doctor's Prescription:", fontWeight = FontWeight.Bold, color = DoctorBlue)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    state.patientPrescriptions.forEach { Text("• $it"); Spacer(modifier = Modifier.height(4.dp)) }
                                }
                                if (state.currentMeds.isEmpty() && state.patientPrescriptions.isEmpty()) {
                                    Text("No prescriptions found.")
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showPrescriptions = false }) { Text("Close") } }
                    )
                }

                if (showHistory) {
                    AlertDialog(
                        onDismissRequest = { showHistory = false },
                        title = { Text("Previous Visits") },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                if (state.previousVisits.isEmpty()) {
                                    Text("No previous visits found.")
                                } else {
                                    state.previousVisits.forEachIndexed { index, visit ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("Visit ${state.previousVisits.size - index}", fontWeight = FontWeight.Bold, color = DoctorBlue)
                                                visit.at?.let { Text("Date: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                                visit.problem?.let { Text("Problem: $it") }
                                                visit.doctorNotes?.let { Text("Notes: $it") }
                                                visit.procedureType?.let { Text("Procedure: $it") }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SymptomBadge(label: String, value: Int, bg: Color, text: Color) {
    Surface(color = bg, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: $value", color = text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
    }
}
