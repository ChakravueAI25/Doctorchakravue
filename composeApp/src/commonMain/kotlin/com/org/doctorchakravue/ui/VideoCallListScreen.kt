package com.org.doctorchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.VideoCallRequest
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen

/**
 * VideoCallListScreen - Shows list of video call requests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallListScreen(
    onBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    val repository = remember { ApiRepository() }
    var callRequests by remember { mutableStateOf<List<VideoCallRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val loadRequests = {
        isLoading = true
        error = null
    }

    LaunchedEffect(Unit) {
        loadRequests()
        try {
            // Fetch videocall requests from backend
            val requests = repository.getVideoCallRequests()
            callRequests = requests
            println("Loaded ${requests.size} video call requests")
        } catch (e: Exception) {
            error = "Failed to load requests: ${e.message}"
            println("Error loading video call requests: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Video Call Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DoctorGreen)
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(80.dp), tint = Color.Red.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Error Loading Requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else if (callRequests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(80.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("No Video Call Requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DoctorBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("When patients request video consultations, they will appear here.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(callRequests) { request ->
                    if (request.id != null) {
                        VideoCallRequestCard(request = request, onClick = { onNavigateToDetail(request.id!!) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoCallRequestCard(request: VideoCallRequest, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(DoctorBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VideoCall, null, tint = DoctorBlue, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.patientName ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(request.timestamp ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            val (statusColor, statusBg) = when (request.status) {
                "pending" -> DoctorGreen to DoctorGreen.copy(alpha = 0.1f)
                "missed" -> Color.Red to Color.Red.copy(alpha = 0.1f)
                else -> Color.Gray to Color.Gray.copy(alpha = 0.1f)
            }
            Box(modifier = Modifier.background(statusBg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text((request.status ?: "unknown").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
