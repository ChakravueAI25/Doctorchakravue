package com.org.doctorchakravue.features.submissions

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
import com.org.doctorchakravue.core.ui.theme.DoctorBlue
import com.org.doctorchakravue.core.ui.theme.DoctorGreen
import com.org.doctorchakravue.data.DoctorRepository
import com.org.doctorchakravue.data.Submission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionDetailScreen(
    submission: Submission,
    onBack: () -> Unit,
    onNavigateToCall: (appId: String, token: String, channelName: String) -> Unit = { _, _, _ -> }
) {
    val repository = remember { DoctorRepository() }
    val viewModel = remember { SubmissionViewModel(repository, submission) }
    val state by viewModel.state.collectAsState()

    var noteText by remember { mutableStateOf("") }
    var commentsExpanded by remember { mutableStateOf(false) }
    var showPrescriptions by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    // Auto-close on success
    LaunchedEffect(state.sendSuccess) {
        if (state.sendSuccess) onBack()
    }

    // Navigate to call when ready
    LaunchedEffect(state.callReady) {
        if (state.callReady && state.callAppId != null && state.callToken != null && state.callChannelName != null) {
            onNavigateToCall(state.callAppId!!, state.callToken!!, state.callChannelName!!)
            viewModel.clearCallState()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Submission Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
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
                // 1. Patient Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = "https://doctor.chakravue.co.in/files/${submission.imageId}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Patient name and timestamp
                    Text(
                        text = state.patientName.ifEmpty { submission.patientName ?: "Unknown" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DoctorBlue
                    )
                    if (state.timestamp.isNotEmpty()) {
                        Text(
                            text = "Submitted: ${state.timestamp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Pain Scale
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

                    // Symptoms Badges - Using real data
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SymptomBadge("Redness", state.redness, Color(0xFFFFEBEE), Color(0xFFD32F2F))
                        SymptomBadge("Swelling", state.swelling, Color(0xFFFFF3E0), Color(0xFFF57C00))
                        SymptomBadge("Discharge", state.discharge, Color(0xFFE3F2FD), Color(0xFF1976D2))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Patient Comments Dropdown - Real data
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
                                Text(
                                    text = state.comments.ifEmpty { "No comments provided." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Medical History Buttons
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

                    // 5. Doctor's Note Input
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

                    // Action Buttons
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

        // Prescriptions Dialog
        if (showPrescriptions) {
            AlertDialog(
                onDismissRequest = { showPrescriptions = false },
                title = { Text("Medications & Prescriptions") },
                text = {
                    Column {
                        if (state.currentMeds.isNotEmpty()) {
                            Text("Current Medications:", fontWeight = FontWeight.Bold, color = DoctorBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            state.currentMeds.forEach {
                                Text("• $it", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        if (state.patientPrescriptions.isNotEmpty()) {
                            Text("Doctor's Prescription:", fontWeight = FontWeight.Bold, color = DoctorBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            state.patientPrescriptions.forEach {
                                Text("• $it", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        if (state.currentMeds.isEmpty() && state.patientPrescriptions.isEmpty()) {
                            Text("No prescriptions found.")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrescriptions = false }) { Text("Close") }
                }
            )
        }

        // Previous Visits Dialog
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
                                        Text(
                                            "Visit ${state.previousVisits.size - index}",
                                            fontWeight = FontWeight.Bold,
                                            color = DoctorBlue
                                        )
                                        visit.at?.let {
                                            Text("Date: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                        visit.problem?.let {
                                            Text("Problem: $it", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        visit.doctorNotes?.let {
                                            Text("Notes: $it", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        visit.procedureType?.let {
                                            Text("Procedure: $it", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistory = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun SymptomBadge(label: String, value: Int, bg: Color, text: Color) {
    Surface(color = bg, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: $value", color = text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
    }
}
