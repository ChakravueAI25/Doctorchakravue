package com.org.doctorchakravue.features.adherence

import androidx.compose.animation.AnimatedVisibility
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
import com.org.doctorchakravue.core.ui.theme.AppBackground
import com.org.doctorchakravue.core.ui.theme.AppTheme
import com.org.doctorchakravue.core.ui.theme.DoctorBlue
import com.org.doctorchakravue.core.ui.theme.DoctorGreen
import com.org.doctorchakravue.data.AdherencePatient
import com.org.doctorchakravue.data.DoctorRepository
import com.org.doctorchakravue.data.MedicationEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceScreen(
    onBack: () -> Unit = {}
) {
    val repository = remember { DoctorRepository() }
    var patients by remember { mutableStateOf<List<AdherencePatient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPatient by remember { mutableStateOf<AdherencePatient?>(null) }

    // Load adherence data
    LaunchedEffect(Unit) {
        val doctorId = repository.getDoctorId()
        patients = repository.getAdherenceList(doctorId)
        isLoading = false
    }

    AppTheme {
        AppBackground {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Drug Adherence", fontWeight = FontWeight.Bold) },
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
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DoctorGreen)
                    }
                } else if (selectedPatient != null) {
                    // Detail view
                    PatientAdherenceDetail(
                        patient = selectedPatient!!,
                        onBack = { selectedPatient = null }
                    )
                } else {
                    // List view
                    if (patients.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No medication history found.", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(patients) { patient ->
                                PatientAdherenceCard(
                                    patient = patient,
                                    onClick = { selectedPatient = patient }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientAdherenceCard(
    patient: AdherencePatient,
    onClick: () -> Unit
) {
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

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DoctorBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "?",
                    color = DoctorBlue,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Last active: ${formatTimestamp(patient.lastMedicationAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    "$totalCount total entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Adherence badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$adherenceRate%",
                    color = adherenceColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "adherence",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientAdherenceDetail(
    patient: AdherencePatient,
    onBack: () -> Unit
) {
    val name = patient.patientName ?: "Unknown Patient"
    val history = patient.medicationHistory ?: emptyList()

    // Group by date - using explicit type
    val groupedHistory: Map<String, List<MedicationEntry>> = remember(history) {
        history.groupBy { entry ->
            entry.createdAt?.take(10) ?: "Unknown"
        }
    }

    // Sort keys descending
    val sortedDates = remember(groupedHistory) {
        groupedHistory.keys.sortedDescending()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("$name's Progress", fontWeight = FontWeight.Bold) },
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
        if (sortedDates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No history available", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(sortedDates.size) { index ->
                    val date = sortedDates[index]
                    val entries = groupedHistory[date] ?: emptyList()
                    DayAccordion(date = date, entries = entries, isFirstDay = index == 0)
                }
            }
        }
    }
}

@Composable
private fun DayAccordion(
    date: String,
    entries: List<MedicationEntry>,
    isFirstDay: Boolean
) {
    var expanded by remember { mutableStateOf(isFirstDay) }

    val total = entries.size
    val takenCount = entries.count { (it.taken ?: 0) == 1 }
    val progress = if (total > 0) takenCount.toFloat() / total else 0f

    val progressColor = when {
        progress >= 1f -> DoctorGreen
        progress >= 0.5f -> Color(0xFFFFA726)
        else -> Color(0xFFEF5350)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatDatePretty(date),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "$takenCount / $total Taken",
                            color = progressColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = progressColor,
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            // Expanded content
            AnimatedVisibility(visible = expanded) {
                Column {
                    entries.forEach { entry ->
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        MedicationEntryRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationEntryRow(entry: MedicationEntry) {
    val isTaken = (entry.taken ?: 0) == 1
    val medName = entry.medicine ?: "Unknown"
    val timeStr = entry.createdAt?.let { formatTime(it) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isTaken) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isTaken) DoctorGreen else Color(0xFFEF5350),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            medName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
        Text(
            timeStr,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

// Helper functions
private fun formatTimestamp(iso: String?): String {
    if (iso.isNullOrEmpty()) return "Unknown"
    return try {
        // Simple parsing - just show date part
        iso.take(10)
    } catch (e: Exception) {
        iso
    }
}

private fun formatDatePretty(date: String): String {
    // Date comes as "2025-01-08" format
    return try {
        val parts = date.split("-")
        if (parts.size == 3) {
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val month = months.getOrElse(parts[1].toInt()) { parts[1] }
            "$month ${parts[2]}, ${parts[0]}"
        } else date
    } catch (e: Exception) {
        date
    }
}

private fun formatTime(iso: String): String {
    return try {
        // Extract time from ISO string like "2025-01-08T14:30:00"
        if (iso.contains("T")) {
            iso.substringAfter("T").take(5)
        } else ""
    } catch (e: Exception) {
        ""
    }
}
