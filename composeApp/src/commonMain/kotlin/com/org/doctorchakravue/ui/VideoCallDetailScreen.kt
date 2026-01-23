package com.org.doctorchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.VideoCallRequest
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen

/**
 * VideoCallDetailScreen - Shows details of a video call request.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallDetailScreen(
    callId: String,
    onBack: () -> Unit = {},
    onStartCall: (appId: String, token: String, channelName: String) -> Unit = { _, _, _ -> }
) {
    val repository = remember { ApiRepository() }
    var callRequest by remember { mutableStateOf<VideoCallRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isJoiningCall by remember { mutableStateOf(false) }

    LaunchedEffect(callId) {
        callRequest = null // Backend not ready
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Call Request", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DoctorGreen)
            }
        } else if (callRequest == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp)).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.VideoCall, null, modifier = Modifier.size(80.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Call Not Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DoctorBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This call request may have expired or been cancelled.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = DoctorBlue), shape = RoundedCornerShape(12.dp)) {
                            Text("Go Back")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(DoctorBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(callRequest?.patientName?.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = DoctorBlue)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(callRequest?.patientName ?: "Unknown", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Requested: ${callRequest?.timestamp ?: ""}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))

                Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp)).padding(20.dp)) {
                    Column {
                        Text("Call Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DoctorBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailRow("Status", (callRequest?.status ?: "unknown").replaceFirstChar { it.uppercase() })
                        DetailRow("Patient ID", callRequest?.patientId ?: "")
                        DetailRow("Request Time", callRequest?.timestamp ?: "")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Decline")
                    }
                    Button(
                        onClick = { isJoiningCall = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DoctorGreen),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isJoiningCall
                    ) {
                        if (isJoiningCall) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Call, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join Call")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
