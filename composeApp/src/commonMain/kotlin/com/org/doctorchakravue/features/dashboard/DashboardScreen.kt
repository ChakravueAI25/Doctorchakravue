package com.org.doctorchakravue.features.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.org.doctorchakravue.core.ui.theme.DoctorBlue
import com.org.doctorchakravue.core.ui.theme.DoctorGreen
import com.org.doctorchakravue.core.ui.theme.DoctorLightGray
import com.org.doctorchakravue.data.DoctorRepository
import com.org.doctorchakravue.features.dashboard.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
    onNavigateToSubmissionDetail: (com.org.doctorchakravue.data.Submission) -> Unit = {},
    onNavigateToAdherence: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPainScaleHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToVideoCallList: () -> Unit = {}
) {
    val viewModel = remember { DashboardViewModel(DoctorRepository()) }
    val state by viewModel.state.collectAsState()

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAutoRefresh()
        }
    }

    // No per-screen Scaffold with bottom nav - bottom nav is now centralized in App.kt
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DoctorGreen)
        }
    } else {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            ) {
                // 1. HEADER (Profile & Greeting)
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Good Morning,", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                            Text("Dr. ${state.doctorName}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        // Profile Pic (Click to go to ProfileScreen)
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
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 2. URGENT REVIEWS (Horizontal Scroll)
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
                                    .background(
                                        Color.White.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
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

                // 3. VIDEO CALL REQUESTS (Empty state ready)
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

                    // Empty state - backend not ready
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.85f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 4. QUICK ACTIONS (Horizontal Scroll)
                item {
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        QuickActionItem("📢", "Broadcast", Color(0xFFFF9800)) { onNavigateToNotifications() }
                        QuickActionItem("🔍", "Search", Color(0xFF2196F3)) { }
                        QuickActionItem("📷", "Slitlamp", Color(0xFF9C27B0)) { }
                        QuickActionItem("💊", "Adherence", Color(0xFF4CAF50)) { onNavigateToAdherence() }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 5. RECENT ACTIVITY HEADER
                item {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 6. ACTIVITY LIST (limited to most recent entries)
                val recentActivities = state.history.take(5)
                if (recentActivities.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(24.dp)
                        ) {
                            Text("No recent activity.", color = Color.Gray)
                        }
                    }
                } else {
                    items(recentActivities) { submission ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.85f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onNavigateToSubmissionDetail(submission) }
                        ) {
                            HistoryItem(submission)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Bottom Padding
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// Simple Quick Action using emoji instead of Material Icons
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
