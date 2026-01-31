package com.org.doctorchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.PatientSimple
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen

/**
 * PatientsScreen - Shows list of patients with search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(onBack: () -> Unit = {}) {
    val repository = remember { ApiRepository() }
    var allPatients by remember { mutableStateOf<List<PatientSimple>>(emptyList()) }
    var filteredPatients by remember { mutableStateOf<List<PatientSimple>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Filter patients based on search text
    LaunchedEffect(searchText, allPatients) {
        filteredPatients = if (searchText.isBlank()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                patient.name?.contains(searchText, ignoreCase = true) == true ||
                patient.email?.contains(searchText, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(Unit) {
        allPatients = repository.getPatients()
        filteredPatients = allPatients
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with search functionality
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (isSearchVisible) {
                // Search field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search patients...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                            focusedLabelColor = Color.White,
                            cursorColor = Color.White
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        },
                        trailingIcon = if (searchText.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.White
                                    )
                                }
                            }
                        } else null
                    )

                    // Close search button
                    TextButton(
                        onClick = {
                            isSearchVisible = false
                            searchText = ""
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Header with search icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Patients",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { isSearchVisible = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search patients",
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DoctorGreen)
            }
        } else if (filteredPatients.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (searchText.isNotEmpty()) "No Matching Patients" else "No Patients Found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DoctorBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchText.isNotEmpty())
                            "No patients found matching \"$searchText\". Try a different search term."
                        else
                            "Your patient list will appear here once patients register.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Show search results count if searching
            if (searchText.isNotEmpty()) {
                Text(
                    text = "${filteredPatients.size} patient(s) found",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredPatients) { patient ->
                    PatientCard(patient = patient, searchText = searchText)
                }
            }
        }
    }
}


@Composable
private fun PatientCard(patient: PatientSimple, searchText: String = "") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DoctorBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (patient.name?.firstOrNull()?.uppercase() ?: "?"),
                    color = DoctorBlue,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = patient.name ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = patient.email ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Show search match indicator if searching
                if (searchText.isNotEmpty() &&
                    (patient.name?.contains(searchText, ignoreCase = true) == true ||
                     patient.email?.contains(searchText, ignoreCase = true) == true)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = DoctorGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Match",
                            style = MaterialTheme.typography.labelSmall,
                            color = DoctorGreen,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
