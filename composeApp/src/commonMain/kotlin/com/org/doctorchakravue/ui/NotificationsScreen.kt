package com.org.doctorchakravue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.PatientSimple
import com.org.doctorchakravue.ui.theme.AppBackground
import com.org.doctorchakravue.ui.theme.AppTheme
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen
import kotlinx.coroutines.launch

/**
 * NotificationsScreen - Compose and send notifications to patients.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {}
) {
    val repository = remember { ApiRepository() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sendToAll by remember { mutableStateOf(true) }
    var patients by remember { mutableStateOf<List<PatientSimple>>(emptyList()) }
    var selectedEmails by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingPatients by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var showPatientSelector by remember { mutableStateOf(false) }

    LaunchedEffect(sendToAll) {
        if (!sendToAll && patients.isEmpty()) {
            isLoadingPatients = true
            patients = repository.getPatients()
            isLoadingPatients = false
        }
    }

    fun sendNotification() {
        if (message.isBlank()) return
        scope.launch {
            isSending = true
            val doctorId = repository.getDoctorId()
            val success = repository.sendNotification(
                doctorId = doctorId,
                title = title.ifBlank { "Message from Dr. ${repository.getDoctorName()}" },
                message = message,
                sendToAll = sendToAll,
                selectedEmails = selectedEmails.toList()
            )
            isSending = false
            if (success) onBack()
        }
    }

    AppTheme {
        AppBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                // NON-SCROLLABLE HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                // SCROLLABLE CONTENT
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoctorBlue,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DoctorBlue,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Recipients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(
                            selected = sendToAll,
                            onClick = { sendToAll = true },
                            colors = RadioButtonDefaults.colors(selectedColor = DoctorGreen)
                        )
                        Text("All Patients", modifier = Modifier.clickable { sendToAll = true })
                        Spacer(modifier = Modifier.width(24.dp))
                        RadioButton(
                            selected = !sendToAll,
                            onClick = { sendToAll = false },
                            colors = RadioButtonDefaults.colors(selectedColor = DoctorGreen)
                        )
                        Text("Select Patients", modifier = Modifier.clickable { sendToAll = false })
                    }

                    if (!sendToAll) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Selected: ${selectedEmails.size} patients", fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { showPatientSelector = true }) {
                                        Text("Select", color = DoctorBlue)
                                    }
                                }
                                if (isLoadingPatients) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.CenterHorizontally),
                                        color = DoctorGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { sendNotification() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoctorGreen),
                        enabled = !isSending && message.isNotBlank()
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Send, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Notification", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showPatientSelector) {
                    AlertDialog(
                        onDismissRequest = { showPatientSelector = false },
                        title = { Text("Select Patients") },
                        text = {
                            if (patients.isEmpty()) {
                                Text("No patients available.")
                            } else {
                                LazyColumn(modifier = Modifier.height(300.dp)) {
                                    items(patients) { patient ->
                                        val email = patient.email ?: ""
                                        val isSelected = selectedEmails.contains(email)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedEmails = if (isSelected) selectedEmails - email else selectedEmails + email
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    selectedEmails = if (it) selectedEmails + email else selectedEmails - email
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = DoctorGreen)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(patient.name ?: "Unknown", fontWeight = FontWeight.Medium)
                                                Text(email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showPatientSelector = false }) {
                                Text("Done", color = DoctorGreen)
                            }
                        }
                    )
                }
                }
            }
        }
    }

