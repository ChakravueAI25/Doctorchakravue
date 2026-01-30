package com.org.doctorchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.AdherencePatient
import com.org.doctorchakravue.model.Submission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ----------------------------------------------------
// STATE
// ----------------------------------------------------
data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val doctorName: String = "",
    val urgentReviews: List<Submission> = emptyList(),
    val history: List<Submission> = emptyList(),
    val visionSubmissions: List<Submission> = emptyList(),
    val videoCallRequests: List<com.org.doctorchakravue.model.VideoCallRequest> = emptyList(),
    val adherencePatients: List<AdherencePatient> = emptyList()
)

// ----------------------------------------------------
// VIEWMODEL
// ----------------------------------------------------
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
                val adherence = repository.getAdherenceList(doctorId)

                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        doctorName = name,
                        urgentReviews = urgent,
                        history = hist,
                        visionSubmissions = vision,
                        videoCallRequests = vrequests,
                        adherencePatients = adherence
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
                val adherence = repository.getAdherenceList(doctorId)

                _state.update {
                    it.copy(
                        isRefreshing = false,
                        urgentReviews = urgent,
                        history = hist,
                        visionSubmissions = vision,
                        videoCallRequests = vrequests,
                        adherencePatients = adherence
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
}

// ----------------------------------------------------
// SCREEN
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSubmissionDetail: (Submission) -> Unit = {},
    onNavigateToAdherence: (AdherencePatient) -> Unit = {},
    onNavigateToPainScaleHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToVideoCallList: () -> Unit = {},
    onNavigateToSlitLamp: () -> Unit = {}
) {
    val viewModel = remember { DashboardViewModel(ApiRepository()) }
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.stopAutoRefresh() }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF334671))
        }
        return
    }

    Column(Modifier.fillMaxSize()) {

        // ...existing code...
        // Replace header settings icon with camera icon for slitlamp
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Hello,  Dr. ${state.doctorName}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onNavigateToSlitLamp() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 18.sp)
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFC107))
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.doctorName.firstOrNull()?.uppercase() ?: "D",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
        // ...existing code...

        // ---------------- SCROLLABLE CONTENT ----------------
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // ---------- STATS CARDS ----------
                item {
                    val todayUrgentCount = state.urgentReviews.countTodaySubmissions()
                    val todayVideoCallCount = state.videoCallRequests.countTodayVideoCalls()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassStatCard(
                            line1 = "Today's",
                            line2 = "Consultations",
                            value = "0",
                            modifier = Modifier.weight(1f)
                        )
                        GlassStatCard(
                            line1 = "Urgent",
                            line2 = "Reviews",
                            value = "$todayUrgentCount",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToPainScaleHistory() }
                        )
                        GlassStatCard(
                            line1 = "Video call",
                            line2 = "Requests",
                            value = "$todayVideoCallCount",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToVideoCallList() }
                        )
                    }
                }

                // ---------- NEXT APPOINTMENTS ----------
                item {
                    SectionHeader("Your Next Appointments")
                    Spacer(Modifier.height(8.dp))
                    EmptyStateCard("No upcoming appointments")
                }

                // ---------- URGENT REVIEWS CAROUSEL ----------
                item {
                    SectionHeader(
                        title = "Urgent Reviews",
                        action = "View all",
                        onAction = onNavigateToPainScaleHistory
                    )
                    Spacer(Modifier.height(8.dp))
                    UrgentReviewCarousel(
                        items = state.urgentReviews.take(4),
                        onClick = onNavigateToSubmissionDetail
                    )
                }

                // ---------- DRUG ADHERENCE ----------
                item {
                    SectionHeader("Drug Adherence")
                    Spacer(Modifier.height(8.dp))
                    if (state.adherencePatients.isEmpty()) {
                        EmptyStateCard("No adherence data available")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.adherencePatients.take(4).forEach { patient ->
                                AdherenceSummaryCard(patient) {
                                    onNavigateToAdherence(patient)
                                }
                            }
                        }
                    }
                }

                // ---------- VIDEO CALL REQUEST SUBMISSIONS ----------
                item {
                    SectionHeader("Video Call Requests")
                    Spacer(Modifier.height(8.dp))
                    if (state.videoCallRequests.isEmpty()) {
                        EmptyStateCard("No video call requests")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.videoCallRequests.take(4).forEach { request ->
                                VideoCallRequestCard(request) {
                                    onNavigateToVideoCallList()
                                }
                            }
                        }
                    }
                }

                // ---------- VISION TEST SUBMISSIONS ----------
                item {
                    SectionHeader("Vision Test Submissions")
                    Spacer(Modifier.height(8.dp))
                    if (state.visionSubmissions.isEmpty()) {
                        EmptyStateCard("No vision test submissions")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.visionSubmissions.take(4).forEach { submission ->
                                VisionItem(submission) {
                                    onNavigateToSubmissionDetail(submission)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ----------------------------------------------------
// COMPONENTS
// ----------------------------------------------------

@Composable
private fun GlassStatCard(
    line1: String,
    line2: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(92.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.35f),
                RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it }
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    line1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    line2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFC107)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (action != null && onAction != null) {
            Text(
                action,
                color = Color(0xFF334671),
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

@Composable
private fun UrgentReviewCarousel(
    items: List<Submission>,
    onClick: (Submission) -> Unit
) {
    if (items.isEmpty()) {
        EmptyStateCard("No urgent reviews")
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { submission ->
            UrgentReviewCard(submission) { onClick(submission) }
        }
    }
}

@Composable
private fun UrgentReviewCard(submission: Submission, onClick: () -> Unit) {
    val imageUrl = "https://doctor.chakravue.co.in/files/${submission.imageId}"
    val painScale = submission.painScale ?: 0

    val painColor = when {
        painScale >= 7 -> Color(0xFFD32F2F)
        painScale >= 4 -> Color(0xFFF57C00)
        else -> Color(0xFF334671)
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.height(120.dp).fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(12.dp)) {
                Text(
                    submission.patientName ?: "Unknown",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
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
private fun AdherenceSummaryCard(patient: AdherencePatient, onClick: () -> Unit) {
    val isValidPatient = !patient.patientName.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isValidPatient) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isValidPatient) Color(0xFFF8F9FA) else Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    patient.patientName ?: "Unknown",
                    fontWeight = FontWeight.Bold,
                    color = if (isValidPatient) Color.Black else Color.Gray
                )
                Text(
                    "Last updated: ${formatTimestamp(patient.lastMedicationAt)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            val history = patient.medicationHistory ?: emptyList()
            val totalCount = history.size
            val takenCount = history.count { (it.taken ?: 0) == 1 }
            val adherenceRate = if (totalCount > 0) (takenCount.toFloat() / totalCount * 100).toInt() else 0
            val adherenceColor = when {
                adherenceRate >= 80 -> Color(0xFF334671)
                adherenceRate >= 50 -> Color(0xFFF57C00)
                else -> Color(0xFFD32F2F)
            }
            Text(
                "$adherenceRate%",
                color = if (isValidPatient) adherenceColor else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun VideoCallRequestCard(request: com.org.doctorchakravue.model.VideoCallRequest, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(request.patientName ?: "Unknown", fontWeight = FontWeight.Bold)
                Text(
                    "Requested: ${formatTimestamp(request.timestamp)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            val statusColor = when (request.status) {
                "completed" -> Color(0xFF334671)
                "pending" -> Color(0xFFF57C00)
                else -> Color(0xFF999999)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    request.status?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}


@Composable
private fun VisionItem(submission: Submission, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
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
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    submission.patientName ?: "Unknown",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    submission.formName ?: "Vision Test",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Text(
                "View",
                color = Color(0xFF334671),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyStateCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F2))
    ) {
        Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text, color = Color.Gray)
        }
    }
}


private fun formatTimestamp(iso: String?): String = iso?.take(10) ?: "Unknown"

/* ============================================================
   DATE FILTERING EXTENSION FUNCTIONS
   ============================================================ */

private fun List<Submission>.countTodaySubmissions(): Int {
    val todayDate = getTodayDateString()
    return this.count { submission ->
        val submissionDate = submission.timestamp?.take(10) ?: ""
        submissionDate == todayDate
    }
}

private fun List<com.org.doctorchakravue.model.VideoCallRequest>.countTodayVideoCalls(): Int {
    val todayDate = getTodayDateString()
    return this.count { request ->
        val requestDate = request.timestamp?.take(10) ?: ""
        requestDate == todayDate
    }
}

private fun getTodayDateString(): String {
    val currentMillis = com.org.doctorchakravue.platform.currentTimeMillis()
    val timeInSeconds = currentMillis / 1000L
    val daysSinceEpoch = timeInSeconds / 86400L

    var year = 1970
    var daysLeft = daysSinceEpoch

    // Calculate year
    while (true) {
        val daysInYear = if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 366 else 365
        if (daysLeft >= daysInYear) {
            daysLeft -= daysInYear
            year++
        } else {
            break
        }
    }

    // Calculate month and day
    val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
        daysInMonth[1] = 29
    }

    var month = 1
    var dayOfMonth = daysLeft.toInt() + 1

    for (i in daysInMonth.indices) {
        if (dayOfMonth <= daysInMonth[i]) {
            month = i + 1
            break
        }
        dayOfMonth -= daysInMonth[i]
    }

    val monthStr = if (month < 10) "0$month" else month.toString()
    val dayStr = if (dayOfMonth < 10) "0$dayOfMonth" else dayOfMonth.toString()

    return "$year-$monthStr-$dayStr"
}
