package com.org.doctorchakravue.features.submissions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import com.org.doctorchakravue.core.ui.theme.DoctorBlue
import com.org.doctorchakravue.core.ui.theme.DoctorGreen
import com.org.doctorchakravue.data.DoctorRepository
import com.org.doctorchakravue.data.Submission

/**
 * PainScaleHistoryScreen - Shows full history of pain scale submissions.
 * Uses LazyVerticalGrid with 2 cards per row.
 * Replaces "Adherence" bottom nav destination for now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainScaleHistoryScreen(
    onBack: () -> Unit = {},
    onNavigateToSubmissionDetail: (Submission) -> Unit = {}
) {
    val repository = remember { DoctorRepository() }
    var submissions by remember { mutableStateOf<List<Submission>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val doctorId = repository.getDoctorId()
        submissions = repository.getHistory(doctorId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pain Scale History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DoctorGreen)
            }
        } else if (submissions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No submissions yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(submissions) { submission ->
                    PainScaleCard(
                        submission = submission,
                        onClick = { onNavigateToSubmissionDetail(submission) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PainScaleCard(
    submission: Submission,
    onClick: () -> Unit
) {
    val imageUrl = "https://doctor.chakravue.co.in/files/${submission.imageId}"
    val painScale = submission.painScale ?: 0

    val painColor = when {
        painScale >= 7 -> Color(0xFFD32F2F) // High pain - Red
        painScale >= 4 -> Color(0xFFF57C00) // Medium pain - Orange
        else -> DoctorGreen // Low pain - Green
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Eye Image
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )

            // Info section
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = submission.patientName ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = DoctorBlue
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp
                Text(
                    text = formatTimestamp(submission.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pain Scale Badge
                Box(
                    modifier = Modifier
                        .background(
                            painColor.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
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

private fun formatTimestamp(timestamp: String?): String {
    if (timestamp.isNullOrEmpty()) return ""
    // Simple formatting - just show date part
    return try {
        timestamp.substringBefore("T")
    } catch (e: Exception) {
        timestamp
    }
}
