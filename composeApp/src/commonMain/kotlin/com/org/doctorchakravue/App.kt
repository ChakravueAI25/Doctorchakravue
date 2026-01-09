package com.org.doctorchakravue

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.org.doctorchakravue.core.ui.theme.ChakravueTheme
import com.org.doctorchakravue.core.ui.surface.ChakravueSurface
import com.org.doctorchakravue.data.DoctorRepository
import com.org.doctorchakravue.data.Submission
import com.org.doctorchakravue.features.adherence.AdherenceScreen
import com.org.doctorchakravue.features.auth.LoginScreen
import com.org.doctorchakravue.features.call.DoctorCallScreen
import com.org.doctorchakravue.features.dashboard.DashboardScreen
import com.org.doctorchakravue.features.notifications.NotificationScreen
import com.org.doctorchakravue.features.submissions.SubmissionDetailScreen
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    ChakravueTheme {
        ChakravueSurface {
            val navController = rememberNavController()
            val repository = remember { DoctorRepository() }

            // Determine start destination based on login status
            val startDestination = remember {
                if (repository.isLoggedIn()) "dashboard" else "login"
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
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

                // 2. Dashboard
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
                            navController.navigate("adherence")
                        },
                        onNavigateToNotifications = {
                            navController.navigate("notifications")
                        }
                    )
                }

                // 3. Submission Detail
                composable("submission/{data}") { backStackEntry ->
                    val data = backStackEntry.arguments?.getString("data")?.replace("%2F", "/")
                    if (data != null) {
                        val submission = Json.decodeFromString<Submission>(data)
                        SubmissionDetailScreen(
                            submission = submission,
                            onBack = { navController.popBackStack() },
                            onNavigateToCall = { appId, token, channelName ->
                                // URL-encode the token to handle slashes and special characters
                                val safeToken = token.encodeURLPathPart()
                                navController.navigate("call/$appId/$safeToken/$channelName")
                            }
                        )
                    }
                }

                // 4. Video Call Screen
                composable("call/{appId}/{token}/{channelName}") { backStackEntry ->
                    val appId = backStackEntry.arguments?.getString("appId") ?: ""
                    // Navigation library auto-decodes URL-encoded strings
                    val token = backStackEntry.arguments?.getString("token") ?: ""
                    val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                    DoctorCallScreen(
                        appId = appId,
                        token = token,
                        channelName = channelName,
                        onEndCall = { navController.popBackStack() }
                    )
                }

                // 5. Adherence Screen
                composable("adherence") {
                    AdherenceScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // 6. Notifications Screen
                composable("notifications") {
                    NotificationScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

