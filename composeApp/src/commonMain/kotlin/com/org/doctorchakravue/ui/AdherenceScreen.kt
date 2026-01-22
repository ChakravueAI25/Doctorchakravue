package com.org.doctorchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.AdherencePatient
import com.org.doctorchakravue.ui.theme.AppBackground
import com.org.doctorchakravue.ui.theme.AppTheme
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen

/**
 * AdherenceScreen - Drug adherence tracking for patients.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceScreen(
    onNavigateToDetail: (AdherencePatient) -> Unit = {}
) {
    val repository = remember { ApiRepository() }
    var patients by remember { mutableStateOf<List<AdherencePatient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val doctorId = repository.getDoctorId()
        patients = repository.getAdherenceList(doctorId)
        isLoading = false
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
                    Text("Drug Adherence", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }

                // SCROLLABLE CONTENT
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DoctorGreen)
                    }
                } else {
                    if (patients.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Medication, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No medication history found.", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(patients) { patient ->
                                PatientAdherenceCard(patient = patient, onClick = { onNavigateToDetail(patient) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientAdherenceCard(patient: AdherencePatient, onClick: () -> Unit) {
    val name = patient.patientName ?: "Unknown Patient"
    val history = patient.medicationHistory ?: emptyList()
    val totalCount = history.size
    val takenCount = history.count { (it.taken ?: 0) == 1 }
    val adherenceRate = if (totalCount > 0) (takenCount.toFloat() / totalCount * 100).toInt() else 0

    val adherenceColor = when {
        adherenceRate >= 80 -> DoctorGreen
        adherenceRate >= 50 -> Color(0xFFFFA726)
        else -> Color(0xFFEF5350)
    }

    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(DoctorBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.firstOrNull()?.uppercase() ?: "?", color = DoctorBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold)
                Text("Last active: ${formatTimestamp(patient.lastMedicationAt)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("$totalCount total entries", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$adherenceRate%", color = adherenceColor, fontWeight = FontWeight.Bold)
                Text("adherence", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}

private fun formatTimestamp(iso: String?): String = iso?.take(10) ?: "Unknown"
