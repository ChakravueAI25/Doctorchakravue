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
import com.org.doctorchakravue.data.SessionManager
import com.org.doctorchakravue.model.VideoCallRequest
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen
import kotlinx.coroutines.launch

/**
 * VideoCallDetailScreen - Shows details of a video call request.
 * The "Join Call" button now fetches a token and calls onStartCall().
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallDetailScreen(
    callId: String,
    onBack: () -> Unit = {},
    onStartCall: (appId: String, token: String, channelName: String) -> Unit = { _, _, _ -> }
) {
    val repository = remember { ApiRepository() }
    val sessionManager = remember { SessionManager() }
    val scope = rememberCoroutineScope()

    var callRequest by remember { mutableStateOf<VideoCallRequest?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isJoiningCall by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    // Load the call request details
    LaunchedEffect(callId) {
        isLoading = true
        try {
            val requests = repository.getVideoCallRequests()
            callRequest = requests.find { it.id == callId }
        } catch (e: Exception) {
            callRequest = null
        }
        isLoading = false
    }

    fun joinCall() {
        val request = callRequest ?: return
        scope.launch {
            isJoiningCall = true
            joinError = null
            try {
                val doctorId = sessionManager.getDoctorId()
                val patientId = request.patientId ?: request.patientName ?: callId
                val channelName = "call_$patientId"

                val tokenResponse = repository.getCallToken(channelName)
                if (tokenResponse != null) {
                    val appId = tokenResponse.resolvedAppId
                    println("[VideoCall] Token response → app_id='$appId' token.len=${tokenResponse.token.length}")

                    // Validate App ID before starting call
                    if (appId.isBlank()) {
                        println("[VideoCall] ERROR: resolvedAppId is blank. Cannot start call.")
                        joinError = "Video call setup failed: No Agora App ID received from server. Please contact support."
                        isJoiningCall = false
                        return@launch
                    }

                    val validAppIdPattern = Regex("^[0-9a-fA-F]{32}$")
                    if (!validAppIdPattern.matches(appId)) {
                        println("[VideoCall] ERROR: Invalid App ID format: '$appId' (len=${appId.length})")
                        joinError = "Video call setup failed: Invalid Agora App ID ('${appId.take(10)}...'). Please contact support."
                        isJoiningCall = false
                        return@launch
                    }

                    repository.initiateCall(doctorId, patientId, channelName)
                    onStartCall(appId, tokenResponse.token, channelName)
                } else {
                    joinError = "Failed to get call token. Please try again."
                }
            } catch (e: Exception) {
                joinError = "Error: ${e.message}"
            }
            isJoiningCall = false
        }
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
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .padding(32.dp),
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
                    Text(
                        callRequest?.patientName?.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = DoctorBlue
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(callRequest?.patientName ?: "Unknown", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Requested: ${callRequest?.timestamp ?: ""}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Call Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DoctorBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        DetailRow("Status", (callRequest?.status ?: "unknown").replaceFirstChar { it.uppercase() })
                        DetailRow("Patient ID", callRequest?.patientId ?: "")
                        DetailRow("Request Time", callRequest?.timestamp ?: "")
                    }
                }

                // Show error if token fetch failed
                if (joinError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(joinError!!, color = Color.Red, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Decline")
                    }
                    Button(
                        onClick = { joinCall() },
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
