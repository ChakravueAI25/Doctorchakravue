package com.org.doctorchakravue

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.org.doctorchakravue.app.Navigator
import com.org.doctorchakravue.core.ui.theme.AppTheme
import com.org.doctorchakravue.data.DoctorRepository
import com.org.doctorchakravue.data.Submission
import com.org.doctorchakravue.features.adherence.AdherenceScreen
import com.org.doctorchakravue.features.auth.LoginScreen
import com.org.doctorchakravue.features.call.DoctorCallScreen
import com.org.doctorchakravue.features.call.VideoCallDetailScreen
import com.org.doctorchakravue.features.call.VideoCallListScreen
import com.org.doctorchakravue.features.dashboard.DashboardScreen
import com.org.doctorchakravue.features.notifications.NotificationScreen
import com.org.doctorchakravue.features.patients.PatientListScreen
import com.org.doctorchakravue.features.profile.ProfileScreen
import com.org.doctorchakravue.features.submissions.PainScaleHistoryScreen
import com.org.doctorchakravue.features.submissions.SubmissionDetailScreen
import com.org.doctorchakravue.ui.components.BottomNavBar
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppTheme {
        val navController = rememberNavController()
        val repository = remember { DoctorRepository() }

        // Track current route for bottom nav visibility
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")

        // Determine start destination based on login status
        val startDestination = remember {
            if (repository.isLoggedIn()) "dashboard" else "login"
        }

        // Check if bottom nav should be shown
        val showBottomNav = Navigator.shouldShowBottomNav(currentRoute)

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomNav) {
                    BottomNavBar(
                        currentScreen = currentRoute ?: "dashboard",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                // Pop up to dashboard to avoid building up a large back stack
                                popUpTo("dashboard") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (showBottomNav) padding else androidx.compose.foundation.layout.PaddingValues())
            ) {
                // 1. Login Screen
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                // 2. Dashboard (Home)
                composable("dashboard") {
                    DashboardScreen(
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        },
                        onNavigateToSubmissionDetail = { submission ->
                            val json = Json.encodeToString(submission)
                            val encodedJson = json.replace("/", "%2F")
                            navController.navigate("submission/$encodedJson")
                        },
                        onNavigateToAdherence = {
                            // Now goes to Adherence screen (kept for quick action button)
                            navController.navigate("adherence")
                        },
                        onNavigateToNotifications = {
                            navController.navigate("notifications")
                        },
                        onNavigateToPainScaleHistory = {
                            navController.navigate("pain_scale_history")
                        },
                        onNavigateToProfile = {
                            navController.navigate("profile")
                        },
                        onNavigateToVideoCallList = {
                            navController.navigate("video_call_list")
                        }
                    )
                }

                // 3. Patients List
                composable("patients") {
                    PatientListScreen()
                }

                // 4. Pain Scale History (Replaces Adherence in bottom nav)
                composable("pain_scale_history") {
                    PainScaleHistoryScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToSubmissionDetail = { submission ->
                            val json = Json.encodeToString(submission)
                            val encodedJson = json.replace("/", "%2F")
                            navController.navigate("submission/$encodedJson")
                        }
                    )
                }

                // 5. Notifications
                composable("notifications") {
                    NotificationScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // 6. Profile Screen (Full screen, not dialog)
                composable("profile") {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    )
                }

                // 7. Submission Detail
                composable("submission/{data}") { backStackEntry ->
                    val data = backStackEntry.arguments?.getString("data")?.replace("%2F", "/")
                    if (data != null) {
                        val submission = Json.decodeFromString<Submission>(data)
                        SubmissionDetailScreen(
                            submission = submission,
                            onBack = { navController.popBackStack() },
                            onNavigateToCall = { appId, token, channelName ->
                                val safeToken = token.encodeURLPathPart()
                                navController.navigate("call/$appId/$safeToken/$channelName")
                            }
                        )
                    }
                }

                // 8. Video Call Screen (Active call)
                composable("call/{appId}/{token}/{channelName}") { backStackEntry ->
                    val appId = backStackEntry.arguments?.getString("appId") ?: ""
                    val token = backStackEntry.arguments?.getString("token") ?: ""
                    val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                    DoctorCallScreen(
                        appId = appId,
                        token = token,
                        channelName = channelName,
                        onEndCall = { navController.popBackStack() }
                    )
                }

                // 9. Video Call List (Empty state ready)
                composable("video_call_list") {
                    VideoCallListScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToDetail = { callId ->
                            navController.navigate("video_call_detail/$callId")
                        }
                    )
                }

                // 10. Video Call Detail (Empty state ready)
                composable("video_call_detail/{callId}") { backStackEntry ->
                    val callId = backStackEntry.arguments?.getString("callId") ?: ""
                    VideoCallDetailScreen(
                        callId = callId,
                        onBack = { navController.popBackStack() },
                        onStartCall = { appId, token, channelName ->
                            val safeToken = token.encodeURLPathPart()
                            navController.navigate("call/$appId/$safeToken/$channelName")
                        }
                    )
                }

                // 11. Adherence Screen (Accessed via Quick Action)
                composable("adherence") {
                    AdherenceScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
