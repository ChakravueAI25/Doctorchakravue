package com.org.doctorchakravue.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.org.doctorchakravue.data.ApiRepository
import com.org.doctorchakravue.model.AdherencePatient
import com.org.doctorchakravue.model.Submission
import com.org.doctorchakravue.ui.*
import com.org.doctorchakravue.ui.theme.BottomNavBar
import com.org.doctorchakravue.ui.theme.AppTheme
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AppTheme {
        val navController = rememberNavController()
        val repository = remember { ApiRepository() }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")

        val startDestination = remember {
            if (repository.isLoggedIn()) "dashboard" else "login"
        }

        val showBottomNav = Navigator.shouldShowBottomNav(currentRoute)

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomNav) {
                    BottomNavBar(
                        currentScreen = currentRoute ?: "dashboard",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo("dashboard") { saveState = true }
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
                    .padding(if (showBottomNav) padding else PaddingValues()),
                enterTransition = NavigationAnimations.enterTransition,
                exitTransition = NavigationAnimations.exitTransition,
                popEnterTransition = NavigationAnimations.popEnterTransition,
                popExitTransition = NavigationAnimations.popExitTransition
            ) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate("dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable("dashboard") {
                    DashboardScreen(
                        onNavigateToSubmissionDetail = { submission ->
                            val json = Json.encodeToString(submission)
                            val encodedJson = json.replace("/", "%2F")
                            navController.navigate("submission/$encodedJson")
                        },
                        onNavigateToAdherence = { patient ->
                            val json = Json.encodeToString(patient)
                            val encodedJson = json.replace("/", "%2F")
                            navController.navigate("adherence_detail/$encodedJson")
                        },
                        onNavigateToPainScaleHistory = { navController.navigate("pain_scale_history") },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToVideoCallList = { navController.navigate("video_call_list") },
                        onNavigateToSlitLamp = { navController.navigate("slit_lamp") }
                    )
                }

                composable("patients") { PatientsScreen() }

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

                composable("notifications") {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }

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

                composable("submission/{data}") { backStackEntry ->
                    val data = backStackEntry.arguments?.getString("data")?.replace("%2F", "/")
                    if (data != null) {
                        val submission = Json.decodeFromString<Submission>(data)
                        PainScaleDetailScreen(
                            submission = submission,
                            onBack = { navController.popBackStack() },
                            onNavigateToCall = { appId, token, channelName ->
                                val safeToken = token.encodeURLPathPart()
                                navController.navigate("call/$appId/$safeToken/$channelName")
                            }
                        )
                    }
                }

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

                composable("video_call_list") {
                    VideoCallListScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToDetail = { callId -> navController.navigate("video_call_detail/$callId") }
                    )
                }

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

                composable("adherence") {
                    AdherenceScreen(
                        onNavigateToDetail = { patient ->
                            val json = Json.encodeToString(patient)
                            val encodedJson = json.replace("/", "%2F")
                            navController.navigate("adherence_detail/$encodedJson")
                        }
                    )
                }

                composable("adherence_detail/{patientData}") { backStackEntry ->
                    val data = backStackEntry.arguments?.getString("patientData")?.replace("%2F", "/")

                    var patient: AdherencePatient? by remember { mutableStateOf(null) }
                    var hasError by remember { mutableStateOf(false) }

                    LaunchedEffect(data) {
                        if (data != null) {
                            try {
                                patient = Json.decodeFromString<AdherencePatient>(data)
                                hasError = patient?.patientName.isNullOrBlank() == true
                            } catch (e: Exception) {
                                hasError = true
                            }
                        }
                    }

                    if (hasError) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Error loading patient data",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { navController.popBackStack() }) {
                                    Text("Go Back")
                                }
                            }
                        }
                    } else if (patient != null) {
                        PatientAdherenceDetailScreen(
                            patient = patient!!,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable("slit_lamp") {
                    SlitLampScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
