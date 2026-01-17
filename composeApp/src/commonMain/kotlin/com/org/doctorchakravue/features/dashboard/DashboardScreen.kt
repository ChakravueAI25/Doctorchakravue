package com.org.doctorchakravue.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.org.doctorchakravue.core.ui.theme.DoctorLightGray
import com.org.doctorchakravue.data.DoctorRepository
import com.org.doctorchakravue.features.dashboard.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit = {},
    onNavigateToSubmissionDetail: (com.org.doctorchakravue.data.Submission) -> Unit = {},
    onNavigateToAdherence: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val viewModel = remember { DashboardViewModel(DoctorRepository()) }
    val state by viewModel.state.collectAsState()

    // Bottom Sheet State
    var showLogoutSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAutoRefresh()
        }
    }

    AppTheme {
        AppBackground {
            Scaffold(
                // BOTTOM NAV
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            icon = { Text("🏠") },
                            label = { Text("Home") },
                            selected = true,
                            onClick = { /* Stay here */ },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = DoctorGreen)
                        )
                        NavigationBarItem(
                            icon = { Text("📋") },
                            label = { Text("History") },
                            selected = false,
                            onClick = { /* TODO: Navigate to History */ }
                        )
                        NavigationBarItem(
                            icon = { Text("🔔") },
                            label = { Text("Notify") },
                            selected = false,
                            onClick = { onNavigateToNotifications() }
                        )
                    }
                }
            ) { padding ->
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DoctorGreen)
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize().padding(padding)
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
                                    // Profile Pic (Click to Logout)
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(DoctorBlue)
                                            .clickable { showLogoutSheet = true },
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
                                    Text("See all", color = DoctorGreen, style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    if (state.urgentReviews.isEmpty()) {
                                        Text("No pending reviews.", color = Color.Gray)
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

                            // 3. QUICK ACTIONS (Horizontal Scroll)
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

                            // 4. RECENT ACTIVITY HEADER
                            item {
                                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 5. ACTIVITY LIST
                            if (state.history.isEmpty()) {
                                item {
                                    Text("No recent activity.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                                }
                            } else {
                                items(state.history) { submission ->
                                    Box(
                                        modifier = Modifier.clickable {
                                            onNavigateToSubmissionDetail(submission)
                                        }
                                    ) {
                                        HistoryItem(submission)
                                    }
                                    HorizontalDivider(color = DoctorLightGray)
                                }
                            }

                            // Bottom Padding for Nav Bar
                            item { Spacer(modifier = Modifier.height(20.dp)) }
                        }
                    }
                }
            }

            // LOGOUT BOTTOM SHEET
            if (showLogoutSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showLogoutSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dr. ${state.doctorName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Do you want to log out?", color = Color.Gray)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.logout()
                                showLogoutSheet = false
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Logout", color = Color.Red)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
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
