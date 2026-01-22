package com.org.doctorchakravue.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.org.doctorchakravue.model.AdherencePatient
import com.org.doctorchakravue.model.MedicationEntry
import com.org.doctorchakravue.ui.theme.DoctorGreen

/**
 * PatientAdherenceDetailScreen - Shows medication history for a patient
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAdherenceDetailScreen(patient: AdherencePatient, onBack: () -> Unit) {
    val name = patient.patientName ?: "Unknown Patient"
    val history = patient.medicationHistory ?: emptyList()

    val groupedHistory: Map<String, List<MedicationEntry>> = remember(history) {
        history.groupBy { entry -> entry.createdAt?.take(10) ?: "Unknown" }
    }
    val sortedDates = remember(groupedHistory) { groupedHistory.keys.sortedDescending() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // NON-SCROLLABLE HEADER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("$name's Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // SCROLLABLE CONTENT
        if (sortedDates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No history available", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
private fun DayAccordion(date: String, entries: List<MedicationEntry>, isFirstDay: Boolean) {
    var expanded by remember { mutableStateOf(isFirstDay) }
    val total = entries.size
    val takenCount = entries.count { (it.taken ?: 0) == 1 }
    val progress = if (total > 0) takenCount.toFloat() / total else 0f
    val progressColor = when { progress >= 1f -> DoctorGreen; progress >= 0.5f -> Color(0xFFFFA726); else -> Color(0xFFEF5350) }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)), shape = RoundedCornerShape(12.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDatePretty(date), fontWeight = FontWeight.Bold)
                        Text("$takenCount / $total Taken", color = progressColor, fontWeight = FontWeight.Bold)
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
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = Color.Gray)
            }
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
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (isTaken) Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint = if (isTaken) DoctorGreen else Color(0xFFEF5350), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(entry.medicine ?: "Unknown", modifier = Modifier.weight(1f), color = Color.DarkGray)
        Text(entry.createdAt?.let { formatTime(it) } ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

private fun formatTimestamp(iso: String?): String = iso?.take(10) ?: "Unknown"

private fun formatDatePretty(date: String): String {
    return try {
        val parts = date.split("-")
        if (parts.size == 3) {
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            "${months.getOrElse(parts[1].toInt()) { parts[1] }} ${parts[2]}, ${parts[0]}"
        } else date
    } catch (e: Exception) { date }
}

private fun formatTime(iso: String): String = if (iso.contains("T")) iso.substringAfter("T").take(5) else ""
