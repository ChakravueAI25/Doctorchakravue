package com.org.doctorchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.Submission
import com.org.doctorchakravue.ui.theme.DoctorBlue
import com.org.doctorchakravue.ui.theme.DoctorGreen
import com.org.doctorchakravue.ui.theme.DoctorLightGray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// --- State ---
data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val doctorName: String = "",
    val urgentReviews: List<Submission> = emptyList(),
    val history: List<Submission> = emptyList(),
    val visionSubmissions: List<Submission> = emptyList()
    ,
    val videoCallRequests: List<com.org.doctorchakravue.model.VideoCallRequest> = emptyList()
)

// --- ViewModel ---
class DashboardViewModel(private val repository: ApiRepository) {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var autoRefreshJob: Job? = null

    companion object {
        private const val AUTO_REFRESH_INTERVAL = 30_000L
    }

    init {
        loadDashboard()
        startAutoRefresh()
    }

    private fun loadDashboard() {
        scope.launch {
            val doctorId = repository.getDoctorId()
            val name = repository.getDoctorName()

            if (doctorId.isNotEmpty()) {
                val urgent = repository.getUrgentSubmissions(doctorId)
                val hist = repository.getHistory(doctorId)
                val vision = repository.getVisionSubmissions(doctorId)
                val vrequests = repository.getVideoCallRequests()

                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        doctorName = name,
                        urgentReviews = urgent,
                        history = hist,
                        visionSubmissions = vision
                        ,
                        videoCallRequests = vrequests
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = scope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL)
                refreshData(silent = true)
            }
        }
    }

    fun refresh() {
        refreshData(silent = false)
    }

    private fun refreshData(silent: Boolean) {
        scope.launch {
            if (!silent) {
                _state.update { it.copy(isRefreshing = true) }
            }

            val doctorId = repository.getDoctorId()
            if (doctorId.isNotEmpty()) {
                val urgent = repository.getUrgentSubmissions(doctorId)
                val hist = repository.getHistory(doctorId)
                val vision = repository.getVisionSubmissions(doctorId)
                val vrequests = repository.getVideoCallRequests()

                _state.update {
                    it.copy(
                        isRefreshing = false,
                        urgentReviews = urgent,
                        history = hist,
                        visionSubmissions = vision
                        ,
                        videoCallRequests = vrequests
                    )
                }
            } else {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun logout() {
        stopAutoRefresh()
        repository.logout()
    }
}

// --- Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
    onNavigateToSubmissionDetail: (Submission) -> Unit = {},
    onNavigateToAdherence: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPainScaleHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToVideoCallList: () -> Unit = {}
) {
    val viewModel = remember { DashboardViewModel(ApiRepository()) }
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAutoRefresh()
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DoctorGreen)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // NON-SCROLLABLE HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hello,", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    Text("Dr. ${state.doctorName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DoctorBlue)
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Dr", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // SCROLLABLE CONTENT
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                ) {
                    //  SPACING
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    //  URGENT REVIEWS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Urgent Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "See all",
                            color = DoctorGreen,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onNavigateToPainScaleHistory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        if (state.urgentReviews.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(24.dp)
                            ) {
                                Text("No pending reviews.", color = Color.Gray)
                            }
                        } else {
                            state.urgentReviews.forEach { submission ->
                                UrgentReviewCard(submission) {
                                    onNavigateToSubmissionDetail(submission)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                //  VIDEO CALL REQUESTS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Video Call Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "See all",
                            color = DoctorGreen,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { onNavigateToVideoCallList() }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val vlist = state.videoCallRequests.take(3)  // Get top 3 latest requests

                    if (vlist.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.VideoCall,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color.Gray.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No pending call requests",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Display top 3 requests as cards
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            vlist.forEach { request ->
                                VideoCallRequestCard(request = request, onClick = { onNavigateToVideoCallList() })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                //  VISION TEST SUBMISSIONS
                item {
                    Text("Vision Test submissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val visionTests = state.visionSubmissions.take(2)
                if (visionTests.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(24.dp)
                        ) {
                            Text("No vision test submissions.", color = Color.Gray)
                        }
                    }
                } else {
                    items(visionTests) { submission ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToSubmissionDetail(submission) }
                        ) {
                            VisionTestSubmissionItem(submission)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

// --- Components ---
@Composable
private fun QuickActionItem(emoji: String, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(end = 20.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun UrgentReviewCard(submission: Submission, onClick: () -> Unit) {
    val imageUrl = "https://doctor.chakravue.co.in/files/${submission.imageId}"
    val painScale = submission.painScale ?: 0

    val painColor = when {
        painScale >= 7 -> Color(0xFFD32F2F)
        painScale >= 4 -> Color(0xFFF57C00)
        else -> DoctorGreen
    }

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .height(110.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = submission.patientName ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(painColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Pain: $painScale/10",
                        color = painColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(submission: Submission) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(DoctorLightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("👁", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = submission.patientName ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Pain Scale: ${submission.painScale ?: 0}/10",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Text(
            text = "View",
            style = MaterialTheme.typography.labelSmall,
            color = DoctorGreen
        )
    }
}

@Composable
private fun VisionTestSubmissionItem(submission: Submission) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE8D5F2)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                submission.patientName?.firstOrNull()?.uppercase() ?: "A",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6A4C93)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = submission.patientName ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = submission.formName ?: "Vision Test",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Text(
            text = "View",
            style = MaterialTheme.typography.labelSmall,
            color = DoctorGreen
        )
    }
}

@Composable
private fun VideoCallRequestCard(
    request: com.org.doctorchakravue.model.VideoCallRequest,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DoctorBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = DoctorGreen
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.patientName ?: "Unknown Patient",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = request.reason ?: "Video call requested",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = request.status ?: "pending",
                style = MaterialTheme.typography.labelSmall,
                color = DoctorGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
